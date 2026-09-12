package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.config.WhisperSettingsManager;
import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.util.CustomNames;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.RoleType;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.permissions.Permissions;
import com.autumnwind.botb.world.PlayerWaypoints;

/** Sends game state to clients: full catch-up on join, and broadcasts when daytime, day/night, clock hands, whisper settings, custom names, or lobby counts change. */
public final class StateBroadcaster {

    private StateBroadcaster() {}

    /** Send the active whisper settings to every connected client. */
    public static void broadcastWhisperSettings(MinecraftServer server) {
        SyncWhisperSettingsS2CPayload pkt = new SyncWhisperSettingsS2CPayload(
                WhisperSettingsManager.get());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, pkt);
        }
    }

    /**
     * Brings a joining or rejoining player up to date with a game already in progress.
     *
     * <p>The rest of the mod only pushes state when it <em>changes</em>, so without this a
     * reconnecting client sits blank (no role, no seats, no nomination) until the next
     * nomination or death happens to broadcast one. Every piece is guarded, so joining a server
     * with no game running sends nothing.
     *
     * <p>Fast-updating state is deliberately left out: vote progress and the timer both
     * re-broadcast on their own timers within ~100ms of joining.
     */
    public static void sendCurrentStateTo(MinecraftServer server, ServerPlayer player) {
        // Their own role, silently, because a rejoin must not replay the reveal animation and sound.
        // This is the doctored assignment the storyteller last sent, so it re-tells the same
        // story: a Drunk stays their Townsfolk, and under Tor the living stay NO_ROLE.
        PendingRoleAssignment assignment = ServerState.PLAYER_ROLES.get(player.getUUID());
        if (assignment != null) {
            int travelerCount = (int) ServerState.PLAYER_ROLES.values().stream()
                    .map(a -> ServerState.currentScript != null ? a.resolveCustomRole(ServerState.currentScript) : a)
                    .filter(a -> a.getRoleType() == RoleType.TRAVELER)
                    .count();
            ServerPlayNetworking.send(player, SendRoleS2CPayload.ofAssignment(
                    assignment, ServerState.PLAYER_ROLES.size(), travelerCount, true));
        }

        if (!ServerState.PLAYER_SEAT_NUMBERS.isEmpty()) {
            ServerPlayNetworking.send(player, new SendSeatsS2CPayload(new HashMap<>(ServerState.PLAYER_SEAT_NUMBERS)));
        }

        if (!ServerState.PLAYER_DEATH_STATUS.isEmpty()) {
            ServerPlayNetworking.send(player, new SendDeathStatusS2CPayload(new HashMap<>(ServerState.PLAYER_DEATH_STATUS)));
        }

        if (ServerState.currentNight > 0 || ServerState.currentDay > 0) {
            ServerPlayNetworking.send(player, new SyncDayNightS2CPayload(
                    ServerState.currentNight, ServerState.currentDay, ServerState.executionToday));
        }

        // Nomination eligibility, the current nominee, marks and exile state. Sent whenever a
        // day is under way, since "nobody may nominate yet" is itself state worth having.
        if (DaytimeState.areNominationsOpen() || DaytimeState.hasActiveNomination() || DaytimeState.hasActiveExile()) {
            ServerPlayNetworking.send(player, buildDaytimeStatePayload());

            if (!DaytimeState.getLeverStates().isEmpty()) {
                ServerPlayNetworking.send(player, new LeverStateUpdateS2CPayload(DaytimeState.getLeverStates(),
                        DaytimeState.getBansheeDoubleVotePlayers(), DaytimeState.getBansheeDoubleVoteActivePlayers()));
            }
        }

        sendClockHandsTo(server, player);
    }

    /**
     * Points the joining player's clock hands at whatever is currently happening. Hand targets are
     * live player positions rather than stored state, so they're looked up fresh. Fade-in and
     * swivel are off, since the animation belongs to the moment the nomination was made.
     */
    public static void sendClockHandsTo(MinecraftServer server, ServerPlayer player) {
        int mode;
        UUID hourHandPlayer = null;
        UUID minuteHandPlayer = null;

        if (DaytimeState.hasActiveExile()) {
            mode = ClockHandsStateS2CPayload.MODE_EXILE;
            minuteHandPlayer = DaytimeState.getCurrentExileTarget();
        } else if (DaytimeState.hasActiveNomination()) {
            minuteHandPlayer = DaytimeState.getCurrentNominee();
            if (DaytimeState.isVoteInProgress()) {
                mode = ClockHandsStateS2CPayload.MODE_VOTING;
            } else {
                mode = ClockHandsStateS2CPayload.MODE_NOMINATION;
                hourHandPlayer = DaytimeState.getCurrentNominator();
            }
        } else {
            return; // Nothing to point at
        }

        ServerPlayNetworking.send(player, new ClockHandsStateS2CPayload(
                mode,
                ServerConfig.CLOCK_CENTER,
                ServerConfig.CLOCK_HAND_SCALE,
                positionOf(server, player, hourHandPlayer),
                positionOf(server, player, minuteHandPlayer),
                false,
                false
        ));
    }

    /**
     * Where a hand should point. {@code receiver} is checked before the player list because a
     * player is not registered there yet while their own join is being handled. Without it, a
     * reconnecting nominee is sent a null target and never sees the hand aimed at themselves.
     */
    public static Vec3 positionOf(MinecraftServer server, ServerPlayer receiver, UUID uuid) {
        if (uuid == null) return null;
        if (uuid.equals(receiver.getUUID())) return receiver.position();
        ServerPlayer target = server.getPlayerList().getPlayer(uuid);
        return target != null ? target.position() : null;
    }

    /**
     * Helper method to broadcast daytime state to all players.
     */
    public static void broadcastDaytimeState(MinecraftServer server) {
        SyncDaytimeStateS2CPayload payload = buildDaytimeStatePayload();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /** The current daytime snapshot, for broadcasting or for catching one player up. */
    public static SyncDaytimeStateS2CPayload buildDaytimeStatePayload() {
        return new SyncDaytimeStateS2CPayload(
                DaytimeState.getCanNominateMap(),
                DaytimeState.getCanBeNominatedMap(),
                DaytimeState.getGhostVoteMap(),
                DaytimeState.getNominationsRemainingMap(),
                DaytimeState.getCurrentNominator(),
                DaytimeState.getCurrentNominee(),
                DaytimeState.getMarkedForExecution(),
                DaytimeState.getVotesForMarkedPlayer(),
                DaytimeState.areNominationsOpen(),
                DaytimeState.isOrganGrinderModeActiveToday(),
                DaytimeState.getStorytellerMFE(),
                DaytimeState.getStorytellerMFEVotes(),
                DaytimeState.canStorytellerBeNominated(),
                // Exile state
                DaytimeState.getCanBeExiledMap(),
                DaytimeState.getCurrentExileCaller(),
                DaytimeState.getCurrentExileTarget(),
                DaytimeState.isExileSupportInProgress(),
                DaytimeState.getLockedExileSupportCount(),
                // Voudon state
                DaytimeState.isVoudonModeActive(),
                DaytimeState.getVoudonPlayerUuid()
        );
    }

    public static void broadcastDayNightState(MinecraftServer server) {
        SyncDayNightS2CPayload payload = new SyncDayNightS2CPayload(
                ServerState.currentNight,
                ServerState.currentDay,
                ServerState.executionToday
        );

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
        PlayerWaypoints.refresh(server);
    }

    /**
     * Broadcasts clock hands state to all players.
     * @param server The server instance
     * @param mode The clock hands mode (HIDDEN=0, NOMINATION=1, VOTING=2)
     * @param hourHandTargetPos Position for hour hand to point at (null if hidden)
     * @param minuteHandTargetPos Position for minute hand to point at (null if hidden)
     * @param fadeIn True if hands should fade in
     * @param swivel True if hands should do swivel animation (nomination only)
     */
    public static void broadcastClockHandsState(
            MinecraftServer server,
            int mode,
            Vec3 hourHandTargetPos,
            Vec3 minuteHandTargetPos,
            boolean fadeIn,
            boolean swivel
    ) {
        ClockHandsStateS2CPayload payload = new ClockHandsStateS2CPayload(
                mode,
                ServerConfig.CLOCK_CENTER,
                ServerConfig.CLOCK_HAND_SCALE,
                hourHandTargetPos,
                minuteHandTargetPos,
                fadeIn,
                swivel
        );

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static int lastLobbyPlayers = -1;

    private static int lastLobbyStorytellers = -1;

    public static LobbyCountsS2CPayload computeLobbyCounts(MinecraftServer server) {
        List<ServerPlayer> online = server.getPlayerList().getPlayers();
        int storytellers = 0;
        for (ServerPlayer p : online) {
            if (p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) storytellers++;
        }
        return new LobbyCountsS2CPayload(online.size() - storytellers, storytellers);
    }

    /** Sends every custom player name to one client (on join). */
    public static void sendCustomNamesTo(ServerPlayer player) {
        ServerPlayNetworking.send(player, new CustomNamesS2CPayload(new HashMap<>(CustomNames.all())));
    }

    /**
     * Broadcasts the custom names after a change and refreshes the changed player's tab-list
     * entry, which vanilla only re-sends when told to.
     */
    public static void syncCustomNames(MinecraftServer server, ServerPlayer changed) {
        CustomNamesS2CPayload payload = new CustomNamesS2CPayload(new HashMap<>(CustomNames.all()));
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(online, payload);
        }
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, changed));
    }

    /** Sends current lobby counts to one player (used on join). */
    public static void sendLobbyCountsTo(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        ServerPlayNetworking.send(player, computeLobbyCounts(server));
    }

    /** Broadcasts lobby counts to everyone when they changed since the last broadcast. */
    public static void syncLobbyCounts(MinecraftServer server) {
        LobbyCountsS2CPayload counts = computeLobbyCounts(server);
        if (counts.players() == lastLobbyPlayers && counts.storytellers() == lastLobbyStorytellers) return;
        lastLobbyPlayers = counts.players();
        lastLobbyStorytellers = counts.storytellers();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, counts);
        }
    }
}
