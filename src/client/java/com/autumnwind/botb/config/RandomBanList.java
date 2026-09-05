package com.autumnwind.botb.config;

import com.autumnwind.botb.util.ScriptJson;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Characters excluded from the Script Builder's random draw, stored by normalized id.
 *
 * <p>Purely a personal convenience. Nothing here is synced to the server or to co-storytellers,
 * and it has no effect on a script once it's built. A banned character can still be added by hand, and
 * doing so lifts the ban, since the two states contradict each other.
 *
 * <p>Ids are stored rather than roles so an entry survives a character leaving the
 * {@link CustomRoleLibrary} and coming back, and so official and homebrew bans work identically.
 */
public final class RandomBanList {

    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("blood-on-the-blocktower-random-bans.json");

    private static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Insertion-ordered so the file stays stable between writes. */
    private static final Set<String> BANNED = new LinkedHashSet<>();

    private RandomBanList() {}

    public static void load() {
        BANNED.clear();
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();

            int version = root.has("version") ? root.get("version").getAsInt() : 0;
            if (version != VERSION) return;

            if (!root.has("banned") || !root.get("banned").isJsonArray()) return;
            for (JsonElement element : root.getAsJsonArray("banned")) {
                String id = ScriptJson.normalizeId(element.getAsString());
                if (!id.isEmpty()) BANNED.add(id);
            }
        } catch (IOException | RuntimeException e) {
            // A corrupt ban list shouldn't take the client down, so start empty and let the next
            // toggle rewrite the file.
            BANNED.clear();
        }
    }

    private static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", VERSION);

            JsonArray banned = new JsonArray();
            for (String id : BANNED) {
                banned.add(id);
            }
            root.add("banned", banned);

            Files.writeString(FILE, GSON.toJson(root));
        } catch (IOException e) {
            // Best-effort, like the other client-side config writes.
        }
    }

    public static boolean isBanned(String roleId) {
        return BANNED.contains(ScriptJson.normalizeId(roleId));
    }

    /** Bans or unbans one character. Returns true if that changed anything. */
    public static boolean setBanned(String roleId, boolean banned) {
        String id = ScriptJson.normalizeId(roleId);
        if (id.isEmpty()) return false;

        boolean changed = banned ? BANNED.add(id) : BANNED.remove(id);
        if (changed) save();
        return changed;
    }

    /** Bans or unbans every given character in one write. Anything not listed is left alone. */
    public static void setBannedAll(Collection<String> roleIds, boolean banned) {
        boolean changed = false;
        for (String roleId : roleIds) {
            String id = ScriptJson.normalizeId(roleId);
            if (id.isEmpty()) continue;
            changed |= banned ? BANNED.add(id) : BANNED.remove(id);
        }
        if (changed) save();
    }
}
