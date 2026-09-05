package com.autumnwind.botb.voicechat;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.plugins.impl.VoicechatServerApiImpl;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Server-side Simple Voice Chat access. All SVC calls route through here so the mod
 * runs without SVC installed: SVC classes are only touched inside {@link Impl}, which
 * is never classloaded when the mod is absent.
 */
public final class VoiceChatServerCompat {

    public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("voicechat");

    private VoiceChatServerCompat() {}

    /** The player's current group id, or null when ungrouped, disconnected, or SVC is unavailable. */
    @Nullable
    public static UUID getGroupId(UUID playerUuid) {
        if (!LOADED) return null;
        try {
            return Impl.getGroupId(playerUuid);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Removes the player from their current voice chat group. */
    public static void leaveGroup(UUID playerUuid) {
        if (!LOADED) return;
        try {
            Impl.leaveGroup(playerUuid);
        } catch (Throwable t) {
        }
    }

    /** Puts the target into the storyteller's current group (or out of any group if the storyteller has none). */
    public static void matchGroup(UUID storytellerUuid, UUID targetUuid) {
        if (!LOADED) return;
        try {
            Impl.matchGroup(storytellerUuid, targetUuid);
        } catch (Throwable t) {
        }
    }

    private static final class Impl {
        static UUID getGroupId(UUID playerUuid) {
            VoicechatServerApi api = VoicechatServerApiImpl.instance();
            if (api == null) return null;
            VoicechatConnection connection = api.getConnectionOf(playerUuid);
            if (connection == null) return null;
            Group group = connection.getGroup();
            return group == null ? null : group.getId();
        }

        static void leaveGroup(UUID playerUuid) {
            VoicechatServerApi api = VoicechatServerApiImpl.instance();
            if (api == null) return;
            VoicechatConnection connection = api.getConnectionOf(playerUuid);
            if (connection != null) {
                connection.setGroup(null);
            }
        }

        static void matchGroup(UUID storytellerUuid, UUID targetUuid) {
            VoicechatServerApi api = VoicechatServerApiImpl.instance();
            if (api == null) return;
            VoicechatConnection storyteller = api.getConnectionOf(storytellerUuid);
            VoicechatConnection target = api.getConnectionOf(targetUuid);
            if (target != null) {
                target.setGroup(storyteller != null ? storyteller.getGroup() : null);
            }
        }
    }
}
