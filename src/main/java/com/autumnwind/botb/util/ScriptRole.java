package com.autumnwind.botb.util;

import net.minecraft.util.Identifier;

/**
 * Sealed interface representing either an official Role enum, a CustomRole, or a NonPlayerCharacter.
 * This allows code to work with all role types uniformly.
 */
public sealed interface ScriptRole permits ScriptRole.Official, ScriptRole.Custom, ScriptRole.Fabled {

    /**
     * Get the role ID (lowercase, no spaces).
     */
    String getId();

    /**
     * Get the display name (uppercase with spaces).
     */
    String getDisplayName();

    /**
     * Get the role's team.
     */
    RoleType getTeam();

    /**
     * Get the role's ability text.
     */
    String getAbility();

    /**
     * Get the role icon texture identifier.
     */
    Identifier getIcon();

    /**
     * Check if this role is good by default.
     */
    boolean isDefaultGood();

    /**
     * Check if this is a custom role.
     */
    boolean isCustom();

    /**
     * Get the official Role enum if this is an official role.
     * @throws IllegalStateException if this is a custom role
     */
    default Role asRole() {
        if (this instanceof Official o) {
            return o.role();
        }
        throw new IllegalStateException("Cannot get Role from custom role: " + getId());
    }

    /**
     * Get the CustomRole if this is a custom role.
     * @throws IllegalStateException if this is an official role
     */
    default CustomRole asCustomRole() {
        if (this instanceof Custom c) {
            return c.customRole();
        }
        throw new IllegalStateException("Cannot get CustomRole from official role: " + getId());
    }

    /**
     * Wrapper for official Role enum values.
     */
    record Official(Role role) implements ScriptRole {
        @Override
        public String getId() {
            // Underscores are used for official role ids
            return role.name().toLowerCase().replace("_", "");
        }

        @Override
        public String getDisplayName() {
            return role.getDisplayName();
        }

        @Override
        public RoleType getTeam() {
            return role.getType();
        }

        @Override
        public String getAbility() {
            return role.getDescription();
        }

        @Override
        public Identifier getIcon() {
            return role.getIcon();
        }

        @Override
        public boolean isDefaultGood() {
            return role.isDefaultGood();
        }

        @Override
        public boolean isCustom() {
            return false;
        }
    }

    /**
     * Wrapper for custom homebrew roles.
     */
    record Custom(CustomRole customRole) implements ScriptRole {
        @Override
        public String getId() {
            return customRole.id();
        }

        @Override
        public String getDisplayName() {
            return customRole.getDisplayName();
        }

        @Override
        public RoleType getTeam() {
            return customRole.team();
        }

        @Override
        public String getAbility() {
            return customRole.ability();
        }

        @Override
        public Identifier getIcon() {
            // If this custom role has no image URLs, check if it's an official role wrapper
            // (Official travelers/fabled are wrapped as CustomRole for compatibility)
            if (customRole.imageUrls().isEmpty()) {
                // Try to find matching official role by ID
                for (Role role : Role.values()) {
                    String normalizedRoleId = role.name().toLowerCase().replace("_", "");
                    if (normalizedRoleId.equals(customRole.id())) {
                        return role.getIcon();
                    }
                }
            }
            return UrlTextureLoader.getTexture(customRole);
        }

        @Override
        public boolean isDefaultGood() {
            return customRole.isDefaultGood();
        }

        @Override
        public boolean isCustom() {
            return true;
        }
    }

    /**
     * Wrapper for Fabled and Loric characters (non-player characters).
     */
    record Fabled(NonPlayerCharacter fabledCharacter) implements ScriptRole {
        @Override
        public String getId() {
            return fabledCharacter.id();
        }

        @Override
        public String getDisplayName() {
            return fabledCharacter.getDisplayName();
        }

        @Override
        public RoleType getTeam() {
            return fabledCharacter.getRoleType();
        }

        @Override
        public String getAbility() {
            return fabledCharacter.ability();
        }

        @Override
        public Identifier getIcon() {
            // If this fabled has no image URL, check if it's an official fabled/loric wrapper
            String imageUrl = fabledCharacter.imageUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                // Try to find matching official role by ID
                for (Role role : Role.values()) {
                    String normalizedRoleId = role.name().toLowerCase().replace("_", "");
                    if (normalizedRoleId.equals(fabledCharacter.id())) {
                        return role.getIcon();
                    }
                }
            }
            return UrlTextureLoader.getTexture(imageUrl);
        }

        @Override
        public boolean isDefaultGood() {
            return true; // Fabled are neutral
        }

        @Override
        public boolean isCustom() {
            return true; // Treat as custom for display purposes
        }

        /**
         * Check if this is a fabled character (vs loric).
         */
        public boolean isFabled() {
            return fabledCharacter.isFabled();
        }

        /**
         * Check if this is a loric character.
         */
        public boolean isLoric() {
            return fabledCharacter.isLoric();
        }
    }
}
