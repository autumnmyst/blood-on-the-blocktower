package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Represents a single reminder token.
 * @param text The reminder's identity, see {@link Reminders}. Shown through {@link #displayText()}.
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

    public Component displayText() {
        return Reminders.display(text, role);
    }

    /**
     * The icon used for custom reminders (when role is empty).
     */
    public static final ResourceLocation CUSTOM_ICON = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/reminder_custom.png");
    public static final ResourceLocation DRUNK_ICON = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/drunk.png");
    public static final ResourceLocation POISONED_ICON = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/poisoned.png");
    public static final ResourceLocation VORTOX_ICON = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/vortox.png");
    public static final ResourceLocation PLAYER_REMINDER_ICON = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/reminder_custom.png"); // Placeholder, actual rendering uses player head
    public static final ResourceLocation GOOD_ICON = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/good.png");
    public static final ResourceLocation EVIL_ICON = ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/icons/evil.png");

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
        return Reminders.isMad(text) && role.isPresent();
    }

    /**
     * Checks if this is a storyteller-minion reminder (for Plague Doctor).
     */
    public boolean isStorytellerMinionReminder() {
        return Reminders.isStorytellerMinion(text) && role.isPresent() && role.get().getType() == RoleType.MINION;
    }

    /**
     * Gets the icon for this reminder.
     * @return The associated role's icon, or the CUSTOM_ICON if it's a custom reminder.
     * Note: For player reminders, this returns a placeholder. Actual rendering should use player head texture.
     */
    public ResourceLocation getIcon() {
        // Player reminder - return placeholder (should be rendered with player head)
        if (isPlayerReminder()) {
            return PLAYER_REMINDER_ICON;
        }

        // If role is present (and not NO_ROLE), use the role's icon (prioritize over Good/Evil thumbs)
        // This allows "Good" and "Evil" reminders with an associated role to show that role's icon
        // NO_ROLE is excluded so global Good/Evil reminders fall through to text-based icon checks
        if (role.isPresent() && role.get() != Role.NO_ROLE) {
            // Vortox icon is special case
            if (role.get() == Role.VORTOX && text.equals(Reminders.VORTOX)) {
                return VORTOX_ICON;
            }
            return role.get().getIcon();
        }

        // Custom role reminder - get icon from URL loader
        if (customRoleId.isPresent()) {
            return UrlTextureLoader.getTextureByCustomRoleId(customRoleId.get());
        }

        // Good/Evil alignment reminders (only if no associated role)
        if (text.equals(Reminders.GOOD)) {
            return GOOD_ICON;
        }
        if (text.equals(Reminders.EVIL)) {
            return EVIL_ICON;
        }

        // If role is empty but text is exactly "Drunk" or "Poisoned" (for NightOrderHUD icons)
        if (text.equals(Reminders.DRUNK)) {
            return DRUNK_ICON;
        }
        if (text.equals(Reminders.POISONED)) {
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
            return Reminders.isRoleMarker(text, r);
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
            boolean isVortoxEffect = role.get() == Role.VORTOX && text.equals(Reminders.VORTOX_EFFECT);
            boolean isXaanEffect = role.get() == Role.XAAN && text.equals(Reminders.X);
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
        if (text.equals(Reminders.GOOD)) {
            return RoleType.TOWNSFOLK.getColor(); // Townsfolk blue
        }
        if (text.equals(Reminders.EVIL)) {
            return RoleType.MINION.getColor(); // Minion red
        }
        // Text-based reminders ("Drunk", "Poisoned") have no alignment color
        return RoleType.NONE.getColor();
    }

    // Packet codec for network transmission
    public static final StreamCodec<RegistryFriendlyByteBuf, Reminder> PACKET_CODEC = new StreamCodec<>() {
        @Override
        public Reminder decode(RegistryFriendlyByteBuf buf) {
            String text = ByteBufCodecs.STRING_UTF8.decode(buf);
            boolean hasRole = ByteBufCodecs.BOOL.decode(buf);
            Optional<Role> role = hasRole ? Optional.of(Role.PACKET_CODEC.decode(buf)) : Optional.empty();
            boolean hasCustomRoleId = ByteBufCodecs.BOOL.decode(buf);
            Optional<String> customRoleId = hasCustomRoleId ? Optional.of(ByteBufCodecs.STRING_UTF8.decode(buf)) : Optional.empty();
            boolean hasPlayerUuid = ByteBufCodecs.BOOL.decode(buf);
            Optional<UUID> playerUuid = hasPlayerUuid ? Optional.of(UUIDUtil.STREAM_CODEC.decode(buf)) : Optional.empty();
            return new Reminder(text, role, customRoleId, playerUuid);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, Reminder reminder) {
            ByteBufCodecs.STRING_UTF8.encode(buf, reminder.text());
            ByteBufCodecs.BOOL.encode(buf, reminder.role().isPresent());
            if (reminder.role().isPresent()) {
                Role.PACKET_CODEC.encode(buf, reminder.role().get());
            }
            ByteBufCodecs.BOOL.encode(buf, reminder.customRoleId().isPresent());
            if (reminder.customRoleId().isPresent()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, reminder.customRoleId().get());
            }
            ByteBufCodecs.BOOL.encode(buf, reminder.playerUuid().isPresent());
            if (reminder.playerUuid().isPresent()) {
                UUIDUtil.STREAM_CODEC.encode(buf, reminder.playerUuid().get());
            }
        }
    };
}
