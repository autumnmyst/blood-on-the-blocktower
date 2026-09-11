package com.autumnwind.botb.daytime;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.networking.ClockHandsStateS2CPayload;
import com.autumnwind.botb.networking.PlaySoundS2CPayload;
import com.autumnwind.botb.states.ServerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import java.util.UUID;
import com.autumnwind.botb.networking.StateBroadcaster;
import com.autumnwind.botb.world.TeamManager;
import net.minecraft.server.permissions.Permissions;
import java.util.Optional;
import net.minecraft.world.scores.TeamColor;

/**
 * Manages nomination logic including validation, execution, and visual effects.
 */
public class NominationManager {

    /**
     * Validates if a nomination is legal.
     * @param nominator The player nominating
     * @param nominee The player being nominated
     * @param override If true, skip eligibility checks
     * @return true if nomination is valid
     */
    public static boolean validateNomination(UUID nominator, UUID nominee, boolean override) {
        if (nominator == null || nominee == null) {
            BloodOnTheBlocktower.LOGGER.debug("[Nomination] Validation failed: nominator or nominee is null");
            return false;
        }

        if (!override) {
            // Check eligibility
            boolean canNominate = DaytimeState.canNominate(nominator);
            boolean canBeNominated = DaytimeState.canBeNominated(nominee);
            boolean isTraveler = DaytimeState.isTraveler(nominee);

            if (!canNominate) {
                BloodOnTheBlocktower.LOGGER.debug("[Nomination] Validation failed: nominator {} cannot nominate (canNominate=false)", nominator);
                return false;
            }
            if (!canBeNominated) {
                BloodOnTheBlocktower.LOGGER.debug("[Nomination] Validation failed: nominee {} cannot be nominated (canBeNominated=false, isTraveler={})", nominee, isTraveler);
                return false;
            }
        }

        BloodOnTheBlocktower.LOGGER.debug("[Nomination] Validation passed for nominator {} -> nominee {} (override={})", nominator, nominee, override);
        return true;
    }

    /**
     * Executes a nomination: applies effects, updates state, sends messages.
     * @param server The server instance
     * @param nominator The nominating player's UUID
     * @param nominee The nominated player's UUID
     * @param alivePlayerCount Number of alive players
     */
    public static void executeNomination(MinecraftServer server, UUID nominator, UUID nominee, int alivePlayerCount) {
        // Clear any existing nomination first (removes glows, restores eligibility)
        if (DaytimeState.hasActiveNomination()) {
            resetNomination(server);
        }

        // Check if nominee is unseated (e.g., storyteller in Atheist script)
        // Unseated players don't have a seat number assigned
        boolean isUnseatedNominee = !ServerState.PLAYER_SEAT_NUMBERS.containsKey(nominee);

        // Update state
        DaytimeState.setCurrentNominator(nominator);
        DaytimeState.setCurrentNominee(nominee);

        // Initialize ElectionState for this nomination (voting phase starts later)
        // Note: organGrinderMode is not known yet - will be set when vote starts
        ElectionConfig config = ElectionConfig.forVote(false);
        ElectionState.beginElection(
                ElectionType.VOTE,
                config,
                nominee,
                nominator,
                ServerState.PLAYER_SEAT_NUMBERS
        );

        // Use one nomination from nominator's pool (Banshee players may have 2)
        DaytimeState.useNomination(nominator);
        // Only set canNominate to false if no nominations remaining
        if (!DaytimeState.hasNominationsRemaining(nominator)) {
            DaytimeState.setCanNominate(nominator, false);
        }
        if (!isUnseatedNominee) {
            // Only mark seated players as "can't be nominated again"
            DaytimeState.setCanBeNominated(nominee, false);
        } else {
            // Unseated nominee (e.g., storyteller in Atheist) - mark storyteller as nominated
            DaytimeState.setStorytellerCanBeNominated(false);
        }

        // Get nominee player - works for both seated and unseated players
        ServerPlayer nomineePlayer = server.getPlayerList().getPlayer(nominee);

        // Apply white glowing effect to nominee (for all nominees including unseated/storyteller)
        // All players stay on botb_player team for white glow - travelers only use botb_traveler during exile
        if (nomineePlayer != null) {
            Scoreboard scoreboard = server.getScoreboard();
            String playerName = nomineePlayer.getGameProfile().name();

            // Ensure player is on botb_player team for white glow color
            PlayerTeam playerTeam = scoreboard.getPlayerTeam(TeamManager.PLAYER_TEAM);
            if (playerTeam == null) {
                playerTeam = scoreboard.addPlayerTeam(TeamManager.PLAYER_TEAM);
                playerTeam.setColor(Optional.of(TeamColor.WHITE));
            }
            if (scoreboard.getPlayersTeam(playerName) != playerTeam) {
                scoreboard.addPlayerToTeam(playerName, playerTeam);
            }

            nomineePlayer.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    Integer.MAX_VALUE, // Infinite duration
                    0,
                    false,
                    false,
                    false // Don't show icon in HUD
            ));
        }

        // Get player names
        String nominatorName = Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
        String nomineeName = Component.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();

        ServerPlayer nominatorPlayer = server.getPlayerList().getPlayer(nominator);
        if (nominatorPlayer != null) {
            nominatorName = nominatorPlayer.getName().getString();
        }
        if (nomineePlayer != null) {
            nomineeName = nomineePlayer.getName().getString();
        }

        // Calculate votes required
        int votesRequired = (int) Math.ceil(alivePlayerCount / 2.0);

        // Build message
        Component titleText = Component.translatable("message.blood-on-the-blocktower.daytime.nominates",
                Component.literal(nominatorName).withStyle(style -> style.withColor(0x4FC3F7)), // Light blue (canNominate color)
                Component.literal(nomineeName).withStyle(style -> style.withColor(0xFF8C00))) // Orange (canBeNominated color)
                .withStyle(ChatFormatting.WHITE);

        Component subtitleText;
        int votesForTie = DaytimeState.getVotesForMarkedPlayer();
        if (votesForTie > 0) {
            votesRequired = votesForTie + 1;
        }
        if (DaytimeState.getMarkedForExecution() != null) {
            subtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.votes_to_tie_execute", votesForTie, votesRequired)
                    .withStyle(ChatFormatting.GRAY);
        } else {
            subtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.votes_required", votesRequired)
                    .withStyle(ChatFormatting.GRAY);
        }

        // Hidden subtitle for non-operators on OG days (light purple color)
        Component hiddenSubtitleText = Component.translatable("message.blood-on-the-blocktower.daytime.votes_required_hidden")
                .withStyle(style -> style.withColor(0xDA70D6)); // Light purple/orchid

        // Send title and chat message to all players
        boolean isOGDay = DaytimeState.isOrganGrinderModeActiveToday();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // On OG days, non-operators see hidden vote counts
            Component messageSubtitle = (isOGDay && !player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) ? hiddenSubtitleText : subtitleText;
            player.sendSystemMessage(titleText.copy().append(" ").append(messageSubtitle), false);

            // Send nomination sound
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(PlaySoundS2CPayload.NOMINATION));
        }

        // Broadcast clock hands state - show both hands pointing at nominator and nominee
        Vec3 nominatorPos = nominatorPlayer != null ? nominatorPlayer.position() : null;
        Vec3 nomineePos = nomineePlayer != null ? nomineePlayer.position() : null;
        StateBroadcaster.broadcastClockHandsState(
                server,
                ClockHandsStateS2CPayload.MODE_NOMINATION,
                nominatorPos,
                nomineePos,
                true,  // fadeIn
                true   // swivel
        );
    }

    /**
     * Clears the current nomination and removes glowing effects.
     * @param server The server instance
     */
    public static void resetNomination(MinecraftServer server) {
        UUID nominee = DaytimeState.getCurrentNominee();

        // Remove glowing effect from nominee
        // UNLESS they are the current MFE player (in which case they should keep the red glow)
        if (nominee != null) {
            UUID currentMFE = DaytimeState.getMarkedForExecution();
            boolean isMFE = nominee.equals(currentMFE);

            ServerPlayer nomineePlayer = server.getPlayerList().getPlayer(nominee);
            if (nomineePlayer != null && !isMFE) {
                // Only remove glow if they're NOT the MFE player
                nomineePlayer.removeEffect(MobEffects.GLOWING);

                // Ensure player stays on botb_player team (travelers only use botb_traveler during exile)
                Scoreboard scoreboard = server.getScoreboard();
                String playerName = nomineePlayer.getGameProfile().name();

                PlayerTeam playerTeam = scoreboard.getPlayerTeam(TeamManager.PLAYER_TEAM);
                if (playerTeam == null) {
                    playerTeam = scoreboard.addPlayerTeam(TeamManager.PLAYER_TEAM);
                    playerTeam.setColor(Optional.of(TeamColor.WHITE));
                }
                if (scoreboard.getPlayersTeam(playerName) != playerTeam) {
                    scoreboard.addPlayerToTeam(playerName, playerTeam);
                }
            }
            // If they ARE MFE, they keep their glow and botb_mfe team - no changes needed
        }

        // Clear state
        DaytimeState.resetNomination();

        // End election in ElectionState if this is a vote election
        if (ElectionState.hasActiveElection() && ElectionState.isVoteType()) {
            ElectionState.endElection();
        }

        // Hide clock hands
        StateBroadcaster.broadcastClockHandsState(
                server,
                ClockHandsStateS2CPayload.MODE_HIDDEN,
                null,
                null,
                false,  // fadeIn (will fade out instead)
                false   // swivel
        );
    }

    /**
     * Applies or removes the red glowing effect for MFE player.
     * Uses team colors to make the glow red.
     * @param server The server instance
     * @param player The player UUID
     * @param apply If true, apply the effect. If false, remove it
     */
    public static void updateMarkedGlow(MinecraftServer server, UUID player, boolean apply) {
        if (player == null) return;

        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player);
        if (serverPlayer == null) return;

        Scoreboard scoreboard = server.getScoreboard();
        String playerName = serverPlayer.getGameProfile().name();

        if (apply) {
            // Order matters: the glow takes its colour from the team the player is on when the
            // effect is applied, so it comes off first and goes back on last.
            serverPlayer.removeEffect(MobEffects.GLOWING);

            // Off whichever team they normally sit on.
            PlayerTeam playerTeam = scoreboard.getPlayerTeam(TeamManager.PLAYER_TEAM);
            if (playerTeam != null && scoreboard.getPlayersTeam(playerName) == playerTeam) {
                scoreboard.removePlayerFromTeam(playerName, playerTeam);
            }
            PlayerTeam travelerTeam = scoreboard.getPlayerTeam(TeamManager.TRAVELER_TEAM);
            if (travelerTeam != null && scoreboard.getPlayersTeam(playerName) == travelerTeam) {
                scoreboard.removePlayerFromTeam(playerName, travelerTeam);
            }

            // The red MFE team, created on first use. The colour is reasserted every time in
            // case something else changed it.
            PlayerTeam mfeTeam = scoreboard.getPlayerTeam(TeamManager.MFE_TEAM);
            if (mfeTeam == null) {
                mfeTeam = scoreboard.addPlayerTeam(TeamManager.MFE_TEAM);
            }
            mfeTeam.setColor(Optional.of(TeamColor.RED));

            // Verify and retry once if the scoreboard didn't take it.
            if (!scoreboard.addPlayerToTeam(playerName, mfeTeam) || scoreboard.getPlayersTeam(playerName) != mfeTeam) {
                scoreboard.addPlayerToTeam(playerName, mfeTeam);
                if (scoreboard.getPlayersTeam(playerName) != mfeTeam) {
                    BloodOnTheBlocktower.LOGGER.warn("Failed to assign {} to botb_mfe team after retry", playerName);
                }
            }

            // Red, because they're on the red team now.
            serverPlayer.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    false // Don't show icon in HUD
            ));
        } else {
            // Remove from MFE team if player is on it
            PlayerTeam mfeTeam = scoreboard.getPlayerTeam(TeamManager.MFE_TEAM);
            if (mfeTeam != null && scoreboard.getPlayersTeam(playerName) == mfeTeam) {
                scoreboard.removePlayerFromTeam(playerName, mfeTeam);
            }

            // Remove glowing effect
            serverPlayer.removeEffect(MobEffects.GLOWING);

            // Add player back to botb_player team (travelers only use botb_traveler during exile)
            PlayerTeam playerTeam = scoreboard.getPlayerTeam(TeamManager.PLAYER_TEAM);
            if (playerTeam == null) {
                playerTeam = scoreboard.addPlayerTeam(TeamManager.PLAYER_TEAM);
                playerTeam.setColor(Optional.of(TeamColor.WHITE));
            }
            scoreboard.addPlayerToTeam(playerName, playerTeam);
        }
    }
}
