package com.autumnwind.botb.networking;

import com.autumnwind.botb.daytime.*;
import com.autumnwind.botb.states.ServerState;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import com.autumnwind.botb.world.TeamManager;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

/** Server-bound packet handlers: Ending the game and the final reveal. */
final class GameEndHandlers {

    private GameEndHandlers() {}

    static void register() {
        ModPackets.registerGuarded(EndGameC2SPayload.ID, (payload, context) -> {
            if (context.player().hasPermissions(2)) {
                boolean goodWins = payload.goodWins();

                // Get grimoire data from storyteller's payload (true roles, not fake drunk roles)
                Map<UUID, PendingRoleAssignment> allRoles = payload.roles();
                Map<UUID, Integer> allSeatNumbers = payload.seatNumbers();
                Map<UUID, List<Reminder>> allReminders = payload.reminders();
                List<String> demonBluffs = payload.demonBluffs();

                // Send grimoire data and animation to ALL players FIRST (including storyteller and observers)
                for (ServerPlayer player : context.server().getPlayerList().getPlayers()) {
                    // Send grimoire data (full grimoire with true roles)
                    // NOTE: This only updates StorytellerState for animation display, NOT ClientState
                    // isTargetedSend = false because this is a broadcast to all players
                    // Demon bluffs included so players can see them in grimoire after animation
                    ServerPlayNetworking.send(player, new SendGrimoireS2CPayload(
                        allRoles,
                        allSeatNumbers,
                        allReminders,
                        demonBluffs,
                        false
                    ));

                    // Send animation trigger (this sets gameEnding = true on client)
                    ServerPlayNetworking.send(player, new GameEndAnimationS2CPayload(goodWins));
                }

                // Dead players stop being invisible so everyone can see who died, while their
                // death status stays. The respawn handler checks this flag before re-applying.
                ServerState.gameEnded = true;
                for (ServerPlayer online : context.server().getPlayerList().getPlayers()) {
                    if (ServerState.PLAYER_DEATH_STATUS.getOrDefault(online.getUUID(), false)) {
                        online.removeEffect(MobEffects.INVISIBILITY);
                    }
                }

                // Delay reveal team assignment so nametag colors flip at the same moment
                // floating role icons become visible on clients (after the ~1s fade-to-black).
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        context.server().execute(() -> TeamManager.assignRevealTeams(context.server(), allRoles));
                    }
                }, 1000);

                // Assign actual roles to players (like revealroles command)
                // Send AFTER animation trigger to prevent role receive sound from playing
                int activePlayerCount = allRoles.size();
                // Traveler count not needed during game end animation (HUD hidden)
                int travelerCount = 0;

                // Delay role send by 3 seconds to allow animation to start and prevent premature role display
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        context.server().execute(() -> {
                            // Send actual roles to each player (reveals Drunk, Marionette, Lunatic, Ogre, etc.)
                            // Use silent = true to prevent role receive sound during game end
                            allRoles.forEach((uuid, assignment) -> {
                                ServerPlayer player = context.server().getPlayerList().getPlayer(uuid);
                                if (player != null) {
                                    ServerPlayNetworking.send(player, SendRoleS2CPayload.ofAssignment(assignment, activePlayerCount, travelerCount, true));
                                }
                            });
                        });
                    }
                }, 3000); // 3 second delay
            }
        });
    }
}
