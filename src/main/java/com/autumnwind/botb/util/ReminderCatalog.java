package com.autumnwind.botb.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;

/**
 * Holds static definitions for all available reminder tokens.
 * Supports both official roles and custom roles.
 */
public class ReminderCatalog {

    /**
     * Defines a reminder that can be selected in the list.
     * @param role The associated role (null for custom roles or fabled).
     * @param customRoleId The custom role ID (null for official roles). For fabled, prefix with "fabled:"
     * @param text The reminder text.
     * @param isGlobal If true, this reminder is available based on script, not just
     * if the role is actively in play.
     * @param playerUuid For player reminders (Harpy), the UUID of the target player.
     */
    public record ReminderDefinition(Role role, String customRoleId, String text, boolean isGlobal, Optional<UUID> playerUuid) {
        // Convenience constructor for official role reminders
        public ReminderDefinition(Role role, String text, boolean isGlobal) {
            this(role, null, text, isGlobal, Optional.empty());
        }

        // Convenience constructor for custom role reminders
        public ReminderDefinition(String customRoleId, String text, boolean isGlobal) {
            this(null, customRoleId, text, isGlobal, Optional.empty());
        }

        // Check if this is a custom role reminder
        public boolean isCustomRole() {
            return customRoleId != null && !customRoleId.startsWith("fabled:");
        }

        // Check if this is a fabled reminder
        public boolean isFabled() {
            return customRoleId != null && customRoleId.startsWith("fabled:");
        }

        // Get the fabled ID (without the "fabled:" prefix)
        public String fabledId() {
            if (isFabled()) {
                return customRoleId.substring(7); // Remove "fabled:" prefix
            }
            return null;
        }

        // Factory method for fabled reminders
        public static ReminderDefinition forFabled(String fabledId, String text, boolean isGlobal) {
            return new ReminderDefinition(null, "fabled:" + fabledId, text, isGlobal, Optional.empty());
        }

        public Component displayText() {
            return Reminders.display(text, Optional.ofNullable(role));
        }
    }

    private static final List<ReminderDefinition> DEFINITIONS = new ArrayList<>();
    private static final List<ReminderDefinition> DYNAMIC_DEFINITIONS = new ArrayList<>();

    /**
     * Initializes the static list of reminder definitions from the JSON data.
     */
    public static void init() {
        if (!DEFINITIONS.isEmpty()) return; // Only init once

        // Used to prevent duplicate reminders *per role*
        Set<String> remindersForThisRole = new HashSet<>();

        // --- WASHERWOMAN ---
        remindersForThisRole.clear();
        addReminder(Role.WASHERWOMAN, Reminders.TOWNSFOLK, false, remindersForThisRole);
        addReminder(Role.WASHERWOMAN, "Wrong", false, remindersForThisRole);

        // --- LIBRARIAN ---
        remindersForThisRole.clear();
        addReminder(Role.LIBRARIAN, Reminders.OUTSIDER, false, remindersForThisRole);
        addReminder(Role.LIBRARIAN, "Wrong", false, remindersForThisRole);

        // --- INVESTIGATOR ---
        remindersForThisRole.clear();
        addReminder(Role.INVESTIGATOR, Reminders.MINION, false, remindersForThisRole);
        addReminder(Role.INVESTIGATOR, "Wrong", false, remindersForThisRole);

        // --- CHEF --- (No reminders)
        // --- EMPATH --- (No reminders)

        // --- FORTUNE TELLER ---
        remindersForThisRole.clear();
        addReminder(Role.FORTUNE_TELLER, Reminders.RED_HERRING, false, remindersForThisRole);

        // --- UNDERTAKER ---
        remindersForThisRole.clear();
        addReminder(Role.UNDERTAKER, Reminders.DIED_TODAY, false, remindersForThisRole);

        // --- MONK ---
        remindersForThisRole.clear();
        addReminder(Role.MONK, "Safe", false, remindersForThisRole);

        // --- RAVENKEEPER --- (No reminders)

        // --- VIRGIN ---
        remindersForThisRole.clear();
        addReminder(Role.VIRGIN, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- SLAYER ---
        remindersForThisRole.clear();
        addReminder(Role.SLAYER, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- SOLDIER --- (No reminders)
        // --- MAYOR --- (No reminders)

        // --- BUTLER ---
        remindersForThisRole.clear();
        addReminder(Role.BUTLER, "Master", false, remindersForThisRole);

        // --- DRUNK ---
        remindersForThisRole.clear();
        addReminder(Role.DRUNK, "Is The Drunk", true, remindersForThisRole); // (Global)

        // --- RECLUSE --- (No reminders)
        // --- SAINT --- (No reminders)

        // --- POISONER ---
        remindersForThisRole.clear();
        addReminder(Role.POISONER, Reminders.POISONED, false, remindersForThisRole);

        // --- SPY --- (No reminders)

        // --- SCARLET WOMAN ---
        remindersForThisRole.clear();
        addReminder(Role.SCARLET_WOMAN, Reminders.IS_THE_DEMON, false, remindersForThisRole);

        // --- BARON --- (No reminders)

        // --- IMP ---
        remindersForThisRole.clear();
        addReminder(Role.IMP, Reminders.DEAD, false, remindersForThisRole);

        // --- GRANDMOTHER ---
        remindersForThisRole.clear();
        addReminder(Role.GRANDMOTHER, Reminders.GRANDCHILD, false, remindersForThisRole);
        addReminder(Role.GRANDMOTHER, Reminders.DEAD, false, remindersForThisRole);

        // --- SAILOR ---
        remindersForThisRole.clear();
        addReminder(Role.SAILOR, Reminders.DRUNK, false, remindersForThisRole);

        // --- CHAMBERMAID --- (No reminders)

        // --- EXORCIST ---
        remindersForThisRole.clear();
        addReminder(Role.EXORCIST, Reminders.CHOSEN, false, remindersForThisRole);

        // --- INNKEEPER ---
        remindersForThisRole.clear();
        addReminder(Role.INNKEEPER, "Safe", false, remindersForThisRole);
        // "Safe" is repeated, addReminder() will skip it
        addReminder(Role.INNKEEPER, Reminders.DRUNK, false, remindersForThisRole);

        // --- GAMBLER ---
        remindersForThisRole.clear();
        addReminder(Role.GAMBLER, Reminders.DEAD, false, remindersForThisRole);

        // --- GOSSIP ---
        remindersForThisRole.clear();
        addReminder(Role.GOSSIP, Reminders.DEAD, false, remindersForThisRole);

        // --- COURTIER ---
        remindersForThisRole.clear();
        addReminder(Role.COURTIER, "Drunk 1", false, remindersForThisRole);
        addReminder(Role.COURTIER, "Drunk 2", false, remindersForThisRole);
        addReminder(Role.COURTIER, "Drunk 3", false, remindersForThisRole);
        addReminder(Role.COURTIER, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- PROFESSOR ---
        remindersForThisRole.clear();
        addReminder(Role.PROFESSOR, "Alive", false, remindersForThisRole);
        addReminder(Role.PROFESSOR, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- MINSTREL ---
        remindersForThisRole.clear();
        addReminder(Role.MINSTREL, Reminders.EVERYONE_IS_DRUNK, false, remindersForThisRole);

        // --- TEA LADY ---
        remindersForThisRole.clear();
        addReminder(Role.TEA_LADY, "Cannot Die", false, remindersForThisRole);
        // "Cannot Die" is repeated, addReminder() will skip it

        // --- PACIFIST --- (No reminders)

        // --- FOOL ---
        remindersForThisRole.clear();
        addReminder(Role.FOOL, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- GOON ---
        remindersForThisRole.clear();
        addReminder(Role.GOON, Reminders.DRUNK, false, remindersForThisRole);

        // --- LUNATIC ---
        remindersForThisRole.clear();
        addReminder(Role.LUNATIC, Reminders.CHOSEN, false, remindersForThisRole);
        // "Chosen" is repeated, addReminder() will skip it

        // --- TINKER ---
        remindersForThisRole.clear();
        addReminder(Role.TINKER, Reminders.DEAD, false, remindersForThisRole);

        // --- MOONCHILD ---
        remindersForThisRole.clear();
        addReminder(Role.MOONCHILD, Reminders.DEAD, false, remindersForThisRole);

        // --- GODFATHER ---
        remindersForThisRole.clear();
        addReminder(Role.GODFATHER, Reminders.DIED_TODAY, false, remindersForThisRole);
        addReminder(Role.GODFATHER, Reminders.DEAD, false, remindersForThisRole);

        // --- DEVILS ADVOCATE ---
        remindersForThisRole.clear();
        addReminder(Role.DEVILS_ADVOCATE, "Survives Execution", false, remindersForThisRole);

        // --- ASSASSIN ---
        remindersForThisRole.clear();
        addReminder(Role.ASSASSIN, Reminders.DEAD, false, remindersForThisRole);
        addReminder(Role.ASSASSIN, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- MASTERMIND --- (No reminders)

        // --- ZOMBUUL ---
        remindersForThisRole.clear();
        addReminder(Role.ZOMBUUL, Reminders.DIED_TODAY, false, remindersForThisRole);
        addReminder(Role.ZOMBUUL, Reminders.DEAD, false, remindersForThisRole);

        // --- PUKKA ---
        remindersForThisRole.clear();
        addReminder(Role.PUKKA, Reminders.POISONED, false, remindersForThisRole);
        // "Poisoned" is repeated, addReminder() will skip it
        addReminder(Role.PUKKA, Reminders.DEAD, false, remindersForThisRole);

        // --- SHABALOTH ---
        remindersForThisRole.clear();
        addReminder(Role.SHABALOTH, Reminders.DEAD, false, remindersForThisRole);
        // "Dead" is repeated, addReminder() will skip it
        addReminder(Role.SHABALOTH, "Alive", false, remindersForThisRole);

        // --- PO ---
        remindersForThisRole.clear();
        addReminder(Role.PO, Reminders.DEAD, false, remindersForThisRole);
        // "Dead" is repeated, addReminder() will skip it
        addReminder(Role.PO, "3 Attacks", false, remindersForThisRole);

        // --- CLOCKMAKER --- (No reminders)
        // --- DREAMER --- (No reminders)

        // --- SNAKE CHARMER ---
        remindersForThisRole.clear();
        addReminder(Role.SNAKE_CHARMER, Reminders.POISONED, false, remindersForThisRole);

        // --- MATHEMATICIAN ---
        remindersForThisRole.clear();
        addReminder(Role.MATHEMATICIAN, "Abnormal", false, remindersForThisRole);

        // --- FLOWERGIRL ---
        remindersForThisRole.clear();
        addReminder(Role.FLOWERGIRL, "Demon Voted", false, remindersForThisRole);
        addReminder(Role.FLOWERGIRL, "Demon Not Voted", false, remindersForThisRole);

        // --- TOWN CRIER ---
        remindersForThisRole.clear();
        addReminder(Role.TOWN_CRIER, "Minion Nominated", false, remindersForThisRole);
        addReminder(Role.TOWN_CRIER, "Minions Not Nominated", false, remindersForThisRole);

        // --- ORACLE --- (No reminders)
        // --- SAVANT --- (No reminders)

        // --- SEAMSTRESS ---
        remindersForThisRole.clear();
        addReminder(Role.SEAMSTRESS, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- PHILOSOPHER ---
        remindersForThisRole.clear();
        addReminder(Role.PHILOSOPHER, Reminders.DRUNK, false, remindersForThisRole);
        addReminder(Role.PHILOSOPHER, "Is The Philosopher", false, remindersForThisRole);

        // --- ARTIST ---
        remindersForThisRole.clear();
        addReminder(Role.ARTIST, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- JUGGLER ---
        remindersForThisRole.clear();
        addReminder(Role.JUGGLER, "Correct", false, remindersForThisRole);

        // --- SAGE --- (No reminders)
        // --- MUTANT --- (No reminders)

        // --- BARBER ---
        remindersForThisRole.clear();
        addReminder(Role.BARBER, "Haircuts Tonight", false, remindersForThisRole);

        // --- SWEETHEART ---
        remindersForThisRole.clear();
        addReminder(Role.SWEETHEART, Reminders.DRUNK, false, remindersForThisRole);

        // --- KLUTZ --- (No reminders)

        // --- WITCH ---
        remindersForThisRole.clear();
        addReminder(Role.WITCH, "Cursed", false, remindersForThisRole);

        // --- CERENOVUS ---
        remindersForThisRole.clear();
        addReminder(Role.CERENOVUS, Reminders.MAD, false, remindersForThisRole);

        // --- PIT-HAG --- (No reminders)

        // --- EVIL TWIN ---
        remindersForThisRole.clear();
        addReminder(Role.EVIL_TWIN, Reminders.TWIN, false, remindersForThisRole);

        // --- FANG GU ---
        remindersForThisRole.clear();
        addReminder(Role.FANG_GU, Reminders.DEAD, false, remindersForThisRole);
        addReminder(Role.FANG_GU, Reminders.ONCE, false, remindersForThisRole);

        // --- VIGORMORTIS ---
        remindersForThisRole.clear();
        addReminder(Role.VIGORMORTIS, Reminders.DEAD, false, remindersForThisRole);
        addReminder(Role.VIGORMORTIS, Reminders.HAS_ABILITY, false, remindersForThisRole);
        addReminder(Role.VIGORMORTIS, Reminders.POISONED, false, remindersForThisRole);

        // --- NO DASHII ---
        remindersForThisRole.clear();
        addReminder(Role.NO_DASHII, Reminders.DEAD, false, remindersForThisRole);
        addReminder(Role.NO_DASHII, Reminders.POISONED, false, remindersForThisRole);

        // --- VORTOX ---
        remindersForThisRole.clear();
        addReminder(Role.VORTOX, Reminders.DEAD, false, remindersForThisRole);
// --- KICKSTARTER ROLES ---

        // --- NOBLE ---
        remindersForThisRole.clear();
        addReminder(Role.NOBLE, "Know", false, remindersForThisRole);

        // --- PIXIE ---
        remindersForThisRole.clear();
        addReminder(Role.PIXIE, Reminders.MAD, false, remindersForThisRole);
        addReminder(Role.PIXIE, Reminders.HAS_ABILITY, false, remindersForThisRole);

        // --- GENERAL --- (No reminders)
        // --- KING --- (No reminders)

        // --- LYCANTHROPE ---
        remindersForThisRole.clear();
        addReminder(Role.LYCANTHROPE, "Faux Paw", false, remindersForThisRole);
        addReminder(Role.LYCANTHROPE, Reminders.DEAD, false, remindersForThisRole);

        // --- ENGINEER ---
        remindersForThisRole.clear();
        addReminder(Role.ENGINEER, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- HUNTSMAN ---
        remindersForThisRole.clear();
        addReminder(Role.HUNTSMAN, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- ALCHEMIST ---
        remindersForThisRole.clear();
        addReminder(Role.ALCHEMIST, "Is The Alchemist", true, remindersForThisRole); // (Global)

        // --- CANNIBAL ---
        remindersForThisRole.clear();
        addReminder(Role.CANNIBAL, Reminders.POISONED, false, remindersForThisRole);
        addReminder(Role.CANNIBAL, "Lunch", false, remindersForThisRole);

        // --- AMNESIAC ---
        remindersForThisRole.clear();
        addReminder(Role.AMNESIAC, "?", false, remindersForThisRole);

        // --- FARMER --- (No reminders)
        // --- MAGICIAN --- (No reminders)

        // --- POPPY GROWER ---
        remindersForThisRole.clear();
        addReminder(Role.POPPY_GROWER, "Evil Wakes", false, remindersForThisRole);

        // --- ATHEIST --- (No reminders)

        // --- GOLEM ---
        remindersForThisRole.clear();
        addReminder(Role.GOLEM, Reminders.MAY_NOT_NOMINATE, false, remindersForThisRole);

        // --- DAMSEL ---
        remindersForThisRole.clear();
        addReminder(Role.DAMSEL, "Guess Used", false, remindersForThisRole);

        // --- SNITCH --- (No reminders)
        // --- HERETIC --- (No reminders)

        // --- PUZZLEMASTER ---
        remindersForThisRole.clear();
        addReminder(Role.PUZZLEMASTER, Reminders.DRUNK, false, remindersForThisRole);
        addReminder(Role.PUZZLEMASTER, "Guess Used", false, remindersForThisRole);

        // --- MEZEPHELES ---
        remindersForThisRole.clear();
        addReminder(Role.MEZEPHELES, "Turns Evil", false, remindersForThisRole);
        addReminder(Role.MEZEPHELES, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- FEARMONGER ---
        remindersForThisRole.clear();
        addReminder(Role.FEARMONGER, "Fear", false, remindersForThisRole);

        // --- PSYCHOPATH --- (No reminders)

        // --- MARIONETTE ---
        remindersForThisRole.clear();
        addReminder(Role.MARIONETTE, "Is The Marionette", true, remindersForThisRole); // (Global)

        // --- BOOMDANDY --- (No reminders)

        // --- LEGION ---
        remindersForThisRole.clear();
        addReminder(Role.LEGION, Reminders.DEAD, false, remindersForThisRole);
        addReminder(Role.LEGION, "About To Die", false, remindersForThisRole);

        // --- LLEECH ---
        remindersForThisRole.clear();
        addReminder(Role.LLEECH, Reminders.DEAD, false, remindersForThisRole);
        addReminder(Role.LLEECH, Reminders.POISONED, false, remindersForThisRole);

        // --- AL HADIKHIA ---
        remindersForThisRole.clear();
        addReminder(Role.AL_HADIKHIA, Reminders.ONE, false, remindersForThisRole);
        addReminder(Role.AL_HADIKHIA, Reminders.TWO, false, remindersForThisRole);
        addReminder(Role.AL_HADIKHIA, Reminders.THREE, false, remindersForThisRole);

        // --- RIOT --- (No reminders)

        // --- LEVIATHAN ---
        remindersForThisRole.clear();
        addReminder(Role.LEVIATHAN, "Good Player Executed", false, remindersForThisRole);


        // --- CAROUSEL ROLES ---

        // --- STEWARD ---
        remindersForThisRole.clear();
        addReminder(Role.STEWARD, "Know", false, remindersForThisRole);

        // --- KNIGHT ---
        remindersForThisRole.clear();
        addReminder(Role.KNIGHT, "Know", false, remindersForThisRole);

        // --- SHUGENJA --- (No reminders)

        // --- BOUNTY HUNTER ---
        remindersForThisRole.clear();
        addReminder(Role.BOUNTY_HUNTER, Reminders.KNOWN, false, remindersForThisRole);

        // --- HIGH PRIESTESS --- (No reminders)

        // --- BALLOONIST ---
        remindersForThisRole.clear();
        addReminder(Role.BALLOONIST, "Know", false, remindersForThisRole);

        // --- PREACHER ---
        remindersForThisRole.clear();
        addReminder(Role.PREACHER, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- VILLAGE IDIOT ---
        remindersForThisRole.clear();
        addReminder(Role.VILLAGE_IDIOT, Reminders.DRUNK, false, remindersForThisRole);

        // --- CULT LEADER --- (No reminders)

        // --- ACROBAT ---
        remindersForThisRole.clear();
        addReminder(Role.ACROBAT, Reminders.CHOSEN, false, remindersForThisRole);
        addReminder(Role.ACROBAT, Reminders.DEAD, false, remindersForThisRole);

        // --- ALSAAHIR --- (No reminders)

        // --- NIGHTWATCHMAN ---
        remindersForThisRole.clear();
        addReminder(Role.NIGHTWATCHMAN, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- FISHERMAN ---
        remindersForThisRole.clear();
        addReminder(Role.FISHERMAN, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- PRINCESS ---
        remindersForThisRole.clear();
        addReminder(Role.PRINCESS, Reminders.DOESNT_KILL, false, remindersForThisRole);

        // --- CHOIRBOY --- (No reminders)

        // --- BANSHEE ---
        remindersForThisRole.clear();
        addReminder(Role.BANSHEE, Reminders.HAS_ABILITY, false, remindersForThisRole);

        // --- HERMIT --- (No reminders)

        // --- OGRE ---
        remindersForThisRole.clear();
        addReminder(Role.OGRE, "Friend", false, remindersForThisRole);

        // --- PLAGUE DOCTOR ---
        remindersForThisRole.clear();
        addReminder(Role.PLAGUE_DOCTOR, Reminders.STORYTELLER_ABILITY, false, remindersForThisRole);

        // --- HATTER ---
        remindersForThisRole.clear();
        addReminder(Role.HATTER, "Tea Party Tonight", false, remindersForThisRole);

        // --- POLITICIAN --- (No reminders)
        // --- ZEALOT --- (No reminders)

        // --- HARPY ---
        remindersForThisRole.clear();
        addReminder(Role.HARPY, Reminders.MAD, false, remindersForThisRole);
        addReminder(Role.HARPY, "2nd", false, remindersForThisRole);

        // --- WIZARD ---
        remindersForThisRole.clear();
        addReminder(Role.WIZARD, "?", false, remindersForThisRole);

        // --- WIDOW ---
        remindersForThisRole.clear();
        addReminder(Role.WIDOW, Reminders.POISONED, false, remindersForThisRole);
        addReminder(Role.WIDOW, "Knows", false, remindersForThisRole);

        // --- XAAN ---
        remindersForThisRole.clear();
        addReminder(Role.XAAN, "Night 1", false, remindersForThisRole);
        addReminder(Role.XAAN, "Night 2", false, remindersForThisRole);
        addReminder(Role.XAAN, "Night 3", false, remindersForThisRole);
        addReminder(Role.XAAN, Reminders.X, false, remindersForThisRole);

        // --- WRAITH --- (No reminders)

        // --- SUMMONER --- (No reminders)

        // --- GOBLIN ---
        remindersForThisRole.clear();
        addReminder(Role.GOBLIN, "Claimed", false, remindersForThisRole);

        // --- VIZIER --- (No reminders)

        // --- ORGAN GRINDER ---
        remindersForThisRole.clear();
        addReminder(Role.ORGAN_GRINDER, "About To Die", false, remindersForThisRole);
        addReminder(Role.ORGAN_GRINDER, Reminders.DRUNK, false, remindersForThisRole);

        // --- BOFFIN --- (No reminders)

        // --- YAGGABABBLE ---
        remindersForThisRole.clear();
        addReminder(Role.YAGGABABBLE, Reminders.DEAD, false, remindersForThisRole);

        // --- LIL MONSTA ---
        remindersForThisRole.clear();
        addReminder(Role.LIL_MONSTA, Reminders.IS_THE_DEMON, true, remindersForThisRole); // (Global)
        addReminder(Role.LIL_MONSTA, Reminders.DEAD, true, remindersForThisRole); // (Global)

        // --- KAZALI ---
        remindersForThisRole.clear();
        addReminder(Role.KAZALI, Reminders.DEAD, false, remindersForThisRole);

        // --- OJO ---
        remindersForThisRole.clear();
        addReminder(Role.OJO, Reminders.DEAD, false, remindersForThisRole);

        // --- LORD OF TYPHON ---
        remindersForThisRole.clear();
        addReminder(Role.LORD_OF_TYPHON, Reminders.DEAD, false, remindersForThisRole);

        // ====================================================================
        // --- TRAVELERS ---
        // ====================================================================

        // --- THIEF ---
        remindersForThisRole.clear();
        addReminder(Role.THIEF, Reminders.NEGATIVE_VOTE, false, remindersForThisRole);

        // --- BUREAUCRAT ---
        remindersForThisRole.clear();
        addReminder(Role.BUREAUCRAT, Reminders.THREE_VOTES, false, remindersForThisRole);

        // --- GUNSLINGER --- (No reminders)
        // --- SCAPEGOAT --- (No reminders)
        // --- BEGGAR --- (No reminders)

        // --- APPRENTICE ---
        remindersForThisRole.clear();
        addReminder(Role.APPRENTICE, "Is The Apprentice", true, remindersForThisRole); // (Global)

        // --- MATRON --- (No reminders)

        // --- JUDGE ---
        remindersForThisRole.clear();
        addReminder(Role.JUDGE, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- VOUDON --- (No reminders)

        // --- BISHOP ---
        remindersForThisRole.clear();
        addReminder(Role.BISHOP, "Nominate Good", false, remindersForThisRole);
        addReminder(Role.BISHOP, "Nominate Evil", false, remindersForThisRole);

        // --- BARISTA ---
        remindersForThisRole.clear();
        addReminder(Role.BARISTA, "Sober & Healthy", false, remindersForThisRole);
        addReminder(Role.BARISTA, "Acts Twice", false, remindersForThisRole);
        addReminder(Role.BARISTA, "?", false, remindersForThisRole);

        // --- HARLOT ---
        remindersForThisRole.clear();
        addReminder(Role.HARLOT, Reminders.DEAD, false, remindersForThisRole);

        // --- BUTCHER --- (No reminders)
        // --- DEVIANT --- (No reminders)

        // --- BONE COLLECTOR ---
        remindersForThisRole.clear();
        addReminder(Role.BONE_COLLECTOR, Reminders.NO_ABILITY, false, remindersForThisRole);
        addReminder(Role.BONE_COLLECTOR, Reminders.HAS_ABILITY, false, remindersForThisRole);

        // --- CACKLEJACK ---
        remindersForThisRole.clear();
        addReminder(Role.CACKLEJACK, "Not Me", false, remindersForThisRole);

        // --- GANGSTER --- (No reminders)

        // --- GNOME ---
        remindersForThisRole.clear();
        addReminder(Role.GNOME, "Amigo", false, remindersForThisRole);

        // ====================================================================
        // --- FABLED ---
        // ====================================================================

        // --- ANGEL ---
        remindersForThisRole.clear();
        addReminder(Role.ANGEL, "Protected", false, remindersForThisRole);
        addReminder(Role.ANGEL, "Something Bad", false, remindersForThisRole);

        // --- BUDDHIST --- (No reminders)

        // --- DEUS EX FIASCO ---
        remindersForThisRole.clear();
        addReminder(Role.DEUS_EX_FIASCO, "Whoopsie", false, remindersForThisRole);

        // --- DJINN --- (No reminders)
        // --- DOOMSAYER --- (No reminders)

        // --- DUCHESS ---
        remindersForThisRole.clear();
        addReminder(Role.DUCHESS, "Visitor", false, remindersForThisRole);
        addReminder(Role.DUCHESS, "False Info", false, remindersForThisRole);

        // --- FERRYMAN --- (No reminders)

        // --- FIBBIN ---
        remindersForThisRole.clear();
        addReminder(Role.FIBBIN, Reminders.NO_ABILITY, false, remindersForThisRole);

        // --- FIDDLER --- (No reminders)

        // --- HELL'S LIBRARIAN ---
        remindersForThisRole.clear();
        addReminder(Role.HELLS_LIBRARIAN, "Something Bad", false, remindersForThisRole);

        // --- REVOLUTIONARY ---
        remindersForThisRole.clear();
        addReminder(Role.REVOLUTIONARY, "Register Falsely?", false, remindersForThisRole);

        // --- SENTINEL --- (No reminders)

        // --- SPIRIT OF IVORY ---
        remindersForThisRole.clear();
        addReminder(Role.SPIRIT_OF_IVORY, "No More Evil", false, remindersForThisRole);

        // --- TOYMAKER ---
        remindersForThisRole.clear();
        addReminder(Role.TOYMAKER, Reminders.FINAL_NIGHT_NO_ATTACK, false, remindersForThisRole);

        // ====================================================================
        // --- LORIC ---
        // ====================================================================

        // --- BIG WIG --- (No reminders)
        // --- BOOTLEGGER --- (No reminders)
        // --- GARDENER --- (No reminders)

        // --- GOD OF UG ---
        remindersForThisRole.clear();
        addReminder(Role.GOD_OF_UG, Reminders.UG_HAT, false, remindersForThisRole);

        // --- HINDU --- (No reminders)

        // --- KNAVES --- (No reminders)

        // --- POPE --- (No reminders)

        // --- STORM CATCHER ---
        remindersForThisRole.clear();
        addReminder(Role.STORM_CATCHER, "Stormcaught", false, remindersForThisRole);

        // --- TOR --- (No reminders)

        // --- VENTRILOQUIST ---
        remindersForThisRole.clear();
        addReminder(Role.VENTRILOQUIST, Reminders.MAD, false, remindersForThisRole);

        // --- ZENOMANCER ---
        remindersForThisRole.clear();
        addReminder(Role.ZENOMANCER, "Goal", false, remindersForThisRole);

        // --- GLOBAL ALIGNMENT REMINDERS (available to all players) ---
        // Using NO_ROLE as a placeholder since these are alignment markers, not role-specific
        remindersForThisRole.clear();
        DEFINITIONS.add(new ReminderDefinition(Role.NO_ROLE, Reminders.GOOD, true));
        DEFINITIONS.add(new ReminderDefinition(Role.NO_ROLE, Reminders.EVIL, true));
    }

    /**
     * Helper method to add a reminder only if it's not a duplicate for that role.
     */
    private static void addReminder(Role role, String text, boolean isGlobal, Set<String> tracker) {
        if (tracker.add(text)) { // .add() returns true if the set did not already contain the element
            DEFINITIONS.add(new ReminderDefinition(role, text, isGlobal));
        }
    }

    /**
     * Gets all reminder definitions (static + dynamic).
     * @return A list of ReminderDefinitions.
     */
    public static List<ReminderDefinition> getDefinitions() {
        init(); // Ensure list is populated
        List<ReminderDefinition> all = new ArrayList<>(DEFINITIONS);
        all.addAll(DYNAMIC_DEFINITIONS);
        return all;
    }

    /**
     * Register reminders for a custom role.
     */
    public static void registerCustomRoleReminders(CustomRole cr) {
        Set<String> seen = new HashSet<>();

        for (String text : cr.reminders()) {
            if (seen.add(text)) {
                DYNAMIC_DEFINITIONS.add(new ReminderDefinition(cr.id(), text, false));
            }
        }

        for (String text : cr.remindersGlobal()) {
            if (seen.add(text)) {
                DYNAMIC_DEFINITIONS.add(new ReminderDefinition(cr.id(), text, true));
            }
        }
    }

    /**
     * Clear all dynamic reminders.
     * Call this when changing scripts.
     */
    public static void clearDynamicReminders() {
        DYNAMIC_DEFINITIONS.clear();
    }

    /**
     * Register all custom roles from a script.
     * Always registers travelers (even if not enabled) so their reminders are available
     * when a player is assigned a traveler role.
     */
    public static void registerScriptReminders(Script script) {
        clearDynamicReminders();
        if (script == null) return;

        // Register all custom roles
        if (script.customRoles() != null) {
            for (CustomRole cr : script.customRoles()) {
                registerCustomRoleReminders(cr);
            }
        }

        // Custom traveler reminders, so they're available when a player is assigned a
        // traveler. Official travelers have their reminders defined elsewhere.
        if (script.travelers() != null) {
            for (ScriptRole sr : script.travelers()) {
                if (sr instanceof ScriptRole.Custom custom) {
                    registerCustomRoleReminders(custom.customRole());
                }
            }
        }
    }
}