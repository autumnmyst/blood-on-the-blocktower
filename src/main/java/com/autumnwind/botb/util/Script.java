package com.autumnwind.botb.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.Locale;

/**
 * Represents a Blood on the Clocktower script with both official and custom roles.
 */
public record Script(
    String name,
    String author,
    List<Role> roles,                    // Official townsfolk/outsider/minion/demon roles only
    List<CustomRole> customRoles,        // Custom townsfolk/outsider/minion/demon role definitions
    List<ScriptRole> fabled,             // ALL fabled characters (official as ScriptRole.Official, custom as ScriptRole.Fabled)
    List<ScriptRole> loric,              // ALL loric characters (official as ScriptRole.Official, custom as ScriptRole.Fabled)
    List<ScriptRole> travelers,          // ALL travelers (official as ScriptRole.Official, custom as ScriptRole.Custom)
    List<String> firstNightOrder,        // From _meta, null if not provided
    List<String> otherNightOrder,        // From _meta, null if not provided
    String logo,                         // URL to script logo image
    String almanac,                      // URL to main almanac HTML
    List<String> extraAlmanacs,          // URLs to extra almanac HTML pages (role data only)
    List<String> bootlegger,             // Bootlegger special rules from _meta, null if not provided
    String rawJson                       // Original JSON for network transmission
) {

    /**
     * Decode limits. These run before any permission check, on the network thread, for every
     * client, so they exist to stop a crafted packet rather than a real script: the protocol
     * already caps a payload at 1 MB, real scripts are tens of KB of JSON nested 3-4 deep, and
     * a gzip bomb or a 10,000-deep array is the only thing that gets anywhere near these.
     */
    private static final int MAX_COMPRESSED_BYTES = 1024 * 1024;
    private static final int MAX_JSON_BYTES = 16 * 1024 * 1024;
    private static final int MAX_JSON_DEPTH = 64;
    private static final int MAX_EXTRA_ALMANACS = 32;

    /**
     * Packet codec that transmits the raw JSON as compressed bytes.
     * This allows large scripts to be sent without hitting the 32767 character string limit.
     */
    public static final PacketCodec<ByteBuf, Script> PACKET_CODEC = new PacketCodec<>() {
        @Override
        public Script decode(ByteBuf buf) {
            int length = buf.readInt();
            if (length < 0 || length > MAX_COMPRESSED_BYTES) {
                throw new io.netty.handler.codec.DecoderException("Script payload length out of range: " + length);
            }
            if (buf.readableBytes() < length) {
                throw new io.netty.handler.codec.DecoderException("Script payload truncated");
            }
            byte[] compressed = new byte[length];
            buf.readBytes(compressed);
            String json = decompress(compressed);
            Script script = fromJson(json).orElse(null);
            if (script == null) {
                throw new io.netty.handler.codec.DecoderException("Script payload is not a valid script");
            }
            return script;
        }

        @Override
        public void encode(ByteBuf buf, Script script) {
            byte[] compressed = compress(script.rawJson());
            buf.writeInt(compressed.length);
            buf.writeBytes(compressed);
        }
    };

    public static final PacketCodec<ByteBuf, Optional<Script>> OPTIONAL_PACKET_CODEC =
            PACKET_CODEC.collect(PacketCodecs::optional);

    /**
     * Compress a string using GZIP.
     */
    private static byte[] compress(String str) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(str.getBytes(StandardCharsets.UTF_8));
            }
            return baos.toByteArray();
        } catch (IOException e) {
            // Fallback to uncompressed if compression fails
            return str.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Decompress GZIP bytes to a string.
     */
    private static String decompress(byte[] compressed) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
                // Read in chunks so an inflated size beyond the cap fails before it is allocated
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int n;
                while ((n = gzip.read(chunk)) != -1) {
                    if (out.size() + n > MAX_JSON_BYTES) {
                        throw new io.netty.handler.codec.DecoderException("Script JSON exceeds " + MAX_JSON_BYTES + " bytes");
                    }
                    out.write(chunk, 0, n);
                }
                return out.toString(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            // Fallback: try to read as uncompressed
            return new String(compressed, StandardCharsets.UTF_8);
        }
    }

    /**
     * Whether the JSON nests deeper than {@code max} levels of arrays/objects, ignoring
     * brackets inside strings. Gson recurses per level, so a deeply nested document would
     * overflow the stack before it ever produced a syntax error.
     */
    private static boolean exceedsNestingDepth(String json, int max) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '[' || c == '{') {
                if (++depth > max) return true;
            } else if (c == ']' || c == '}') {
                depth--;
            }
        }
        return false;
    }

    /**
     * Get a combined list of all roles (official + custom + travelers) as ScriptRole, ordered
     * by type.
     */
    public List<ScriptRole> allRoles() {
        List<ScriptRole> all = new ArrayList<>();

        for (Role r : roles) {
            all.add(new ScriptRole.Official(r));
        }
        for (CustomRole cr : customRoles) {
            all.add(new ScriptRole.Custom(cr));
        }
        if (travelers != null) {
            all.addAll(travelers);
        }

        // Sort by type order: Townsfolk, Outsider, Minion, Demon, Traveler
        all.sort((a, b) -> {
            int orderA = getTypeOrder(a.getTeam());
            int orderB = getTypeOrder(b.getTeam());
            return Integer.compare(orderA, orderB);
        });

        return all;
    }

    /**
     * Canonical team display order for role/reminder lists across the UI:
     * Townsfolk → Outsider → Minion → Demon → Traveler → other (Fabled/Loric/etc.).
     * Used by {@link #allRoles}, {@code ReminderChooseScreen}, and others to ensure
     * a consistent ordering regardless of how the script JSON declares its roles.
     */
    public static int getTypeOrder(RoleType type) {
        return switch (type) {
            case TOWNSFOLK -> 0;
            case OUTSIDER -> 1;
            case MINION -> 2;
            case DEMON -> 3;
            case TRAVELER -> 4;
            default -> 5;
        };
    }

    /**
     * Look up a custom role by its ID.
     * Only returns custom roles (not official travelers).
     * For travelers, use getTraveler() or getScriptRole() instead.
     */
    public Optional<CustomRole> getCustomRole(String id) {
        String normalizedId = normalizeRoleId(id);
        // Check custom roles
        if (customRoles != null) {
            Optional<CustomRole> fromCustom = customRoles.stream()
                .filter(cr -> normalizeRoleId(cr.id()).equals(normalizedId))
                .findFirst();
            if (fromCustom.isPresent()) {
                return fromCustom;
            }
        }
        // Check custom travelers (ScriptRole.Custom only, not official)
        if (travelers != null) {
            for (ScriptRole sr : travelers) {
                if (sr instanceof ScriptRole.Custom custom) {
                    if (normalizeRoleId(custom.customRole().id()).equals(normalizedId)) {
                        return Optional.of(custom.customRole());
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Look up an official role by its ID.
     */
    public Optional<Role> getOfficialRole(String id) {
        String normalizedId = normalizeRoleId(id);
        return roles.stream()
            .filter(r -> normalizeRoleId(r.name()).equals(normalizedId))
            .findFirst();
    }

    /**
     * Check if a role ID refers to a custom role in this script.
     */
    public boolean isCustomRole(String id) {
        return getCustomRole(id).isPresent();
    }

    /**
     * Check if a role ID is in this script (either official or custom).
     */
    public boolean hasRole(String id) {
        return getCustomRole(id).isPresent() || getOfficialRole(id).isPresent();
    }

    /**
     * Get a ScriptRole by ID (checks all lists: roles, customRoles, travelers, fabled, loric).
     */
    public Optional<ScriptRole> getScriptRole(String id) {
        String normalizedId = normalizeRoleId(id);

        // Check official roles (townsfolk, outsider, minion, demon)
        Optional<Role> official = getOfficialRole(id);
        if (official.isPresent()) {
            return Optional.of(new ScriptRole.Official(official.get()));
        }

        // Check custom roles
        Optional<CustomRole> custom = getCustomRole(id);
        if (custom.isPresent()) {
            return Optional.of(new ScriptRole.Custom(custom.get()));
        }

        // Check travelers (already ScriptRole)
        if (travelers != null) {
            for (ScriptRole sr : travelers) {
                if (normalizeRoleId(sr.getId()).equals(normalizedId)) {
                    return Optional.of(sr);
                }
            }
        }

        // Check fabled (already ScriptRole)
        if (fabled != null) {
            for (ScriptRole sr : fabled) {
                if (normalizeRoleId(sr.getId()).equals(normalizedId)) {
                    return Optional.of(sr);
                }
            }
        }

        // Check loric (already ScriptRole)
        if (loric != null) {
            for (ScriptRole sr : loric) {
                if (normalizeRoleId(sr.getId()).equals(normalizedId)) {
                    return Optional.of(sr);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Normalize a role ID for comparison (lowercase, no underscores/spaces/dashes).
     */
    private static String normalizeRoleId(String id) {
        return id.toLowerCase(Locale.ROOT).replaceAll("[_\\s-]", "");
    }

    /**
     * Find an official Role enum by normalized ID.
     */
    private static Role findRoleByNormalizedId(String id) {
        String normalizedId = normalizeRoleId(id);
        for (Role role : Role.values()) {
            if (normalizeRoleId(role.name()).equals(normalizedId)) {
                return role;
            }
        }
        return null;
    }

    /**
     * Add an official role to the appropriate list based on its type.
     * - Travelers go to the travelers list as ScriptRole.Official
     * - Fabled go to the fabled list as ScriptRole.Official
     * - Loric go to the loric list as ScriptRole.Official
     * - All others (townsfolk, outsider, minion, demon) go to the roles list
     */
    private static void addOfficialRoleToList(
            Role role,
            List<Role> officialRoles,
            List<ScriptRole> travelers,
            List<ScriptRole> fabled,
            List<ScriptRole> loric
    ) {
        switch (role.getType()) {
            case TRAVELER -> travelers.add(new ScriptRole.Official(role));
            case FABLED -> fabled.add(new ScriptRole.Official(role));
            case LORIC -> loric.add(new ScriptRole.Official(role));
            default -> officialRoles.add(role);
        }
    }

    /**
     * Parses a JSON string from the clipboard into a Script object.
     * Supports both old format (meta map + role strings) and new format (array of objects with "id" fields).
     * Also handles custom role objects with full definitions.
     *
     * @param jsonString The raw JSON string.
     * @return An Optional containing the Script if parsing was successful, otherwise empty.
     */
    @SuppressWarnings("unchecked")
    public static Optional<Script> fromJson(String jsonString) {
        if (jsonString == null || exceedsNestingDepth(jsonString, MAX_JSON_DEPTH)) {
            return Optional.empty();
        }
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Object>>(){}.getType();
            List<Object> rawList = gson.fromJson(jsonString, type);

            String scriptName = "Unknown Script";
            String scriptAuthor = "Unknown Author";
            List<Role> officialRoles = new ArrayList<>();
            List<CustomRole> customRoles = new ArrayList<>();
            List<ScriptRole> fabledList = new ArrayList<>();
            List<ScriptRole> loricList = new ArrayList<>();
            List<ScriptRole> travelersList = new ArrayList<>();
            List<String> firstNightOrder = null;
            List<String> otherNightOrder = null;
            String logo = null;
            String almanac = null;
            List<String> extraAlmanacs = null;
            List<String> bootlegger = null;

            if (rawList == null || rawList.isEmpty()) return Optional.empty();

            for (Object obj : rawList) {
                if (obj instanceof String roleString) {
                    // Official role as bare string (old format)
                    Role role = findRoleByNormalizedId(roleString);
                    if (role != null && role != Role.NO_ROLE) {
                        // Route to the correct list based on role type
                        addOfficialRoleToList(role, officialRoles, travelersList, fabledList, loricList);
                    }

                } else if (obj instanceof Map) {
                    Map<String, Object> entry = (Map<String, Object>) obj;
                    String id = (String) entry.get("id");

                    if (id == null) continue;

                    if ("_meta".equals(id)) {
                        // Metadata entry
                        scriptName = (String) entry.getOrDefault("name", scriptName);
                        scriptAuthor = (String) entry.getOrDefault("author", scriptAuthor);

                        // Night order arrays from script
                        Object fnOrder = entry.get("firstNight");
                        if (fnOrder instanceof List) {
                            firstNightOrder = new ArrayList<>((List<String>) fnOrder);
                        }
                        Object onOrder = entry.get("otherNight");
                        if (onOrder instanceof List) {
                            otherNightOrder = new ArrayList<>((List<String>) onOrder);
                        }

                        // Logo URL
                        logo = (String) entry.get("logo");

                        // Almanac URL
                        almanac = (String) entry.get("almanac");

                        // Extra almanac URLs
                        Object extraAlmanacsObj = entry.get("extraAlmanacs");
                        if (extraAlmanacsObj instanceof List) {
                            extraAlmanacs = new ArrayList<>((List<String>) extraAlmanacsObj);
                            // Every client fetches each of these in parallel; keep the fan-out sane
                            if (extraAlmanacs.size() > MAX_EXTRA_ALMANACS) {
                                extraAlmanacs = new ArrayList<>(extraAlmanacs.subList(0, MAX_EXTRA_ALMANACS));
                            }
                        }

                        // Bootlegger special rules: an array of strings, or a single string
                        Object bootleggerObj = entry.get("bootlegger");
                        if (bootleggerObj instanceof List<?> rules) {
                            bootlegger = new ArrayList<>();
                            for (Object rule : rules) {
                                if (rule instanceof String s && !s.isBlank()) {
                                    bootlegger.add(s);
                                }
                            }
                        } else if (bootleggerObj instanceof String s && !s.isBlank()) {
                            bootlegger = new ArrayList<>(List.of(s));
                        }

                    } else if (entry.containsKey("team") || entry.containsKey("ability")) {
                        // This is a custom role/character definition (has team or ability)
                        // BUT first check if this ID matches an official role - if so, use the official version
                        Role officialRole = findRoleByNormalizedId(id);
                        if (officialRole != null && officialRole != Role.NO_ROLE) {
                            // It's an official role, even though it has team/ability fields
                            addOfficialRoleToList(officialRole, officialRoles, travelersList, fabledList, loricList);
                        } else {
                            // Truly a custom role/character
                            String team = (String) entry.getOrDefault("team", "townsfolk");

                            if (team.equalsIgnoreCase("fabled")) {
                                // Parse as Fabled character, wrap as ScriptRole.Fabled
                                NonPlayerCharacter fabled = NonPlayerCharacter.fromJsonMap(entry, NonPlayerCharacter.FabledType.FABLED);
                                fabledList.add(new ScriptRole.Fabled(fabled));
                            } else if (team.equalsIgnoreCase("loric")) {
                                // Parse as Loric character, wrap as ScriptRole.Fabled
                                NonPlayerCharacter loric = NonPlayerCharacter.fromJsonMap(entry, NonPlayerCharacter.FabledType.LORIC);
                                loricList.add(new ScriptRole.Fabled(loric));
                            } else if (team.equalsIgnoreCase("traveller") || team.equalsIgnoreCase("traveler")) {
                                // Parse as Traveler (CustomRole with TRAVELER type), wrap as ScriptRole.Custom
                                CustomRole traveler = CustomRole.fromJsonMap(entry);
                                travelersList.add(new ScriptRole.Custom(traveler));
                            } else {
                                // Regular custom role
                                CustomRole cr = CustomRole.fromJsonMap(entry);
                                customRoles.add(cr);
                            }
                        }

                    } else {
                        // Official role as object {"id": "washerwoman"}
                        Role role = findRoleByNormalizedId(id);
                        if (role != null && role != Role.NO_ROLE) {
                            // Route to the correct list based on role type
                            addOfficialRoleToList(role, officialRoles, travelersList, fabledList, loricList);
                        }
                    }
                }
            }

            if (officialRoles.isEmpty() && customRoles.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new Script(
                scriptName,
                scriptAuthor,
                officialRoles,
                customRoles,
                fabledList,
                loricList,
                travelersList,
                firstNightOrder,
                otherNightOrder,
                logo,
                almanac,
                extraAlmanacs,
                bootlegger,
                jsonString
            ));

        } catch (JsonSyntaxException | ClassCastException e) {
            return Optional.empty();
        }
    }

    /**
     * Check if this script contains any custom roles.
     */
    public boolean hasCustomRoles() {
        return customRoles != null && !customRoles.isEmpty();
    }

    /**
     * Check if this script contains any custom roles or travelers.
     */
    public boolean hasCustomRolesOrTravelers() {
        if (customRoles != null && !customRoles.isEmpty()) return true;
        return travelers != null && !travelers.isEmpty();
    }

    /**
     * Get all custom roles, including custom travelers.
     * Use this for night order building and other places where travelers should be treated like custom roles.
     * Note: Only includes CUSTOM travelers (not official ones) since those have night order in NightOrder.java.
     */
    public List<CustomRole> allCustomRoles() {
        List<CustomRole> all = new ArrayList<>();
        if (customRoles != null) {
            all.addAll(customRoles);
        }
        if (travelers != null) {
            for (ScriptRole sr : travelers) {
                if (sr instanceof ScriptRole.Custom custom) {
                    all.add(custom.customRole());
                }
            }
        }
        return all;
    }

    /** True if this script carries the given fabled or loric, by id. */
    public boolean hasFabledOrLoric(String id) {
        return getFabledOrLoric(id).isPresent();
    }

    /**
     * Check if this script has a logo.
     */
    public boolean hasLogo() {
        return logo != null && !logo.isEmpty();
    }

    /**
     * Check if this script declares Bootlegger special rules in its _meta.
     */
    public boolean hasBootleggerRules() {
        return bootlegger != null && !bootlegger.isEmpty();
    }

    /**
     * Check if this script declares an almanac of its own. Only the main slot counts: that's the
     * one carrying script-level synopsis, overview and changelog, so it's what gates the script
     * details screen.
     */
    public boolean hasAlmanac() {
        return almanac != null && !almanac.isEmpty();
    }

    /**
     * Check if any almanac page is reachable from this script, main or extra. Extras carry role
     * data only, and a script assembled from borrowed homebrew has extras but no almanac of its own,
     * and its characters still need their pages fetched.
     */
    public boolean hasAnyAlmanac() {
        return hasAlmanac() || (extraAlmanacs != null && !extraAlmanacs.isEmpty());
    }

    /**
     * Check if this script has any fabled characters.
     */
    public boolean hasFabled() {
        return fabled != null && !fabled.isEmpty();
    }

    /**
     * Check if this script has any loric characters.
     */
    public boolean hasLoric() {
        return loric != null && !loric.isEmpty();
    }

    /**
     * Check if this script has any fabled or loric characters.
     */
    public boolean hasFabledOrLoric() {
        return hasFabled() || hasLoric();
    }

    /**
     * Check if this script has any travelers.
     */
    public boolean hasTravelers() {
        return travelers != null && !travelers.isEmpty();
    }

    /**
     * Get all fabled and loric characters combined as ScriptRole.
     */
    public List<ScriptRole> allFabledAndLoric() {
        List<ScriptRole> all = new ArrayList<>();
        if (fabled != null) all.addAll(fabled);
        if (loric != null) all.addAll(loric);
        return all;
    }

    /**
     * Get a fabled/loric character by ID.
     * Returns as ScriptRole (either Official or Fabled).
     */
    public Optional<ScriptRole> getFabledOrLoric(String id) {
        String normalizedId = normalizeRoleId(id);
        if (fabled != null) {
            for (ScriptRole sr : fabled) {
                if (normalizeRoleId(sr.getId()).equals(normalizedId)) {
                    return Optional.of(sr);
                }
            }
        }
        if (loric != null) {
            for (ScriptRole sr : loric) {
                if (normalizeRoleId(sr.getId()).equals(normalizedId)) {
                    return Optional.of(sr);
                }
            }
        }
        return Optional.empty();
    }
}
