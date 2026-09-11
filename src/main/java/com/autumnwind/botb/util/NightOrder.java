package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.locale.Language;

public class NightOrder {

    /**
     * Death trigger types for triggered death-based roles.
     * Only used when deathBased=true AND triggered=true.
     */
    public enum DeathTriggerType {
        ANY,    // Triggers when marked dead for any reason (including execution)
        NIGHT,  // Triggers when killed at night (currentDay != currentNight)
        DEMON,  // Triggers when killed at night WITH appropriate demon reminder token
        OTHER   // Triggers based on another player's death, not the role holder's (e.g., Grandmother's grandchild)
    }

    /**
     * Represents a single action in the night order.
     *
     * @param role              The role performing the action, if any.
     * @param staticAction      The static action being performed, if any.
     * @param seatTeleport      If true, auto-teleport will work for this action.
     * @param markedByDefault   If present, determines the default "marked" state for this role.
     * @param instructionKey    Lang key of the instruction text shown to the Storyteller, null for none.
     * @param triggered         If true, this role's night visit is triggered (can happen at nearly any point when marked).
     * @param deathBased        If true, presence/triggering is based on death status instead of marked status.
     * @param deathTriggerType  The type of death trigger (only used when deathBased=true AND triggered=true).
     * @param executionBased    If true, this role only appears if there was an execution today (e.g., Undertaker).
     * @param announcementOnly  If true, this is a storyteller reminder (e.g. "announce the Leviathan"), not a
     *                          night action: it gets a visit in the storyteller's walkthrough but is left out
     *                          of the player-facing night order in the Script Reference.
     */
    public record NightOrderInfo(
            Optional<Role> role,
            Optional<StaticAction> staticAction,
            boolean seatTeleport,
            Optional<Boolean> markedByDefault,
            String instructionKey,
            boolean triggered,
            boolean deathBased,
            DeathTriggerType deathTriggerType,
            boolean executionBased,
            boolean announcementOnly
    ) {
        // --- Factory methods ---

        // First Night Role
        public static NightOrderInfo fnRole(Role role, boolean seatTeleport) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.empty(), key(role, true), false, false, DeathTriggerType.ANY, false, false);
        }

        // Other Night Role
        public static NightOrderInfo onRole(Role role, boolean seatTeleport, boolean marked) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.of(marked), key(role, false), false, false, DeathTriggerType.ANY, false, false);
        }

        // Other Night Role with a triggered flag
        public static NightOrderInfo onRole(Role role, boolean seatTeleport, boolean marked, boolean triggered) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.of(marked), key(role, false), triggered, false, DeathTriggerType.ANY, false, false);
        }

        // Other Night Role with a triggered flag and deathBased flag
        public static NightOrderInfo onRole(Role role, boolean seatTeleport, boolean marked, boolean triggered, boolean deathBased) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.of(marked), key(role, false), triggered, deathBased, DeathTriggerType.ANY, false, false);
        }

        // Other Night Role with a triggered flag, deathBased flag, and death trigger type
        public static NightOrderInfo onRole(Role role, boolean seatTeleport, boolean marked, boolean triggered, boolean deathBased, DeathTriggerType deathTriggerType) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.of(marked), key(role, false), triggered, deathBased, deathTriggerType, false, false);
        }

        // Other Night Role with executionBased flag AND deathBased flag (for Undertaker)
        // deathBased=true, triggered=false means: alive = active (loses ability when dead)
        // executionBased=true means: only shows if there was an execution today
        // markedByDefault is empty (no marker visible - not markable)
        public static NightOrderInfo onRoleDeathAndExecutionBased(Role role, boolean seatTeleport) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.empty(), key(role, false), false, true, DeathTriggerType.ANY, true, false);
        }

        // First Night announcement: a storyteller reminder after dawn, not a night action
        public static NightOrderInfo fnAnnouncement(Role role) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), false, Optional.empty(), key(role, true), false, false, DeathTriggerType.ANY, false, true);
        }

        private static String key(Role role, boolean firstNight) {
            return key(role.getId(), firstNight);
        }

        private static String key(String id, boolean firstNight) {
            return "nightorder." + BloodOnTheBlocktower.MOD_ID + "." + id + (firstNight ? ".first" : ".other");
        }

        // Static Action
        public static NightOrderInfo staticAction(StaticAction action, boolean seatTeleport, boolean firstNight) {
            return new NightOrderInfo(Optional.empty(), Optional.of(action), seatTeleport, Optional.empty(), key(action.name().toLowerCase(Locale.ROOT), firstNight), false, false, DeathTriggerType.ANY, false, false);
        }

        /** The instruction text to show the Storyteller. */
        public String roleInstructions() {
            return instructionKey != null && Language.getInstance().has(instructionKey) ? Language.getInstance().getOrDefault(instructionKey) : "";
        }

        // --- Helper accessors ---
        public boolean isRole() { return role.isPresent(); }
        public boolean isStatic() { return staticAction.isPresent(); }

        public Role getRole() { return role.orElseThrow(); }
        public StaticAction getStaticAction() { return staticAction.orElseThrow(); }

        public boolean isMarkedByDefault() {
            return markedByDefault.orElse(false);
        }

        public boolean isTriggered() {
            return triggered;
        }

        public boolean isDeathBased() {
            return deathBased;
        }

        public DeathTriggerType getDeathTriggerType() {
            return deathTriggerType;
        }

        public boolean isExecutionBased() {
            return executionBased;
        }

        public boolean isAnnouncementOnly() {
            return announcementOnly;
        }
    }

    // Enum for static, non-role night actions
    public enum StaticAction {
        DAWN,
        NOMINATIONS,
        MINION_INFO,
        DEMON_INFO,
        DUSK
    }

    // --- NEW NIGHT ORDER LISTS ---

    private static final List<NightOrderInfo> FIRST_NIGHT_ORDER = List.of(
            NightOrderInfo.staticAction(StaticAction.DUSK, false, true),
            // Fabled/Loric
            NightOrderInfo.fnRole(Role.ANGEL, false),
            NightOrderInfo.fnRole(Role.BUDDHIST, false),
            NightOrderInfo.fnRole(Role.TOYMAKER, false),
            NightOrderInfo.fnRole(Role.STORM_CATCHER, false),
            // Continue with demons/minions setup
            NightOrderInfo.fnRole(Role.KAZALI, true),
            // Travelers
            NightOrderInfo.fnRole(Role.APPRENTICE, true),
            NightOrderInfo.fnRole(Role.BARISTA, true),
            NightOrderInfo.fnRole(Role.BUREAUCRAT, true),
            NightOrderInfo.fnRole(Role.THIEF, true),
            NightOrderInfo.fnRole(Role.BOFFIN, true),
            NightOrderInfo.fnRole(Role.PHILOSOPHER, true),
            NightOrderInfo.fnRole(Role.ALCHEMIST, true),
            NightOrderInfo.fnRole(Role.POPPY_GROWER, false),
            NightOrderInfo.fnRole(Role.YAGGABABBLE, true),
            NightOrderInfo.fnRole(Role.MAGICIAN, false),
            NightOrderInfo.staticAction(StaticAction.MINION_INFO, true, true),
            NightOrderInfo.fnRole(Role.DAMSEL, false),
            NightOrderInfo.fnRole(Role.SNITCH, false),
            NightOrderInfo.fnRole(Role.LUNATIC, true),
            NightOrderInfo.fnRole(Role.SUMMONER, true),
            NightOrderInfo.staticAction(StaticAction.DEMON_INFO, true, true),
            NightOrderInfo.fnRole(Role.KING, false),
            NightOrderInfo.fnRole(Role.SAILOR, true),
            NightOrderInfo.fnRole(Role.MARIONETTE, false),
            NightOrderInfo.fnRole(Role.ENGINEER, true),
            NightOrderInfo.fnRole(Role.PREACHER, true),
            NightOrderInfo.fnRole(Role.LIL_MONSTA, true),
            NightOrderInfo.fnRole(Role.LLEECH, true),
            // Xaan sits here on the official sheet but has no entry: the Xaan never wakes, and
            // its poisoning is handled by the "Night N" reminder placing an "X" icon and a
            // paragraph on every Townsfolk visit that night (see NightOrderBuilder/RoleHelpers).
            NightOrderInfo.fnRole(Role.POISONER, true),
            NightOrderInfo.fnRole(Role.WIDOW, true),
            NightOrderInfo.fnRole(Role.COURTIER, true),
            NightOrderInfo.fnRole(Role.WIZARD, true),
            NightOrderInfo.fnRole(Role.SNAKE_CHARMER, true),
            NightOrderInfo.fnRole(Role.GODFATHER, true),
            NightOrderInfo.fnRole(Role.ORGAN_GRINDER, true),
            NightOrderInfo.fnRole(Role.DEVILS_ADVOCATE, true),
            NightOrderInfo.fnRole(Role.EVIL_TWIN, true),
            NightOrderInfo.fnRole(Role.WITCH, true),
            NightOrderInfo.fnRole(Role.CERENOVUS, true),
            NightOrderInfo.fnRole(Role.FEARMONGER, true),
            NightOrderInfo.fnRole(Role.HARPY, true),
            NightOrderInfo.fnRole(Role.MEZEPHELES, true),
            NightOrderInfo.fnRole(Role.PUKKA, true),
            NightOrderInfo.fnRole(Role.PIXIE, true),
            NightOrderInfo.fnRole(Role.HUNTSMAN, true),
            NightOrderInfo.fnRole(Role.AMNESIAC, true),
            NightOrderInfo.fnRole(Role.WASHERWOMAN, true),
            NightOrderInfo.fnRole(Role.LIBRARIAN, true),
            NightOrderInfo.fnRole(Role.INVESTIGATOR, true),
            NightOrderInfo.fnRole(Role.CHEF, true),
            NightOrderInfo.fnRole(Role.EMPATH, true),
            NightOrderInfo.fnRole(Role.FORTUNE_TELLER, true),
            NightOrderInfo.fnRole(Role.BUTLER, true),
            NightOrderInfo.fnRole(Role.GRANDMOTHER, true),
            NightOrderInfo.fnRole(Role.CLOCKMAKER, true),
            NightOrderInfo.fnRole(Role.DREAMER, true),
            NightOrderInfo.fnRole(Role.SEAMSTRESS, true),
            NightOrderInfo.fnRole(Role.STEWARD, true),
            NightOrderInfo.fnRole(Role.KNIGHT, true),
            NightOrderInfo.fnRole(Role.NOBLE, true),
            NightOrderInfo.fnRole(Role.BALLOONIST, true),
            NightOrderInfo.fnRole(Role.SHUGENJA, true),
            NightOrderInfo.fnRole(Role.VILLAGE_IDIOT, true),
            NightOrderInfo.fnRole(Role.BOUNTY_HUNTER, true),
            NightOrderInfo.fnRole(Role.NIGHTWATCHMAN, true),
            NightOrderInfo.fnRole(Role.CULT_LEADER, false),
            NightOrderInfo.fnRole(Role.ARTIST, true),
            NightOrderInfo.fnRole(Role.SAVANT, true),
            NightOrderInfo.fnRole(Role.FISHERMAN, true),
            NightOrderInfo.fnRole(Role.SPY, true),
            NightOrderInfo.fnRole(Role.OGRE, true),
            NightOrderInfo.fnRole(Role.HIGH_PRIESTESS, true),
            NightOrderInfo.fnRole(Role.GENERAL, true),
            NightOrderInfo.fnRole(Role.CHAMBERMAID, true),
            NightOrderInfo.fnRole(Role.MATHEMATICIAN, true),
            NightOrderInfo.staticAction(StaticAction.DAWN, false, true),
            NightOrderInfo.fnAnnouncement(Role.LEVIATHAN),
            NightOrderInfo.fnAnnouncement(Role.VIZIER),
            NightOrderInfo.staticAction(StaticAction.NOMINATIONS, false, true)
    );

    private static final List<NightOrderInfo> OTHER_NIGHTS_ORDER = List.of(
            NightOrderInfo.staticAction(StaticAction.DUSK, false, false),
            // First so a Wraith without their ability is told before any evil player is visited
            NightOrderInfo.onRole(Role.WRAITH, true, false, false, true),
            // Fabled
            NightOrderInfo.onRole(Role.DUCHESS, true, false),
            NightOrderInfo.onRole(Role.TOYMAKER, false, false),
            // Travelers
            NightOrderInfo.onRole(Role.BARISTA, true, false, false, true),
            NightOrderInfo.onRole(Role.CACKLEJACK, true, false),
            NightOrderInfo.onRole(Role.BUREAUCRAT, true, false, false, true),
            NightOrderInfo.onRole(Role.THIEF, true, false, false, true),
            NightOrderInfo.onRole(Role.HARLOT, true, false, false, true),
            NightOrderInfo.onRole(Role.BONE_COLLECTOR, true, false, false, true),
            NightOrderInfo.onRole(Role.PHILOSOPHER, true, false, false, true),
            NightOrderInfo.onRole(Role.POPPY_GROWER, false, false, true, true),
            NightOrderInfo.onRole(Role.SAILOR, true, false, false, true),
            NightOrderInfo.onRole(Role.ENGINEER, true, false, false, true),
            NightOrderInfo.onRole(Role.PREACHER, true, false, false, true),
            // Xaan: no entry on other nights either, see the first-night note above
            NightOrderInfo.onRole(Role.POISONER, true, false, false, true),
            NightOrderInfo.onRole(Role.COURTIER, true, false, false, true),
            NightOrderInfo.onRole(Role.INNKEEPER, true, false, false, true),
            NightOrderInfo.onRole(Role.WIZARD, true, true),
            NightOrderInfo.onRole(Role.GAMBLER, true, false, false, true),
            NightOrderInfo.onRole(Role.ACROBAT, true, false, false, true),
            NightOrderInfo.onRole(Role.SNAKE_CHARMER, true, false, false, true),
            NightOrderInfo.onRole(Role.MONK, true, false, false, true),
            NightOrderInfo.onRole(Role.ORGAN_GRINDER, true, false, false, true),
            NightOrderInfo.onRole(Role.DEVILS_ADVOCATE, true, false, false, true),
            NightOrderInfo.onRole(Role.WITCH, true, false, false, true),
            NightOrderInfo.onRole(Role.CERENOVUS, true, false, false, true),
            NightOrderInfo.onRole(Role.PIT_HAG, true, false, false, true),
            NightOrderInfo.onRole(Role.FEARMONGER, true, false, false, true),
            NightOrderInfo.onRole(Role.HARPY, true, false, false, true),
            NightOrderInfo.onRole(Role.MEZEPHELES, false, true),
            NightOrderInfo.onRole(Role.SCARLET_WOMAN, false, false, true, true, DeathTriggerType.OTHER),
            NightOrderInfo.onRole(Role.SUMMONER, true, false, false, true),
            NightOrderInfo.onRole(Role.LUNATIC, true, false, false, true),
            NightOrderInfo.onRole(Role.EXORCIST, true, false, false, true),
            NightOrderInfo.onRole(Role.LYCANTHROPE, true, false, false, true),
            NightOrderInfo.onRole(Role.PRINCESS, false, true),
            NightOrderInfo.onRole(Role.LEGION, false, false, false, true),
            NightOrderInfo.onRole(Role.IMP, true, false, false, true),
            NightOrderInfo.onRole(Role.ZOMBUUL, true, false, false, true),
            NightOrderInfo.onRole(Role.PUKKA, true, false, false, true),
            NightOrderInfo.onRole(Role.SHABALOTH, true, false, false, true),
            NightOrderInfo.onRole(Role.PO, true, false, false, true),
            NightOrderInfo.onRole(Role.FANG_GU, true, false, false, true),
            NightOrderInfo.onRole(Role.NO_DASHII, true, false, false, true),
            NightOrderInfo.onRole(Role.VORTOX, true, false, false, true),
            NightOrderInfo.onRole(Role.LORD_OF_TYPHON, true, false, false, true),
            NightOrderInfo.onRole(Role.VIGORMORTIS, true, false, false, true),
            NightOrderInfo.onRole(Role.OJO, true, false, false, true),
            NightOrderInfo.onRole(Role.AL_HADIKHIA, true, false, false, true),
            NightOrderInfo.onRole(Role.LLEECH, true, false, false, true),
            NightOrderInfo.onRole(Role.LIL_MONSTA, true, true),
            NightOrderInfo.onRole(Role.YAGGABABBLE, true, false, false, true),
            NightOrderInfo.onRole(Role.KAZALI, true, false, false, true),
            NightOrderInfo.onRole(Role.ASSASSIN, true, false, false, true),
            NightOrderInfo.onRole(Role.GODFATHER, true, false, false, true),
            NightOrderInfo.onRole(Role.GOSSIP, false, false),
            NightOrderInfo.onRole(Role.HATTER, true, false, true, true, DeathTriggerType.ANY),
            NightOrderInfo.onRole(Role.BARBER, true, false, true, true, DeathTriggerType.ANY),
            NightOrderInfo.onRole(Role.SWEETHEART, false, false, true, true, DeathTriggerType.ANY),
            NightOrderInfo.onRole(Role.PLAGUE_DOCTOR, false, false, true, true, DeathTriggerType.ANY),
            NightOrderInfo.onRole(Role.SAGE, true, false, true, true, DeathTriggerType.DEMON),
            NightOrderInfo.onRole(Role.BANSHEE, true, false, true, true, DeathTriggerType.DEMON),
            NightOrderInfo.onRole(Role.PROFESSOR, true, false, false, true),
            NightOrderInfo.onRole(Role.CHOIRBOY, true, false, true, true, DeathTriggerType.OTHER),
            NightOrderInfo.onRole(Role.HUNTSMAN, true, false, false, true),
            NightOrderInfo.onRole(Role.DAMSEL, true, false),
            NightOrderInfo.onRole(Role.AMNESIAC, true, true),
            NightOrderInfo.onRole(Role.FARMER, false, false, true, true, DeathTriggerType.NIGHT),
            NightOrderInfo.onRole(Role.TINKER, false, false, false, true),
            NightOrderInfo.onRole(Role.MOONCHILD, false, false),
            NightOrderInfo.onRole(Role.GRANDMOTHER, false, false, true,true, DeathTriggerType.OTHER),
            NightOrderInfo.onRole(Role.RAVENKEEPER, true, false, true, true, DeathTriggerType.NIGHT),
            NightOrderInfo.onRole(Role.EMPATH, true, false, false, true),
            NightOrderInfo.onRole(Role.FORTUNE_TELLER, true, false, false, true),
            NightOrderInfo.onRoleDeathAndExecutionBased(Role.UNDERTAKER, true),
            NightOrderInfo.onRole(Role.DREAMER, true, false, false, true),
            NightOrderInfo.onRole(Role.FLOWERGIRL, true, false, false, true),
            NightOrderInfo.onRole(Role.TOWN_CRIER, true, false, false, true),
            NightOrderInfo.onRole(Role.ORACLE, true, false, false, true),
            NightOrderInfo.onRole(Role.SEAMSTRESS, true, false, false, true),
            NightOrderInfo.onRole(Role.JUGGLER, true, true),
            NightOrderInfo.onRole(Role.BALLOONIST, true, false, false, true),
            NightOrderInfo.onRole(Role.VILLAGE_IDIOT, true, false, false, true),
            NightOrderInfo.onRole(Role.KING, true, false, false, true),
            NightOrderInfo.onRole(Role.BOUNTY_HUNTER, true, false),
            NightOrderInfo.onRole(Role.NIGHTWATCHMAN, true, false, false, true),
            NightOrderInfo.onRole(Role.CULT_LEADER, false, false, false, true),
            NightOrderInfo.onRole(Role.BUTLER, true, false, false, true),
            NightOrderInfo.onRole(Role.ARTIST, true, false, false, true),
            NightOrderInfo.onRole(Role.SAVANT, true, false, false, true),
            NightOrderInfo.onRole(Role.FISHERMAN, true, false, false, true),
            NightOrderInfo.onRole(Role.SPY, true, false, false, true),
            NightOrderInfo.onRole(Role.HIGH_PRIESTESS, true, false, false, true),
            NightOrderInfo.onRole(Role.GENERAL, true, false, false, true),
            NightOrderInfo.onRole(Role.CHAMBERMAID, true, false, false, true),
            NightOrderInfo.onRole(Role.MATHEMATICIAN, true, false, false, true),
            NightOrderInfo.onRole(Role.TOR, false, false),
            NightOrderInfo.onRole(Role.RIOT, true, false),
            NightOrderInfo.staticAction(StaticAction.DAWN, false, false),
            NightOrderInfo.staticAction(StaticAction.NOMINATIONS, false, false)
    );


    public static List<NightOrderInfo> getFirstNightOrder() {
        return FIRST_NIGHT_ORDER;
    }

    public static List<NightOrderInfo> getOtherNightOrder() {
        return OTHER_NIGHTS_ORDER;
    }
}