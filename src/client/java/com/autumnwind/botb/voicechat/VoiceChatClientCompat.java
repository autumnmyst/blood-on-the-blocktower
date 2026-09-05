package com.autumnwind.botb.voicechat;

import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.ClientPlayerStateManager;
import de.maxhenkel.voicechat.voice.client.ClientVoicechat;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Client-side Simple Voice Chat access. All SVC calls route through here so the mod
 * runs without SVC installed: SVC classes are only touched inside {@link Impl}, which
 * is never classloaded when the mod is absent.
 */
public final class VoiceChatClientCompat {

    public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("voicechat");

    private VoiceChatClientCompat() {}

    /** The local player's current group id, or null when ungrouped or SVC is unavailable. */
    @Nullable
    public static UUID getLocalGroupId() {
        if (!LOADED) return null;
        try {
            return Impl.getLocalGroupId();
        } catch (Throwable t) {
            return null;
        }
    }

    /** A player's current group id, or null when ungrouped or SVC is unavailable. */
    @Nullable
    public static UUID getPlayerGroupId(UUID playerUuid) {
        if (!LOADED) return null;
        try {
            return Impl.getPlayerGroupId(playerUuid);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Whether the player is currently talking; false when SVC is unavailable. */
    public static boolean isPlayerTalking(UUID playerUuid) {
        if (!LOADED) return false;
        try {
            return Impl.isPlayerTalking(playerUuid);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Whether a player should be faded based on group membership: spectators always fade,
     * otherwise players fade when their group differs from the local player's.
     */
    public static boolean calculateFading(@Nullable UUID localGroupId, @Nullable UUID playerGroupId,
                                          boolean isLocalSpectator, boolean isPlayerSpectator) {
        if (isPlayerSpectator) {
            return true;
        }
        if (localGroupId != null) {
            return !localGroupId.equals(playerGroupId);
        }
        return playerGroupId != null;
    }

    private static final class Impl {
        static UUID getLocalGroupId() {
            ClientPlayerStateManager manager = ClientManager.getPlayerStateManager();
            return manager == null ? null : manager.getGroupID();
        }

        static UUID getPlayerGroupId(UUID playerUuid) {
            ClientPlayerStateManager manager = ClientManager.getPlayerStateManager();
            if (manager == null) return null;
            PlayerState state = manager.getState(playerUuid);
            return (state != null && state.hasGroup()) ? state.getGroup() : null;
        }

        static boolean isPlayerTalking(UUID playerUuid) {
            ClientVoicechat client = ClientManager.getClient();
            return client != null && client.getTalkCache().isTalking(playerUuid);
        }
    }
}
