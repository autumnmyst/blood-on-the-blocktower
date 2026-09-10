package com.autumnwind.botb.config;

import com.autumnwind.botb.util.CustomRole;
import com.autumnwind.botb.util.NonPlayerCharacter;
import com.autumnwind.botb.util.Script;
import com.autumnwind.botb.util.ScriptJson;
import com.autumnwind.botb.util.ScriptRole;
import com.autumnwind.botb.util.UrlTextureLoaderImpl;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A persistent pool of homebrew characters harvested from imported scripts, available to the
 * Script Builder as extra palette entries regardless of which script is currently loaded.
 *
 * <p>Importing custom roles is deliberately additive and independent of the active script:
 * you can pull the interesting characters out of half a dozen homebrew scripts over time and
 * then mix them into a script of your own.
 *
 * <p>Each entry stores the character's <em>raw JSON definition</em> exactly as it appeared in
 * the source script. Round-tripping the original object rather than re-serializing a parsed
 * {@link CustomRole} means fields this mod doesn't read (or doesn't read yet) survive, and
 * there's no second copy of the JSON schema to keep in sync.
 *
 * <p>Alongside each definition we keep the almanac URLs from the source script's {@code _meta}.
 * When the builder saves a script containing library characters it folds those URLs into the
 * new script's {@code extraAlmanacs}, so {@code CharacterDetailsScreen}'s existing
 * {@code AlmanacParser.fetchAlmanacs(almanac, extraAlmanacs)} call finds their almanac pages
 * with no changes on that side.
 */
public final class CustomRoleLibrary {

    private static final Path FILE = BotbConfigDir.resolve("custom_roles.json");

    private static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Normalized id → entry, in insertion order so the palette is stable between sessions. */
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private CustomRoleLibrary() {}

    /**
     * One stored character.
     *
     * @param role         the parsed character, ready for display
     * @param definition   the raw JSON object it was parsed from, re-emitted verbatim on save
     * @param sourceScript name of the script it was imported from, shown in the palette tooltip
     * @param almanacs     almanac URLs from that script's {@code _meta}
     */
    public record Entry(ScriptRole role, JsonObject definition, String sourceScript, List<String> almanacs) {
        public String normalizedId() {
            return ScriptJson.normalizeId(role.getId());
        }
    }

    public static void load() {
        ENTRIES.clear();
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();

            // Future schema migrations would branch here. Only v1 exists, so anything else is
            // left alone rather than partially applied, the same stance as GrimoirePersistence.
            int version = root.has("version") ? root.get("version").getAsInt() : 0;
            if (version != VERSION) return;

            if (!root.has("roles") || !root.get("roles").isJsonArray()) return;
            for (JsonElement element : root.getAsJsonArray("roles")) {
                if (!element.isJsonObject()) continue;
                JsonObject stored = element.getAsJsonObject();
                if (!stored.has("definition") || !stored.get("definition").isJsonObject()) continue;

                JsonObject definition = stored.getAsJsonObject("definition");
                Optional<ScriptRole> role = ScriptJson.toScriptRole(definition);
                // An official role wouldn't belong here because it's already in the palette.
                if (role.isEmpty() || !role.get().isCustom()) continue;

                String sourceScript = stored.has("sourceScript")
                        ? stored.get("sourceScript").getAsString()
                        : "";
                List<String> almanacs = new ArrayList<>();
                if (stored.has("almanacs") && stored.get("almanacs").isJsonArray()) {
                    for (JsonElement url : stored.getAsJsonArray("almanacs")) {
                        almanacs.add(url.getAsString());
                    }
                }

                Entry entry = new Entry(role.get(), definition, sourceScript, almanacs);
                ENTRIES.put(entry.normalizedId(), entry);
            }
        } catch (IOException | RuntimeException e) {
            // A corrupt library shouldn't take the client down, so start empty and let the next
            // import rewrite the file.
            ENTRIES.clear();
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", VERSION);

            JsonArray roles = new JsonArray();
            for (Entry entry : ENTRIES.values()) {
                JsonObject stored = new JsonObject();
                stored.addProperty("sourceScript", entry.sourceScript());
                JsonArray almanacs = new JsonArray();
                for (String url : entry.almanacs()) {
                    almanacs.add(url);
                }
                stored.add("almanacs", almanacs);
                stored.add("definition", entry.definition());
                roles.add(stored);
            }
            root.add("roles", roles);

            Files.writeString(FILE, GSON.toJson(root));
        } catch (IOException e) {
            // Best-effort, like the other client-side config writes.
        }
    }

    /** All stored characters, in import order. */
    public static List<Entry> all() {
        return List.copyOf(ENTRIES.values());
    }

    public static Optional<Entry> get(String roleId) {
        return Optional.ofNullable(ENTRIES.get(ScriptJson.normalizeId(roleId)));
    }

    public static boolean contains(String roleId) {
        return ENTRIES.containsKey(ScriptJson.normalizeId(roleId));
    }

    /** Forget a character. Returns true if it was there. */
    public static boolean remove(String roleId) {
        if (ENTRIES.remove(ScriptJson.normalizeId(roleId)) == null) return false;
        save();
        return true;
    }

    /**
     * Forget every stored character. Returns how many were dropped.
     *
     * <p>Only touches the library. A homebrew character already on the script being built keeps
     * its definition in the builder and still saves correctly.
     */
    public static int clear() {
        int removed = ENTRIES.size();
        if (removed == 0) return 0;
        ENTRIES.clear();
        save();
        return removed;
    }

    /**
     * Add every homebrew character in a script to the library, leaving the current script and
     * the builder's working set untouched.
     *
     * <p>A character already in the library is overwritten by the newer definition. Re-importing
     * an updated version of a script is the normal way to pick up its edits.
     *
     * @return {@code [added, updated]} counts
     */
    public static int[] importFrom(Script script) {
        if (script == null) return new int[]{0, 0};

        Map<String, JsonObject> definitions = ScriptJson.indexDefinitions(script.rawJson());
        List<String> almanacs = almanacUrlsOf(script);

        int added = 0;
        int updated = 0;
        for (ScriptRole role : customCharactersOf(script)) {
            String key = ScriptJson.normalizeId(role.getId());
            JsonObject definition = definitions.get(key);
            // Without the raw definition there's nothing durable to store, so the character would
            // come back as an unresolvable bare id on the next launch.
            if (definition == null) continue;

            boolean existed = ENTRIES.containsKey(key);
            ENTRIES.put(key, new Entry(role, definition, script.name(), almanacs));
            if (existed) {
                updated++;
            } else {
                added++;
            }
        }

        if (added + updated > 0) {
            save();
            preloadTextures();
        }
        return new int[]{added, updated};
    }

    /**
     * Every homebrew character on a script: custom roles, custom travelers, and custom
     * fabled/loric. Official characters are skipped because they're already in the palette, and
     * {@code Script.fromJson} wraps them as {@link ScriptRole.Official} regardless of how much
     * detail the source document gave them.
     */
    public static List<ScriptRole> customCharactersOf(Script script) {
        List<ScriptRole> out = new ArrayList<>();
        if (script == null) return out;

        if (script.customRoles() != null) {
            for (CustomRole customRole : script.customRoles()) {
                out.add(new ScriptRole.Custom(customRole));
            }
        }
        addCustomOnly(out, script.travelers());
        addCustomOnly(out, script.fabled());
        addCustomOnly(out, script.loric());
        return out;
    }

    private static void addCustomOnly(List<ScriptRole> out, List<ScriptRole> source) {
        if (source == null) return;
        for (ScriptRole role : source) {
            if (role instanceof ScriptRole.Custom || role instanceof ScriptRole.Fabled) {
                out.add(role);
            }
        }
    }

    /** The script's almanac URLs (main first, then extras), deduplicated and blank-free. */
    public static List<String> almanacUrlsOf(Script script) {
        List<String> urls = new ArrayList<>();
        if (script == null) return urls;
        if (script.almanac() != null && !script.almanac().isBlank()) {
            urls.add(script.almanac());
        }
        if (script.extraAlmanacs() != null) {
            for (String url : script.extraAlmanacs()) {
                if (url != null && !url.isBlank() && !urls.contains(url)) {
                    urls.add(url);
                }
            }
        }
        return urls;
    }

    /**
     * Warm the texture cache for every stored character. Icons would load lazily on first
     * render anyway. Doing it up front just avoids a grid full of placeholders when the
     * builder opens.
     */
    public static void preloadTextures() {
        for (Entry entry : ENTRIES.values()) {
            if (entry.role() instanceof ScriptRole.Custom custom) {
                for (String url : custom.customRole().imageUrls()) {
                    UrlTextureLoaderImpl.getTextureForUrl(url);
                }
            } else if (entry.role() instanceof ScriptRole.Fabled fabled) {
                NonPlayerCharacter character = fabled.fabledCharacter();
                if (character.imageUrl() != null && !character.imageUrl().isEmpty()) {
                    UrlTextureLoaderImpl.getTextureForUrl(character.imageUrl());
                }
            }
        }
    }
}
