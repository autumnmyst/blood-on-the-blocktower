package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.states.StorytellerState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

import java.util.*;

/**
 * Utility for getting all players on the server regardless of render distance.
 * Uses PlayerListEntry (tab list) as the source of truth instead of client.world.getPlayers().
 * Caches last-seen skin/name info for assigned players so we can still show a face for
 * them when they disconnect mid-game (with a disconnected flag set so callers can fade
 * and overlay an indicator).
 */
public class PlayerListUtil {

    /**
     * Player info record containing all commonly needed data.
     * {@code disconnected} is {@code true} when we only have this entry because the
     * player left the server (i.e., it's served from {@link #CACHED_INFO} rather than
     * a live {@code PlayerListEntry}).
     */
    public record PlayerInfo(UUID uuid, String name, Identifier skinTexture, boolean isSpectator, boolean disconnected) {}

    private static final Map<UUID, PlayerInfo> CACHED_INFO = new HashMap<>();

    private static final Identifier DISCONNECT_OVERLAY =
            Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/icons/reminder_custom.png");

    private static PlayerInfo fromEntry(PlayerListEntry entry) {
        UUID uuid = entry.getProfile().getId();
        MinecraftClient client = MinecraftClient.getInstance();
        boolean haveLiveEntity = client != null && client.world != null
                && client.world.getPlayerByUuid(uuid) != null;

        String resolvedName = resolveDisplayName(entry, client, uuid);

        // Don't downgrade a cached custom name when the live entity has unloaded. A
        // renaming mod that only modifies the entity would otherwise cause the UI to
        // revert to the profile name once the player walks out of view distance. Skin
        // and spectator status are still refreshed from the live entry.
        if (!haveLiveEntity) {
            PlayerInfo previous = CACHED_INFO.get(uuid);
            if (previous != null) {
                resolvedName = previous.name();
            }
        }

        PlayerInfo info = new PlayerInfo(
                uuid,
                resolvedName,
                entry.getSkinTextures().texture(),
                entry.getGameMode() == GameMode.SPECTATOR,
                false
        );
        // Only cache assigned players, those in the current grimoire (storyteller's full
        // view or a regular player's own bookkeeping). Unassigned players never need
        // disconnect-fallback display so we don't waste memory on them.
        if (StorytellerState.PENDING_ROLES.containsKey(uuid)) {
            CACHED_INFO.put(uuid, info);
        }
        return info;
    }

    /**
     * Resolves the display string for a player: the mod's own custom name if one is set, then
     * the live entity's {@code getName()}, then the player list entry's display name, finally
     * the raw Mojang profile name.
     */
    public static String resolveDisplayName(PlayerListEntry entry, MinecraftClient client, UUID uuid) {
        String custom = CustomNames.get(uuid);
        if (custom != null) return custom;
        if (client != null && client.world != null) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) client.world.getPlayerByUuid(uuid);
            if (player != null) {
                String name = player.getName().getString();
                if (name != null && !name.isEmpty()) return name;
            }
        }
        Text displayName = entry.getDisplayName();
        if (displayName != null) {
            String s = displayName.getString();
            if (s != null && !s.isEmpty()) return s;
        }
        return entry.getProfile().getName();
    }

    /**
     * Gets info for all players currently on the server.
     * This includes players beyond render distance.
     */
    public static List<PlayerInfo> getAllPlayers(MinecraftClient client) {
        if (client.getNetworkHandler() == null) return Collections.emptyList();

        List<PlayerInfo> result = new ArrayList<>();
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            result.add(fromEntry(entry));
        }
        return result;
    }

    /**
     * Gets info for all players as a map keyed by UUID for quick lookup.
     */
    public static Map<UUID, PlayerInfo> getAllPlayersMap(MinecraftClient client) {
        if (client.getNetworkHandler() == null) return Collections.emptyMap();

        Map<UUID, PlayerInfo> result = new HashMap<>();
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            result.put(entry.getProfile().getId(), fromEntry(entry));
        }
        return result;
    }

    /**
     * Gets info for a single player by UUID.
     * Returns null if the player is not on the server.
     */
    public static PlayerInfo getPlayer(MinecraftClient client, UUID uuid) {
        if (client.getNetworkHandler() == null) return null;

        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(uuid);
        if (entry == null) return null;

        return fromEntry(entry);
    }

    /**
     * Gets info for a single player by UUID, falling back to the cached last-seen entry
     * if they're no longer in the live player list. The returned {@link PlayerInfo} will
     * have {@code disconnected = true} when the fallback is in use. Returns null only if
     * we have never seen that UUID assigned to a grimoire role.
     */
    public static PlayerInfo getPlayerOrCached(MinecraftClient client, UUID uuid) {
        PlayerInfo live = getPlayer(client, uuid);
        if (live != null) return live;

        PlayerInfo cached = CACHED_INFO.get(uuid);
        if (cached == null) return null;
        if (cached.disconnected()) return cached;
        return new PlayerInfo(cached.uuid(), cached.name(), cached.skinTexture(), cached.isSpectator(), true);
    }

    /**
     * Draws a player's head skin at the given position. Handles disconnect fallback
     * automatically, so disconnected players render at 40% alpha with an exclamation
     * overlay sourced from {@code reminder_custom.png}.
     * <p>
     * Does nothing if we have never cached info for the UUID.
     */
    public static void drawPlayerHead(DrawContext context, MinecraftClient client, UUID uuid, int x, int y, int size) {
        PlayerInfo info = getPlayerOrCached(client, uuid);
        if (info == null) return;
        drawPlayerHead(context, info, x, y, size);
    }

    /**
     * Like {@link #drawPlayerHead(DrawContext, MinecraftClient, UUID, int, int, int)} but
     * using a pre-resolved {@link PlayerInfo}, useful when the caller already has the
     * record and wants to avoid the second lookup.
     */
    public static void drawPlayerHead(DrawContext context, PlayerInfo info, int x, int y, int size) {
        boolean disconnected = info.disconnected();
        if (disconnected) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, 0.4f);
        }
        context.drawTexture(info.skinTexture(), x, y, size, size, 8.0f, 8.0f, 8, 8, 64, 64);
        context.drawTexture(info.skinTexture(), x, y, size, size, 40.0f, 8.0f, 8, 8, 64, 64);
        if (disconnected) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            context.drawTexture(DISCONNECT_OVERLAY, x, y, size, size, 0f, 0f, 108, 108, 108, 108);
            RenderSystem.disableBlend();
        }
    }
}
