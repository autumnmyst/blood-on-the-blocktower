package com.autumnwind.botb.networking;

import com.autumnwind.botb.hud.NightOrderHudManager;
import com.autumnwind.botb.hud.nightorderhud.RoleHelpers;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.states.StorytellerState;
import com.autumnwind.botb.util.NightOrder;
import com.autumnwind.botb.util.RoleVisit;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.server.permissions.Permissions;

/** Client-bound packet receivers for day/night state and the storyteller's night order. */
final class NightOrderReceivers {

    private NightOrderReceivers() {}

    static void register() {
        // Client receives day/night sync
        ClientPlayNetworking.registerGlobalReceiver(SyncDayNightS2CPayload.ID, (payload, context) -> {
            boolean gameStarting = ClientState.currentNight == 0 && ClientState.currentDay == 0 && payload.night() == 1;
            ClientState.currentNight = payload.night();
            ClientState.currentDay = payload.day();
            ClientState.executionToday = payload.executionToday();

            // Game start: snapshot the outsider count for the Xaan
            if (gameStarting) {
                context.client().execute(() -> {
                    if (context.client().player != null && context.client().player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                        StorytellerState.setupOutsiderCount = RoleHelpers.countAssignedOutsiders();
                        StorytellerState.syncGrimoire();
                    }
                });
            }
        });

        // Client receives rebuild night order trigger
        ClientPlayNetworking.registerGlobalReceiver(RebuildNightOrderS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // If we're in setup (night 0), clear triggered visits and reset visit tracking
                if (ClientState.currentNight == 0) {
                    StorytellerState.triggeredVisits.clear();
                    StorytellerState.currentNightVisitIndex = 0;
                    StorytellerState.currentVisitSourceIndex = null;
                    StorytellerState.currentVisitIsTriggered = false;
                    StorytellerState.currentTriggerChainIndex = 0;
                }
                NightOrderHudManager.rebuildActiveNightOrder();
            });
        });

        // Client receives sync night visit trigger (moves storyteller to Dawn/Dusk)
        ClientPlayNetworking.registerGlobalReceiver(SyncNightVisitS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Only process for operators
                if (context.client().player == null || !context.client().player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                    return;
                }

                // Find the target visit (Dawn or Dusk) in the active night order
                String targetType = payload.visitType();
                NightOrder.StaticAction targetAction =
                        SyncNightVisitS2CPayload.DAWN.equals(targetType)
                                ? NightOrder.StaticAction.DAWN
                                : NightOrder.StaticAction.DUSK;

                for (int i = 0; i < StorytellerState.activeNightOrder.size(); i++) {
                    RoleVisit visit = StorytellerState.activeNightOrder.get(i);
                    if (visit.staticAction() != null && visit.staticAction() == targetAction) {
                        StorytellerState.currentNightVisitIndex = i;
                        break;
                    }
                }
            });
        });
    }
}
