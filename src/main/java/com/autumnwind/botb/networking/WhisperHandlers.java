package com.autumnwind.botb.networking;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.config.WhisperSettingsManager;
import com.autumnwind.botb.daytime.*;
import java.util.*;

/** Server-bound packet handlers: Whisper settings changes. */
final class WhisperHandlers {

    private WhisperHandlers() {}

    static void register() {
        ModPackets.registerGuarded(UpdateWhisperSettingsC2SPayload.ID, (payload, context) -> {
            if (!context.player().hasPermissionLevel(2)) {
                return;
            }
            WhisperSettingsManager.set(payload.settings());
            ServerConfig.save();
            StateBroadcaster.broadcastWhisperSettings(context.server());
        });
    }
}
