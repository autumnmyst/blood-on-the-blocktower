package com.autumnwind.botb.daytime;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.networking.ClockHandsStateS2CPayload;
import com.autumnwind.botb.networking.PlaySoundS2CPayload;
import com.autumnwind.botb.states.ServerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;
import com.autumnwind.botb.networking.StateBroadcaster;
import com.autumnwind.botb.world.TeamManager;

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
        ServerPlayerEntity nomineePlayer = server.getPlayerManager().getPlayer(nominee);

        // Apply white glowing effect to nominee (for all nominees including unseated/storyteller)
        // All players stay on botb_player team for white glow - travelers only use botb_traveler during exile
        if (nomineePlayer != null) {
            Scoreboard scoreboard = server.getScoreboard();
            String playerName = nomineePlayer.getGameProfile().getName();

            // Ensure player is on botb_player team for white glow color
            Team playerTeam = scoreboard.getTeam(TeamManager.PLAYER_TEAM);
            if (playerTeam == null) {
                playerTeam = scoreboard.addTeam(TeamManager.PLAYER_TEAM);
                playerTeam.setColor(Formatting.WHITE);
            }
            if (scoreboard.getScoreHolderTeam(playerName) != playerTeam) {
                scoreboard.addScoreHolderToTeam(playerName, playerTeam);
            }

            nomineePlayer.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.GLOWING,
                    Integer.MAX_VALUE, // Infinite duration
                    0,
                    false,
                    false,
                    false // Don't show icon in HUD
            ));
        }

        // Get player names
        String nominatorName = Text.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();
        String nomineeName = Text.translatable("gui.blood-on-the-blocktower.common.unknown_player").getString();

        ServerPlayerEntity nominatorPlayer = server.getPlayerManager().getPlayer(nominator);
        if (nominatorPlayer != null) {
            nominatorName = nominatorPlayer.getName().getString();
        }
        if (nomineePlayer != null) {
            nomineeName = nomineePlayer.getName().getString();
        }

        // Calculate votes required
        int votesRequired = (int) Math.ceil(alivePlayerCount / 2.0);

        // Build message
        Text titleText = Text.translatable("message.blood-on-the-blocktower.daytime.nominates",
                Text.literal(nominatorName).styled(style -> style.withColor(0x4FC3F7)), // Light blue (canNominate color)
                Text.literal(nomineeName).styled(style -> style.withColor(0xFF8C00))) // Orange (canBeNominated color)
                .formatted(Formatting.WHITE);

        Text subtitleText;
        int votesForTie = DaytimeState.getVotesForMarkedPlayer();
        if (votesForTie > 0) {
            votesRequired = votesForTie + 1;
        }
        if (DaytimeState.getMarkedForExecution() != null) {
            subtitleText = Text.translatable("message.blood-on-the-blocktower.daytime.votes_to_tie_execute", votesForTie, votesRequired)
                    .formatted(Formatting.GRAY);
        } else {
            subtitleText = Text.translatable("message.blood-on-the-blocktower.daytime.votes_required", votesRequired)
                    .formatted(Formatting.GRAY);
        }

        // Hidden subtitle for non-operators on OG days (light purple color)
        Text hiddenSubtitleText = Text.translatable("message.blood-on-the-blocktower.daytime.votes_required_hidden")
                .styled(style -> style.withColor(0xDA70D6)); // Light purple/orchid

        // Send title and chat message to all players
        boolean isOGDay = DaytimeState.isOrganGrinderModeActiveToday();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // On OG days, non-operators see hidden vote counts
            Text messageSubtitle = (isOGDay && !player.hasPermissionLevel(2)) ? hiddenSubtitleText : subtitleText;
            player.sendMessage(titleText.copy().append(" ").append(messageSubtitle), false);

            // Send nomination sound
            ServerPlayNetworking.send(player, new PlaySoundS2CPayload(PlaySoundS2CPayload.NOMINATION));
        }

        // Broadcast clock hands state - show both hands pointing at nominator and nominee
        Vec3d nominatorPos = nominatorPlayer != null ? nominatorPlayer.getPos() : null;
        Vec3d nomineePos = nomineePlayer != null ? nomineePlayer.getPos() : null;
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

            ServerPlayerEntity nomineePlayer = server.getPlayerManager().getPlayer(nominee);
            if (nomineePlayer != null && !isMFE) {
                // Only remove glow if they're NOT the MFE player
                nomineePlayer.removeStatusEffect(StatusEffects.GLOWING);

                // Ensure player stays on botb_player team (travelers only use botb_traveler during exile)
                Scoreboard scoreboard = server.getScoreboard();
                String playerName = nomineePlayer.getGameProfile().getName();

                Team playerTeam = scoreboard.getTeam(TeamManager.PLAYER_TEAM);
                if (playerTeam == null) {
                    playerTeam = scoreboard.addTeam(TeamManager.PLAYER_TEAM);
                    playerTeam.setColor(Formatting.WHITE);
                }
                if (scoreboard.getScoreHolderTeam(playerName) != playerTeam) {
                    scoreboard.addScoreHolderToTeam(playerName, playerTeam);
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

        ServerPlayerEntity serverPlayer = server.getPlayerManager().getPlayer(player);
        if (serverPlayer == null) return;

        Scoreboard scoreboard = server.getScoreboard();
        String playerName = serverPlayer.getGameProfile().getName();

        if (apply) {
            // Order matters: the glow takes its colour from the team the player is on when the
            // effect is applied, so it comes off first and goes back on last.
            serverPlayer.removeStatusEffect(StatusEffects.GLOWING);

            // Off whichever team they normally sit on.
            Team playerTeam = scoreboard.getTeam(TeamManager.PLAYER_TEAM);
            if (playerTeam != null && scoreboard.getScoreHolderTeam(playerName) == playerTeam) {
                scoreboard.removeScoreHolderFromTeam(playerName, playerTeam);
            }
            Team travelerTeam = scoreboard.getTeam(TeamManager.TRAVELER_TEAM);
            if (travelerTeam != null && scoreboard.getScoreHolderTeam(playerName) == travelerTeam) {
                scoreboard.removeScoreHolderFromTeam(playerName, travelerTeam);
            }

            // The red MFE team, created on first use. The colour is reasserted every time in
            // case something else changed it.
            Team mfeTeam = scoreboard.getTeam(TeamManager.MFE_TEAM);
            if (mfeTeam == null) {
                mfeTeam = scoreboard.addTeam(TeamManager.MFE_TEAM);
            }
            mfeTeam.setColor(Formatting.RED);

            // Verify and retry once if the scoreboard didn't take it.
            if (!scoreboard.addScoreHolderToTeam(playerName, mfeTeam) || scoreboard.getScoreHolderTeam(playerName) != mfeTeam) {
                scoreboard.addScoreHolderToTeam(playerName, mfeTeam);
                if (scoreboard.getScoreHolderTeam(playerName) != mfeTeam) {
                    BloodOnTheBlocktower.LOGGER.warn("Failed to assign {} to botb_mfe team after retry", playerName);
                }
            }

            // Red, because they're on the red team now.
            serverPlayer.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.GLOWING,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    false // Don't show icon in HUD
            ));
        } else {
            // Remove from MFE team if player is on it
            Team mfeTeam = scoreboard.getTeam(TeamManager.MFE_TEAM);
            if (mfeTeam != null && scoreboard.getScoreHolderTeam(playerName) == mfeTeam) {
                scoreboard.removeScoreHolderFromTeam(playerName, mfeTeam);
            }

            // Remove glowing effect
            serverPlayer.removeStatusEffect(StatusEffects.GLOWING);

            // Add player back to botb_player team (travelers only use botb_traveler during exile)
            Team playerTeam = scoreboard.getTeam(TeamManager.PLAYER_TEAM);
            if (playerTeam == null) {
                playerTeam = scoreboard.addTeam(TeamManager.PLAYER_TEAM);
                playerTeam.setColor(Formatting.WHITE);
            }
            scoreboard.addScoreHolderToTeam(playerName, playerTeam);
        }
    }
}
