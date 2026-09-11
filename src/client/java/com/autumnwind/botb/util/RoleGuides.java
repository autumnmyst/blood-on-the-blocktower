package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.minecraft.util.Language;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * How to run every role the mod special-cases. Only roles listed here appear in the Role
 * Guides screen. The text is in the lang file, paragraphs separated by newlines.
 */
public final class RoleGuides {

    private RoleGuides() {}

    private static final Set<Role> GUIDES = EnumSet.noneOf(Role.class);

    private static void guide(Role role) {
        GUIDES.add(role);
    }

    static {
        // Townsfolk
        guide(Role.ALCHEMIST);
        guide(Role.AMNESIAC);
        guide(Role.ATHEIST);
        guide(Role.BANSHEE);
        guide(Role.BOUNTY_HUNTER);
        guide(Role.CANNIBAL);
        guide(Role.CHOIRBOY);
        guide(Role.EXORCIST);
        guide(Role.FARMER);
        guide(Role.FORTUNE_TELLER);
        guide(Role.GRANDMOTHER);
        guide(Role.INVESTIGATOR);
        guide(Role.JUGGLER);
        guide(Role.KING);
        guide(Role.KNIGHT);
        guide(Role.MINSTREL);
        guide(Role.LIBRARIAN);
        guide(Role.MAGICIAN);
        guide(Role.NOBLE);
        guide(Role.PHILOSOPHER);
        guide(Role.PIXIE);
        guide(Role.POPPY_GROWER);
        guide(Role.PREACHER);
        guide(Role.RAVENKEEPER);
        guide(Role.SAGE);
        guide(Role.SNITCH);
        guide(Role.STEWARD);
        guide(Role.UNDERTAKER);
        guide(Role.WASHERWOMAN);

        // Outsiders
        guide(Role.BARBER);
        guide(Role.DAMSEL);
        guide(Role.DRUNK);
        guide(Role.HATTER);
        guide(Role.HERMIT);
        guide(Role.LUNATIC);
        guide(Role.MUTANT);
        guide(Role.OGRE);
        guide(Role.PLAGUE_DOCTOR);
        guide(Role.SWEETHEART);

        // Minions
        guide(Role.BOFFIN);
        guide(Role.CERENOVUS);
        guide(Role.EVIL_TWIN);
        guide(Role.GODFATHER);
        guide(Role.HARPY);
        guide(Role.MARIONETTE);
        guide(Role.MEZEPHELES);
        guide(Role.PRINCESS);
        guide(Role.ORGAN_GRINDER);
        guide(Role.SCARLET_WOMAN);
        guide(Role.SUMMONER);
        guide(Role.VIZIER);
        guide(Role.WITCH);
        guide(Role.WIZARD);
        guide(Role.WRAITH);
        guide(Role.XAAN);

        // Demons
        guide(Role.AL_HADIKHIA);
        guide(Role.FANG_GU);
        guide(Role.LEGION);
        guide(Role.LEVIATHAN);
        guide(Role.LIL_MONSTA);
        guide(Role.LORD_OF_TYPHON);
        guide(Role.RIOT);
        guide(Role.VIGORMORTIS);
        guide(Role.VORTOX);
        guide(Role.ZOMBUUL);

        // Travelers
        guide(Role.BEGGAR);
        guide(Role.BISHOP);
        guide(Role.BONE_COLLECTOR);
        guide(Role.BUREAUCRAT);
        guide(Role.BUTCHER);
        guide(Role.THIEF);
        guide(Role.VOUDON);

        // Fabled
        guide(Role.BOOTLEGGER);
        guide(Role.BUDDHIST);
        guide(Role.DJINN);
        guide(Role.DUCHESS);
        guide(Role.SENTINEL);
        guide(Role.SPIRIT_OF_IVORY);
        guide(Role.STORM_CATCHER);
        guide(Role.TOYMAKER);

        // Loric
        guide(Role.GOD_OF_UG);
        guide(Role.POPE);
        guide(Role.TOR);
    }

    /** Roles with a guide, grouped by team and then alphabetically. */
    public static List<Role> roles() {
        List<Role> roles = new ArrayList<>(GUIDES);
        roles.sort(Comparator.comparing((Role r) -> r.getType().ordinal()).thenComparing(Role::getDisplayName));
        return roles;
    }

    public static boolean has(Role role) {
        return GUIDES.contains(role);
    }

    public static String get(Role role) {
        return Language.getInstance().get("guide." + BloodOnTheBlocktower.MOD_ID + "." + role.getId());
    }
}
