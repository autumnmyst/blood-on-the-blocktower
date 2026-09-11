package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents a single reminder token.
 * @param text The text to display on hover.
 * @param role The role this reminder is associated with (for the icon).
 * If empty, this is a custom reminder or a custom role reminder.
 * @param customRoleId The custom role ID this reminder is associated with (for custom role reminders).
 * If empty, this is an official role reminder.
 * @param playerUuid The player UUID for player reminders (for Harpy madness).
 * If present, this is a player reminder.
 */
public record Reminder(String text, Optional<Role> role, Optional<String> customRoleId, Optional<UUID> playerUuid) {

    // Convenience constructor for official role reminders
    public Reminder(String text, Optional<Role> role) {
        this(text, role, Optional.empty(), Optional.empty());
    }

    // Official role reminder attached to a player.
    public Reminder(String text, Optional<Role> role, Optional<UUID> playerUuid) {
        this(text, role, Optional.empty(), playerUuid);
    }

    // Factory for custom role reminders
    public static Reminder forCustomRole(String customRoleId, String text) {
        return new Reminder(text, Optional.empty(), Optional.of(customRoleId), Optional.empty());
    }

    // Factory for fabled reminders (stored with "fabled:" prefix)
    public static Reminder forFabled(String fabledId, String text) {
        return new Reminder(text, Optional.empty(), Optional.of("fabled:" + fabledId), Optional.empty());
    }

    // Check if this is a fabled reminder
    public boolean isFabled() {
        return customRoleId.isPresent() && customRoleId.get().startsWith("fabled:");
    }

    /**
     * The icon used for custom reminders (when role is empty).
     */
    public static final Identifier CUSTOM_ICON = Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/icons/reminder_custom.png");
    public static final Identifier DRUNK_ICON = Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/drunk.png");
    public static final Identifier POISONED_ICON = Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/icons/poisoned.png");
    public static final Identifier VORTOX_ICON = Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/vortox.png");
    public static final Identifier PLAYER_REMINDER_ICON = Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/icons/reminder_custom.png"); // Placeholder, actual rendering uses player head
    public static final Identifier GOOD_ICON = Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/icons/good.png");
    public static final Identifier EVIL_ICON = Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/icons/evil.png");

    /**
     * Checks if this is a player reminder (for Harpy madness).
     */
    public boolean isPlayerReminder() {
        return playerUuid.isPresent();
    }

    /**
     * Checks if this is a custom role reminder.
     */
    public boolean isCustomRoleReminder() {
        return customRoleId.isPresent();
    }

    /**
     * Checks if this is a mad role reminder (for Cerenovus madness).
     */
    public boolean isMadRoleReminder() {
        return text.startsWith("Mad: ") && role.isPresent();
    }

    /**
     * Checks if this is a storyteller-minion reminder (for Plague Doctor).
     */
    public boolean isStorytellerMinionReminder() {
        return text.startsWith("ST: ") && role.isPresent() && role.get().getType() == RoleType.MINION;
    }

    /**
     * Gets the icon for this reminder.
     * @return The associated role's icon, or the CUSTOM_ICON if it's a custom reminder.
     * Note: For player reminders, this returns a placeholder. Actual rendering should use player head texture.
     */
    public Identifier getIcon() {
        // Player reminder - return placeholder (should be rendered with player head)
        if (isPlayerReminder()) {
            return PLAYER_REMINDER_ICON;
        }

        // If role is present (and not NO_ROLE), use the role's icon (prioritize over Good/Evil thumbs)
        // This allows "Good" and "Evil" reminders with an associated role to show that role's icon
        // NO_ROLE is excluded so global Good/Evil reminders fall through to text-based icon checks
        if (role.isPresent() && role.get() != Role.NO_ROLE) {
            // Vortox icon is special case
            if (role.get() == Role.VORTOX && text.equals("Vortox")) {
                return VORTOX_ICON;
            }
            return role.get().getIcon();
        }

        // Custom role reminder - get icon from URL loader
        if (customRoleId.isPresent()) {
            return UrlTextureLoader.getTextureByCustomRoleId(customRoleId.get());
        }

        // Good/Evil alignment reminders (only if no associated role)
        if (text.equals("Good")) {
            return GOOD_ICON;
        }
        if (text.equals("Evil")) {
            return EVIL_ICON;
        }

        // If role is empty but text is exactly "Drunk" or "Poisoned" (for NightOrderHUD icons)
        if (text.equals("Drunk")) {
            return DRUNK_ICON;
        }
        if (text.equals("Poisoned")) {
            return POISONED_ICON;
        }
        return CUSTOM_ICON;
    }

    /**
     * Gets the RoleType for this reminder.
     * Works for both official roles and custom roles.
     * @param script The current script (needed to look up custom roles)
     * @return The RoleType, or NONE if not determinable
     */
    public RoleType getRoleType(Script script) {
        if (role.isPresent()) {
            return role.get().getType();
        }
        if (customRoleId.isPresent() && script != null) {
            return script.getCustomRole(customRoleId.get())
                    .map(CustomRole::team)
                    .orElse(RoleType.NONE);
        }
        return RoleType.NONE;
    }

    /**
     * Checks if this reminder represents a demon role (official or custom).
     * @param script The current script (needed to look up custom roles)
     */
    public boolean isDemonReminder(Script script) {
        return getRoleType(script) == RoleType.DEMON;
    }

    /**
     * Checks if this is an AssociatedRoleReminder - a reminder whose text is the
     * associated role's ALL CAPS name (e.g. text="EMPATH" with role=EMPATH, or
     * text="CARTOGRAPHER" with a custom Cartographer role). These get a colored
     * border in the UI and grant the player visits/abilities for that role.
     *
     * Reminders that merely carry a role's icon (e.g. "Is The Drunk" with role=DRUNK,
     * "Chosen" with role=LUNATIC) are NOT associated role reminders.
     *
     * Only ability-granting role types qualify: Townsfolk, Outsider, Minion, Demon, Traveler.
     *
     * @param script The current script (needed to look up custom role display names)
     */
    public boolean isAssociatedRoleReminder(Script script) {
        if (role.isPresent() && role.get() != Role.NO_ROLE) {
            Role r = role.get();
            RoleType type = r.getType();
            if (type != RoleType.TOWNSFOLK && type != RoleType.OUTSIDER &&
                    type != RoleType.MINION && type != RoleType.DEMON &&
                    type != RoleType.TRAVELER) {
                return false;
            }
            return text.trim().equals(r.name().replace('_', ' '));
        }
        if (customRoleId.isPresent() && script != null) {
            return script.getCustomRole(customRoleId.get())
                    .map(cr -> text.trim().equals(cr.getDisplayName()))
                    .orElse(false);
        }
        return false;
    }

    /**
     * Gets the alignment color for this reminder's border.
     * Used in the HUD and ReminderChooseScreen.
     */
    public int getAlignmentColor() {
        return getAlignmentColor(null);
    }

    /**
     * Gets the alignment color for this reminder's border with script lookup for custom roles.
     * @param script The current script (needed to look up custom role teams)
     */
    public int getAlignmentColor(Script script) {
        // Player reminders get minion color border
        if (isPlayerReminder()) {
            return RoleType.MINION.getColor();
        }

        if (role.isPresent()) {
            // The global effects do not get borders
            boolean isVortoxEffect = role.get() == Role.VORTOX && text.equals("Vortox Effect");
            boolean isXaanEffect = role.get() == Role.XAAN && text.equals("X");
            if (isVortoxEffect || isXaanEffect) {
                return RoleType.NONE.getColor();
            }
            return role.get().getType().getColor();
        }

        // Custom role reminder - get team color from script
        if (customRoleId.isPresent() && script != null) {
            return script.getCustomRole(customRoleId.get())
                    .map(cr -> cr.team().getColor())
                    .orElse(RoleType.NONE.getColor());
        }

        // Special colors for "Good" and "Evil" reminders
        if (text.equals("Good")) {
            return RoleType.TOWNSFOLK.getColor(); // Townsfolk blue
        }
        if (text.equals("Evil")) {
            return RoleType.MINION.getColor(); // Minion red
        }
        // Text-based reminders ("Drunk", "Poisoned") have no alignment color
        return RoleType.NONE.getColor();
    }

    // Packet codec for network transmission
    public static final PacketCodec<RegistryByteBuf, Reminder> PACKET_CODEC = new PacketCodec<>() {
        @Override
        public Reminder decode(RegistryByteBuf buf) {
            String text = PacketCodecs.STRING.decode(buf);
            boolean hasRole = PacketCodecs.BOOL.decode(buf);
            Optional<Role> role = hasRole ? Optional.of(Role.PACKET_CODEC.decode(buf)) : Optional.empty();
            boolean hasCustomRoleId = PacketCodecs.BOOL.decode(buf);
            Optional<String> customRoleId = hasCustomRoleId ? Optional.of(PacketCodecs.STRING.decode(buf)) : Optional.empty();
            boolean hasPlayerUuid = PacketCodecs.BOOL.decode(buf);
            Optional<UUID> playerUuid = hasPlayerUuid ? Optional.of(Uuids.PACKET_CODEC.decode(buf)) : Optional.empty();
            return new Reminder(text, role, customRoleId, playerUuid);
        }

        @Override
        public void encode(RegistryByteBuf buf, Reminder reminder) {
            PacketCodecs.STRING.encode(buf, reminder.text());
            PacketCodecs.BOOL.encode(buf, reminder.role().isPresent());
            if (reminder.role().isPresent()) {
                Role.PACKET_CODEC.encode(buf, reminder.role().get());
            }
            PacketCodecs.BOOL.encode(buf, reminder.customRoleId().isPresent());
            if (reminder.customRoleId().isPresent()) {
                PacketCodecs.STRING.encode(buf, reminder.customRoleId().get());
            }
            PacketCodecs.BOOL.encode(buf, reminder.playerUuid().isPresent());
            if (reminder.playerUuid().isPresent()) {
                Uuids.PACKET_CODEC.encode(buf, reminder.playerUuid().get());
            }
        }
    };
}
