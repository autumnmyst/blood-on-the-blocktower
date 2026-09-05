package com.autumnwind.botb.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
        addReminder(Role.WASHERWOMAN, "Townsfolk", false, remindersForThisRole);
        addReminder(Role.WASHERWOMAN, "Wrong", false, remindersForThisRole);

        // --- LIBRARIAN ---
        remindersForThisRole.clear();
        addReminder(Role.LIBRARIAN, "Outsider", false, remindersForThisRole);
        addReminder(Role.LIBRARIAN, "Wrong", false, remindersForThisRole);

        // --- INVESTIGATOR ---
        remindersForThisRole.clear();
        addReminder(Role.INVESTIGATOR, "Minion", false, remindersForThisRole);
        addReminder(Role.INVESTIGATOR, "Wrong", false, remindersForThisRole);

        // --- CHEF --- (No reminders)
        // --- EMPATH --- (No reminders)

        // --- FORTUNE TELLER ---
        remindersForThisRole.clear();
        addReminder(Role.FORTUNE_TELLER, "Red Herring", false, remindersForThisRole);

        // --- UNDERTAKER ---
        remindersForThisRole.clear();
        addReminder(Role.UNDERTAKER, "Died Today", false, remindersForThisRole);

        // --- MONK ---
        remindersForThisRole.clear();
        addReminder(Role.MONK, "Safe", false, remindersForThisRole);

        // --- RAVENKEEPER --- (No reminders)

        // --- VIRGIN ---
        remindersForThisRole.clear();
        addReminder(Role.VIRGIN, "No Ability", false, remindersForThisRole);

        // --- SLAYER ---
        remindersForThisRole.clear();
        addReminder(Role.SLAYER, "No Ability", false, remindersForThisRole);

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
        addReminder(Role.POISONER, "Poisoned", false, remindersForThisRole);

        // --- SPY --- (No reminders)

        // --- SCARLET WOMAN ---
        remindersForThisRole.clear();
        addReminder(Role.SCARLET_WOMAN, "Is The Demon", false, remindersForThisRole);

        // --- BARON --- (No reminders)

        // --- IMP ---
        remindersForThisRole.clear();
        addReminder(Role.IMP, "Dead", false, remindersForThisRole);

        // --- GRANDMOTHER ---
        remindersForThisRole.clear();
        addReminder(Role.GRANDMOTHER, "Grandchild", false, remindersForThisRole);
        addReminder(Role.GRANDMOTHER, "Dead", false, remindersForThisRole);

        // --- SAILOR ---
        remindersForThisRole.clear();
        addReminder(Role.SAILOR, "Drunk", false, remindersForThisRole);

        // --- CHAMBERMAID --- (No reminders)

        // --- EXORCIST ---
        remindersForThisRole.clear();
        addReminder(Role.EXORCIST, "Chosen", false, remindersForThisRole);

        // --- INNKEEPER ---
        remindersForThisRole.clear();
        addReminder(Role.INNKEEPER, "Safe", false, remindersForThisRole);
        // "Safe" is repeated, addReminder() will skip it
        addReminder(Role.INNKEEPER, "Drunk", false, remindersForThisRole);

        // --- GAMBLER ---
        remindersForThisRole.clear();
        addReminder(Role.GAMBLER, "Dead", false, remindersForThisRole);

        // --- GOSSIP ---
        remindersForThisRole.clear();
        addReminder(Role.GOSSIP, "Dead", false, remindersForThisRole);

        // --- COURTIER ---
        remindersForThisRole.clear();
        addReminder(Role.COURTIER, "Drunk 1", false, remindersForThisRole);
        addReminder(Role.COURTIER, "Drunk 2", false, remindersForThisRole);
        addReminder(Role.COURTIER, "Drunk 3", false, remindersForThisRole);
        addReminder(Role.COURTIER, "No Ability", false, remindersForThisRole);

        // --- PROFESSOR ---
        remindersForThisRole.clear();
        addReminder(Role.PROFESSOR, "Alive", false, remindersForThisRole);
        addReminder(Role.PROFESSOR, "No Ability", false, remindersForThisRole);

        // --- MINSTREL ---
        remindersForThisRole.clear();
        addReminder(Role.MINSTREL, "Everyone Is Drunk", false, remindersForThisRole);

        // --- TEA LADY ---
        remindersForThisRole.clear();
        addReminder(Role.TEA_LADY, "Cannot Die", false, remindersForThisRole);
        // "Cannot Die" is repeated, addReminder() will skip it

        // --- PACIFIST --- (No reminders)

        // --- FOOL ---
        remindersForThisRole.clear();
        addReminder(Role.FOOL, "No Ability", false, remindersForThisRole);

        // --- GOON ---
        remindersForThisRole.clear();
        addReminder(Role.GOON, "Drunk", false, remindersForThisRole);

        // --- LUNATIC ---
        remindersForThisRole.clear();
        addReminder(Role.LUNATIC, "Chosen", false, remindersForThisRole);
        // "Chosen" is repeated, addReminder() will skip it

        // --- TINKER ---
        remindersForThisRole.clear();
        addReminder(Role.TINKER, "Dead", false, remindersForThisRole);

        // --- MOONCHILD ---
        remindersForThisRole.clear();
        addReminder(Role.MOONCHILD, "Dead", false, remindersForThisRole);

        // --- GODFATHER ---
        remindersForThisRole.clear();
        addReminder(Role.GODFATHER, "Died Today", false, remindersForThisRole);
        addReminder(Role.GODFATHER, "Dead", false, remindersForThisRole);

        // --- DEVILS ADVOCATE ---
        remindersForThisRole.clear();
        addReminder(Role.DEVILS_ADVOCATE, "Survives Execution", false, remindersForThisRole);

        // --- ASSASSIN ---
        remindersForThisRole.clear();
        addReminder(Role.ASSASSIN, "Dead", false, remindersForThisRole);
        addReminder(Role.ASSASSIN, "No Ability", false, remindersForThisRole);

        // --- MASTERMIND --- (No reminders)

        // --- ZOMBUUL ---
        remindersForThisRole.clear();
        addReminder(Role.ZOMBUUL, "Died Today", false, remindersForThisRole);
        addReminder(Role.ZOMBUUL, "Dead", false, remindersForThisRole);

        // --- PUKKA ---
        remindersForThisRole.clear();
        addReminder(Role.PUKKA, "Poisoned", false, remindersForThisRole);
        // "Poisoned" is repeated, addReminder() will skip it
        addReminder(Role.PUKKA, "Dead", false, remindersForThisRole);

        // --- SHABALOTH ---
        remindersForThisRole.clear();
        addReminder(Role.SHABALOTH, "Dead", false, remindersForThisRole);
        // "Dead" is repeated, addReminder() will skip it
        addReminder(Role.SHABALOTH, "Alive", false, remindersForThisRole);

        // --- PO ---
        remindersForThisRole.clear();
        addReminder(Role.PO, "Dead", false, remindersForThisRole);
        // "Dead" is repeated, addReminder() will skip it
        addReminder(Role.PO, "3 Attacks", false, remindersForThisRole);

        // --- CLOCKMAKER --- (No reminders)
        // --- DREAMER --- (No reminders)

        // --- SNAKE CHARMER ---
        remindersForThisRole.clear();
        addReminder(Role.SNAKE_CHARMER, "Poisoned", false, remindersForThisRole);

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
        addReminder(Role.SEAMSTRESS, "No Ability", false, remindersForThisRole);

        // --- PHILOSOPHER ---
        remindersForThisRole.clear();
        addReminder(Role.PHILOSOPHER, "Drunk", false, remindersForThisRole);
        addReminder(Role.PHILOSOPHER, "Is The Philosopher", false, remindersForThisRole);

        // --- ARTIST ---
        remindersForThisRole.clear();
        addReminder(Role.ARTIST, "No Ability", false, remindersForThisRole);

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
        addReminder(Role.SWEETHEART, "Drunk", false, remindersForThisRole);

        // --- KLUTZ --- (No reminders)

        // --- WITCH ---
        remindersForThisRole.clear();
        addReminder(Role.WITCH, "Cursed", false, remindersForThisRole);

        // --- CERENOVUS ---
        remindersForThisRole.clear();
        addReminder(Role.CERENOVUS, "Mad", false, remindersForThisRole);

        // --- PIT-HAG --- (No reminders)

        // --- EVIL TWIN ---
        remindersForThisRole.clear();
        addReminder(Role.EVIL_TWIN, "Twin", false, remindersForThisRole);

        // --- FANG GU ---
        remindersForThisRole.clear();
        addReminder(Role.FANG_GU, "Dead", false, remindersForThisRole);
        addReminder(Role.FANG_GU, "Once", false, remindersForThisRole);

        // --- VIGORMORTIS ---
        remindersForThisRole.clear();
        addReminder(Role.VIGORMORTIS, "Dead", false, remindersForThisRole);
        addReminder(Role.VIGORMORTIS, "Has Ability", false, remindersForThisRole);
        addReminder(Role.VIGORMORTIS, "Poisoned", false, remindersForThisRole);

        // --- NO DASHII ---
        remindersForThisRole.clear();
        addReminder(Role.NO_DASHII, "Dead", false, remindersForThisRole);
        addReminder(Role.NO_DASHII, "Poisoned", false, remindersForThisRole);

        // --- VORTOX ---
        remindersForThisRole.clear();
        addReminder(Role.VORTOX, "Dead", false, remindersForThisRole);
// --- KICKSTARTER ROLES ---

        // --- NOBLE ---
        remindersForThisRole.clear();
        addReminder(Role.NOBLE, "Know", false, remindersForThisRole);

        // --- PIXIE ---
        remindersForThisRole.clear();
        addReminder(Role.PIXIE, "Mad", false, remindersForThisRole);
        addReminder(Role.PIXIE, "Has Ability", false, remindersForThisRole);

        // --- GENERAL --- (No reminders)
        // --- KING --- (No reminders)

        // --- LYCANTHROPE ---
        remindersForThisRole.clear();
        addReminder(Role.LYCANTHROPE, "Faux Paw", false, remindersForThisRole);
        addReminder(Role.LYCANTHROPE, "Dead", false, remindersForThisRole);

        // --- ENGINEER ---
        remindersForThisRole.clear();
        addReminder(Role.ENGINEER, "No Ability", false, remindersForThisRole);

        // --- HUNTSMAN ---
        remindersForThisRole.clear();
        addReminder(Role.HUNTSMAN, "No Ability", false, remindersForThisRole);

        // --- ALCHEMIST ---
        remindersForThisRole.clear();
        addReminder(Role.ALCHEMIST, "Is The Alchemist", true, remindersForThisRole); // (Global)

        // --- CANNIBAL ---
        remindersForThisRole.clear();
        addReminder(Role.CANNIBAL, "Poisoned", false, remindersForThisRole);
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
        addReminder(Role.GOLEM, "May Not Nominate", false, remindersForThisRole);

        // --- DAMSEL ---
        remindersForThisRole.clear();
        addReminder(Role.DAMSEL, "Guess Used", false, remindersForThisRole);

        // --- SNITCH --- (No reminders)
        // --- HERETIC --- (No reminders)

        // --- PUZZLEMASTER ---
        remindersForThisRole.clear();
        addReminder(Role.PUZZLEMASTER, "Drunk", false, remindersForThisRole);
        addReminder(Role.PUZZLEMASTER, "Guess Used", false, remindersForThisRole);

        // --- MEZEPHELES ---
        remindersForThisRole.clear();
        addReminder(Role.MEZEPHELES, "Turns Evil", false, remindersForThisRole);
        addReminder(Role.MEZEPHELES, "No Ability", false, remindersForThisRole);

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
        addReminder(Role.LEGION, "Dead", false, remindersForThisRole);
        addReminder(Role.LEGION, "About To Die", false, remindersForThisRole);

        // --- LLEECH ---
        remindersForThisRole.clear();
        addReminder(Role.LLEECH, "Dead", false, remindersForThisRole);
        addReminder(Role.LLEECH, "Poisoned", false, remindersForThisRole);

        // --- AL HADIKHIA ---
        remindersForThisRole.clear();
        addReminder(Role.AL_HADIKHIA, "1", false, remindersForThisRole);
        addReminder(Role.AL_HADIKHIA, "2", false, remindersForThisRole);
        addReminder(Role.AL_HADIKHIA, "3", false, remindersForThisRole);

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
        addReminder(Role.BOUNTY_HUNTER, "Known", false, remindersForThisRole);

        // --- HIGH PRIESTESS --- (No reminders)

        // --- BALLOONIST ---
        remindersForThisRole.clear();
        addReminder(Role.BALLOONIST, "Know", false, remindersForThisRole);

        // --- PREACHER ---
        remindersForThisRole.clear();
        addReminder(Role.PREACHER, "No Ability", false, remindersForThisRole);

        // --- VILLAGE IDIOT ---
        remindersForThisRole.clear();
        addReminder(Role.VILLAGE_IDIOT, "Drunk", false, remindersForThisRole);

        // --- CULT LEADER --- (No reminders)

        // --- ACROBAT ---
        remindersForThisRole.clear();
        addReminder(Role.ACROBAT, "Chosen", false, remindersForThisRole);
        addReminder(Role.ACROBAT, "Dead", false, remindersForThisRole);

        // --- ALSAAHIR --- (No reminders)

        // --- NIGHTWATCHMAN ---
        remindersForThisRole.clear();
        addReminder(Role.NIGHTWATCHMAN, "No Ability", false, remindersForThisRole);

        // --- FISHERMAN ---
        remindersForThisRole.clear();
        addReminder(Role.FISHERMAN, "No Ability", false, remindersForThisRole);

        // --- PRINCESS ---
        remindersForThisRole.clear();
        addReminder(Role.PRINCESS, "Doesn't Kill", false, remindersForThisRole);

        // --- CHOIRBOY --- (No reminders)

        // --- BANSHEE ---
        remindersForThisRole.clear();
        addReminder(Role.BANSHEE, "Has Ability", false, remindersForThisRole);

        // --- HERMIT --- (No reminders)

        // --- OGRE ---
        remindersForThisRole.clear();
        addReminder(Role.OGRE, "Friend", false, remindersForThisRole);

        // --- PLAGUE DOCTOR ---
        remindersForThisRole.clear();
        addReminder(Role.PLAGUE_DOCTOR, "Storyteller Ability", false, remindersForThisRole);

        // --- HATTER ---
        remindersForThisRole.clear();
        addReminder(Role.HATTER, "Tea Party Tonight", false, remindersForThisRole);

        // --- POLITICIAN --- (No reminders)
        // --- ZEALOT --- (No reminders)

        // --- HARPY ---
        remindersForThisRole.clear();
        addReminder(Role.HARPY, "Mad", false, remindersForThisRole);
        addReminder(Role.HARPY, "2nd", false, remindersForThisRole);

        // --- WIZARD ---
        remindersForThisRole.clear();
        addReminder(Role.WIZARD, "?", false, remindersForThisRole);

        // --- WIDOW ---
        remindersForThisRole.clear();
        addReminder(Role.WIDOW, "Poisoned", false, remindersForThisRole);
        addReminder(Role.WIDOW, "Knows", false, remindersForThisRole);

        // --- XAAN ---
        remindersForThisRole.clear();
        addReminder(Role.XAAN, "Night 1", false, remindersForThisRole);
        addReminder(Role.XAAN, "Night 2", false, remindersForThisRole);
        addReminder(Role.XAAN, "Night 3", false, remindersForThisRole);
        addReminder(Role.XAAN, "X", false, remindersForThisRole);

        // --- WRAITH --- (No reminders)

        // --- SUMMONER --- (No reminders)

        // --- GOBLIN ---
        remindersForThisRole.clear();
        addReminder(Role.GOBLIN, "Claimed", false, remindersForThisRole);

        // --- VIZIER --- (No reminders)

        // --- ORGAN GRINDER ---
        remindersForThisRole.clear();
        addReminder(Role.ORGAN_GRINDER, "About To Die", false, remindersForThisRole);
        addReminder(Role.ORGAN_GRINDER, "Drunk", false, remindersForThisRole);

        // --- BOFFIN --- (No reminders)

        // --- YAGGABABBLE ---
        remindersForThisRole.clear();
        addReminder(Role.YAGGABABBLE, "Dead", false, remindersForThisRole);

        // --- LIL MONSTA ---
        remindersForThisRole.clear();
        addReminder(Role.LIL_MONSTA, "Is The Demon", true, remindersForThisRole); // (Global)
        addReminder(Role.LIL_MONSTA, "Dead", true, remindersForThisRole); // (Global)

        // --- KAZALI ---
        remindersForThisRole.clear();
        addReminder(Role.KAZALI, "Dead", false, remindersForThisRole);

        // --- OJO ---
        remindersForThisRole.clear();
        addReminder(Role.OJO, "Dead", false, remindersForThisRole);

        // --- LORD OF TYPHON ---
        remindersForThisRole.clear();
        addReminder(Role.LORD_OF_TYPHON, "Dead", false, remindersForThisRole);

        // ====================================================================
        // --- TRAVELERS ---
        // ====================================================================

        // --- THIEF ---
        remindersForThisRole.clear();
        addReminder(Role.THIEF, "Negative Vote", false, remindersForThisRole);

        // --- BUREAUCRAT ---
        remindersForThisRole.clear();
        addReminder(Role.BUREAUCRAT, "3 Votes", false, remindersForThisRole);

        // --- GUNSLINGER --- (No reminders)
        // --- SCAPEGOAT --- (No reminders)
        // --- BEGGAR --- (No reminders)

        // --- APPRENTICE ---
        remindersForThisRole.clear();
        addReminder(Role.APPRENTICE, "Is The Apprentice", true, remindersForThisRole); // (Global)

        // --- MATRON --- (No reminders)

        // --- JUDGE ---
        remindersForThisRole.clear();
        addReminder(Role.JUDGE, "No Ability", false, remindersForThisRole);

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
        addReminder(Role.HARLOT, "Dead", false, remindersForThisRole);

        // --- BUTCHER --- (No reminders)
        // --- DEVIANT --- (No reminders)

        // --- BONE COLLECTOR ---
        remindersForThisRole.clear();
        addReminder(Role.BONE_COLLECTOR, "No Ability", false, remindersForThisRole);
        addReminder(Role.BONE_COLLECTOR, "Has Ability", false, remindersForThisRole);

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
        addReminder(Role.FIBBIN, "No Ability", false, remindersForThisRole);

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
        addReminder(Role.TOYMAKER, "Final Night: No Attack", false, remindersForThisRole);

        // ====================================================================
        // --- LORIC ---
        // ====================================================================

        // --- BIG WIG --- (No reminders)
        // --- BOOTLEGGER --- (No reminders)
        // --- GARDENER --- (No reminders)

        // --- GOD OF UG ---
        remindersForThisRole.clear();
        addReminder(Role.GOD_OF_UG, "Ug hat", false, remindersForThisRole);

        // --- HINDU --- (No reminders)

        // --- KNAVES --- (No reminders)

        // --- POPE --- (No reminders)

        // --- STORM CATCHER ---
        remindersForThisRole.clear();
        addReminder(Role.STORM_CATCHER, "Stormcaught", false, remindersForThisRole);

        // --- TOR --- (No reminders)

        // --- VENTRILOQUIST ---
        remindersForThisRole.clear();
        addReminder(Role.VENTRILOQUIST, "Mad", false, remindersForThisRole);

        // --- ZENOMANCER ---
        remindersForThisRole.clear();
        addReminder(Role.ZENOMANCER, "Goal", false, remindersForThisRole);

        // --- GLOBAL ALIGNMENT REMINDERS (available to all players) ---
        // Using NO_ROLE as a placeholder since these are alignment markers, not role-specific
        remindersForThisRole.clear();
        DEFINITIONS.add(new ReminderDefinition(Role.NO_ROLE, "Good", true));
        DEFINITIONS.add(new ReminderDefinition(Role.NO_ROLE, "Evil", true));
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