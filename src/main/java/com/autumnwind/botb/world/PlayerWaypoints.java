package com.autumnwind.botb.world;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.states.ServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.world.waypoints.Waypoint;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Locator bar: players see each other and their seat by day, only their house by night, never storytellers. */
public final class PlayerWaypoints {

    private static final int SEAT_COLOR = 0xFFAA00;
    private static final int HOUSE_COLOR = 0x55FFFF;

    private static final Map<UUID, Set<UUID>> shown = new HashMap<>();

    private PlayerWaypoints() {}

    public static boolean isNight() {
        return ServerState.currentNight > ServerState.currentDay;
    }

    public static boolean isStoryteller(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean hides(ServerPlayer source, ServerPlayer receiver) {
        if (isStoryteller(receiver)) return false;
        return isStoryteller(source) || isNight();
    }

    public static void refresh(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refresh(player);
        }
    }

    public static void refresh(ServerPlayer player) {
        reconnectPlayers(player);
        sendMarkers(player);
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
                wanted.put(id, ClientboundTrackedWaypointPacket.addWaypointPosition(id, icon(isNight() ? HOUSE_COLOR : SEAT_COLOR), pos));
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
