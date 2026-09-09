package com.autumnwind.botb.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;

/**
 * Reads and writes raw Blood on the Clocktower script JSON.
 *
 * <p>{@link Script} only parses one direction. It turns a JSON document into a record and
 * keeps the original text in {@code rawJson} because that string, not the parsed fields, is
 * what {@link Script#PACKET_CODEC} actually transmits. Anything that <em>builds</em> a script
 * (the Script Builder) therefore has to produce real JSON, which is what {@link #build} does.
 *
 * <p>Custom characters round-trip as their original JSON object rather than being rebuilt
 * field-by-field from {@link CustomRole} / {@link NonPlayerCharacter}. That keeps fields the
 * mod doesn't parse yet (and any it stops parsing later) intact across an import → save cycle.
 */
public final class ScriptJson {

    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private ScriptJson() {}

    /**
     * Normalize a role id for comparison, matching the rule {@code Script} uses internally
     * (lowercase, no underscores/spaces/dashes) so ids agree across both classes.
     */
    public static String normalizeId(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).replaceAll("[_\\s-]", "");
    }

    /**
     * Index every full character <em>definition</em> in a script document by normalized id.
     *
     * <p>Bare references ({@code {"id": "washerwoman"}} or a plain string) are skipped, since they
     * carry nothing worth preserving. Only entries with {@code team} or {@code ability}, the
     * same test {@code Script.fromJson} uses to spot a definition, are indexed.
     */
    public static Map<String, JsonObject> indexDefinitions(String rawJson) {
        Map<String, JsonObject> out = new HashMap<>();
        if (rawJson == null || rawJson.isEmpty()) return out;
        try {
            JsonElement parsed = JsonParser.parseString(rawJson);
            if (!parsed.isJsonArray()) return out;
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("id") || !obj.get("id").isJsonPrimitive()) continue;
                String id = obj.get("id").getAsString();
                if ("_meta".equals(id)) continue;
                if (!obj.has("team") && !obj.has("ability")) continue;
                out.put(normalizeId(id), obj);
            }
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            // A malformed document just yields no definitions, and callers fall back to bare refs.
        }
        return out;
    }

    /**
     * Turn a raw character definition back into a {@link ScriptRole}, mirroring the dispatch
     * in {@code Script.fromJson}: fabled and loric become {@link ScriptRole.Fabled}, everything
     * else (including travelers) becomes {@link ScriptRole.Custom}.
     *
     * <p>Definitions whose id matches an official role resolve to {@link ScriptRole.Official}
     * instead, so an official character republished with full text doesn't become a duplicate
     * custom entry.
     */
    public static Optional<ScriptRole> toScriptRole(JsonObject definition) {
        if (definition == null || !definition.has("id")) return Optional.empty();
        try {
            Map<String, Object> map = GSON.fromJson(definition, MAP_TYPE);
            if (map == null) return Optional.empty();

            String id = String.valueOf(map.getOrDefault("id", ""));
            Role official = findOfficialRole(id);
            if (official != null) {
                return Optional.of(new ScriptRole.Official(official));
            }

            String team = String.valueOf(map.getOrDefault("team", "townsfolk"));
            if (team.equalsIgnoreCase("fabled")) {
                return Optional.of(new ScriptRole.Fabled(
                        NonPlayerCharacter.fromJsonMap(map, NonPlayerCharacter.FabledType.FABLED)));
            }
            if (team.equalsIgnoreCase("loric")) {
                return Optional.of(new ScriptRole.Fabled(
                        NonPlayerCharacter.fromJsonMap(map, NonPlayerCharacter.FabledType.LORIC)));
            }
            return Optional.of(new ScriptRole.Custom(CustomRole.fromJsonMap(map)));
        } catch (JsonParseException | ClassCastException | IllegalStateException e) {
            return Optional.empty();
        }
    }

    /**
     * Find the official {@link Role} an id refers to, or null if it's homebrew.
     */
    public static Role findOfficialRole(String id) {
        String normalized = normalizeId(id);
        if (normalized.isEmpty()) return null;
        for (Role role : Role.values()) {
            if (role == Role.NO_ROLE) continue;
            if (normalizeId(role.name()).equals(normalized)) {
                return role;
            }
        }
        return null;
    }

    /**
     * Build a script JSON document from a {@code _meta} block plus a list of characters.
     *
     * <p>Official roles are emitted as bare {@code {"id": ...}} references, while custom characters
     * are emitted as whatever definition object {@code definitions} holds for them, falling
     * back to a bare reference when none is known (which would make the character unresolvable,
     * so callers should keep the definition map complete).
     *
     * <p>Empty/blank meta values are omitted rather than written as nulls, matching what real
     * script-tool exports look like.
     */
    public static String build(Meta meta, List<ScriptRole> roles, Map<String, JsonObject> definitions) {
        JsonArray root = new JsonArray();

        JsonObject metaObj = new JsonObject();
        metaObj.addProperty("id", "_meta");
        metaObj.addProperty("name", blankToDefault(meta.name(), "Custom Script"));
        metaObj.addProperty("author", blankToDefault(meta.author(), ""));
        addIfPresent(metaObj, "logo", meta.logo());
        addIfPresent(metaObj, "almanac", meta.almanac());
        addIfNotEmpty(metaObj, "extraAlmanacs", meta.extraAlmanacs());
        addIfNotEmpty(metaObj, "bootlegger", meta.bootlegger());
        addIfNotEmpty(metaObj, "firstNight", meta.firstNightOrder());
        addIfNotEmpty(metaObj, "otherNight", meta.otherNightOrder());
        root.add(metaObj);

        for (ScriptRole role : roles) {
            JsonObject definition = role.isCustom() ? definitions.get(normalizeId(role.getId())) : null;
            if (definition != null) {
                root.add(definition.deepCopy());
            } else {
                JsonObject ref = new JsonObject();
                ref.addProperty("id", role.getId());
                root.add(ref);
            }
        }

        return PRETTY_GSON.toJson(root);
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void addIfPresent(JsonObject obj, String key, String value) {
        if (value != null && !value.isBlank()) {
            obj.addProperty(key, value);
        }
    }

    private static void addIfNotEmpty(JsonObject obj, String key, List<String> values) {
        if (values == null || values.isEmpty()) return;
        JsonArray array = new JsonArray();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                array.add(new JsonPrimitive(value));
            }
        }
        if (!array.isEmpty()) {
            obj.add(key, array);
        }
    }

    /**
     * The {@code _meta} fields the builder carries across a save.
     */
    public record Meta(
            String name,
            String author,
            String logo,
            String almanac,
            List<String> extraAlmanacs,
            List<String> bootlegger,
            List<String> firstNightOrder,
            List<String> otherNightOrder
    ) {}
}
