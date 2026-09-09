package com.autumnwind.botb.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * How to run every role the mod special-cases. Only roles with an entry appear in the Role
 * Guides screen. Paragraphs are separated by newlines.
 */
public final class RoleGuides {

    private RoleGuides() {}

    private static final Map<Role, String> GUIDES = new EnumMap<>(Role.class);

    private static void guide(Role role, String text) {
        GUIDES.put(role, text);
    }

    static {
        // Townsfolk
        guide(Role.ALCHEMIST, "Open the Alchemist's reminders and pick the minion ability you're giving them. The Role HUD tells you which one it is, and the minion's visits get added to the night order like any other role reminder.");
        guide(Role.AMNESIAC, "Marked by default (ctrl-click to unmark). Their Other Nights visit appears every night until you unmark them.");
        guide(Role.ATHEIST, "The setup validator won't let you send any evil players while an Atheist is assigned.\nDuring nominations there's a Storyteller head in the middle of the grimoire. Alt-click the nominator and then the head to nominate yourself, once per day. If you get executed you're teleported to the execution spot like anyone else.");
        guide(Role.BANSHEE, "Triggers when the Banshee dies at night with the demon's \"Dead\" reminder on them. Add the Banshee's \"Has Ability\" reminder and from then on they can nominate twice, vote twice, and aren't treated as dead for voting.\nFlipping their lever off and on again switches between 2 votes and 1, so they can pick how many to use.");
        guide(Role.BOUNTY_HUNTER, "The validator needs exactly one evil townsfolk per Bounty Hunter. Put \"Known\" on that player and the Role HUD tells you who it is.");
        guide(Role.CANNIBAL, "When a living player is executed, a visit triggers after dusk reminding you the Cannibal has a new ability. Add the executed role as a role reminder on the Cannibal and they get its visits at that role's position. First night only roles get a triggered visit right away.");
        guide(Role.CHOIRBOY, "Triggers when the King dies at night with the demon's \"Dead\" reminder on them, and the Role HUD tells you who the demon is.");
        guide(Role.EXORCIST, "Put the Exorcist's \"Chosen\" reminder on the demon and the demon's visit will be skipped that night.");
        guide(Role.FARMER, "Triggers when the Farmer dies at night, unless they're droisoned.");
        guide(Role.FORTUNE_TELLER, "Put \"Red Herring\" on a player and the Role HUD shows you who it is.");
        guide(Role.GRANDMOTHER, "Put \"Grandchild\" on the grandchild. The first night Role HUD tells you who they are, and if they die at night with the demon's \"Dead\" reminder on them, the Grandmother's visit triggers.");
        guide(Role.INVESTIGATOR, "Put \"Minion\" on the real minion and \"Wrong\" on the decoy. The Role HUD lists both players and which minion they are.");
        guide(Role.JUGGLER, "Marked by default (ctrl-click to unmark). Their Other Nights visit appears every night until you unmark them.");
        guide(Role.KING, "The first night, Demon Info gets a King icon and the Role HUD tells you to show the demon who they are. After that, the King only wakes when there are at least as many dead players as living ones.");
        guide(Role.KNIGHT, "Put \"Know\" on the two players and the Role HUD lists them.");
        guide(Role.LIBRARIAN, "Put \"Outsider\" on the real outsider and \"Wrong\" on the decoy. The Role HUD lists both players and which outsider they are.");
        guide(Role.MAGICIAN, "No visit of their own. Minion Info and Demon Info get a Magician icon and the Role HUD lists the Magician alongside the demon and minions.");
        guide(Role.NOBLE, "Put \"Know\" on the three players and the Role HUD lists them.");
        guide(Role.PHILOSOPHER, "Add the role they chose as a role reminder. Their own visit goes away and they get that role's visits instead, with a triggered visit right away if the role only acts on the first night. If the chosen role is in play, give that player the drunk reminder.");
        guide(Role.PIXIE, "Add the townsfolk as a role reminder, which will enable the Pixie's madness HUD once you send roles again. If they've been mad and that townsfolk dies, add \"Has Ability\" to turn on the townsfolk visits.");
        guide(Role.POPPY_GROWER, "No visit of their own. Minion Info and Demon Info get a Poppy Grower icon instead. When they die, a visit triggers targeting the demon and all the minions.");
        guide(Role.PREACHER, "Put the Preacher's \"No Ability\" reminder on a minion to disable their visits and other ability effects.");
        guide(Role.RAVENKEEPER, "Triggers when the Ravenkeeper dies at night.");
        guide(Role.SAGE, "Triggers when the Sage dies at night with the demon's \"Dead\" reminder on them.");
        guide(Role.SNITCH, "No visit of their own. Minion Info gets a Snitch icon and instructions instead.");
        guide(Role.STEWARD, "Put \"Know\" on the good player and the Role HUD tells you who it is.");
        guide(Role.UNDERTAKER, "Only wakes if someone was executed today, and the Role HUD tells you which role.");
        guide(Role.WASHERWOMAN, "Put \"Townsfolk\" on the real townsfolk and \"Wrong\" on the decoy. The Role HUD lists both players and which townsfolk they are.");

        // Outsiders
        guide(Role.BARBER, "Triggers when the Barber dies, targeting the demon. If you swap two players' roles, alt-ctrl right click each of them to send their new role individually as you visit them.");
        guide(Role.DAMSEL, "No first night visit, Minion Info gets a Damsel icon instead. A Huntsman on the script lets the validator accept an extra outsider.");
        guide(Role.DRUNK, "Add a not-in-play townsfolk as a role reminder, and the player will be sent that role instead. They get the fake role's visits, but it doesn't affect anything else.");
        guide(Role.HATTER, "When the Hatter dies, a visit triggers targeting the demon and all the minions.");
        guide(Role.HERMIT, "Assigning the Hermit adds every outsider on the script as a role reminder. Outsider abilities are real, anything else is fake.\nIf you give them the Drunk, add a townsfolk too and that's what gets sent (same for the Lunatic and a demon).");
        guide(Role.LUNATIC, "Add a demon as a role reminder, and the player will be sent that role instead. The first night they get the demon's first night instructions, and on other nights the demon's instructions are added onto the Lunatic's own visit. With the Zombuul, they're skipped if someone has the Zombuul's \"Died Today\" reminder.");
        guide(Role.MUTANT, "Shows up in your madness HUD automatically, but not the player's.");
        guide(Role.OGRE, "Always sent as good no matter what alignment you set. The real alignment is only sent at game end.");
        guide(Role.PLAGUE_DOCTOR, "When they die, a visit triggers. Add \"Storyteller Ability\", then pick the minion from their reminders, and you get that minion's visit with no teleport (works with the Organ Grinder).");
        guide(Role.SWEETHEART, "Triggers when the Sweetheart dies, unless they're droisoned.");

        // Minions
        guide(Role.BOFFIN, "Open the demon's reminders and pick the good ability. The Boffin's visit targets both of them and the Role HUD tells you what ability the demon has.");
        guide(Role.CERENOVUS, "Put \"Mad\" on the player, then open their reminders again and give them \"Mad: [Role]\". Send roles again to update their madness HUD.");
        guide(Role.EVIL_TWIN, "Put \"Twin\" on the good twin and the Role HUD tells you who it is.");
        guide(Role.GODFATHER, "The first night Role HUD lists the outsiders in play. Put the Godfather's \"Died Today\" reminder on an outsider that died to enable the Godfather's night visit.");
        guide(Role.HARPY, "Put \"Mad\" on the first player, then open their reminders again and add a reminder for the second player. Send roles again to update their madness HUD.");
        guide(Role.MARIONETTE, "Add a not-in-play good role as a role reminder, and the player will be sent that role instead. They don't wake for Minion Info, and Demon Info tells the demon who they are.");
        guide(Role.MEZEPHELES, "Marked by default (ctrl-click to unmark). Their Other Nights visit appears every night until you unmark them.");
        guide(Role.ORGAN_GRINDER, "While the Organ Grinder has their ability, votes are secret. Players are blinded and levers are muted during the vote. Only Storytellers see the results and the player marked for execution.");
        guide(Role.SCARLET_WOMAN, "Triggers when the demon dies with 5 or more players alive. Put the Fang Gu's \"Once\" reminder on the old Fang Gu when they jump to suppress this trigger.");
        guide(Role.SUMMONER, "The validator expects no demon and an extra minion. The Summoner only wakes on night 3.");
        guide(Role.VIZIER, "There's a first day visit as a reminder to announce them.");
        guide(Role.WITCH, "Skipped once 3 or fewer players are alive.");
        guide(Role.WIZARD, "Marked by default (ctrl-click to unmark). Their Other Nights visit appears every night until you unmark them.");
        guide(Role.WRAITH, "When you move to any evil player's visit, the Wraith gets teleported there too. Mark the Wraith (ctrl-click) to supress teleports, and add a visit just after dusk to inform them not visit other players. Dying doesn't unmark them.");
        guide(Role.XAAN, "The outsider count gets recorded on the Xaan as a \"Night N\" reminder at the first dusk, a custom reminder with the same format works too. On that night and the following day, every townsfolk counts as poisoned. You can manually cause the X icon by adding the Xaan's \"X\" reminder to any player, though it won't poison anyone on its own.");

        // Demons
        guide(Role.AL_HADIKHIA, "The \"AH\" button in storyteller tools turns on the homebrew version, which asks every player if they want to live or die each night. The 1, 2, and 3 reminders count as demon kills for triggers.");
        guide(Role.FANG_GU, "The validator expects an extra outsider. Put \"Once\" on the old Fang Gu when they jump to suppress the Scarlet Woman's trigger.");
        guide(Role.LEGION, "The validator needs most of the players to be Legion. When only evil players vote, you're told the vote counts for zero, but players aren't.");
        guide(Role.LEVIATHAN, "There's a first day visit as a reminder to announce them. From day 5 on you get a game over visit after nominations.");
        guide(Role.LIL_MONSTA, "The validator lets you send no demon and an extra minion. Put the global \"Is The Demon\" reminder on whoever's babysitting and the minions will get the Lil' Monsta visit.");
        guide(Role.LORD_OF_TYPHON, "The validator checks that evil players are seated in a line with the Lord of Typhon in the middle.");
        guide(Role.RIOT, "Night 3 you get a visit to change every minion to Riot. On day 3, nominations gets the Riot instructions. The deaths and the countdown are managed by the Storyteller.");
        guide(Role.VIGORMORTIS, "Put \"Has Ability\" on a killed minion to have them keep their ability.");
        guide(Role.VORTOX, "Every townsfolk visit gets a Vortox icon and the Role HUD reminds you to lie.");
        guide(Role.ZOMBUUL, "Wakes even when dead, unless someone has the Zombuul's \"Died Today\" reminder.");

        // Travelers
        guide(Role.BEGGAR, "Ctrl-alt-click a dead player's head to toggle their ghost vote.");
        guide(Role.BISHOP, "Only you can nominate. Alt-click the Storyteller head in the middle, then the player.");
        guide(Role.BONE_COLLECTOR, "Put the Bone Collector's \"Has Ability\" on the dead player to restore their ability.");
        guide(Role.BUREAUCRAT, "Put \"3 Votes\" on the player to make their vote counts triple.");
        guide(Role.BUTCHER, "After an execution, nominations stay open and only the Butcher can nominate.");
        guide(Role.THIEF, "Put \"Negative Vote\" on the player and their vote counts as negative.");
        guide(Role.VOUDON, "While the Voudon has their ability, only dead players and the Voudon can vote, and ghost votes don't get used up. Exiles aren't affected.");

        // Fabled
        guide(Role.BOOTLEGGER, "Ctrl-click the Bootlegger in the script builder to write the script's special rules. They replace the ability text wherever it's shown.");
        guide(Role.BUDDHIST, "Dawn gets a Buddhist icon as a reminder.");
        guide(Role.DJINN, "Jinxes are added to the script reference automatically, but are not enforced. The script reference has a ? next to the jinxes that opens the Djinn's details page.");
        guide(Role.DUCHESS, "Put \"Visitor\" or \"False Info\" on the players and the Duchess visit targets them.");
        guide(Role.SENTINEL, "The validator allows one more or one fewer outsider.");
        guide(Role.SPIRIT_OF_IVORY, "The validator allows at most one extra evil player. It's up to the Storyteller to enforce this throughout a game.");
        guide(Role.STORM_CATCHER, "The first night visit wakes every evil player.");
        guide(Role.TOYMAKER, "Minion Info and Demon Info run even under 7 players. Put \"Final Night: No Attack\" on the demon and the Role HUD will display that.");

        // Loric
        guide(Role.GOD_OF_UG, "Put \"Ug hat\" on the player and their vote counts double.");
        guide(Role.POPE, "Lets you assign the same role twice and use in-play roles as bluffs.");
        guide(Role.TOR, "Send roles sends every living player no role. There's a reminder visit just before dawn for the Storyteller to send roles to inform dead players who they are. You can also inform them individually by alt-ctrl right clicking a specific dead player.");
    }

    /** Roles with a guide, grouped by team and then alphabetically. */
    public static List<Role> roles() {
        List<Role> roles = new ArrayList<>(GUIDES.keySet());
        roles.sort(Comparator.comparing((Role r) -> r.getType().ordinal()).thenComparing(Role::getDisplayName));
        return roles;
    }

    public static String get(Role role) {
        return GUIDES.getOrDefault(role, "");
    }
}
