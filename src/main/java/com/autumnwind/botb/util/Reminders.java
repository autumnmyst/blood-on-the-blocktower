package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;

/**
 * Reminder text is a reminder's identity: it is what gets compared, synced, and saved.
 * The strings here are the ones game logic looks for, so nothing else spells them out.
 * Display goes through {@link #display}, which translates known text and shows anything
 * else, such as hand-typed or script-defined reminders, as written.
 */
public final class Reminders {

    private Reminders() {}

    public static final String HAS_ABILITY = "Has Ability";
    public static final String NO_ABILITY = "No Ability";
    public static final String STORYTELLER_ABILITY = "Storyteller Ability";
    public static final String DIED_TODAY = "Died Today";
    public static final String DEAD = "Dead";
    public static final String IS_THE_DEMON = "Is The Demon";
    public static final String ONCE = "Once";
    public static final String CHOSEN = "Chosen";
    public static final String TWIN = "Twin";
    public static final String RED_HERRING = "Red Herring";
    public static final String GRANDCHILD = "Grandchild";
    public static final String SEEN = "Seen";
    public static final String KNOWN = "Known";
    public static final String MAD = "Mad";
    public static final String TOWNSFOLK = "Townsfolk";
    public static final String OUTSIDER = "Outsider";
    public static final String MINION = "Minion";
    public static final String GOOD = "Good";
    public static final String EVIL = "Evil";
    public static final String DRUNK = "Drunk";
    public static final String POISONED = "Poisoned";
    public static final String EVERYONE_IS_DRUNK = "Everyone Is Drunk";
    public static final String VORTOX = "Vortox";
    public static final String VORTOX_EFFECT = "Vortox Effect";
    public static final String X = "X";
    public static final String DOESNT_KILL = "Doesn't Kill";
    public static final String FINAL_NIGHT_NO_ATTACK = "Final Night: No Attack";
    public static final String RIOT = "Riot";
    public static final String ORGAN_GRINDER = "Organ Grinder";
    public static final String BISHOP = "Bishop";
    public static final String BUDDHIST = "Buddhist";
    public static final String LEGION = "Legion";
    public static final String UG_HAT = "Ug hat";
    public static final String THREE_VOTES = "3 Votes";
    public static final String NEGATIVE_VOTE = "Negative Vote";
    public static final String MAY_NOT_NOMINATE = "May Not Nominate";
    public static final String ONE = "1";
    public static final String TWO = "2";
    public static final String THREE = "3";

    private static final String MAD_PREFIX = "Mad: ";
    private static final String STORYTELLER_MINION_PREFIX = "ST: ";
    private static final Pattern NIGHT = Pattern.compile("Night (\\d+)");

    private static final String KEY_PREFIX = "reminder." + BloodOnTheBlocktower.MOD_ID + ".";
    private static final String MAD_ROLE_KEY = KEY_PREFIX + "mad_role";
    private static final String STORYTELLER_MINION_KEY = KEY_PREFIX + "storyteller_minion";
    private static final String NIGHT_KEY = KEY_PREFIX + "night";

    /** An associated role marker: the role's own name in caps, granting that role's ability. */
    public static String roleMarker(Role role) {
        return role.name().replace('_', ' ');
    }

    public static boolean isRoleMarker(String text, Role role) {
        return text.trim().equals(roleMarker(role));
    }

    /** Cerenovus madness about a role. */
    public static String mad(Role role) {
        return MAD_PREFIX + role.getDisplayName();
    }

    public static boolean isMad(String text) {
        return text.startsWith(MAD_PREFIX);
    }

    /** Plague Doctor's storyteller-held minion. */
    public static String storytellerMinion(Role role) {
        return STORYTELLER_MINION_PREFIX + role.getDisplayName();
    }

    public static boolean isStorytellerMinion(String text) {
        return text.startsWith(STORYTELLER_MINION_PREFIX);
    }

    /** Whether the reminder's own name has a space, ignoring a Mad: or ST: prefix. */
    public static boolean isMultiWord(String text) {
        String name = isMad(text) ? text.substring(MAD_PREFIX.length())
                : isStorytellerMinion(text) ? text.substring(STORYTELLER_MINION_PREFIX.length()) : text;
        return name.trim().contains(" ");
    }

    /** The Xaan's night. */
    public static String night(int night) {
        return "Night " + night;
    }

    public static boolean isNight(String text) {
        return NIGHT.matcher(text).matches();
    }

    /** The N of a "Night N" reminder, or -1. */
    public static int nightNumber(String text) {
        Matcher m = NIGHT.matcher(text);
        return m.matches() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** Translated when the text is a known reminder, otherwise shown as written. */
    public static Component display(String text, Optional<Role> role) {
        // Hand-typed caps are an identity claim and stay as typed, even when they spell a catalog word
        if (role.isEmpty() && text.equals(text.toUpperCase(Locale.ROOT))) return Component.literal(text);
        if (role.isPresent()) {
            Component roleName = Component.literal(role.get().getDisplayName());
            if (isRoleMarker(text, role.get())) return roleName;
            if (isMad(text)) return displayMad(roleName);
            if (isStorytellerMinion(text)) return Component.translatable(STORYTELLER_MINION_KEY, roleName);
        }
        int night = nightNumber(text);
        if (night >= 0) return Component.translatable(NIGHT_KEY, night);
        String key = key(text);
        if (key.isEmpty()) return Component.literal(text);
        return Component.translatableWithFallback(KEY_PREFIX + key, text);
    }

    public static Component displayMad(Component roleName) {
        return Component.translatable(MAD_ROLE_KEY, roleName);
    }

    /** Lang key fragment for reminder text: "Doesn't Kill" becomes doesnt_kill. */
    public static String key(String text) {
        String slug = text.toLowerCase(Locale.ROOT).replace("'", "").replaceAll("[^a-z0-9]+", "_");
        return slug.replaceAll("^_|_$", "");
    }
}
