package com.autumnwind.botb.networking;

import com.autumnwind.botb.gui.WhisperSettingsScreen;
import com.autumnwind.botb.hud.WhisperEffectManager;
import com.autumnwind.botb.states.ClientWhisperSettings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-bound packet receivers for whisper settings and whisper effects. */
final class WhisperReceivers {

    private WhisperReceivers() {}

    static void register() {
        // Whisper settings sync, fired on join + on every storyteller change. Updates
        // both the player-facing read-only view and (for ops) the editor's last-synced
        // baseline that "Cancel" reverts to.
        ClientPlayNetworking.registerGlobalReceiver(SyncWhisperSettingsS2CPayload.ID, (payload, context) -> {
            ClientWhisperSettings.current = payload.settings();
            context.client().execute(() -> {
                if (context.client().currentScreen instanceof WhisperSettingsScreen ws) {
                    ws.onSettingsSync(payload.settings());
                }
            });
        });

        // Per-whisper visual + audio effect.
        ClientPlayNetworking.registerGlobalReceiver(WhisperEffectS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> WhisperEffectManager.onEffect(payload));
        });
    }
}
