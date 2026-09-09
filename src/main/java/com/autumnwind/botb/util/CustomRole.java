package com.autumnwind.botb.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Represents a custom (homebrew) role parsed from a script JSON.
 */
public record CustomRole(
    String id,                          // e.g., "cartographer"
    String name,                        // e.g., "Cartographer"
    RoleType team,                      // Parsed from "townsfolk", "outsider", etc.
    String ability,                     // The ability text
    String flavor,                      // Flavor text
    List<String> imageUrls,             // 1-3 URLs for alignment variants
    double firstNight,                  // 0 = doesn't wake, else priority
    double otherNight,                  // 0 = doesn't wake, else priority
    String firstNightReminder,          // Instructions for ST
    String otherNightReminder,          // Instructions for ST
    List<String> reminders,             // Local reminder tokens
    List<String> remindersGlobal,       // Global reminder tokens
    boolean setup,                      // Affects game setup
    List<Jinx> jinxes                   // Jinx definitions
) {
    /**
     * Represents a jinx between this role and another.
     */
    public record Jinx(String roleId, String reason) {
        @SuppressWarnings("unchecked")
        public static Jinx fromMap(Map<String, Object> map) {
            String id = (String) map.getOrDefault("id", "");
            String reason = (String) map.getOrDefault("reason", "");
            return new Jinx(id, reason);
        }
    }

    /**
     * Parse a CustomRole from a JSON map.
     */
    @SuppressWarnings("unchecked")
    public static CustomRole fromJsonMap(Map<String, Object> map) {
        String id = (String) map.getOrDefault("id", "unknown");
        String name = (String) map.getOrDefault("name", id);
        String teamStr = (String) map.getOrDefault("team", "townsfolk");
        RoleType team = parseTeam(teamStr);
        String ability = (String) map.getOrDefault("ability", "");
        String flavor = (String) map.getOrDefault("flavor", "");

        // Parse image(s) - can be string or array
        List<String> imageUrls = new ArrayList<>();
        Object imageObj = map.get("image");
        if (imageObj instanceof String) {
            imageUrls.add((String) imageObj);
        } else if (imageObj instanceof List) {
            imageUrls.addAll((List<String>) imageObj);
        }

        // Night order priorities
        double firstNight = parseDouble(map.get("firstNight"), 0);
        double otherNight = parseDouble(map.get("otherNight"), 0);

        // Night reminders
        String firstNightReminder = (String) map.getOrDefault("firstNightReminder", "");
        String otherNightReminder = (String) map.getOrDefault("otherNightReminder", "");

        // Reminders
        List<String> reminders = new ArrayList<>();
        Object remindersObj = map.get("reminders");
        if (remindersObj instanceof List) {
            reminders.addAll((List<String>) remindersObj);
        }

        List<String> remindersGlobal = new ArrayList<>();
        Object remindersGlobalObj = map.get("remindersGlobal");
        if (remindersGlobalObj instanceof List) {
            remindersGlobal.addAll((List<String>) remindersGlobalObj);
        }

        // Setup flag
        boolean setup = Boolean.TRUE.equals(map.get("setup"));

        // Jinxes
        List<Jinx> jinxes = new ArrayList<>();
        Object jinxesObj = map.get("jinxes");
        if (jinxesObj instanceof List) {
            for (Object jinxEntry : (List<Object>) jinxesObj) {
                if (jinxEntry instanceof Map) {
                    jinxes.add(Jinx.fromMap((Map<String, Object>) jinxEntry));
                }
            }
        }

        return new CustomRole(id, name, team, ability, flavor, imageUrls,
            firstNight, otherNight, firstNightReminder, otherNightReminder,
            reminders, remindersGlobal, setup, jinxes);
    }

    private static RoleType parseTeam(String team) {
        return switch (team.toLowerCase(Locale.ROOT)) {
            case "townsfolk" -> RoleType.TOWNSFOLK;
            case "outsider" -> RoleType.OUTSIDER;
            case "minion" -> RoleType.MINION;
            case "demon" -> RoleType.DEMON;
            case "traveller", "traveler" -> RoleType.TRAVELER;
            case "fabled" -> RoleType.FABLED;
            case "loric" -> RoleType.LORIC;
            default -> RoleType.TOWNSFOLK;
        };
    }

    private static double parseDouble(Object obj, double defaultValue) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Returns the display name in uppercase for consistency with official roles.
     */
    public String getDisplayName() {
        return name.toUpperCase(Locale.ROOT);
    }

    /**
     * Check if this role is good by default.
     */
    public boolean isDefaultGood() {
        return team.isDefaultGood();
    }

    /**
     * Get the appropriate image URL based on alignment.
     * @param isGood Whether the player is good-aligned
     * @return The URL for the appropriate image
     */
    public String getImageUrl(boolean isGood) {
        if (imageUrls.isEmpty()) return "";
        if (imageUrls.size() == 1) return imageUrls.get(0);
        if (imageUrls.size() == 2) return isGood ? imageUrls.get(0) : imageUrls.get(1);
        // 3 URLs: [neutral, good, evil]
        return isGood ? imageUrls.get(1) : imageUrls.get(2);
    }

    /**
     * Get the neutral/default image URL (for travellers or before alignment is known).
     */
    public String getNeutralImageUrl() {
        if (imageUrls.isEmpty()) return "";
        if (imageUrls.size() == 3) return imageUrls.get(0);
        return imageUrls.get(0);
    }

    /**
     * Check if this role wakes on the first night.
     */
    public boolean wakesFirstNight() {
        return firstNight > 0;
    }

    /**
     * Check if this role wakes on other nights.
     */
    public boolean wakesOtherNights() {
        return otherNight > 0;
    }
}
