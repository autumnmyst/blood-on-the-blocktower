package com.autumnwind.botb.util;

import java.util.List;
import java.util.Optional;

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
     * @param roleInstructions  The instruction text to show the Storyteller.
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
            String roleInstructions,
            boolean triggered,
            boolean deathBased,
            DeathTriggerType deathTriggerType,
            boolean executionBased,
            boolean announcementOnly
    ) {
        // --- Factory methods ---

        // First Night Role
        public static NightOrderInfo fnRole(Role role, boolean seatTeleport, String instructions) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.empty(), instructions, false, false, DeathTriggerType.ANY, false, false);
        }

        // Other Night Role
        public static NightOrderInfo onRole(Role role, boolean seatTeleport, boolean marked, String instructions) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.of(marked), instructions, false, false, DeathTriggerType.ANY, false, false);
        }

        // Other Night Role with a triggered flag
        public static NightOrderInfo onRole(Role role, boolean seatTeleport, boolean marked, boolean triggered, String instructions) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.of(marked), instructions, triggered, false, DeathTriggerType.ANY, false, false);
        }

        // Other Night Role with a triggered flag and deathBased flag
        public static NightOrderInfo onRole(Role role, boolean seatTeleport, boolean marked, boolean triggered, boolean deathBased, String instructions) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.of(marked), instructions, triggered, deathBased, DeathTriggerType.ANY, false, false);
        }

        // Other Night Role with a triggered flag, deathBased flag, and death trigger type
        public static NightOrderInfo onRole(Role role, boolean seatTeleport, boolean marked, boolean triggered, boolean deathBased, DeathTriggerType deathTriggerType, String instructions) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.of(marked), instructions, triggered, deathBased, deathTriggerType, false, false);
        }

        // Other Night Role with executionBased flag AND deathBased flag (for Undertaker)
        // deathBased=true, triggered=false means: alive = active (loses ability when dead)
        // executionBased=true means: only shows if there was an execution today
        // markedByDefault is empty (no marker visible - not markable)
        public static NightOrderInfo onRoleDeathAndExecutionBased(Role role, boolean seatTeleport, String instructions) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), seatTeleport, Optional.empty(), instructions, false, true, DeathTriggerType.ANY, true, false);
        }

        // First Night announcement: a storyteller reminder after dawn, not a night action
        public static NightOrderInfo fnAnnouncement(Role role, String instructions) {
            return new NightOrderInfo(Optional.of(role), Optional.empty(), false, Optional.empty(), instructions, false, false, DeathTriggerType.ANY, false, true);
        }

        // Static Action
        public static NightOrderInfo staticAction(StaticAction action, boolean seatTeleport, String instructions) {
            return new NightOrderInfo(Optional.empty(), Optional.of(action), seatTeleport, Optional.empty(), instructions, false, false, DeathTriggerType.ANY, false, false);
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
            NightOrderInfo.staticAction(StaticAction.DUSK, false, ""),
            // Fabled/Loric
            NightOrderInfo.fnRole(Role.ANGEL, false, "Inform the group which player(s) are protected by the Angel. Mark them as 'Protected'."),
            NightOrderInfo.fnRole(Role.BUDDHIST, false, "Inform the group which player(s) are affected by the Buddhist."),
            NightOrderInfo.fnRole(Role.TOYMAKER, false, "Do the Minion & Demon Info steps, even though there are less than 7 players."),
            NightOrderInfo.fnRole(Role.STORM_CATCHER, false, "Announce the good 'Stormcaught' character. If that character is in play, mark the player with that character as 'Stormcaught'. Wake each evil player and show them the marked player."),
            // Continue with demons/minions setup
            NightOrderInfo.fnRole(Role.KAZALI, true, "The Kazali chooses a player and a Minion on the character script. They do this for as many Minions as should be in play. Change those players' characters to the chosen Minions. Wake each of those players and inform them which Minion they have become, and that they are now evil."),
            // Travelers
            NightOrderInfo.fnRole(Role.APPRENTICE, true, "Show the Apprentice the 'You are' card, then a Townsfolk or Minion token. In the Grimoire, replace the Apprentice token with that character token, and put the Apprentice's 'Is The Apprentice' reminder by that character token."),
            NightOrderInfo.fnRole(Role.BARISTA, true, "Choose a player, wake them and tell them which Barista power is affecting them. Treat them accordingly (sober/healthy/true info or activate their ability twice)."),
            NightOrderInfo.fnRole(Role.BUREAUCRAT, true, "The Bureaucrat points to a player. Put the Bureaucrat's '3 Votes' reminder by the chosen player's character token."),
            NightOrderInfo.fnRole(Role.THIEF, true, "The Thief points to a player. Put the Thief's 'Negative Vote' reminder by the chosen player's character token."),
            NightOrderInfo.fnRole(Role.BOFFIN, true, "Wake the Boffin and tell them the good ability the Demon has. Put the Boffin back to sleep. Wake the Demon, and tell them there is a Boffin in play and what good ability they have."),
            NightOrderInfo.fnRole(Role.PHILOSOPHER, true, "The Philosopher may choose a good character on the script. If they chose a character: they gain that characters ability. If the character is in play, the original character's player becomes drunk."),
            NightOrderInfo.fnRole(Role.ALCHEMIST, true, "Tell the Alchemist the Minion ability they have"),
            NightOrderInfo.fnRole(Role.POPPY_GROWER, false, "Do not inform the Demon/Minions who each other are"),
            NightOrderInfo.fnRole(Role.YAGGABABBLE, true, "Tell the Yaggababble their secret phrase."),
            NightOrderInfo.fnRole(Role.MAGICIAN, false, "Include the Magician as a potential Demon during Minion Info, and as a potential Minion during Demon Info"),
            NightOrderInfo.staticAction(StaticAction.MINION_INFO, true, "Wake Minions, and tell them who the Demon is and who the other Minions are."),
            NightOrderInfo.fnRole(Role.DAMSEL, false, "Inform all Minions that the Damsel is in play."),
            NightOrderInfo.fnRole(Role.SNITCH, false, "Separately tell each Minion three not-in-play characters. These may be the same or different to each other and the ones shown to the Demon."),
            NightOrderInfo.fnRole(Role.LUNATIC, true, "If 7 or more players: Tell the Lunatic a number of arbitrary 'Minions', players equal to the number of Minions in play. Tell them 3 arbitrary good characters. If the Lunatic was told they were a Demon that would wake tonight: Allow the Lunatic to do the Demon actions. Place their 'chosen' markers. Wake the Demon. Tell them the Lunatic player. If the Lunatic chose players: Tell the Demon each marked player. Remove any Lunatic 'chosen' markers."),
            NightOrderInfo.fnRole(Role.SUMMONER, true, "Tell the Summoner 3 not-in-play good characters."),
            NightOrderInfo.staticAction(StaticAction.DEMON_INFO, true, "Wake Demon. Tell them their Minions. Tell them the 3 'bluff' roles."),
            NightOrderInfo.fnRole(Role.KING, false, "Wake the Demon, and tell them who the King player is."),
            NightOrderInfo.fnRole(Role.SAILOR, true, "The Sailor chooses a living player. Either the Sailor, or the chosen player, is drunk."),
            NightOrderInfo.fnRole(Role.MARIONETTE, false, "Inform the Demon who their Marionette is."),
            NightOrderInfo.fnRole(Role.ENGINEER, true, "The Engineer either chooses not to use their ability, picks a Demon, or picks the relevant number of Minions. If the Engineer chose characters, replace the Demon or Minions with the choices, then wake the relevant players and inform them of their new characters."),
            NightOrderInfo.fnRole(Role.PREACHER, true, "The Preacher chooses a player. If a Minion is chosen, wake the Minion and inform them they were chosen by the Preacher."),
            NightOrderInfo.fnRole(Role.LIL_MONSTA, true, "Wake all Minions together. Allow them to decide who they want to babysit Lil' Monsta."),
            NightOrderInfo.fnRole(Role.LLEECH, true, "The Lleech chooses a player. Place the Poisoned reminder token."),
            // Xaan sits here on the official sheet but has no entry: the Xaan never wakes, and
            // its poisoning is handled by the "Night N" reminder placing an "X" icon and a
            // paragraph on every Townsfolk visit that night (see NightOrderBuilder/RoleHelpers).
            NightOrderInfo.fnRole(Role.POISONER, true, "The Poisoner chooses a player. That player is poisoned."),
            NightOrderInfo.fnRole(Role.WIDOW, true, "Show the Grimoire to the Widow for as long as they need. The Widow chooses a player. That player is poisoned. Wake a good player and tell them a Widow is in play."),
            NightOrderInfo.fnRole(Role.COURTIER, true, "The Courtier may choose a character on the script. If the Courtier used their ability: If that character is in play, that player is drunk."),
            NightOrderInfo.fnRole(Role.WIZARD, true, "The Wizard may make their one wish. If the Wizard's wish requires actions at night, run these."),
            NightOrderInfo.fnRole(Role.SNAKE_CHARMER, true, "The Snake Charmer chooses a player. If that player is the Demon: swap the Demon and Snake Charmer characters and alignments. Wake each player to inform them of their new role and alignment. The new Snake Charmer is poisoned."),
            NightOrderInfo.fnRole(Role.GODFATHER, true, "Tell them each of the Outsiders in play."),
            NightOrderInfo.fnRole(Role.ORGAN_GRINDER, true, "Wake the Organ Grinder. If they choose to be Drunk, mark them as such. If they choose to be sober, remove the Drunk reminder."),
            NightOrderInfo.fnRole(Role.DEVILS_ADVOCATE, true, "The Devil's Advocate chooses a living player. That player survives execution tomorrow."),
            NightOrderInfo.fnRole(Role.EVIL_TWIN, true, "Wake the Evil Twin and their twin together. Tell the twin player that the other is Evil Twin. Tell the Evil Twin player the twin player's character."),
            NightOrderInfo.fnRole(Role.WITCH, true, "The Witch chooses a player. If that player nominates tomorrow they die immediately."),
            NightOrderInfo.fnRole(Role.CERENOVUS, true, "The Cerenovus chooses a player and a good character on the script. Wake that player. Inform them they have been targeted by the Cerenovus and what character they must be mad they are. If the player is not mad about being that character tomorrow, they can be executed."),
            NightOrderInfo.fnRole(Role.FEARMONGER, true, "The Fearmonger chooses a player. Place the Fear token next to that player and announce that a new player has been selected with the Fearmonger ability."),
            NightOrderInfo.fnRole(Role.HARPY, true, "The Harpy chooses two players. Wake the 1st player the Harpy chose, and inform them they have been targeted by a Harpy, and they must be mad that the 2nd player the Harpy chose is evil, or one or both might die."),
            NightOrderInfo.fnRole(Role.MEZEPHELES, true, "Tell the Mezepheles their secret word."),
            NightOrderInfo.fnRole(Role.PUKKA, true, "The Pukka chooses a player. That player is poisoned."),
            NightOrderInfo.fnRole(Role.PIXIE, true, "Tell the Pixie 1 in-play Townsfolk character."),
            NightOrderInfo.fnRole(Role.HUNTSMAN, true, "The Huntsman may choose a player. If they choose the Damsel, wake that player, and inform them which not not-in-play Townsfolk they have become."),
            NightOrderInfo.fnRole(Role.AMNESIAC, true, "Decide the Amnesiac's entire ability. If the Amnesiac's ability causes them to wake tonight: Wake the Amnesiac and run their ability."),
            NightOrderInfo.fnRole(Role.WASHERWOMAN, true, "Tell them an in-play Townsfolk character. Tell them two players, one of which is that character."),
            NightOrderInfo.fnRole(Role.LIBRARIAN, true, "Tell them an in-play Outsider character. Tell them two players, one of which is that character."),
            NightOrderInfo.fnRole(Role.INVESTIGATOR, true, "Tell them an in-play Minion character. Tell them two players, one of which is that character."),
            NightOrderInfo.fnRole(Role.CHEF, true, "Tell them a number (0, 1, 2, …) for how many pairs of neighboring evil players there are."),
            NightOrderInfo.fnRole(Role.EMPATH, true, "Tell them a number (0, 1, 2) for how many evil alive neighbors they have."),
            NightOrderInfo.fnRole(Role.FORTUNE_TELLER, true, "The Fortune Teller chooses two players. They learn either 'yes' or 'no' for whether one of those players is the Demon or red herring."),
            NightOrderInfo.fnRole(Role.BUTLER, true, "The Butler chooses a player. Mark that player as 'Master'."),
            NightOrderInfo.fnRole(Role.GRANDMOTHER, true, "Tell them their grandchild, and their grandchild's good character."),
            NightOrderInfo.fnRole(Role.CLOCKMAKER, true, "Tell them a number (1, 2, 3, etc.) equaling the number of seats from Demon to the closest Minion."),
            NightOrderInfo.fnRole(Role.DREAMER, true, "The Dreamer chooses a player. Tell them 1 good and 1 evil character; one of these is correct."),
            NightOrderInfo.fnRole(Role.SEAMSTRESS, true, "The Seamstress may choose two other players. If the Seamstress chose players, nod 'yes' or shake 'no' for whether they are of the same alignment."),
            NightOrderInfo.fnRole(Role.STEWARD, true, "Tell them the marked good player."),
            NightOrderInfo.fnRole(Role.KNIGHT, true, "Tell them the 2 marked players who are not the Demon."),
            NightOrderInfo.fnRole(Role.NOBLE, true, "Tell them 3 players including one evil player, in no particular order."),
            NightOrderInfo.fnRole(Role.BALLOONIST, true, "Choose a character type. Tell them a player whose character is of that type. Place the Balloonist's 'Know' reminder next to that player."),
            NightOrderInfo.fnRole(Role.SHUGENJA, true, "Tell them if the closest evil player is in a clockwise or counter-clockwise direction. If the two closest evil players are equidistant, tell them either direction arbitrarily."),
            NightOrderInfo.fnRole(Role.VILLAGE_IDIOT, true, "The Village Idiot chooses a player; tell them if that player is good or evil."),
            NightOrderInfo.fnRole(Role.BOUNTY_HUNTER, true, "Tell them 1 evil player. Wake the townsfolk who is evil and inform them they are evil."),
            NightOrderInfo.fnRole(Role.NIGHTWATCHMAN, true, "The Nightwatchman may choose a player. Wake that player, and tell them who the Nightwatchman player is."),
            NightOrderInfo.fnRole(Role.CULT_LEADER, false, "The cult leader might change alignment. If so, inform them accordingly."),
            NightOrderInfo.fnRole(Role.ARTIST, true, "The Artist may ask their yes/no question."),
            NightOrderInfo.fnRole(Role.SAVANT, true, "Tell the Savant 2 pieces of information, one truth and one lie."),
            NightOrderInfo.fnRole(Role.FISHERMAN, true, "The Fisherman may ask for advice to help their team win."),
            NightOrderInfo.fnRole(Role.SPY, true, "Show the Grimoire to the Spy for as long as they need."),
            NightOrderInfo.fnRole(Role.OGRE, true, "The Ogre chooses a player (not themselves) and becomes their alignment."),
            NightOrderInfo.fnRole(Role.HIGH_PRIESTESS, true, "Tell them the player you believe they should talk to most."),
            NightOrderInfo.fnRole(Role.GENERAL, true, "Tell the General that either good is winning, evil is winning, or that it's anyone's game."),
            NightOrderInfo.fnRole(Role.CHAMBERMAID, true, "The Chambermaid chooses two players. Tell them a number (0, 1, 2, …) for how many of those players wake tonight for their ability."),
            NightOrderInfo.fnRole(Role.MATHEMATICIAN, true, "Tell them the number (0, 1, 2, etc.) of players whose ability malfunctioned due to other abilities."),
            NightOrderInfo.staticAction(StaticAction.DAWN, false, ""),
            NightOrderInfo.fnAnnouncement(Role.LEVIATHAN, "Announce 'The Leviathan is in play. This is Day 1.'"),
            NightOrderInfo.fnAnnouncement(Role.VIZIER, "Announce the Vizier player."),
            NightOrderInfo.staticAction(StaticAction.NOMINATIONS, false, "Open nominations for the day. Players may now nominate and be nominated.")
    );

    private static final List<NightOrderInfo> OTHER_NIGHTS_ORDER = List.of(
            NightOrderInfo.staticAction(StaticAction.DUSK, false, ""),
            // Fabled
            NightOrderInfo.onRole(Role.DUCHESS, true, false, "Wake each player marked 'Visitor' or 'False Info' one at a time. Show them the Duchess token, then fingers (1, 2, 3) equaling the number of evil players marked 'Visitor' or, if you are waking the player marked 'False Info,' show them any number of fingers except the number of evil players marked 'Visitor'."),
            NightOrderInfo.onRole(Role.TOYMAKER, false, false, "If it is a night when a Demon attack could end the game, and the Demon is marked 'Final night: No Attack,' then the Demon does not act tonight. (Do not wake them.)"),
            NightOrderInfo.onRole(Role.WRAITH, true, false, true, "If the Wraith has no ability, inform them they may not visit other players tonight."),
            // Travelers
            NightOrderInfo.onRole(Role.BARISTA, true, false, false, true, "Choose a player, wake them and tell them which Barista power is affecting them. Treat them accordingly (sober/healthy/true info or activate their ability twice)."),
            NightOrderInfo.onRole(Role.CACKLEJACK, true, false, "Replace the character token of any player (besides the player the Cacklejack chose today) with a different character token. Wake that player and show them the 'You are' card and their new character token."),
            NightOrderInfo.onRole(Role.BUREAUCRAT, true, false, false, true, "The Bureaucrat points to a player. Put the Bureaucrat's '3 Votes' reminder by the chosen player's character token."),
            NightOrderInfo.onRole(Role.THIEF, true, false, false, true, "The Thief points to a player. Put the Thief's 'Negative Vote' reminder by the chosen player's character token."),
            NightOrderInfo.onRole(Role.HARLOT, true, false, false, true, "The Harlot points at any player. Then, put the Harlot to sleep. Wake the chosen player, show them the 'This character selected you' token, then the Harlot token. That player either nods their head yes or shakes their head no. If they nodded their head yes, wake the Harlot and show them the chosen player's character token. Then, you may decide that both players die."),
            NightOrderInfo.onRole(Role.BONE_COLLECTOR, true, false, false, true, "The Bone Collector either shakes their head no or points at any dead player. If they pointed at any dead player, put the Bone Collector's 'Has Ability' reminder by the chosen player's character token. (They may need to be woken tonight to use it.)"),
            NightOrderInfo.onRole(Role.PHILOSOPHER, true, false, false, true, "If the Philosopher has not used their ability: The Philosopher may choose a good character on the script. If they chose a character: they gain that characters ability. If the character is in play, the original character's player becomes drunk."),
            NightOrderInfo.onRole(Role.POPPY_GROWER, false, false, true, true, "If the Poppy Grower has died, show the Minions/Demon who each other are."),
            NightOrderInfo.onRole(Role.SAILOR, true, false, false, true, "The previously drunk player is no longer drunk. The Sailor chooses a living player. Either the Sailor, or the chosen player, is drunk."),
            NightOrderInfo.onRole(Role.ENGINEER, true, false, false, true, "The Engineer either chooses not to use their ability, picks a Demon, or picks the relevant number of Minions. If the Engineer chose characters, replace the Demon or Minions with the choices, then wake the relevant players and inform them of their new characters."),
            NightOrderInfo.onRole(Role.PREACHER, true, false, false, true, "The Preacher chooses a player. If a Minion is chosen, wake the Minion and inform them they were chosen by the Preacher."),
            // Xaan: no entry on other nights either, see the first-night note above
            NightOrderInfo.onRole(Role.POISONER, true, false, false, true, "The previously poisoned player is no longer poisoned. The Poisoner chooses a player. That player is poisoned."),
            NightOrderInfo.onRole(Role.COURTIER, true, false, false, true, "The Courtier may choose a character on the script. If the Courtier used their ability: If that character is in play, that player is drunk."),
            NightOrderInfo.onRole(Role.INNKEEPER, true, false, false, true, "The previously protected and drunk players lose those markers. The Innkeeper chooses two players. Those players are protected. One is drunk."),
            NightOrderInfo.onRole(Role.WIZARD, true, true, "If they haven't already, the Wizard may make their one wish. If the Wizard's wish requires actions at night, run these."),
            NightOrderInfo.onRole(Role.GAMBLER, true, false, false, true, "The Gambler chooses a player, and a character on the script. If incorrect, the Gambler dies."),
            NightOrderInfo.onRole(Role.ACROBAT, true, false, false, true,"The Acrobat chooses a player. If that player is or becomes drunk or poisoned tonight (or is the Drunk role), the Acrobat dies."),
            NightOrderInfo.onRole(Role.SNAKE_CHARMER, true, false, false, true, "The Snake Charmer chooses a player. If that player is the Demon: swap the Demon and Snake Charmer character and alignments. Wake each player to inform them of their new role and alignment. The new Snake Charmer is poisoned."),
            NightOrderInfo.onRole(Role.MONK, true, false, false, true, "The previously protected player is no longer protected. The Monk chooses a player not themself. Mark that player 'Safe'."),
            NightOrderInfo.onRole(Role.ORGAN_GRINDER, true, false, false, true, "Wake the Organ Grinder. If they choose to be Drunk, mark them as such. If they choose to be sober, remove the Drunk reminder."),
            NightOrderInfo.onRole(Role.DEVILS_ADVOCATE, true, false, false, true, "The Devil's Advocate chooses a living player, different from the previous night. That player survives execution tomorrow."),
            NightOrderInfo.onRole(Role.WITCH, true, false, false, true, "If there are 4 or more players alive: The Witch chooses a player. If that player nominates tomorrow they die immediately."),
            NightOrderInfo.onRole(Role.CERENOVUS, true, false, false, true, "The Cerenovus chooses a player and a good character on the script. Wake that player. Inform them they have been targeted by the Cerenovus and what character they must be mad they are. If the player is not mad about being that character tomorrow, they can be executed."),
            NightOrderInfo.onRole(Role.PIT_HAG, true, false, false, true, "The Pit-Hag chooses a player and a character on the script. If this character is not in play, wake that player and inform them they are that character (if it's a character that only acts on the first night, perform those actions now). If the character is in play, nothing happens."),
            NightOrderInfo.onRole(Role.FEARMONGER, true, false, false, true, "The Fearmonger chooses a player. If different from the previous night, place the Fear token next to that player and announce that a new player has been selected with the Fearmonger ability."),
            NightOrderInfo.onRole(Role.HARPY, true, false, false, true, "The Harpy chooses two players. Wake the 1st player the Harpy chose, and inform them they have been targeted by a Harpy, and they must be mad that the 2nd player the Harpy chose is evil, or one or both might die."),
            NightOrderInfo.onRole(Role.MEZEPHELES, false, true, "Wake the 1st good player that said the Mezepheles' secret word and inform them they are now evil."),
            NightOrderInfo.onRole(Role.SCARLET_WOMAN, false, false, true, true, DeathTriggerType.OTHER,"If the Scarlet Woman became the Demon today: tell them which demon they have become."),
            NightOrderInfo.onRole(Role.SUMMONER, true, false, false, true, "If it is the 3rd night, wake the Summoner. They choose a player and a Demon on the character script. That player becomes that Demon."),
            NightOrderInfo.onRole(Role.LUNATIC, true, false, false, true, "Allow the Lunatic to do the Demon actions. Place their 'chosen' markers. Wake the Demon. Tell them the Lunatic player. If the Lunatic chose players: Tell the Demon each marked player. Remove any Lunatic 'chosen' markers."),
            NightOrderInfo.onRole(Role.EXORCIST, true, false, false, true, "The Exorcist chooses a player, different from the previous night. If that player is the Demon: Wake the Demon. Tell them the player Exorcist and which player it is. The Demon does not act tonight."),
            NightOrderInfo.onRole(Role.LYCANTHROPE, true, false, false, true, "The Lycanthrope chooses a living player: if good, they die and the Demon does not kill tonight."),
            NightOrderInfo.onRole(Role.PRINCESS, false, false, "If it was the Princess' first day today, and they nominated and executed a player, the Demon doesn't kill."),
            NightOrderInfo.onRole(Role.LEGION, false, false, false, true, "The Storyteller may choose a player, that player dies."),
            NightOrderInfo.onRole(Role.IMP, true, false, false, true, "The Imp chooses a player. That player dies. If the Imp chose themselves: one of the alive Minions becomes the Imp. Inform the new Imp of their character."),
            NightOrderInfo.onRole(Role.ZOMBUUL, true, false, false, true, "If no-one died during the day: The Zombuul chooses a player. That player dies."),
            NightOrderInfo.onRole(Role.PUKKA, true, false, false, true, "The Pukka chooses a player. That player is poisoned. The previously poisoned player dies."),
            NightOrderInfo.onRole(Role.SHABALOTH, true, false, false, true, "One player that the Shabaloth chose the previous night might be resurrected. The Shabaloth chooses two players. Those players die."),
            NightOrderInfo.onRole(Role.PO, true, false, false, true, "If the Po chose no-one the previous night: The Po chooses three players. Otherwise: The Po may choose a player. Chosen players die"),
            NightOrderInfo.onRole(Role.FANG_GU, true, false, false, true, "The Fang Gu chooses a player. That player dies. Or, if that player was an Outsider and there are no other Fang Gu in play: The Fang Gu dies instead of the chosen player. The chosen player is now an evil Fang Gu. Wake the new Fang Gu and tell them their new role and that they are evil."),
            NightOrderInfo.onRole(Role.NO_DASHII, true, false, false, true, "The No Dashii chooses a player. That player dies."),
            NightOrderInfo.onRole(Role.VORTOX, true, false, false, true, "The Vortox chooses a player. That player dies."),
            NightOrderInfo.onRole(Role.LORD_OF_TYPHON, true, false, false, true, "The Lord of Typhon chooses a player. That player dies."),
            NightOrderInfo.onRole(Role.VIGORMORTIS, true, false, false, true, "The Vigormortis chooses a player. That player dies. If a Minion, they keep their ability and one of their Townsfolk neighbors is poisoned."),
            NightOrderInfo.onRole(Role.OJO, true, false, false, true, "The Ojo chooses a character on the script. If it is in play, that player dies. If it is not in play, the Storyteller chooses who dies instead."),
            NightOrderInfo.onRole(Role.AL_HADIKHIA, true, false, false, true, "The Al-Hadikhia might choose 3 players. If so, announce the first player, wake them and ask if they would like to live or die, kill or resurrect accordingly, then put to sleep and announce the next player. If all 3 are alive after this, all 3 die."),
            NightOrderInfo.onRole(Role.LLEECH, true, false, false, true, "The Lleech chooses a player. That player dies."),
            NightOrderInfo.onRole(Role.LIL_MONSTA, true, true, "Wake all Minions together. Allow them to decide who they want to babysit Lil' Monsta. The Storyteller may choose a player, that player dies."),
            NightOrderInfo.onRole(Role.YAGGABABBLE, true, false, false, true, "Choose a number of players up to the total number of times the Yaggababble publicly said their secret phrase today, those players die."),
            NightOrderInfo.onRole(Role.KAZALI, true, false, false, true, "The Kazali chooses a player. That player dies"),
            NightOrderInfo.onRole(Role.ASSASSIN, true, false, false, true, "If the Assassin has not used their ability: The Assassin may choose a player. That player dies."),
            NightOrderInfo.onRole(Role.GODFATHER, true, false, false, true, "If an Outsider died today: The Godfather chooses a player. That player dies."),
            NightOrderInfo.onRole(Role.GOSSIP, false, false, "If the Gossip's public statement was true: Choose a player not protected from dying tonight. That player dies."),
            NightOrderInfo.onRole(Role.HATTER, true, false, true, true, DeathTriggerType.ANY, "Wake the Minions and Demon. Each player may choose another character of the same type as their current character. If a second player would end up with the same character as another player, ask them to choose again. Change each player to the character they chose."),
            NightOrderInfo.onRole(Role.BARBER, true, false, true, true, DeathTriggerType.ANY, "If the Barber died today: Wake the Demon. Tell them the Barber has died. The Demon may choose 2 players. If they chose players: Swap those players characters. Wake each player, and inform them of their new character."),
            NightOrderInfo.onRole(Role.SWEETHEART, false, false, true, true, DeathTriggerType.ANY, "Choose a player to be drunk from now on."),
            NightOrderInfo.onRole(Role.PLAGUE_DOCTOR, false, false, true, true, DeathTriggerType.ANY, "The Storyteller now has a Minion ability."),
            NightOrderInfo.onRole(Role.SAGE, true, false, true, true, DeathTriggerType.DEMON,"If the Sage was killed by a Demon: Tell them two players, one of which is that Demon."),
            NightOrderInfo.onRole(Role.BANSHEE, true, false, true, true, DeathTriggerType.DEMON,"If the Banshee was killed by the Demon, announce that the Banshee has died."),
            NightOrderInfo.onRole(Role.PROFESSOR, true, false, false, true, "If the Professor has not used their ability: The Professor may choose a player. If that player is a Townsfolk, they are now alive."),
            NightOrderInfo.onRole(Role.CHOIRBOY, true, false, true, true, DeathTriggerType.OTHER, "If the King was killed by the Demon, wake the Choirboy and tell them who the Demon player is."),
            NightOrderInfo.onRole(Role.HUNTSMAN, true, false, false, true, "The Huntsman may choose a player. If they choose the Damsel, wake that player, and inform them which not not-in-play Townsfolk they have become."),
            NightOrderInfo.onRole(Role.DAMSEL, true, false, "If selected by the Huntsman, wake the Damsel, and inform them which not not-in-play Townsfolk they have become."),
            NightOrderInfo.onRole(Role.AMNESIAC, true, true, "If the Amnesiac's ability causes them to wake tonight: Wake the Amnesiac and run their ability."),
            NightOrderInfo.onRole(Role.FARMER, false, false, true, true, DeathTriggerType.NIGHT,"If a Farmer died tonight, choose another good player and make them the Farmer. Wake this player, inform them they are now the Farmer."),
            NightOrderInfo.onRole(Role.TINKER, false, false, false, true, "The Tinker might die."),
            NightOrderInfo.onRole(Role.MOONCHILD, false, false, "If the Moonchild used their ability to target a player today: If that player is good, they die."),
            NightOrderInfo.onRole(Role.GRANDMOTHER, false, false, true,true, DeathTriggerType.OTHER,"If the Grandmother's grandchild was killed by the Demon tonight: The Grandmother dies."),
            NightOrderInfo.onRole(Role.RAVENKEEPER, true, false, true, true, DeathTriggerType.NIGHT,"If the Ravenkeeper died tonight: The Ravenkeeper chooses a player. Show that player's character token."),
            NightOrderInfo.onRole(Role.EMPATH, true, false, false, true, "Tell them a number (0, 1, 2) for how many evil alive neighbors they have."),
            NightOrderInfo.onRole(Role.FORTUNE_TELLER, true, false, false, true, "The Fortune Teller chooses two players. Tell them either 'yes' or 'no' for whether one of those players is the Demon or red herring."),
            NightOrderInfo.onRoleDeathAndExecutionBased(Role.UNDERTAKER, true, "If a player was executed today: Tell them that player's character."),
            NightOrderInfo.onRole(Role.DREAMER, true, false, false, true, "The Dreamer chooses a player. Tell them 1 good and 1 evil character; one of these is correct."),
            NightOrderInfo.onRole(Role.FLOWERGIRL, true, false, false, true, "Tell them 'yes' or 'no' for whether the Demon voted today. Place the 'Demon Not Voted' marker (remove 'Demon Voted', if any)."),
            NightOrderInfo.onRole(Role.TOWN_CRIER, true, false, false, true, "Tell them 'yes' or 'no' for whether a Minion nominated today. Place the 'Minion Not Nominated' marker (remove 'Minion Nominated', if any)."),
            NightOrderInfo.onRole(Role.ORACLE, true, false, false, true, "Tell them the number (0, 1, 2, etc.) of dead evil players."),
            NightOrderInfo.onRole(Role.SEAMSTRESS, true, false, false, true, "If the Seamstress has not yet used their ability: The Seamstress may choose two other players. If the Seamstress chose players, nod 'yes' or shake 'no' for whether they are of the same alignment."),
            NightOrderInfo.onRole(Role.JUGGLER, true, true, "If today was the Juggler's first day: Tell them the number (0, 1, 2, etc.) of 'Correct' markers. Remove markers."),
            NightOrderInfo.onRole(Role.BALLOONIST, true, false, false, true, "Tell them a player whose character type does not match the type from the previous night. Move the Balloonist's 'Know' reminder next to that player."),
            NightOrderInfo.onRole(Role.VILLAGE_IDIOT, true, false, false, true, "The Village Idiot chooses a player; tell them if that player is good or evil."),
            NightOrderInfo.onRole(Role.KING, true, false, false, true, "If the dead equal or outnumber the living, tell the King a character of a living player."),
            NightOrderInfo.onRole(Role.BOUNTY_HUNTER, true, false, "If the known evil player has died, tell them another evil player. "),
            NightOrderInfo.onRole(Role.NIGHTWATCHMAN, true, false, false, true, "If the Nightwatchman has not used their ability: The Nightwatchman may point to a player. Wake that player, show the 'This character selected you' card and the Nightwatchman token, then point to the Nightwatchman player."),
            NightOrderInfo.onRole(Role.CULT_LEADER, false, false, false, true, "The cult leader might change alignment. If so, inform them accordingly."),
            NightOrderInfo.onRole(Role.BUTLER, true, false, false, true, "The Butler chooses a player. Mark that player as 'Master'."),
            NightOrderInfo.onRole(Role.ARTIST, true, false, false, true, "The Artist may ask their yes/no question."),
            NightOrderInfo.onRole(Role.SAVANT, true, false, false, true, "Tell the Savant 2 pieces of information, one truth and one lie."),
            NightOrderInfo.onRole(Role.FISHERMAN, true, false, false, true, "The Fisherman may ask for advice to help their team win."),
            NightOrderInfo.onRole(Role.SPY, true, false, false, true, "Show the Grimoire to the Spy for as long as they need."),
            NightOrderInfo.onRole(Role.HIGH_PRIESTESS, true, false, false, true, "Tell them the player you believe they should talk to most."),
            NightOrderInfo.onRole(Role.GENERAL, true, false, false, true, "Tell the General that either good is winning, evil is winning, or that it's anyone's game."),
            NightOrderInfo.onRole(Role.CHAMBERMAID, true, false, false, true, "The Chambermaid chooses two players. Tell them a number (0, 1, 2, …) for how many of those players wake tonight for their ability."),
            NightOrderInfo.onRole(Role.MATHEMATICIAN, true, false, false, true, "Tell them the number (0, 1, 2, etc.) of players whose ability malfunctioned due to other abilities."),
            NightOrderInfo.onRole(Role.TOR, false, false, "If a player died tonight, you may wake them and show them their character token (and their alignment if their character has changed since the start of the game)."),
            NightOrderInfo.onRole(Role.RIOT, true, false, "Night 3: Minions become Riot. Visit each Minion and change their character to Riot."),
            NightOrderInfo.staticAction(StaticAction.DAWN, false, ""),
            NightOrderInfo.staticAction(StaticAction.NOMINATIONS, false, "Open nominations for the day. Players may now nominate and be nominated.")
    );


    public static List<NightOrderInfo> getFirstNightOrder() {
        return FIRST_NIGHT_ORDER;
    }

    public static List<NightOrderInfo> getOtherNightOrder() {
        return OTHER_NIGHTS_ORDER;
    }
}