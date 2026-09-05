package com.autumnwind.botb.states;

import com.autumnwind.botb.config.WhisperSettings;

/**
 * Client mirror of the server's {@link WhisperSettings}, updated on join and on every
 * storyteller change via SyncWhisperSettingsS2CPayload. Read by:
 *
 * <ul>
 *   <li>{@code WhisperSettingsScreen} (read-only player view + storyteller editor),</li>
 *   <li>{@code WhisperEffectManager} (only really needs visual/audio modes from the
 *       per-effect payload, but reads this to know whether to even consider effects).</li>
 * </ul>
 *
 * <p>Defaults to {@link WhisperSettings#DEFAULT} until the first sync arrives, so the
 * UI doesn't render with nulls before the join sync packet lands.
 */
public final class ClientWhisperSettings {
    private ClientWhisperSettings() {}

    public static volatile WhisperSettings current = WhisperSettings.DEFAULT;
}
