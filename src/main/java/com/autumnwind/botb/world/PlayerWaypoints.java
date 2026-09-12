package com.autumnwind.botb.world;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.daytime.DaytimeState;
import com.autumnwind.botb.states.ServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.waypoints.Waypoint;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Locator bar: players see each other and their seat by day, only their house by night, never storytellers. */
public final class PlayerWaypoints {

    private static final int SEAT_COLOR = 0xFFAA00;
    private static final int HOUSE_COLOR = 0x55FFFF;
    private static final int DEAD_COLOR = 0x555555;

    private static final Map<UUID, Set<UUID>> shown = new HashMap<>();
    private static Object lastVisibility = null;

    private PlayerWaypoints() {}

    public static boolean isNight() {
        return !ServerState.gameEnded && ServerState.currentNight > ServerState.currentDay;
    }

    private static boolean isElection() {
        return !ServerState.gameEnded
                && (DaytimeState.areNominationsOpen() || DaytimeState.getCurrentNominee() != null || DaytimeState.hasActiveExile());
    }

    /** Seated players stay on the bar while invisible, which vanilla would hide. */
    public static boolean alwaysTransmits(ServerPlayer player) {
        return ServerState.PLAYER_SEAT_NUMBERS.containsKey(player.getUUID()) && !isStoryteller(player);
    }

    // invisibility zeroes the transmit range, so judge by the receiver's range alone
    public static boolean outOfRange(ServerPlayer source, ServerPlayer receiver) {
        if (receiver.isSpectator()) return false;
        if (source.isSpectator() || source.hasIndirectPassenger(receiver)) return true;
        return source.distanceTo(receiver) >= receiver.getAttributeValue(Attributes.WAYPOINT_RECEIVE_RANGE);
    }

    public static boolean isStoryteller(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean hides(ServerPlayer source, ServerPlayer receiver) {
        if (isStoryteller(receiver)) return false;
        if (isStoryteller(source) || isNight()) return true;
        return isElection() && !onTrial(source.level().getServer()).contains(source.getUUID());
    }

    public static void refresh(MinecraftServer server) {
        Set<UUID> storytellers = new HashSet<>();
        Map<UUID, String> teams = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isStoryteller(player)) storytellers.add(player.getUUID());
            teams.put(player.getUUID(), player.getTeam() == null ? "" : player.getTeam().getName());
        }
        Object visibility = List.of(isNight(), isElection(), onTrial(server), storytellers, teams);
        boolean changed = !visibility.equals(lastVisibility);
        lastVisibility = visibility;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncIcon(player);
            if (changed) reconnectPlayers(player);
            sendMarkers(player);
        }
    }

    public static void refresh(ServerPlayer player) {
        syncIcon(player);
        reconnectPlayers(player);
        sendMarkers(player);
    }

    /** Vanilla untracks a player whose transmit range hits zero, so put them back and recolour. */
    public static void transmitRangeChanged(ServerPlayer player) {
        syncIcon(player);
        retrack(player);
    }

    // grey only for dead players on the plain team, so MFE red and reveal colours still show
    private static void syncIcon(ServerPlayer player) {
        boolean dead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(player.getUUID(), false);
        boolean plainTeam = player.getTeam() == null || TeamManager.PLAYER_TEAM.equals(player.getTeam().getName());
        Optional<Integer> color = dead && plainTeam ? Optional.of(DEAD_COLOR) : Optional.empty();
        if (!player.waypointIcon().color.equals(color)) {
            player.waypointIcon().color = color;
            retrack(player);
        }
    }

    private static void retrack(ServerPlayer player) {
        ServerWaypointManager manager = player.level().getWaypointManager();
        manager.untrackWaypoint(player);
        if (player.isTransmittingWaypoint()) manager.trackWaypoint(player);
    }

    // the nominee, the exile target, and whoever wears the public MFE glow
    private static Set<UUID> onTrial(MinecraftServer server) {
        Set<UUID> trial = new HashSet<>();
        if (DaytimeState.getCurrentNominee() != null) trial.add(DaytimeState.getCurrentNominee());
        if (DaytimeState.getCurrentExileTarget() != null) trial.add(DaytimeState.getCurrentExileTarget());
        PlayerTeam mfeTeam = server.getScoreboard().getPlayerTeam(TeamManager.MFE_TEAM);
        if (mfeTeam != null) {
            for (String name : mfeTeam.getPlayers()) {
                ServerPlayer player = server.getPlayerList().getPlayerByName(name);
                if (player != null) trial.add(player.getUUID());
            }
        }
        return trial;
    }

    public static void forget(ServerPlayer player) {
        shown.remove(player.getUUID());
    }

    // vanilla only re-checks a connection once it breaks
    private static void reconnectPlayers(ServerPlayer receiver) {
        ServerWaypointManager manager = receiver.level().getWaypointManager();
        for (ServerPlayer source : receiver.level().players()) {
            if (source == receiver || !source.isTransmittingWaypoint()) continue;
            manager.untrackWaypoint(source);
            manager.trackWaypoint(source);
        }
    }

    private static void sendMarkers(ServerPlayer player) {
        Map<UUID, ClientboundTrackedWaypointPacket> wanted = new HashMap<>();
        Integer seat = ServerState.PLAYER_SEAT_NUMBERS.get(player.getUUID());
        if (seat != null && !isStoryteller(player)) {
            BlockPos pos = isNight() ? ServerConfig.SEAT_HOMES.get(seat) : ServerConfig.TOWN_SQUARE_SEATS.get(seat);
            if (pos != null) {
                UUID id = markerId(isNight() ? "house" : "seat", seat);
                // head height, so the bar stops pointing down while standing on the spot
                wanted.put(id, ClientboundTrackedWaypointPacket.addWaypointPosition(id, icon(isNight() ? HOUSE_COLOR : SEAT_COLOR), pos.above()));
            }
        }

        Set<UUID> current = shown.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
        for (UUID stale : new HashSet<>(current)) {
            if (!wanted.containsKey(stale)) {
                player.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(stale));
                current.remove(stale);
            }
        }
        for (Map.Entry<UUID, ClientboundTrackedWaypointPacket> entry : wanted.entrySet()) {
            if (current.add(entry.getKey())) {
                player.connection.send(entry.getValue());
            }
        }
    }

    private static UUID markerId(String kind, int seat) {
        return UUID.nameUUIDFromBytes(("botb:" + kind + ":" + seat).getBytes(StandardCharsets.UTF_8));
    }

    private static Waypoint.Icon icon(int color) {
        Waypoint.Icon icon = new Waypoint.Icon();
        icon.color = Optional.of(color);
        return icon;
    }
}
