package com.autumnwind.botb.util;

import net.minecraft.text.Text;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Display names players have chosen with /botb setName, keyed by UUID.
 *
 * <p>The server fills this from the saved config and broadcasts it; clients fill it from the
 * sync packet. Player entities read it through the {@code getName()} mixin, so chat, death and
 * join messages, nametags, and the mod's own screens all show the custom name, while the
 * Mojang profile name stays in place for scoreboard teams, command targets, and skins.
 */
public final class CustomNames {

    private CustomNames() {}

    private static final Map<UUID, String> NAMES = new HashMap<>();

    /** The custom name for a player, or null when they use their account name. */
    public static String get(UUID uuid) {
        return NAMES.get(uuid);
    }

    /** The custom name as text, or null. Used by the name mixins. */
    public static Text text(UUID uuid) {
        String name = NAMES.get(uuid);
        return name != null ? Text.literal(name) : null;
    }

    /** Sets or, for a null or blank name, clears a player's custom name. */
    public static void set(UUID uuid, String name) {
        if (name == null || name.isBlank()) {
            NAMES.remove(uuid);
        } else {
            NAMES.put(uuid, name);
        }
    }

    /** Replaces the whole map, e.g. from the saved config or a sync packet. */
    public static void replaceAll(Map<UUID, String> names) {
        NAMES.clear();
        NAMES.putAll(names);
    }

    public static Map<UUID, String> all() {
        return Collections.unmodifiableMap(NAMES);
    }
}
