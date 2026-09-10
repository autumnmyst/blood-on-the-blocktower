package com.autumnwind.botb.networking;

import com.autumnwind.botb.daytime.VotingManager;
import com.autumnwind.botb.hud.GameEndAnimationHUD;
import com.autumnwind.botb.sound.CustomSounds;
import com.autumnwind.botb.sound.ModSounds;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Reminder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-bound packet receivers for the end-of-game reveal. */
final class GameEndReceivers {

    private GameEndReceivers() {}

    static void register() {
        // Client receives request to send grimoire data for game end
        ClientPlayNetworking.registerGlobalReceiver(RequestGameEndS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Gather grimoire data from StorytellerState (true roles, not fake drunk roles)
                Map<UUID, PendingRoleAssignment> roles =
                        new HashMap<>(StorytellerState.PENDING_ROLES);
                Map<UUID, Integer> seatNumbers =
                        new HashMap<>(StorytellerState.PENDING_SEAT_NUMBERS);
                Map<UUID, List<Reminder>> reminders =
                        new HashMap<>(StorytellerState.REMINDERS);
                // Convert ScriptRole bluffs to string format for network
                List<String> demonBluffs = StorytellerState.bluffsToStrings(
                        StorytellerState.DEMON_BLUFFS);

                // Send grimoire data back to server
                ClientPlayNetworking.send(new EndGameC2SPayload(
                        payload.goodWins(),
                        roles,
                        seatNumbers,
                        reminders,
                        demonBluffs
                ));
            });
        });

        // Client receives game end animation trigger
        ClientPlayNetworking.registerGlobalReceiver(GameEndAnimationS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Set game ending flag
                ClientState.gameEnding = true;
                // Latch reveal-active so AFTER_END floating role icons persist after the animation
                ClientState.rolesRevealed = true;

                // Start the game end animation
                GameEndAnimationHUD.startAnimation(payload.goodWins());

                // Play game end sound, with the pack's victory or defeat track if it has one
                if (context.client().player != null && context.client().world != null) {
                    boolean won = GameEndAnimationHUD.localPlayerWon(context.client(), payload.goodWins());
                    CustomSounds.playOneShot(context.client(), CustomSounds.gameEndCandidates(won), ModSounds.GAME_END, 1.0f);
                }
            });
        });

        // Client receives post-reveal reset (normal + hard reset), which clears AFTER_END latch
        // at the same moment server wipes rev_* teams. Paired with VotingManager.resetLeverStates.
        ClientPlayNetworking.registerGlobalReceiver(
                ResetRevealActiveS2CPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> ClientState.rolesRevealed = false);
                }
        );
    }
}
