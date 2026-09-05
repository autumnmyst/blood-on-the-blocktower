package com.autumnwind.botb.event;

import com.autumnwind.botb.config.WhisperSettingsManager;
import com.autumnwind.botb.daytime.DaytimeState;
import com.autumnwind.botb.daytime.VotingManager;
import com.autumnwind.botb.networking.SyncWhisperSettingsS2CPayload;
import com.autumnwind.botb.setup.SetupStick;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.timer.TimerManager;
import com.autumnwind.botb.util.CustomNames;
import java.util.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import com.autumnwind.botb.networking.StateBroadcaster;
import com.autumnwind.botb.util.ServerCommands;
import com.autumnwind.botb.world.TeamManager;

/**
 * Player and block event hooks: join and disconnect bookkeeping, re-applying death
 * invisibility on respawn, and routing block clicks to the setup stick and vote levers.
 */
public final class PlayerEvents {

    private PlayerEvents() {}

    public static void register() {
        // Register player join/leave events for timer boss bar and team assignment
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            TimerManager.addPlayer(handler.getPlayer());

            // Re-applies the correct scoreboard team based on the joining player's UUID:
            //   - If they're the current MFE → botb_mfe (preserves red glow on rejoin)
            //   - Else if they're the current active-exile target → botb_traveler (preserves purple glow)
            //   - Else → botb_player (default, for nametag hiding)
            // Falls back to botb_player when the targeted team object doesn't exist on the scoreboard yet.
            Scoreboard scoreboard = server.getScoreboard();
            Team playerTeam = scoreboard.getTeam(TeamManager.PLAYER_TEAM);
            if (playerTeam == null) {
                playerTeam = scoreboard.addTeam(TeamManager.PLAYER_TEAM);
                playerTeam.setColor(Formatting.WHITE);
            }

            UUID joinUuid = handler.getPlayer().getUuid();
            Team target = playerTeam;
            if (joinUuid.equals(DaytimeState.getMarkedForExecution())) {
                Team mfeTeam = scoreboard.getTeam(TeamManager.MFE_TEAM);
                if (mfeTeam != null) target = mfeTeam;
            } else if (DaytimeState.hasActiveExile()
                    && joinUuid.equals(DaytimeState.getCurrentExileTarget())) {
                Team travelerTeam = scoreboard.getTeam(TeamManager.TRAVELER_TEAM);
                if (travelerTeam != null) target = travelerTeam;
            }

            String name = handler.getPlayer().getGameProfile().getName();
            if (scoreboard.getScoreHolderTeam(name) != target) {
                scoreboard.addScoreHolderToTeam(name, target);
            }

            // Custom names (/botb setName) live in CustomNames, which the name mixins read, so
            // the joining player's saved name already applies. Give the client the full map.
            StateBroadcaster.sendCustomNamesTo(handler.getPlayer());

            // Send current whisper rules so the joining client's read-only player view
            // (and any storyteller's editable view) starts in sync with the server.
            ServerPlayNetworking.send(handler.getPlayer(),
                    new SyncWhisperSettingsS2CPayload(
                            WhisperSettingsManager.get()));

            // Catch them up on a game already in progress. Sends nothing if none is.
            StateBroadcaster.sendCurrentStateTo(server, handler.getPlayer());

            // Current lobby counts for the pre-game grimoire
            StateBroadcaster.sendLobbyCountsTo(handler.getPlayer());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            TimerManager.removePlayer(handler.getPlayer());
        });

        // Register player respawn event to reapply invisibility
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // Reapply invisibility if player is still dead in game
            UUID playerUuid = newPlayer.getUuid();
            boolean isDead = ServerState.PLAYER_DEATH_STATUS.getOrDefault(playerUuid, false);
            if (isDead && !ServerState.gameEnded) {
                String invisibilityCommand = "effect give @s invisibility infinite 0 true";
                ServerCommands.runAs(newPlayer.getServer(), playerUuid.toString(), invisibilityCommand);
            }
        });

        // Register block use event for the setup stick and lever tracking
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ActionResult stickResult = SetupStick.onUseBlock(player, world, hand, hitResult);
            if (stickResult != ActionResult.PASS) {
                return stickResult;
            }
            BlockPos pos = hitResult.getBlockPos();
            return VotingManager.onBlockUse(world, pos, world.getBlockState(pos));
        });

        // Setup stick: left-click sets a position, right-click (block or air) goes back
        AttackBlockCallback.EVENT.register(SetupStick::onAttackBlock);
        UseItemCallback.EVENT.register(SetupStick::onUseItem);
    }
}
