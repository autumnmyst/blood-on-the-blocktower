package com.autumnwind.botb.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a non-player character (Fabled or Loric) from a custom script.
 * These are special characters that affect the game but are not assigned to players.
 */
public record NonPlayerCharacter(
    String id,                          // e.g., "iamspartacus_fall_of_rome"
    String name,                        // e.g., "I Am Spartacus!"
    FabledType type,                    // FABLED or LORIC
    String ability,                     // The ability text
    String flavor,                      // Flavor text
    String imageUrl,                    // URL to character image
    List<String> reminders,             // Reminder tokens
    List<String> remindersGlobal        // Global reminder tokens (visible to all players)
) {
    /**
     * Type of special character.
     */
    public enum FabledType {
        FABLED,
        LORIC
    }

    /**
     * Parse a NonPlayerCharacter from a JSON map.
     */
    @SuppressWarnings("unchecked")
    public static NonPlayerCharacter fromJsonMap(Map<String, Object> map, FabledType type) {
        String id = (String) map.getOrDefault("id", "unknown");
        String name = (String) map.getOrDefault("name", id);
        String ability = (String) map.getOrDefault("ability", "");
        String flavor = (String) map.getOrDefault("flavor", "");

        // Parse image - can be string or array (use first if array)
        String imageUrl = "";
        Object imageObj = map.get("image");
        if (imageObj instanceof String) {
            imageUrl = (String) imageObj;
        } else if (imageObj instanceof List) {
            List<String> images = (List<String>) imageObj;
            if (!images.isEmpty()) {
                imageUrl = images.get(0);
            }
        }

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

        return new NonPlayerCharacter(id, name, type, ability, flavor, imageUrl, reminders, remindersGlobal);
    }

    /**
     * Returns the display name in uppercase for consistency.
     */
    public String getDisplayName() {
        return name.toUpperCase();
    }

    /**
     * Check if this is a Fabled character.
     */
    public boolean isFabled() {
        return type == FabledType.FABLED;
    }

    /**
     * Check if this is a Loric character.
     */
    public boolean isLoric() {
        return type == FabledType.LORIC;
    }

    /**
     * Get the RoleType for this character.
     */
    public RoleType getRoleType() {
        return type == FabledType.FABLED ? RoleType.FABLED : RoleType.LORIC;
    }
}
