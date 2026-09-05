package com.autumnwind.botb.config;

/**
 * Server-side holder for the active {@link WhisperSettings}. Mutated by storyteller
 * updates (UpdateWhisperSettingsC2SPayload), persisted via {@link ServerConfig}, and
 * synced to clients through SyncWhisperSettingsS2CPayload (on join + on change).
 */
public final class WhisperSettingsManager {
    private WhisperSettingsManager() {}

    private static volatile WhisperSettings current = WhisperSettings.DEFAULT;

    public static WhisperSettings get() {
        return current;
    }

    /** Replaces the active settings. Callers are responsible for persistence + broadcast. */
    public static void set(WhisperSettings settings) {
        current = settings == null ? WhisperSettings.DEFAULT : settings;
    }
}
