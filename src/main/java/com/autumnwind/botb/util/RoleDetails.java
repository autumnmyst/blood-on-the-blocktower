package com.autumnwind.botb.util;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoleDetails {

    public record RoleDetailData(String flavorText, String summary, String examples, String artist) {}

    private static final Map<Role, RoleDetailData> DETAILS_MAP = Stream.of(
            // --- TOWNSFOLK ---


            //TB
            entry(Role.CHEF,
                    "This evening's reservations seem odd. Never before has Mrs Mayweather kept company with that scamp from Hudson Lane. Yet, tonight, they have a table for two. Strange.",
                    """
                            The Chef knows if evil players are sitting next to each other.

                            - On the first night, the Chef learns exactly how many pairs of evil players there are in total. A pair is two players, but one player may be a part of two pairs. So, two players sitting next to each other is one pair. Three players sitting next to each other is two pairs. Four players sitting next to each other is three pairs. And so on.
                            """,
                            """
                            - No evil players are sitting next to each other. The Chef learns a '0'.

                            - The Imp is sitting next to the Baron. Across the circle, the Poisoner is sitting next to the Scarlet Woman. The Chef learns a '2'.

                            - The Recluse is sitting between the Imp and the Poisoner. The Chef learns a '1'. Here, the Recluse is registering as evil for the Imp-Recluse pair, but as good for the Poisoner-Recluse pair.
                            """, "The Yogscast"),
            entry(Role.INVESTIGATOR,
                    "It is a fine night for a stroll, wouldn't you say, Mister Morozov? Or should I say... BARON Morozov?",
                    """
                            The Investigator learns that a particular Minion character is in play, but not exactly which player it is.

                            - During the first night, the Investigator learns that one of two players is a specific Minion.

                            - They learn this only once and then learn nothing more.
                            """,
                    """
                            - Amy is the Baron, and Julian is the Mayor. The Investigator learns that either Amy or Julian is the Baron.

                            - Angelus is the Spy, and Lewis is the Poisoner. The Investigator learns that either Angelus or Lewis is the Spy.

                            - Brianna is the Recluse, and Marianna is the Imp. The Investigator learns that either Brianna or Marianna is the Poisoner. (This happens because the Recluse is registering as a Minion—in this case, the Poisoner.)
                            """, "The Yogscast"),
            entry(Role.WASHERWOMAN,
                    "Bloodstains on a dinner jacket? No, this is cooking sherry. How careless.",
                    """
                            The Washerwoman learns that a specific Townsfolk is in play, but not who is playing them.

                            - During the first night, the Washerwoman is woken, shown two players, and learns the character of one of them.

                            - They learn this only once and then learn nothing more.
                            """,
                    """
                            - Evin is the Chef, and Amy is the Ravenkeeper. The Washerwoman learns that either Evin or Amy is the Chef.

                            - Julian is the Imp, and Alex is the Virgin. The Washerwoman learns that either Julian or Alex is the Virgin.

                            - Marianna is the Spy, and Sarah is the Scarlet Woman. The Washerwoman learns that one of them is the Ravenkeeper. Here, the Spy is registering as a Townsfolk—in this case, the Ravenkeeper.
                            """, "The Yogscast"),
            entry(Role.LIBRARIAN,
                    "Certainly madam, under normal circumstances, you may borrow the Codex Malificarium from the library vaults. However, you do not seem to be a member.",
                    """
                            The Librarian learns that a particular Outsider character is in play, but not exactly which player it is.

                            - During the first night, the Librarian learns that one of two players is a specific Outsider.

                            - They learn this only once and then learn nothing more.

                            - The Drunk is an Outsider. If the Librarian learns that one of two players is the Drunk, they do not learn the Townsfolk that the Drunk's player thinks they are.
                            """,
                    """
                            - Benjamin is the Saint, and Filip is the Baron. The Librarian learns that either Benjamin or Filip is the Saint.

                            - There are no Outsiders in this game. The Librarian learns a '0'.

                            - Abdallah is the Drunk, who thinks they are the Monk, and Douglas is the Undertaker. The Librarian learns that either Abdallah or Douglas is the Drunk. (This happens because the Librarian learns the true character. The Drunk is Abdallah’s true character, not the Monk.)
                            """, "The Yogscast"),
            entry(Role.EMPATH,
                    "My skin prickles. Something is not right here. I can feel it.",
                    """
                            The Empath keeps learning if their living neighbours are good or evil.

                            - The Empath only learns how many of their neighbours are evil, not which one is evil.

                            - The Empath does not detect dead players. So, if the Empath is sitting next to a dead player, they do not get info about that dead player. Instead, they get info about the closest alive player in that direction.

                            - The Empath acts after the Demon, so if the Demon kills one of the Empath's alive neighbours, the Empath does not learn about the now-dead player. The Empath's information is accurate at dawn, not at dusk.
                            """,
                    """
                            The Empath neighbours two good players—a Soldier and a Monk . The Empath learns a '0'.

                            The next day, the Soldier is executed. That night, the Monk is killed by the Imp. The Empath now detects the players sitting next to the Soldier and the Monk, which are a Librarian and an evil Gunslinger. The Empath now learns a '1'.

                            There are only three players left alive: the Empath, the Imp, and the Baron. No matter who is seated where, the Empath learns a '2'.
                            """, "The Yogscast"),
            entry(Role.FORTUNE_TELLER,
                    "I sense great evil in your soul! But... that could just be your perfume. I am allergic to Elderberry.",
                    """
                            The Fortune Teller detects who the Demon is, but sometimes thinks good players are Demons.

                            - Each night, the Fortune Teller chooses two players and learns if at least one of them is a Demon. They do not learn which of them is a Demon, just that one of them is. If neither is the Demon, they learn this instead.

                            - Unfortunately, one player, called the Red Herring, will register as a Demon to the Fortune Teller if chosen. The Red Herring is the same player throughout the entire game. This player may be any good player, even the Fortune Teller themself, and the Fortune Teller does not know which player it is.

                            - The Fortune Teller may choose any two players—alive or dead, or even themself. If they choose a dead Demon, then the Fortune Teller still receives a nod.
                            """,
                    """
                            - The Fortune Teller chooses the Monk and the Undertaker and learns a 'no'.

                            - The Fortune Teller chooses the Imp and the Empath, and learns a 'yes'.

                            - The Fortune Teller chooses an alive Butler and a dead Imp, and learns a 'yes'.

                            - The Fortune Teller chooses themselves and a Saint. The Saint is the Red Herring. The Fortune Teller learns a 'yes'.
                            """, "The Yogscast"),
            entry(Role.UNDERTAKER,
                    "Hmmm....what have we here? The left boot is worn down to the heel, with flint shavings under the tongue. This is the garb of a Military man.",
                    """
                            The Undertaker learns which character was executed today.

                            - The player must have died from execution for the Undertaker to learn who they are. Deaths during the day for other reasons, such as a Townsfolk dying due to nominating the Virgin, do not count.

                            - The Undertaker wakes each night except the first, as there have been no executions yet.

                            - If nobody died today, the Undertaker learns nothing. The Storyteller does not wake the Undertaker that night.

                            - If the Drunk is executed, the Undertaker is shown the Drunk character token, not the token for the Townsfolk that the Drunk player thought they were.
                            """,
                    """
                            - The Mayor is executed today. That night, the Undertaker is shown the Mayor token.

                            - The Drunk, who thinks they are the Virgin, is executed today. At night, the Undertaker is shown the Drunk token, because the Undertaker learns a player's true character, as opposed to the one they believe they are.

                            - The Spy is executed. That night, the Undertaker is shown the Butler token, because the Spy is registering as the Butler.

                            - Nobody was executed today. That night, the Undertaker does not wake.
                            """, "The Yogscast"),
            entry(Role.MONK,
                    "'Tis an ill and deathly wind that blows tonight. Come, my brother, take shelter in the abbey while the storm rages. By my word, or by my life, you will be safe.",
                    """
                            The Monk protects other players from the Demon.

                            - Each night except the first, the Monk may choose to protect any player except themself.

                            - If the Demon attacks a player who has been protected by the Monk, then that player does not die. The Demon does not get to attack another player—there is simply no death tonight.

                            - The Monk does not protect against the Demon nominating and executing someone.

                            - Demons may have abilities other than killing. The Monk's protection also prevents all other harmful effects of the Demon's ability, such as poisoning or turning the protected player evil.
                            """,
                    """
                            - The Monk protects the Fortune Teller. The Imp attacks the Fortune Teller. No deaths occur tonight.

                            - The Monk protects the Mayor, and the Imp attacks the Mayor. The Mayor's "another player dies" ability does not trigger, because the Mayor is safe from the Imp. Nobody dies tonight.

                            - The Monk protects the Imp . The Imp chooses to kill themself tonight, but nothing happens. The Imp stays alive and a new Imp is not created.
                            """, "The Yogscast"),
            entry(Role.SLAYER,
                    "Die.",
                    """
                            The Slayer can kill the Demon by guessing who they are.

                            - The Slayer can choose to use their ability at any time during the day, and must declare to everyone when they're using it. If the Slayer chooses the Demon, the Demon dies immediately. Otherwise, nothing happens.

                            - The players do not learn the identity of the dead player. After all, it may have been the Recluse!

                            - A Slayer that uses their ability while poisoned or drunk may not use it again.

                            - The Slayer will want to choose an alive player. Even if the Slayer chooses a dead Imp, nothing happens, because a dead player can't die again.

                            - Players may say whatever they want at any time, so a player who's pretending to be the Slayer may pretend to use the Slayer ability.
                            """,
                    """
                            - The Slayer chooses the Imp. The Imp dies, and good wins!

                            - The Slayer chooses the Recluse. The Storyteller decides that the Recluse registers as the Imp, so the Recluse dies, but the game continues.

                            - The Imp is bluffing as the Slayer. They declare that they use their Slayer ability on the Scarlet Woman. Nothing happens.
                            """, "The Yogscast"),
            entry(Role.SOLDIER,
                    "As David said to Goliath, as Theseus said to the Minotaur, as Arjuna said to Bhagadatta... No.",
                    """
                            The Soldier can not be killed by the Demon.

                            - The Soldier cannot die from the Demon's ability. So, if the Imp attacks the Soldier at night, nothing happens. Nobody dies. The Imp does not get to choose another player to attack instead.

                            - The Soldier can still die by execution, even if the nominator was the Demon. The Soldier is protected from the Demon's ability to kill, not the actions of the Demon player.

                            - Demons may have abilities other than killing. The Soldier is also protected from all other harmful effects of the Demon's ability, such as poisoning or turning the Soldier evil.
                            """,
                    """
                            - The Imp attacks the Soldier. The Soldier does not die, so nobody dies that night.

                            - The Poisoner poisons the Soldier, then the Imp attacks the Soldier. The Soldier dies, since they have no ability.

                            - The Imp attacks the Soldier. The Soldier dies, because they are actually the Drunk.
                            """, "The Yogscast"),
            entry(Role.RAVENKEEPER,
                    "My birds will avenge me! Fly! Fly, my sweet and dutiful pets! To the manor and to the river! To the alleys and to the salons! Fly!",
                    """
                            If the Ravenkeeper dies at night, they get to learn one player's character.

                            - The Ravenkeeper is woken on the night that they die, and chooses a player immediately.

                            - The Ravenkeeper may choose a dead player if they wish.
                            """,
                    """
                            - The Ravenkeeper is killed by the Imp, and then wakes to choose a player. After some deliberation, they choose Benjamin. Benjamin is the Empath, and the Ravenkeeper learns this.

                            - The Imp attacks the Mayor. The Mayor doesn't die, but the Ravenkeeper dies instead, due to the Mayor's ability. The Ravenkeeper is woken and chooses Douglas, who is a dead Recluse. The Ravenkeeper learns that Douglas is the Scarlet Woman, since the Recluse registered as a Minion.
                            """, "The Yogscast"),
            entry(Role.VIRGIN,
                    "I am pure. Let those who are without sin cast themselves down and suffer in my stead. My reputation shall not be stained with your venomous accusations.",
                    """
                            The Virgin may inadvertently execute their accuser, confirming which players are Townsfolk in the process.

                            - If a Townsfolk nominates the Virgin, then that Townsfolk is executed immediately. Because there can only be one execution per day, the nomination process immediately ends, even if a player was about to die.

                            - Only Townsfolk are executed due to the Virgin's ability. If an Outsider, Minion, or Demon nominates the Virgin, nothing happens, and voting continues.

                            - The Virgin's ability is powerful because if a Townsfolk nominates them and dies, then both characters are almost certainly Townsfolk.

                            - After being nominated for the first time, the Virgin loses their ability, even if the nominator did not die, and even if the Virgin was poisoned or drunk.
                            """,
                    """
                            - The Washerwoman nominates the Virgin. The Washerwoman is immediately executed and the day ends.

                            - The Drunk, who thinks they are the Chef, nominates the Virgin. The Drunk remains alive, and the Virgin loses their ability. Players may now vote on whether or not to execute the Virgin. (This happens because the Drunk is not a Townsfolk.)

                            - A dead player nominates the Virgin. The dead, however, cannot nominate. The Storyteller declares that the nomination does not count. The Virgin does not lose their ability.
                            """, "The Yogscast"),
            entry(Role.MAYOR,
                    "We must put our differences aside, and cease this senseless killing. We are all taxpayers after all. Well, most of us.",
                    """
                            The Mayor can win by peaceful means on the final day.

                            - To survive, the Mayor sometimes "accidentally" gets someone else killed. If the Mayor is attacked and would die, the Storyteller may choose that a different player dies. Nobody learns how they died at night, just that they died.

                            - If there are just three players alive at the end of the day, and no execution occurred that day, then the game ends and good wins.

                            - If the Demon attacks the Mayor, and the Storyteller instead chooses a dead player, the Soldier, or a player protected by the Monk, that player does not die tonight.
                            """,
                    """
                            - The Imp attacks the Mayor. The Storyteller chooses that the Ravenkeeper dies instead.

                            - There are three players alive. There are no nominations for execution today. Good wins.
                            """, "The Yogscast"),

            //BMR

            entry(Role.GRANDMOTHER,
                    "Take a jacket if you go outside, dearie. And your thermos. And your scarf. I have a weak heart, you know. Whatever would I do if you caught cold...or worse?",
                    """
                            The Grandmother knows who their grandchild is, but if they are killed by the Demon, the Grandmother dies too.

                            - During the first night, the Grandmother learns their Grandchild—a good player who is a Townsfolk or Outsider. The Grandchild does not learn that they have a Grandmother.

                            - If the Demon kills the Grandchild, the Grandmother dies too. If the Grandchild dies by any other means—such as execution, or another type of death at night—the Grandmother does not also die.
                            """,
                    """
                            - During the first night, the Grandmother wakes and learns that Julian, their Grandchild, is the Professor. Three nights later, Julian is killed by the Demon, so the Grandmother dies too.

                            - The Grandmother knows that Lewis, their Grandchild, is the Gambler. Lewis gambles and dies because of it. The Grandmother remains alive.

                            - The Grandmother knows that Sarah, their Grandchild, is the Tinker. Sarah is killed by the Demon, but the Grandmother is drunk because of the Sailor, so the Grandmother remains alive.
                            """, "The Yogscast"),
            entry(Role.SAILOR,
                    "I'll drink any one of yer under the table! You! The chatterbox! Reckon you can take me? No? Howza 'bout you, Grandma? You ever tried Old McKillys Extra Spiced Rum before? Guaranteed to put hairs on yer chest! Step aboard, aye!",
                    """
                            The Sailor is either drunk or getting somebody else drunk. While the Sailor is sober, they can't die.

                            - Each night, the Sailor chooses a player, who will probably get drunk.

                            - If they choose themself, they lose their “cannot die” ability until they become sober.

                            - If the Sailor chooses a dead player accidentally, the Storyteller prompts them to choose again.

                            - If the Sailor chooses another player, the Storyteller chooses which player is drunk. If they choose a Townsfolk, the Storyteller will usually make the Townsfolk drunk, but if an Outsider, a Minion, or the Demon is chosen, then the Storyteller will usually make the Sailor the drunk one.

                            - While sober, the Sailor cannot die, even if they have not yet woken at night to go drinking.
                            """,
                    """
                            - The Sailor chooses the Exorcist, and the Storyteller decides that the Exorcist is drunk. That night, the Sailor is attacked by the Shabaloth. The Sailor remains alive. The next day, the Sailor is executed but remains alive.

                            - During the day, the Gossip made a public statement they thought was false, but was actually true. That night, the Gossip ability kills a player. The Sailor has made themself drunk, and the Storyteller decides that the Sailor dies.

                            - The Sailor chooses the Mastermind, but the Storyteller decides that the Sailor is drunk. The next day, the Sailor asks to be executed to “prove they are the Sailor,” but dies because they're drunk.
                            """, "The Yogscast"),
            entry(Role.CHAMBERMAID,
                    "I aint seen nothin' untoward, Milady. Begging your pardon, but if I did see somethin', it certainly weren't the master o' the house sneaking into the professor's laboratory 'round eleven o'clock and mixing up fancy potions, just like you said, Miss.",
                    """
                            The Chambermaid learns who woke up at night.

                            - Each night, the Chambermaid chooses two players and learns if they woke tonight. They must choose alive players, and may not choose themself. This does not detect which of those players woke, only how many.

                            - This ability only detects characters who woke in order to use their ability. It does not detect characters who woke for any other reason—such as if the Storyteller woke a Minion to let them know who the Demon is, woke the Demon to give them their starting Demon info, woke a player due to the ability of a different character, or woke someone accidentally.

                            - If the character woke on a previous night but not this night, they are not detected by the Chambermaid.

                            - Players that woke tonight due to their ability but are drunk or poisoned still count as having woke tonight.

                            - If the Chambermaid chooses a dead player accidentally, the Storyteller prompts them to choose again.
                            """,
                    """
                            - The Chambermaid chooses the Exorcist and the Innkeeper, and learns a "2." The next night, the Exorcist chooses the Shabaloth, which will wake the Shabaloth. Later, Chambermaid chooses the Shabaloth and the Fool. Since the Shabaloth only woke due to the Exorcist ability, the Chambermaid learns a "0".

                            - It is the second night. The Chambermaid chooses the Grandmother and the evil Goon, and learns a "2." Only the Goon will wake tonight, but the Chambermaid is drunk.

                            - It is the first night. The Chambermaid chooses the Assassin and the Moonchild, and learns a "0" because the Assassin does not wake to use their ability on the first night. The next night, they choose the Assassin and the Gossip. The Assassin woke but did not use their ability. The Gossip ability is used, but the Gossip does not wake to use it. The Chambermaid learns a "1".
                            """, "The Yogscast"),
            entry(Role.INNKEEPER,
                    "Come inside, fair traveller, and rest your weary bones. Drink and be merry, for the legions of the Dark One shall not harass thee tonight.",
                    """
                            The Innkeeper protects people from death at night, but somebody gets drunk in the process.

                            - The Innkeeper, like the Monk, makes players safe from being killed by the Demon. They are also safe from death caused by Outsiders, Minions, and Townsfolk.

                            - The Innkeeper only protects players at night, not the day.

                            - One of the two players that the Innkeeper chooses becomes drunk for tonight and the next day. This player may be good or evil, but will almost always be good, depending how your game is going. An Innkeeper that chooses themself might become drunk, which means they have no ability and may die tonight—and the other player they chose to protect isn’t safe either.
                            """,
                    """
                            - The Innkeeper protects the Fool and the Chambermaid. The Storyteller chooses that the Fool becomes drunk. Tomorrow, when the Fool is executed, they die, even though they hadn’t used their ability yet.

                            - The Innkeeper protects the Assassin and the Po. The Storyteller chooses that the Assassin becomes drunk. Later that night, the Assassin uses their ability, but nothing happens.

                            - The Innkeeper protects themself and the Pacifist. The Storyteller chooses that the Innkeeper becomes drunk. The Pacifist is attacked by the Demon tonight and dies.
                            """, "The Yogscast"),
            entry(Role.GAMBLER,
                    "Heads, I win. Tails, you lose.",
                    """
                            The Gambler can guess who is who... but pays the ultimate price if they guess wrong.

                            - Each night except the first, the Gambler chooses a player and guesses their character by pointing to its icon on the character sheet. If the guess is correct, nothing happens. If the guess is incorrect, the Gambler dies.

                            - The Gambler does not learn from the Storyteller whether their guess is correct or incorrect.

                            - The Gambler may choose any player, dead or alive, even themself.
                            """,
                    """
                            - The Gambler points to the Minstrel player, then to the Minstrel icon. This guess is correct, so the Gambler remains alive, but is killed by the Demon tonight anyway.

                            - The Devil's Advocate is bluffing as the Pacifist. That night, the Gambler points to the Devil's Advocate player, then to the Pacifist icon. This guess is wrong, so the Gambler dies.
                            """, "The Yogscast"),
            entry(Role.EXORCIST,
                    "We cast you out, every unclean spirit, every satanic power, every onslaught of the infernal adversary, every legion, every diabolical group and sect, in the name and by the power of Our Lord Jesus Christ. We command you, begone and fly far from the Church of God, from the souls made by God in His image and redeemed by the precious blood of the divine Lamb.",
                    """
                            The Exorcist prevents the Demon from waking to attack.

                            - Each night, the Exorcist chooses a player. If they choose a player who is not the Demon, the Demon may still attack. If they choose the Demon, the Demon does not wake tonight, so does not choose players to attack tonight. The Demon learns that they cannot attack and who the Exorcist is.

                            - Any other Demon abilities still function—such as the Zombuul staying alive if killed, the Pukka killing a player they attacked on a previous night, or the Shabaloth regurgitating a player.

                            - The Exorcist may not choose the same player two nights in a row.
                            """,
                    """
                            - he Exorcist chooses the Shabaloth. The Shabaloth does not kill tonight. At dawn, the Storyteller declares that nobody died that night.

                            - The Exorcist chooses the Pukka. The Pukka does not wake to attack tonight, but a player still dies because of the Pukka’s attack during the previous night.

                            - The Po chooses to attack no one. The next night, the Exorcist chooses the Po. The Po does not wake to act tonight. The next night, the Exorcist chooses the Assassin. The Assassin can still attack tonight, and the Po chooses three players to attack, because the Po's last choice was no one.
                            """, "The Yogscast"),
            entry(Role.GOSSIP,
                    "Blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah. Blah.",
                    """
                            The Gossip deliberately speaks lies, in the hope of uncovering the truth.

                            - Each day, the Gossip may make a public statement. If this statement is true, the Storyteller kills a player that night. If it is false, then no players die due to the Gossip.

                            - Mumbled words, whispers, statements the Storyteller doesn’t know are true or false, or statements that someone cannot hear don’t count. Like the Slayer’s ability, the Storyteller and every player must be able to hear and understand the Gossip and be aware that the Gossip is using their ability in order for the Storyteller to judge what happens next.

                            - If the Gossip made a true statement during the day while drunk or poisoned, but is sober and healthy when their ability triggers that night, the Storyteller still kills a player.
                            """,
                    """
                            - During the day, the Gossip says, "I am the Gossip. My public statement is: The Demon is wearing a hat." The Demon is not wearing a hat. That night, nobody dies due to the Gossip’s ability.

                            - The Gossip neighbours the Gambler and the Pacifist. During the day, the drunk Gossip says, "Both my neighbours are good." At dusk, the drunk Gossip becomes sober. That night, the Chambermaid dies.

                            - During the day, the Gossip makes a true statement. That night, the Gossip is killed by the Demon. They lose their ability, so their statement does not kill anyone tonight.
                            """, "Autumn Wind"),
            entry(Role.COURTIER,
                    "I am more afraid of an army of one hundred sheep led by a lion than an army of one hundred lions led by a sheep.",
                    """
                            The Courtier gets a character drunk, without knowing which player it is.

                            - Once per game, the Courtier chooses a character to be drunk for three nights and three days, starting immediately.

                            - The Courtier does not learn if they were successful or not, so they might choose a character that is not in play.

                            - The Courtier chooses a character, not a player. The Courtier player may need to be reminded of this. Evil players bluffing as the Courtier may also need to be reminded.

                            - If the drunk or poisoned Courtier chooses a character, that character is not drunk, even if the Courtier later becomes sober and healthy. If the Courtier made a character drunk, but the Courtier becomes drunk or poisoned, the player they made drunk becomes sober again. If the Courtier becomes sober and healthy again before the three nights and three days have ended, that player becomes drunk yet again.
                            """,
                    """
                            - For four nights, the Courtier wakes but does not use their ability. During the fifth night, they make the Shabaloth drunk. For the next three nights, the Shabaloth acts but cannot kill anybody.

                            - The Courtier uses their ability while drunk. Nothing happens, but they are not woken again.

                            - The Courtier makes the Mastermind drunk. The Po is executed while the Mastermind still lives. The game ends and good wins.
                            """, "The Yogscast"),
            entry(Role.PROFESSOR,
                    "The process is simple. Attach the hydraulic confabulator to the modified chi matrix amplifier, add 20 CCs of pseudodorafine, keep his Z levels above 20%, and your husband will be fine. Now, all we need is a lightning strike.",
                    """
                            The Professor can bring someone back from the dead.

                            - Once per game, the Professor can choose a dead player. If that player is a Townsfolk, they are resurrected, becoming alive again.

                            - If the Professor chooses an Outsider, Minion, or Demon, then nothing happens, and the Professor’s ability is gone.

                            - The resurrected player regains their ability, even a “once per game” ability they used already.

                            - Resurrected Townsfolk may or may not get to act on the night of their resurrection, depending on whether they would act before or after the Professor. If they had a “first night only” or “you start knowing” ability, they immediately wake to use it again, as soon as the Professor goes to sleep.
                            """,
                    """
                            - The Professor chooses a dead player who is claiming to be the Tea Lady. The player is actually the Lunatic. Nobody is resurrected.

                            - The Professor resurrects the Grandmother, who learns a good player’s character. At dawn, all players learn the Grandmother player is alive, but not that the player is the Grandmother.

                            - The drunk Professor chooses to resurrect the Minstrel. Unbeknownst to the Professor, the Minstrel was regurgitated by the Shabaloth and is now alive. The Storyteller shakes their head no, because the Professor must choose a dead player. The Professor chooses the dead Fool instead. Nothing happens because the Professor is drunk, and the Professor cannot use their ability again.
                            """, "The Yogscast"),
            entry(Role.MINSTREL,
                    "And I shall hear, tho' soft you tread above me... And all my dreams will warm and sweeter be... If you'll not fail to tell me that you love me... I simply sleep in peace until you come to me.",
                    """
                            The Minstrel makes everybody drunk if a Minion dies.

                            - If a Minion is executed and dies, all players (except the Minstrel) become drunk immediately and stay drunk all through the night and all the following day. Townsfolk, Outsiders, Minions, and even Demons become drunk. This doesn’t happen if a Minion died at night.

                            - If a dead Minion is executed, the Minstrel ability does not trigger—a dead character cannot die again! If a Minion is executed but does not die, the Minstrel’s ability does not trigger. If the Minstrel is drunk or poisoned when a Minion dies by execution, the Minstrel ability does not trigger.
                            """,
                    """
                            - During the first day, the Pacifist dies. That night, players act as normal, because the Pacifist is not a Minion. During the second day, the Godfather is executed. That night, everyone is drunk, including the Demon, so nobody dies. During the third day, a Minion who is protected by the Devil's Advocate is executed and dies, because the Devil's Advocate is drunk. Again, since a Minion died during the day, the Minstrel makes everyone drunk.

                            - The Assassin is executed, so the Minstrel makes everyone drunk. The next day, the Godfather is executed, so the Minstrel makes everyone drunk again. The Demon could not kill on either night. The next day, a Townsfolk is executed, so that night everyone becomes sober again.

                            - During the day, the Assassin dies, so the Minstrel makes everyone drunk. The following day, the Zombuul is executed and dies for the first time. Good wins, because the Zombuul is drunk and so has no ability.
                            """, "xerca0 (Music Maker Mod)"),
            entry(Role.TEA_LADY,
                    "If you are cold, tea will warm you. If you are too heated, tea will cool you. If you are depressed, tea will cheer you. If you are excited, tea will calm you.",
                    """
                            The Tea Lady protects her neighbours from death... as long as they are good.

                            - If both alive neighbours of the Tea Lady are currently good, those neighbours cannot die. The Demon cannot kill them, nor the Godfather, nor the Gossip. If they are executed, they do not die. The only exception is the Assassin, who can kill someone protected from death.

                            - The Tea Lady’s alive neighbours are the two alive players closest to the Tea Lady—one clockwise and one counterclockwise. Skip past any dead neighbours.

                            - However, if either alive neighbours is evil, or both are, then the Tea Lady does not protect her alive neighbours . If an evil player dies and the Tea Lady is now neighbouring two good players, then neither can die.
                            """,
                    """
                            The Tea Lady starts the game neighbouring the Mastermind and the good Goon. The Mastermind is executed and dies. Now, the Tea Lady's alive neighbours are the good Goon and the Courtier. The Demon attacks the Courtier, but the Courtier remains alive. Later, the Goon turns evil, and the Demon attacks the Courtier again. This time, the Courtier dies.
                            """, "The Yogscast"),
            entry(Role.FOOL,
                    "...and the King said 'What?! I've never even owned a pair of rubber pantaloons, let alone a custard cannon!' Ho-ho! Jolly day!",
                    """
                            The Fool escapes death... once.

                            - The first time the Fool dies for any reason, the Fool remains alive. They don’t learn that their ability saved their life.

                            - If another character’s ability protects the Fool from death, the Fool does not use their ability. Only the time that the Fool would actually for realsy bona fide be dead does the Fool’s ability trigger.
                            """,
                    """
                            - On the first day, the Fool is executed but remains alive. On the fourth day, the Fool is executed again. This time, they die.

                            - The Demon attacks the Fool, who remains alive. Nobody dies that night. The next day, the Fool is executed and dies.

                            - The Demon attacks the Fool, who is protected by the Tea Lady. The Fool remains alive and keeps their ability. Later, after the Tea Lady has died, the Demon attacks the Fool, who is now protected by the Innkeeper. The Fool remains alive and keeps their ability. Later, after the Innkeeper has died, the Demon attacks the Fool, who remains alive again but uses their ability, so next time they will die.
                            """, "The Yogscast"),
            entry(Role.PACIFIST,
                    "Distrust all in whom the impulse to punish is powerful.",
                    """
                            The Pacifist prevents good players from dying by execution.

                            - When a good player is executed, the Storyteller chooses whether they die or live.

                            - As always, when abilities like this function in obvious ways, the group is not told why something has happened, only what has happened. The group learns that an execution succeeded, but that the executed player did not die—that is all.

                            - If a player is executed and remains alive, that still counts as the execution for today. No other nominations may happen.
                            """,
                    """
                            - The Innkeeper is executed, but remains alive because of the Pacifist.

                            - The Pacifist is in play. Good is winning. Even after 7 days, and 5 good players executed, the Pacifist ability has not saved anybody.

                            - The Pacifist is in play. Evil is winning. The Lunatic is executed, but remains alive. The next day, the Professor is executed and dies, because the Pacifist is drunk. The next day, the Pacifist is executed but remains alive because of the Pacifist.
                            """, "The Yogscast"),
            //S&V
            entry(Role.CLOCKMAKER,
                    "Do not disturb me. The tick must continue, for the circle is a symbol of life and contains all things - all answers - in its divine machinery. I must work.",
                    """
                            The Clockmaker learns the distance from the Demon to their nearest Minion.

                            - The Clockmaker only learns this on the first night.

                            - The distance is the number of seated players, starting from the player next to the Demon and ending at the nearest Minion, either clockwise or counterclockwise.
                            """,
                    """
                            - The Fang Gu is sitting next to the Pit-Hag. During the first night, the Clockmaker learns a "1".

                            - Clockwise from the No Dashii sits the Dreamer, the Snake Charmer, then the Evil Twin. Counterclockwise from the No Dashii sits the Mutant, the Sweetheart, the Philosopher, the Sage, then the Witch. Because the Witch is five steps away from the Demon, and the Evil Twin is three steps away from the Demon, the Clockmaker learns a "3" during the first night.

                            - The Fang Gu neighbours two Outsiders, a Drunk and an evil Ogre. Neighboring one of these Outsiders is a Cerenovus. During the first night, the Clockmaker learns a "2", because evil Outsiders are not Minions.
                            """, "The Yogscast"),
            entry(Role.DREAMER,
                    "I remember the Clockmaker. The sky was red and it was raining fractal triangles. There was a smell of violets and a bubbling sound. A woman with glowing eyes and a scraggly beard was hissing at the sky. Then, I awoke.",
                    """
                            The Dreamer learns players' characters, but is not sure if their information is entirely correct.

                            - Each night, the Dreamer chooses a player and learns two characters—one that the player is, and one that the player isn’t.

                            - The false character token depends on the chosen player’s true character type. If the Dreamer chooses a player who is a Townsfolk or Outsider, the false character token is any Minion or Demon. If they choose a player who is a Minion or Demon, the false character token is a Townsfolk or Outsider.

                            - The Dreamer may not choose themself, and may not choose a Traveller.
                            """,
                    """
                            - The Dreamer chooses a player who is the Mutant. The Dreamer learns that this player is either the Mutant or the Cerenovus.

                            - The Dreamer chooses a player who was the Philosopher but gained the Flowergirl ability earlier that night. The Dreamer learns that this player is either the Philosopher or the Vigormortis.

                            - Today, both the Evil Twin and the Artist claimed to be the Artist. That night, the Dreamer chooses the player who is the Evil Twin. If the Storyteller wanted to help the good team, they could show the Evil Twin and the Sweetheart. But the Storyteller decides to help evil, so they show the Evil Twin and the Artist to the Dreamer.

                            - The Dreamer chooses a player who is the Vortox. The Dreamer's information must be false because the Vortox is in play, so the Dreamer learns that this player is either the Oracle or the No Dashii.
                            """, "The Yogscast"),
            entry(Role.SNAKE_CHARMER,
                    "Effendi... I am but a humble man, but my pipe is golden and a single tune will tame the wildest djinn, Inshallah. They say that greed hangs more men than rope. But not I, Effendi... not I.",
                    """
                            The Snake Charmer learns player after player that is not the Demon... but becomes the Demon if they get either too greedy or too bold.

                            - Each night, they choose a player. If that player is not the Demon, nothing happens. If they are the Demon, the Snake Charmer becomes that Demon and turns evil, and the Demon becomes good and poisoned permanently.

                            - If the Philosopher has the Snake Charmer ability and becomes the Demon, the Demon becomes a poisoned Philosopher.

                            - In the strange situation that the Snake Charmer is evil, or the Demon good, their alignments swap as appropriate.
                            """,
                    """
                            - The Snake Charmer chooses a player who is the Pit-Hag, so nothing happens. The Snake Charmer simply goes to sleep. The next night, the Snake Charmer chooses themself, so nothing happens.

                            - The Snake Charmer chooses a player who is the Vigormortis. The Snake Charmer immediately becomes the evil Vigormortis, and the Vigormortis becomes the good Snake Charmer and gets poisoned.

                            - The Pit-Hag turns themself into the Snake Charmer. Then, the Snake Charmer chooses a player who is the Fang Gu. The Snake Charmer becomes the Fang Gu, while the Fang Gu becomes the Snake Charmer and gets poisoned. Both remain evil.
                            """, "The Yogscast"),
            entry(Role.MATHEMATICIAN,
                    "Any consistent formal system x, within which a certain amount of elementary arithmetic can be carried out is incomplete; that is, there are statements of the language of x which can neither be proved nor disproved in x. Ergo, you are drunk.",
                    """
                            The Mathematician knows how many things have gone wrong since dawn today.

                            - When an ability does not work in the intended way due to another character's interference, the Mathematician will learn that it happened. They'll learn that something went wrong if a piece of information was false but was supposed to be true, or if an ability should have worked but didn't, due to another character.

                            - The Mathematician does not learn which players this happened to, only how many times it happened.

                            - The Mathematician does not detect their own ability failing.

                            - The Mathematician does not detect drunkenness or poisoning itself, but does detect when drunk or poisoned players' abilities did not work as intended. The Recluse registering as evil to the Chef, and the poisoned Soldier dying from the Imp's attack, would each be detected. The poisoned Empath getting true information would not.
                            """,
                    """
                            - The poisoned Oracle learns that two dead players are evil, when three dead players are actually evil. All other character abilities work normally. Later that night, the Mathematician learns a "1".

                            - The poisoned Snake Charmer chooses a Townsfolk player, and nothing happens. The drunk Juggler gets correct information. The Savant learns two pieces of true information. Later that night, the Mathematician learns a "1" because the Snake Charmer and Juggler's abilities worked as normal, whilst one of the Savant's facts was true when it should have been false.

                            - A Vortox is in play. Five good players got false information. The Witch is drunk, and when their cursed player nominated, nothing happened. Even though six abilities worked abnormally, the Mathematician learns a "4" due to the Vortox's ability.
                            """, "The Yogscast"),
            entry(Role.FLOWERGIRL,
                    "Yesterday's violets have withered and died, but today my poppies bloom.",
                    """
                            The Flowergirl knows if the Demon voted or not.

                            - A Demon’s vote counts whether or not the nominee was executed.

                            - If the Demon changes players after the original Demon voted but before the Flowergirl wakes to learn their information, the Flowergirl detects the original Demon.

                            - If there are two (or more!) Demons, even dead Demons, the Flowergirl detects if any of them voted. If even one Demon voted, the Flowergirl learns a “yes”.
                            """,
                    """
                            - There was one nomination today. Lots of players voted, the player was executed, but the Demon did not vote. That night, the Flowergirl learns that the Demon did not vote today.

                            - There were three nominations today. The Demon voted during the second nomination. Nobody was executed. That night, the Flowergirl learns that the Demon voted today.
                            """, "The Yogscast"),
            entry(Role.TOWN_CRIER,
                    "Hear ye! Hear ye! Witchcraft in the labyrinth! Genius savant reveals all! Town in danger! Hear Ye!",
                    """
                            The Town Crier knows when Minions nominate.

                            - Each night, the Town Crier learns either a “yes” or a “no”.

                            - They do not learn which players are Minions or how many Minions made nominations, just whether or not any Minions made nominations today.
                            """,
                    """
                            - Today, four players nominated. Two of them were Minions. Many players voted, but there was no execution. That night, the Town Crier learns a “yes.”

                            - The Demon nominated a Townsfolk, and all Minions voted for the execution, which was successful. That night, the Town Crier learns a “no.”
                            """, "The Yogscast"),
            entry(Role.ORACLE,
                    "Only the chosen may gaze beyond the veil. The dead are restless, and they point in silence toward the icy north.",
                    """
                            The Oracle knows how many dead players are evil.

                            - Because the Oracle acts after the Demon attacks each night, the Oracle’s info refers to the players that are dead when dawn breaks and all players open their eyes.

                            - The Oracle detects dead Minions and Demons, but also any other players that are evil, such as Townsfolk and Outsiders that have been turned evil.

                            - When counting the number of dead players, remember to count Townsfolk and Outsider tokens that are upside-down, which means their alignment is the opposite of what is printed.
                            """,
                    """
                            - During the first day, the Flowergirl is executed. That night, the Demon kills the Juggler. The Oracle wakes and learns a "0,” because all dead players are good.

                            - Halfway through the game, seven players are dead. Five of them are good and two of them are evil. During the day, an evil Goon is executed. That night, the Demon kills one of it's Minions. The Oracle wakes and learns a "4,” because four dead players are evil.
                            """, "The Yogscast"),
            entry(Role.SAVANT,
                    "Seventy-two matchsticks on the floor... the sun sets early but the moon is unchanged... a torn piece of cloth... evil in the manor house... three by three... the one we trusted is not what he seems... green light means magnesium... residue, but the pattern is wrong... Seventy-two matchsticks on the floor...",
                    """
                            The Savant gets crazy, amazing information that is different every night and every game, but exactly half of it is completely false.

                            - Each night, the Storyteller chooses two pieces of information to give the Savant... so get creative! One must be true, and one must be false, and the Savant won’t know which is which.

                            - A drunk or poisoned Savant might get two pieces of true information or two pieces of false information.
                            """,
                    """
                            - The Savant learns that "All players wearing glasses are good" and that "One player sitting on the black couch is a Minion."

                            - The Savant learns that "A Snake Charmer is in play" and "Everybody got true information last night."

                            - The Savant learns that "The Demon is a woman" and "Jason is evil."

                            - The Savant learns that "Evin and Amy are the same alignment" and "There is one Outsider in play."
                            """, "The Yogscast"),
            entry(Role.SEAMSTRESS,
                    "Did you hear that stranger in the cashmere coat put the word on our young Belle? And she said yes? Well, that's nothing compared to what Harry and that juggler got up to at the fair! The things I could say if I was a tattletale... my, yes.",
                    """
                            The Seamstress learns whether 2 players are on the same team as each other.

                            - They only get this information once per game, so they had best choose wisely when and who.

                            - They may choose alive or dead players.
                            """,
                    """
                            - During the first night, the Seamstress chooses two players, who are the Barber and the Clockmaker. Because they are both good, the Seamstress learns a "yes.”

                            - During the first three nights, the Seamstress chooses not to use their ability. During the fourth night, they choose two players, who are the Fang Gu and the Sweetheart. The Seamstress learns a "no.”

                            - The Pit-Hag turns the Mathematician into the Witch, who remains good. Later that night, the Seamstress chooses the two players, the Witch and the Town Crier. The Seamstress learns a "yes" because they are both good.
                            """, "The Yogscast"),
            entry(Role.PHILOSOPHER,
                    "If anything is real, beer is real. Drink, for tomorrow we may die.",
                    """
                            The Philosopher has no ability until they decide which character they want to emulate.

                            - They can do this once per game. When they do so, they gain that character’s ability. They do not become that character.

                            - They may want to wait a while to choose. If the Philosopher chooses a character that is already in play, the player of that character becomes drunk. If the Philosopher then dies or becomes drunk or poisoned, the player they are making drunk becomes sober again.

                            - If the Philosopher chose a character that was not in play at the time but is in play now, that character is drunk.

                            - If the Philosopher gains an ability that works at night, they wake when that character would wake. If this ability is used on the first night only, they use it tonight.

                            - If the Philosopher’s ability works while dead, such as the Klutz’s, it works if the Philosopher is dead.
                            """,
                    """
                            - During the first night, the Philosopher chooses to gain the Dreamer's ability. They gain the Dreamer's ability from now on and act when the Dreamer normally acts.

                            - During the third night, the Philosopher chooses to gain the Clockmaker's ability. That night, they learn the distance from the Demon to their nearest Minion.

                            - An Artist is in play. The Philosopher chooses to gain the Artist's ability. The original Artist becomes drunk. Later, the Philosopher dies, so the original Artist becomes sober again. (The original Artist would also become sober if the Philosopher became drunk.)
                            """, "Autumn Wind"),
            entry(Role.ARTIST,
                    "Mon Dieu! C'est lumineux! My work, she is... how you say... Magnifique! Dieu est révélé! Oui.",
                    """
                            The Artist may ask any 1 question, and get an honest answer.

                            - The question may deal with anything at all, phrased in any way they want. The Storyteller honestly answers “yes,” “no,” or “I don’t know.”

                            - The Artist player chooses when to use their ability.
                            """,
                    """
                            - The Artist asks, "Is the Demon sitting in a brown chair?" The Storyteller answers, "No,” because the Demon is in a black chair.

                            - The Artist asks, "Is David the Evil Twin?” and the Storyteller answers, "Yes,” because David is.

                            - The Artist asks, "How many Minions are alive?” and the Storyteller says, "Please ask another question. I cannot answer that with a yes, no, or I don't know."

                            - The Artist asks, "Are we winning?” and the Storyteller answers, "I don't know,” because even though all the Minions are dead, many good players trust the Demon.
                            """, "The Yogscast"),
            entry(Role.JUGGLER,
                    "For my next trick, as per request, I will need a flower, a bag of beans, a toy snake, a paintbrush, and a motorized gasoline-powered hedge trimming device. I warn you, this trick may be my last. Oh dear.",
                    """
                            The Juggler takes the risk of convincing people to reveal their characters on the 1st day, in the hope of guessing as many as possible that are telling the truth.

                            - On the first day, they may guess which players are which characters. That night, the Juggler learns how many guesses they got right...if they are not killed beforehand.

                            - They must make their guesses publicly, so everyone hears what is guessed. They may guess zero characters, or up to five characters, and these characters and players may be different or the same.

                            - If the Juggler made their guesses while drunk or poisoned, but is sober and healthy when their ability triggers that night, then the Storyteller still gives them true information.
                            """,
                    """
                            - During the first day, the Juggler guesses that Alex is the Town Crier, Mia is the No Dashii, and Julian is the Sage. That night, the Juggler learns a "2," meaning two of those guesses were correct.

                            - During the fourth night, the Savant gets turned into the Juggler. The next day, the new Juggler guesses that Benjamin is the Pit-Hag, that Benjamin is the Witch, and that Amy is the Pit-Hag. That night, the Juggler learns a "1.”
                            """, "Autumn Wind"),
            entry(Role.SAGE,
                    "These mountainous tomes guard the secret, I am sure of it! Twixt word and word, it lies in wait. More candles, boy! More ink! These notes may look arcane, but the infernal puzzle is revealing itself.",
                    """
                            The Sage knows nothing while alive, but learns the most important information of all at the moment of their death - who killed them.

                            - The Sage only gets this information when killed by a Demon attack. Being executed does not count.
                            """,
                    """
                            - During the second night, the Demon kills the Sage. The Storyteller points at two players, one of whom is the Demon.

                            - During the final night, the Demon kills the Sage, who is drunk because of the Sweetheart. The Storyteller points at a dead player and one of the remaining three alive players. This information is incorrect.

                            - The Pit-Hag creates a Demon. Because the Pit-Hag ability says that "all deaths tonight are arbitrary,” the Storyteller decides that the old Demon dies, and the Sage dies. Because the Sage died due to the Pit-Hag, not the Demon, the Sage does not wake to learn anything tonight
                            """, "The Yogscast"),
            //Kickstarter
            entry(Role.ALCHEMIST,
                    "Visit the interior of the Earth. By rectification thou shalt find the hidden stone. Above the gold, lieth the red. Kether in Malkuth.",
                    """
                            The Alchemist has a Minion ability.

                            - The Alchemist’s ability is usually that of a not-in-play Minion, but can duplicate an in-play Minion ability.

                            - The Alchemist learns which ability this is on the first night.

                            - They are still a good Townsfolk. They win when good wins, and lose when good loses. They register as good and as the Alchemist.

                            - The Alchemist does not wake to learn who the other Minions are or who the Demon is, like Minions do.

                            - If the Alchemist’s Minion ability adds or removes characters during setup, this still occurs during setup.

                            - If the Alchemist has an ability where the player chooses something, like the Poisoner or the Vizier, the Storyteller may ask the Alchemist to choose differently. The Alchemist must do so.
                            """,
                    """
                            - The Alchemist has the Baron ability. There are 2 extra Outsiders in play.

                            - The Alchemist has the Poisoner's ability. On the first night, they wake and poison the Wizard. On the second night, they wake and poison the Alsaahir. On the third night, they wake and try to poison the Lord of Typhon, but the Storyteller prompts them to choose differently. They poison the King instead. The Lord of Typhon is not poisoned.
                            """, "The Yogscast"),
            entry(Role.AMNESIAC,
                    "Wait. What. Who? Oh, ok. Wait. What?",
                    """
                            The Amnesiac doesn’t know their own ability.

                            - The Storyteller decides what the Amnesiac’s ability is. It may be the same ability as another character in Blood On The Blocktower, something similar, or something original.

                            - The Amnesiac may wake at any time during the night to learn information or to choose a player, or their ability may be passive—not requiring action from the Amnesiac player.

                            - Each day, the Amnesiac talks to the Storyteller in private, and makes a guess as to what their ability is. The Storyteller answers “cold” if the guess is very wrong, “warm” if the guess is on the right track, “hot” if the guess is very close, and “bingo” if the guess is spot on.

                            - Their guess may be specific, such as “Am I learning two players each night that are the same alignment?”, or vague, such as “Is my ability something to do with dead players?”
                            """,
                    """
                            - Each night, the Amnesiac wakes and is prompted to point at two players. The Storyteller shakes their head on the first night, and nods on the second. The Amnesiac guesses “Am I learning if both players are Minions?” The Storyteller says “Hot” because their ability is that they detect if either of the two players are a Minion.

                            - Each night, the Amnesiac learns a number. The Amnesiac is learning how many of their living neighbours are Townsfolk.
                            """, "The Yogscast"),
            entry(Role.ATHEIST,
                    "Let us disperse with unnecessary conjecture and silly paranoia. There is a perfectly rational explanation for everything. Yes, a teacup may indeed be orbiting the planet, too small to see, but I shall drink my tea from the very real china in my very real hands.",
                    """
                            The Atheist knows that all players are good and there is no such thing as Demons.

                            - With the Atheist in play, there are no evil players—no Minions and no Demons.

                            - Good wins if the Storyteller is executed. Any living player may nominate the Storyteller, and the Storyteller is executed if 50% or more of the living players vote.

                            - If the Atheist is not in play and the Storyteller is executed, evil wins.

                            - Good loses if just two players are alive.

                            - The Storyteller may break any of the game’s rules. They may kill a player who nominated to simulate a Witch curse, kill players at night to simulate a Demon attacking, give players false information to simulate drunkenness, change characters at night to simulate a Pit-Hag, or even have the wrong number of Outsiders in play.
                            """,
                    """
                            - The Investigator learns that either the Grandmother or the Seamstress is the Boomdandy. The Sweetheart nominates, and dies, even though there is no Witch in play. The Slayer uses their ability on the Gossip, who dies.

                            - There are three Outsiders in play, when there should be two. The players execute the Storyteller. Good wins.
                            """, "The Yogscast"),
            entry(Role.CANNIBAL,
                    "I don’t like clowns. They taste funny.",
                    """
                            The Cannibal eats executed characters, gaining their ability.

                            - If a good player dies by execution, the Cannibal gains that player’s ability. If an evil player dies by execution, the Cannibal only thinks that they gain an ability, since the Cannibal is poisoned. The Storyteller may be lying to them.

                            - Each time a player dies by execution, the Cannibal loses the ability of the previous player.

                            - Executing a dead player won’t grant the Cannibal an ability. Executing a living player who doesn’t die won’t grant the Cannibal an ability. A player must be executed and die for the Cannibal to gain their ability.

                            - The Cannibal is not told which ability they have gained. They must figure that out for themselves.

                            - If the Cannibal has an “even if dead” ability, such as the Recluse, or an ability that implies it works while dead, such as the Ravenkeeper or Sweetheart, the Cannibal keeps that ability when they die, but loses their Cannibal ability.
                            """,
                    """
                            - The Clockmaker is executed and dies. That night, the Cannibal learns a “2” because the Demon and Minion are two steps apart.

                            - It is the third night and the Widow was executed today. Because the Widow was bluffing as the Fortune Teller, the Cannibal is prompted to choose 2 players, but they learn a “no” after choosing the Demon because they don’t actually have the Fortune Teller's (or Widow’s) ability, because they are poisoned.

                            - It is the fourth night and the Mutant was executed today. The Cannibal doesn’t learn anything tonight, because a real Mutant would not wake.
                            """, "The Yogscast"),
            entry(Role.CHOIRBOY,
                    "I saw it, I did. I was in the pews, tidying the hymn books, when a dreadful tune started from the pipe organ. The organist had a long cloak, and long fingers on the keys. And a hat that looked… just like… yours.",
                    """
                            The Choirboy learns who the Demon is when the King is slain.

                            - The King can be in play without the Choirboy. During the setup phase, if the Choirboy is in play and the King isn’t, the King is added. If a King is already in play, the Choirboy doesn’t add a second King.

                            - If the Demon kills the King using their ability, the Choirboy learns which player is the Demon. The Demon nominating and executing the King doesn’t count. Minions that kill the King, such as the Assassin, don’t count either.

                            - If the Demon attacks the King but doesn’t kill the King, the Choirboy doesn’t learn who the Demon is.

                            - The Choirboy learns which player the Demon is, but does not learn which character.
                            """,
                    """
                            - The Imp attacks the Empath. The Empath dies. The next night, the Imp attacks the King, who is protected by the Monk. The King lives. The next night, the Imp attacks the King, who is no longer protected by the Monk. The King dies. The Choirboy is woken by the Storyteller and learns which player is the Imp.

                            - The Shabaloth kills the King. The drunk Choirboy wakes and wrongly learns that the General is the Demon.
                            """, "The Yogscast"),
            entry(Role.ENGINEER,
                    "If it bends, great. If it breaks, well, it probably needed fixing anyway.",
                    """
                            The Engineer manufactures the threat that the town faces.

                            - The Engineer can choose which Minion characters are in play, or which Demon is in play, but not both.

                            - When the Engineer creates new in-play characters, the Demon player remains the Demon, and the Minion players remain Minions. The number of evil players stays the same.

                            - If the Engineer tries to create an in-play character, that character stays as the same player. The Engineer doesn’t learn this, and may not use their ability again.

                            - If creating Minions, the Engineer chooses the same number of Minions that should be in play for the number of players.

                            - If the Engineer accidentally chooses too many or too few characters, the Storyteller changes as many evil players’ characters as is fair and feasible.

                            - Only characters from the current script may be chosen.
                            """,
                    """
                            - On the second night, the Engineer chooses that the Demon is a Lleech. Lewis, who was the Imp, is now the Lleech.

                            - On the first night, the Engineer changes the Baron into the Boomdandy. There are still an extra two Outsiders in play.

                            - The Fearmonger and the Psychopath are in play, and causing havoc. The Engineer chooses that the Mezepheles and the Spy are in play. The Storyteller chooses to change the Fearmonger into the Mezepheles and the Psychopath into the Spy.

                            - The Spy, Assassin, and Witch are in play. The Engineer chooses that the Spy, Assassin and Mezepheles are in play. The Witch turns into the Mezepheles.
                            """, "Unused Minecraft Gear + edits by Autumn Wind"),
            entry(Role.FARMER,
                    "Even the high and mighty need food on the table. Without us, the city starves.",
                    """
                            The Farmer creates more farmers.

                            - If a Farmer dies at night, another player becomes a Farmer too.

                            - Only players that are good can become Farmers this way.

                            - If this new Farmer also dies at night, another Farmer is created.

                            - Farmers that die during the day, such as by execution, do not create more Farmers.

                            - Farmers that have turned evil, such as from the Mezepheles’ ability, can create more Farmers. But Townsfolk and Outsiders that have turned evil cannot become a Farmer.

                            - Farmers do not learn who each other are, but each player that becomes a Farmer learns that they are now a Farmer.

                            - When a player becomes a Farmer, they are no longer their old character, and do not have that ability. Any ongoing effects of their old ability immediately end.
                            """,
                    """
                            - Julian is the Farmer. The Demon kills him at night. Evin is the Fearmonger, and Sarah is the Alchemist. Sarah becomes the Farmer that night. Evin could not become the Farmer, because he is evil.

                            - On the 2nd night, the Farmer dies. The Choirboy becomes the Farmer. On the 3rd night, the new Farmer dies, and the Heretic becomes a Farmer. There is now no Heretic and no Choirboy in play, and three Farmers in play, two of which are dead.
                            """, "The Yogscast"),
            entry(Role.GENERAL,
                    "I don’t have time for quotes.",
                    """
                            The General knows who is winning.

                            - If the good team is winning, the Storyteller gives a thumbs up. If the evil team is winning, the Storyteller gives a thumbs down. If neither team is winning, or the Storyteller isn’t sure, the Storyteller gives a thumbs to the side.

                            - The Storyteller is the judge on which team is winning. Many factors may be included, such as how many players of each team are still alive, how much information the good team has, how successful the evil team’s bluffs seem to be, which players the group wants to execute next, or how experienced the Demon player is. All of these, and more, will inform the Storyteller’s judgment.

                            - The Storyteller decides who is winning at the point that the General wakes. Previous events in the night may affect their decision.
                            """,
                    """
                            - There are 5 good players alive and 4 evil players alive. Even though the Demon is very suspicious and will probably be executed next, there is a Scarlet Woman in play, who is very trustworthy. The Storyteller says evil is winning.

                            - The Good team has a lot of information, and believes that their false information is indeed false. The only Minion is dead. The Storyteller says good is winning.

                            - The Po is a very experienced player and is coordinating well with the Minions. The Monk is successfully protecting the Savant each night and the good team have correctly identified several good players. However, the Po will probably kill 3 times tomorrow night, so it is anyone’s game. The Storyteller says it's unclear who's winning.
                            """, "The Yogscast"),
            entry(Role.HUNTSMAN,
                    "My cabin is warm and sturdy. My axe by the door, my boots drying by the fire, and elk stew a-simmering… Hark! A scream echoes through the valley! The rain and the mud and the cold, cold wind mask the scent of the wolves, but I know the path and my pace is steady. I am coming.",
                    """
                            The Huntsman saves the Damsel before the Minions find her... hopefully.

                            - The Damsel can be in play without the Huntsman. During the setup phase, if the Huntsman is in play and the Damsel isn’t, the Damsel is added. If a Damsel is already in play, the Huntsman doesn’t add a second Damsel.

                            - If the Huntsman correctly chooses the Damsel at night, the Damsel becomes a not-in-play Townsfolk immediately. The Storyteller chooses which Townsfolk character, and the Damsel learns which one.

                            - When the Damsel becomes a Townsfolk, they gain that Townsfolk ability and lose the Damsel ability.

                            - The Huntsman gets one guess, and makes it at night.

                            - The Minions get one guess in total, and make it publicly during the day. If a Minion guesses who the Damsel is, evil wins. If a Minion incorrectly guesses who the Damsel is, the guess is used, and other Minions cannot win by correctly guessing the Damsel.

                            - If the Damsel is drunk or poisoned but the Huntsman is sober and healthy, the Damsel can still become a Townsfolk.
                            """,
                    """
                            - The Huntsman is woken on the 1st night, but does not use their ability. On the second night, the Huntsman chooses the Damsel player. The Damsel becomes the Undertaker and learns which player died today.

                            - The Huntsman chooses Lachlan. Lachlan is the General, so nothing happens. The Huntsman is no longer woken at night.
                            """, "The Yogscast"),
            entry(Role.KING,
                    """
                            ↑Betwixt the unknown strains of mortal strife→
                            And morbid night, sweet↓ with mystery and woe
                            ←Lies unfettered joys of fate’s long and colored life
                            Who’s garden blooms with each painted Face to Show.""",
                    """
                            The King learns which characters are still alive.

                            - The King gains this ability after a few nights have passed — once the dead players equal or outnumber the living.

                            - At the start of the game, the Demon learns who the King is. If a King is created mid-game, the Demon learns who the King is that night.

                            - The King may not survive long enough to use their ability. Once the number of dead players is equal to or greater than the number of alive players, the King learns one alive character each night.

                            - The King may learn good or evil characters, and may even learn the same character more than once.

                            - There may not be a Choirboy in play. But if there is, and they are still alive when the Demon kills the King, the Choirboy learns who the Demon is.
                            """,
                    """
                            - Amy is the King. There are 12 players alive, and one dead player. On the second night, she learns nothing. On the third night, she learns nothing. On the fourth day, there are 7 dead players and 6 alive players. On the fourth night, Amy learns that the Snitch is alive. On the fifth night, she learns that the Witch is alive.

                            - The Demon knows that Abdallah is the King. Evin is claiming to be the Choirboy, but is the Butler. The Demon takes a risk and kills Abdallah. If Evin was actually the Choirboy, he would have learnt which player was the Demon.
                            """, "The Yogscast"),
            entry(Role.LYCANTHROPE,
                    "Beneath the thin veneer of civilisation lies a howling madness.",
                    """
                            The Lycanthrope roams at night, killing the innocent, whilst the Demon cowers indoors.

                            - The Lycanthrope must choose an alive player each night. If the Lycanthrope chooses a dead player, the Storyteller shakes their head no and prompts the Lycanthrope to choose a different player.

                            - If the player that the Lycanthrope chooses is good, that player dies, and the Demon cannot kill tonight.

                            - If the player the Lycanthrope attacks is evil, that player does not die, and the Demon may still kill tonight.

                            - If the Lycanthrope attacks a good player but that good player doesn’t die, the Demon may still kill tonight.

                            - While the Lycanthrope lives, one good player registers as evil. They cannot be killed by the Lycanthrope.

                            - This evil-registration does not effect win conditions. The good player that registers as evil still wins or loses with the good team.
                            """,
                    """
                            - The Lycanthrope attacks the General. The General dies. Later that night, the Imp attacks the Amnesiac. The Amnesiac does not die, because the Imp cannot kill tonight.

                            - The Lycanthrope attacks the Farmer. The Farmer dies, and another good player becomes a Farmer. The Magician was poisoned by the Pukka last night but does not die tonight, because the Pukka cannot kill tonight.

                            - The Lycanthrope attacks the Godfather. The Godfather does not die, because the Godfather is evil. The Lycanthrope attacks the Zealot, who is registering as evil due to the Lycanthrope’s ability. The Zealot does not die. The Demon attacks the Lycanthrope and the Lycanthrope dies.
                            """, "The Yogscast"),
            entry(Role.MAGICIAN,
                    "1... 2... Abra... 3... 4... Cadabra... *poof!*\n" +
                            "And, as you can see, ladies and gentlemen, Captain Farnsworth’s bag of gold has disappeared! Gone! Without a trace! Thank you, and goodnight!",
                    """
                            The Magician confuses the evil players about who is evil and who isn’t.

                            - On the first night, instead of learning which player is the Demon, the Minions are told that both players—the Demon and the Magician—are the Demon.

                            - On the first night, the Demon learns that the Magician player is one of its Minions.

                            - The Magician does not wake to learn anything.

                            - The Storyteller can point to the Magician and the evil players in any order, so that the evil players won’t know which player is the Magician.

                            - If the Poppy Grower dies and the Demon and Minions learn who each other are mid-game, the Magician ability has an effect that night, just as if it was the first night.
                            """,
                    """
                            The Minions wake to learn that either the Leviathan player or the Magician player is the Demon. The Leviathan player learns that the Fearmonger player, the Assassin player, and the Magician player are the Minions.
                            """, "The Yogscast"),
            entry(Role.NOBLE,
                    "Sarcasm is indeed the lowest form of wit. But speaking in response to your criticism, Sir, it is, nevertheless, a form of wit.",
                    """
                            The Noble learns that one of three players is evil.

                            - The Noble learns their information on the first night only.

                            - If a Noble is created mid-game, the Noble learns their information on their first night.

                            - The Noble learns two good players and one evil player. They may not learn one good player and two evil players. They may not learn three evil players.
                            """,
                    """
                            - The Noble is shown Marianna, Alex, and Abdallah. Marianna and Abdallah are good, and Alex is evil.

                            - The Noble learns Doug, Lachlan and Ben. Doug is the Chambermaid. Lachlan is the Barber. Ben is the Recluse, who has registered as evil to the Noble.

                            - On the third night, the Pit-Hag turns Amy into the Noble. Amy learns that 1 of Evin, Sarah, or Julian is evil. However, Sarah is the evil Spy and has registered as good, and Julian is the Po, who is also evil.
                            """, "The Yogscast"),
            entry(Role.PIXIE,
                    """
                            Round and round the garden, go.
                            Little girls run to and fro.
                            Little boys climb up the tree.
                            Which of these should Pixie be?
                            Ladies smile and go to town.
                            Lords with axe chop forest down.
                            What’s yours is mine. What’s mine, divine.
                            Silly little Pixie, me.""",
                    """
                            The Pixie pretends to be the same character as someone else.

                            - On the first night, the Pixie learns an in-play Townsfolk. The Storyteller chooses which Townsfolk this is. The Pixie does not learn which player is this character.

                            - If the Pixie player pretends that they are this Townsfolk, they gain the ability of this Townsfolk when the Townsfolk dies. They could have spoken loudly about being the character for one day, or pretended to be the character each day this game, or accused the Townsfolk of being a liar—the Storyteller is the judge of whether or not the player was convincing, by “being mad that they are this character”.

                            - When the Townsfolk player dies, the Pixie does not learn this, and is not told that they have gained a new ability. They may learn this has happened if they wake at night and start gaining information, or are prompted to choose players.

                            - If the player the Pixie learns about changes character then dies, the Pixie gains the ability of the Townsfolk the Pixie learnt about, not the new character.
                            """,
                    """
                            - Amy is the Pixie, and knows that the Washerwoman is in play. For three days, Amy claims that the Washerwoman player is lying, because she is the Washerwoman. The Washerwoman is executed. That night, Amy gains the Washerwoman ability, and learns that one of two people is the Monk.

                            - Doug is the Drunk Pixie. He learns that the Lycanthrope is in play. There is no Lycanthrope in play, but a Minion bluffs as the Lycanthrope. The Minion dies. The Storyteller wakes Doug and Doug chooses a player to “attack” each night, but they do not die because Doug does not have the Lycanthrope ability.

                            - On the first day, the Pixie player claims to be the Soldier. The real Soldier also claims to be the Soldier. The Pixie player doesn’t dispute this. When the Soldier dies, the Pixie player does not gain the Soldier ability, as the Storyteller feels that the Pixie did not really pretend to be the Soldier.
                            """, "The Yogscast"),
            entry(Role.POPPY_GROWER,
                    "In the hidden groves of the deep forest, the black poppy dwells. To see its revelry is to be enchanted. To smell its thick aroma is to be lost forever, a slave to the gods of light and dark.",
                    """
                            The Poppy Grower prevents the evil players from learning who each other are.

                            - The Demon still learns three not-in-play characters that are safe to bluff as.

                            - If the Poppy Grower dies, the Demon and Minions learn who each other are, as though it were the first night again.

                            - If the Poppy Grower becomes drunk or poisoned, Demons and Minions do not suddenly learn who each other are. If the Poppy Grower is drunk or poisoned when they die, Demons and Minions do not learn who each other are, since the Poppy Grower has no ability that night.
                            """,
                    """
                            - The Imp, the Poisoner and the Witch are in play. On the first night, the Imp wakes to learn 3 not-in-play characters, but does not learn which players are the Minions. The Poisoner and the Witch do not wake to learn who each other are, and do not learn who the Demon is.

                            - The Poppy Grower is executed, and dies. That night, the Shabaloth learns which players are the Minions. The Godfather and the Baron wake, make eye contact, and learn which player is the Shabaloth.

                            - The Poppy Grower is the Drunk. On the 1st night, the evil players learn who each other are, as normal. On the fourth night, the Demon kills the Poppy Grower. The Demons and Minions do not wake to learn who each other are again because the Poppy Grower is the Drunk.
                            """, "The Yogscast"),
            //Carousel
            entry(Role.ACROBAT,
                    "Ladies and gentlemen, hold fast to your hats, for I shall defy the very laws of gravity and dance upon the air, a marvel of agility and daring, all for your delight and wonder!",
                    """
                            The Acrobat dies when they find a drunk or poisoned player.

                            - Each night except the first, the Acrobat chooses a player. If the chosen player is sober and healthy, nothing happens. If the player is drunk or poisoned, the Acrobat dies.

                            - If the Acrobat is drunk or poisoned, they cannot die to their own ability.

                            - The Acrobat may choose any player, dead or alive, even themself.

                            - If the chosen player is sober and healthy at the time the Acrobat picks, but becomes drunk or poisoned later in the night, the Acrobat dies.

                            - The Acrobat does not learn if the player they selected was drunk, or poisoned, or both.

                            - The Drunk registers as drunk to the Acrobat.
                            """,
                    """
                            - The Sailor chooses the Assassin, and the Storyteller makes the Sailor drunk. The Acrobat chooses the Sailor, and dies because the Sailor is drunk.

                            - The Acrobat chooses the Tinker, who is sober and healthy. Nothing happens.

                            - The Acrobat chooses the Preacher. Later that night, the Pukka poisons the Preacher. The Acrobat dies, because the Preacher is no longer healthy.
                            """, "The Yogscast"),
            entry(Role.ALSAAHIR,
                    "I am here because of you, and you are here because of me.",
                    """
                            The Alsaahir guesses the entire evil team.

                            - The Alsaahir’s guesses need to be public, and they need to be during the day. They don’t have to guess every day.

                            - Other players may pretend to be the Alsaahir and make a guess. Like the Juggler or the Gossip, the Storyteller will briefly pretend that player is the Alsaahir.

                            - If the Alsaahir guesses the Demon player as the Demon, and the Minion players as Minions, the game ends immediately. The Alsaahir must guess all Demon and Minion players.

                            - The Alsaahir doesn’t need to guess specific minion characters, nor specific Demon characters.

                            - If there is more than one Demon in play, all Demons must be guessed, including dead Demons.

                            - If a player is a Minion and Demon, such as Legion, the Alsaahir must guess this player as a Demon.

                            - Once a guess is made, the Alsaahir cannot change their mind later that day and guess again.

                            - The Alsaahir needs to guess Minions and Demons, even if they are good.

                            - If the evil team has changed during the game, the Alsaahir must guess the current evil team, not the starting evil team.
                            """,
                    """
                            - The Alsaahir guesses four good players. Nothing happens.

                            - The Alsaahir guesses that Doug is the Demon, and Ben and Sarah are Minions. Doug is the Demon, and Ben and Sarah are Minions. Good wins immediately.

                            - The drunk Alsaahir guesses that Doug is the Demon, and Ben and Sarah are Minions. Doug is the Demon, and Ben and Sarah are Minions. Nothing happens and the game continues. The next day, the sober Alsaahir guesses that Ben is the Demon and Doug and Sarah are Minions. Nothing happens and the game continues.
                            """, "The Yogscast"),
            entry(Role.BALLOONIST,
                    "More heat! Higher! Higher! Più alto! Ahhh... it is so beautiful from up here, don't you agree? Can you see the children fishing by the river, under the willow? Can you see the glint of the sun on the circus tent-poles? What's this? An old man, alone, passed out in the vineyard? Less heat! Lower! Lower! Vai più in basso!",
                    """
                            The Balloonist learns players of different character types.

                            - Each time the Balloonist learns a player, the player must have a different character type to the previously shown player.

                            - The Balloonist does not learn the character type of the player they learn.

                            - The shown player can be alive or dead.

                            - The shown player can be good or evil.

                            - If the Balloonist is drunk or poisoned, they may learn a character of the same type as the previously shown player. When the Balloonist becomes sober and healthy, they must learn a player of a different character type to the previously shown player.

                            - During setup, the Storyteller may choose to add an Outsider due to the Balloonist’s ability.
                            """,
                    """
                            - Abdallah is the Vizier, Lewis is the High Priestess, and Sarah is the Politician. On the first night, the Balloonist learns Abdallah. On the second night, the Balloonist learns Lewis. On the third night, the Balloonist learns Sarah.

                            - Julian is the Nightwatchman, Alex is the Sailor, and Lachlan is the Puzzlemaster. On the first night, the Balloonist learns Julian. On the second night, the Poisoner chooses the Balloonist. Because the Balloonist is poisoned, the Storyteller chooses to show the Balloonist another Townsfolk, and the Balloonist learns Alex. On the third night, the Balloonist is sober and healthy, and learns Lachlan, who is a different character type to Alex.
                            """, "The Yogscast"),
            entry(Role.BANSHEE,
                    "Gorm do shúile, dearg do ghruaig, ní bheidh sé i bhfad, is a mbeidh tú san uaigh.",
                    """
                            The Banshee becomes more powerful when dead, nominating and voting twice as much.

                            - When alive, the Banshee nominates and votes as normal.

                            - When dead, they may nominate twice per day, even though dead players may normally not nominate at all.

                            - When dead, they may vote for any nomination they wish and do not need a vote token to do so. They may vote twice for the same nomination.

                            - The Banshee only gains these powers if they were killed by the Demon. Dying by execution or to a non-Demon ability does not count.

                            - To vote twice, the Banshee player raises both hands when votes are counted. If the player is unable to do this due to a disability, the Storyteller can count their normal vote twice.
                            """,
                    """
                            - The Kazali kills the Banshee. All players learn that the Banshee has died. Tomorrow, the Banshee nominates the Village Idiot and votes twice, then nominates the Fearmonger and votes twice, then votes twice when the Shugenja is nominated. The next day, the Banshee doesn’t nominate at all, but votes twice for the Kazali.

                            - The Banshee is poisoned. The Ojo kills the Banshee. Nobody learns that the Banshee has died, and for the rest of the game, the Banshee may not nominate, and has just one vote.

                            - The Lycanthrope kills the Banshee. The Banshee does not gain their additional powers and is not announced.
                            """, "The Yogscast"),
            entry(Role.BOUNTY_HUNTER,
                    "Alone, I walk these streets, paved with the sick stench of corruption. Its thickness worms its way into my nostrils, unbidden, burning with revulsion. And anticipation. The illness of this wretched place grows each night. And I... I am the cure.",
                    """
                            The Bounty Hunter tracks down evil players, one at a time.

                            - The Bounty Hunter starts knowing one evil player. When that player dies, they learn another evil player.

                            - The Bounty Hunter only learns the evil player, not their character.

                            - If the Bounty Hunter is drunk or poisoned when they should learn a new player, the Storyteller may show them a good player. When the recently shown player dies, the Bounty Hunter learns a new player that night.

                            - The Bounty Hunter cannot learn the same evil player twice.

                            - If the Bounty Hunter is in the game at setup, one Townsfolk is evil. The Bounty Hunter may learn the evil Townsfolk.
                            """,
                    """
                            - Alex is the Bounty Hunter, Ben is the Harpy, and Abdallah is the Tea Lady. During setup, the Storyteller decides that Abdallah will be the Evil Tea Lady. On the first night, Alex learns Ben. On day 3, Ben is executed. That night, Alex learns Abdallah.

                            - On the first night, the Bounty Hunter learns Julian, who is the evil Baron. When Julian dies, the Poisoner targets the Bounty Hunter. That night, the Bounty Hunter learns Evin, who is the good Magician.

                            - Lachlan is the Drunk who thinks he is the Bounty Hunter. No evil Townsfolk was added at setup, because the Bounty Hunter is not in play. On the first night, Lachlan learns Marianna, who is the good Empath. When Marianna dies, Lachlan learns Doug, who is the good Flowergirl.
                            """, "The Yogscast"),
            entry(Role.CULT_LEADER,
                    "Thinking themselves wise, they became fools.",
                    """
                            The Cult Leader wins if everyone joins their cult.

                            - At the end of each night, the Cult Leader becomes the alignment of a living neighbor.

                            - Once per day, the Cult Leader may publicly choose to form a cult. If all good players vote to join the cult, the game ends immediately and the Cult Leader’s team wins.

                            - The Cult Leader may form a cult at any point in the day.

                            - Voting to join a cult does not require a vote token.

                            - Players may say whatever they want at any time, so a player bluffing as the Cult Leader may pretend to form a cult.
                            """,
                    """
                            - On day 3, the good Cult Leader’s living neighbors are the good Town Crier and the evil Goblin. The Cult Leader requests to form a cult, and all good players vote to join the cult. The game ends and the good team wins!

                            - The Cult Leader neighbors the No Dashii. On day 2, the Cult Leader attempts to form a cult. All players vote to join the cult, but a cult is not formed, because the Cult Leader is poisoned.

                            - The Cult Leader’s living neighbors are the evil Poisoner and the good Fortune Teller. The Poisoner chooses the Cult Leader, and the Kazali kills the Fortune Teller. The Cult Leader’s living neighbors are now the evil Poisoner and the evil Wizard. While the Cult Leader’s living neighbors are both evil, the Cult Leader doesn’t turn evil, because they cannot change alignment while poisoned.
                            """, "The Yogscast"),
            entry(Role.FISHERMAN,
                    "This was my favourite part of the river... see how the sunlight makes a rainbow from the monastery to the market? This was the best place for big fish. And the older I get, the bigger they were.",
                    """
                            The Fisherman knows something that nobody else can know: what should be done.

                            - The Fisherman player chooses when to use their ability.

                            - When they request their information, the Storyteller chooses what piece of advice to give the Fisherman.

                            - The Storyteller’s pieces of advice are not necessarily “facts”. They are strategy tips that the Storyteller believes will help the Fisherman win, if they are followed.

                            - If the Fisherman is drunk or poisoned, the Storyteller may give the Fisherman bad advice.
                            """,
                    """
                            - The Fisherman uses their ability and learns that “You shouldn’t trust Ben”. Ben is the poisoned Empath, and is unknowingly spreading false information.

                            - The Fisherman learns “Keep the players claiming to be Outsiders alive”. These players are secretly the Klutz and the Fearmonger. The Storyteller believes that keeping these players alive is more likely to end up with good executing the Demon.

                            - On the final night, the Fisherman learns to “kill Lewis”. The Fisherman is drunk, and Lewis is a Townsfolk. The Storyteller gave bad (“false”) advice to the Fisherman.
                            """, "The Yogscast"),
            entry(Role.HIGH_PRIESTESS,
                    "There is life behind the personality that uses personalities as masks. There are times when life puts off the mask and deep answers to deep.",
                    """
                            The High Priestess acts on intuition.

                            - The High Priestess can be shown the same player multiple times in a row, or a different player every night.

                            - The shown player can be alive or dead.

                            - The shown player can be good or evil.

                            - There are no official criteria that determine which player the Storyteller must show to the High Priestess. It is up to the Storyteller’s judgement as to what they think will most benefit the High Priestess and the good team in general. It could be because the player has important information that has not been revealed yet. Or because the player is evil and has a bluff that doesn’t make sense. Or because the player is trustworthy and needs to be trusted more. Or because the player is good but on the wrong track and needs to be corrected. Or something new.
                            """,
                    """
                            - On the first night, the High Priestess learns Julian. Julian is the Chef and has useful information to share. On the second night, the High Priestess is shown Marianna. Marianna is the Goblin and the Storyteller thinks that the High Priestess would benefit most from talking to Marianna to find this out as early as possible. On the third night, the High Priestess is shown Doug. Doug is the Drunk whose information is wrong and harming the good team.

                            - For three nights in a row, the High Priestess learns Sarah. Sarah is the Saint and the good team are trying to execute her. On the last night, the High Priestess learns Lewis. Lewis is the Imp, and his story is clashing with several good players.
                            """, "The Yogscast"),
            entry(Role.KNIGHT,
                    "When a man lies, he murders some part of the world.",
                    """
                            The Knight knows players that are not the Demon.

                            - On the first night, the Knight learns two players who are not the Demon.

                            - On subsequent nights, they learn nothing more.

                            - The Knight can learn Townsfolk, Outsiders or even Minions but does not learn which character type they are.
                            """,
                    """
                            - Lewis is the Undertaker, Doug is the Imp and Ben is the Fortune Teller. The Knight learns Lewis and Ben.

                            - Marianna is the Vortox and Abdallah is the Alchemist. The Knight learns Marianna and Abdallah. The Knight must learn Marianna and Abdallah because the Knight's information must be false due to the Vortox ability and therefore include the Demon.
                            """, "The Yogscast"),
            entry(Role.NIGHTWATCHMAN,
                    "The night is cold and lonely, but I have the moon, the stars, the crisp wind and the soft thud of leather boots on cobbled stone for company. Yonder, candlelight flickers behind a murky window...",
                    """
                            The Nightwatchman is known by one player.

                            - At night, the Nightwatchman chooses a player. This player wakes, and learns which player the Nightwatchman is.

                            - The Nightwatchman and their chosen player do not make eye contact. They wake separately.

                            - The Nightwatchman player chooses which night to act.
                            """,
                    """
                            - Lachlan is the Nightwatchman. He chooses Abdallah. Abdallah learns that Lachlan is the Nightwatchman.

                            - Marianna is the drunk Nightwatchman. She chooses Amy. Amy does not wake, and does not learn that Marianna is the Nightwatchman, because the Nightwatchman has no ability.

                            - Ben is the Nightwatchman and Vortox is in play. Ben chooses Sarah. Sarah learns that Lewis is the Nightwatchman. Even though the Nightwatchman has their ability, the information is false.
                            """, "The Yogscast"),
            entry(Role.PREACHER,
                    "It is better to be rich and healthy than poor and sick.",
                    """
                            The Preacher removes Minion abilities.

                            - If the Preacher chooses a Minion, that Minion is woken to learn that they have been preached, and can no longer act while the Preacher is alive, sober, and healthy.

                            - If the Preacher chooses a player who is not a Minion, nothing happens.

                            - The Preacher may choose dead players.

                            - If the Preacher is drunk or poisoned at the time they choose a player, that player is not affected by the Preacher’s ability.

                            - If the Preacher becomes drunk or poisoned, preached Minions regain their abilities until the Preacher is sober and healthy.
                            """,
                    """
                            - The Preacher points to Alex, who is the Engineer. Nothing happens.

                            - The Preacher points to Lachlan, who is the Cerenovus. Lachlan wakes to learn that he has been preached, and is not woken to use his ability.

                            - Doug is the Preacher. He points to Marianna, who is the Pit-Hag. Marianna wakes to learn that she has been preached, and is not woken to use her ability. The following night, the Poisoner points to Doug. Because Doug is now poisoned, Marianna wakes to use her ability.
                            """, "The Yogscast"),
            entry(Role.PRINCESS,
                    "Our words are hounds, bound by silken threads, dear lords. Let kindness weave them true, lest the reigns unravel and rend our court.",
                    """
                            The Princess decides which player dies first.

                            - For the Princess ability to work, the player that the Princess nominated must be the one executed. Players executed but nominated by others don’t count.

                            - The executed player does not have to die for the Princess ability to work.

                            - If the Princess is drunk during the day, then sober at night, they prevent the Demon from killing. If the Princess is sober during the day, but drunk at night, they do not.

                            - At night, non-Demon kills happen as normal.

                            - At night, the Demon still chooses a player to kill, but they do not die. Other parts of the Demon’s ability, such as poisoning players, making false information, etc. happen as normal.

                            - The Princess does not have to nominate on their 1st day.

                            - If a Princess is created mid-game, and they nominate and execute a player on their 1st day as a Princess, the Demon doesn’t kill that night.
                            """,
                    """
                            - The Princess nominates the Preacher. The Preacher is executed, and dies. That night, the Vortox chooses the Pixie, who does not die. The Town Crier learns that a Minion nominated today, which is false information due to the Vortox.

                            - On night 4, the Pit-Hag turns the Dreamer into the Princess. That day, the Princess nominates the Zealot, who is executed. That night, the Kazali chooses the Princess, who does not die.
                            """, "Autumn Wind"),
            entry(Role.SHUGENJA,
                    "これは夢。それも夢。すべて夢です。",
                    """
                            The Shugenja trusts players to their left, or to their right.

                            - The closest evil player is the player with the smallest number of steps from the Shugenja to the evil player.

                            - If the evil players are ‘equidistant’, that means that the closest evil player clockwise is the same number of steps away from the Shugenja as the closest evil player anti-clockwise.

                            - If the evil players are equidistant, the storyteller gives ‘arbitrary’ information to the Shugenja. This means that the Storyteller chooses whether to tell the Shugenja that the closest evil player is clockwise or anti-clockwise.

                            - The Shugenja doesn’t know whether their information is arbitrary or not.

                            - The Shugenja does not learn how many steps away the evil player is.

                            - If a Shugenja is created mid-game, the Shugenja wakes that night to receive their information.
                            """,
                    """
                            - The Organ Grinder is 2 steps away from the Shugenja in a clockwise direction. The Fearmonger is 3 steps away from the Shugenja in an anti-clockwise direction. The Shugenja wakes and learns that the closest evil player is in a clockwise direction.

                            - The Marionette is 1 step away from the Shugenja in a clockwise direction. The Widow is 1 step away from the Shugenja in an anti-clockwise direction. The Shugenja wakes and the Storyteller chooses to tell the Shugenja that the closest evil player is in a clockwise direction.
                            """, "The Yogscast"),
            entry(Role.STEWARD,
                    "How DARE you accuse Her Ladyship of wrongdoing? I’ve known her my entire life! All nine years!",
                    """
                            The Steward knows 1 good player.

                            - The Steward learns a player, but not their character.

                            - The Steward learns their information on the first night of the game.

                            - If created mid-game, then the Steward learns their information that night instead.
                            """,
                    """
                            - The Steward learns that Alex is good. Alex is the Undertaker.

                            - The Pit-Hag turns the Poppy Grower into the Steward. That night, the Steward learns that Abdallah is good. Abdallah is the Spy, and is registering as good.
                            """, "The Yogscast"),
            entry(Role.VILLAGE_IDIOT,
                    "Roses are blue, and violets are red, Please reverse what I just said.",
                    """
                            The Village Idiots are a group that learn players’ alignments.

                            - The Village Idiot that is drunk is chosen by the Storyteller during the game setup.

                            - There may be one, two, or three Village Idiots in play, irrespective of the number of players.

                            - If there is only one Village Idiot in play, they are sober.

                            - The drunk Village Idiot may get true information.

                            - When Village Idiots are added to the game during setup, they replace other Townsfolk.

                            - If a Village Idiot is created mid-game, only one is created.

                            - Village Idiots act one at a time, not all together.

                            - If all sober Village Idiots exit play, the remaining drunk Village Idiot remains drunk.

                            - If a sober Village Idiot becomes drunk or poisoned by other means, the drunk Village Idiot remains drunk.
                            """,
                    """
                            - Doug, Lewis, and Amy are all Village Idiots. Doug is drunk. At night, they all choose Evin, the Kazali. Doug learns that Evin is good. Lewis and Amy learn that Evin is evil.

                            - Ben and Marianna are Village Idiots. Marianna is drunk. Sarah is evil, and bluffing as the Village Idiot. Ben chooses Sarah and learns that she is evil. Marianna chooses the Heretic player and learns that they are good. Sarah claims to have chosen Ben and learnt that he is evil.
                            """, "The Yogscast"),


            // --- OUTSIDERS ---


            //TB
            entry(Role.BUTLER,
                    """
                            Yes, sir...
                            No, sir...
                            Certainly, sir.""",
                    """
                            The Butler may only vote when their Master (another player) votes.

                            - Each night, the Butler chooses a player to be their Master. This may be the same player as last night or a different one.

                            - If the Master has their hand raised to vote, or if the Master's vote has already been counted, the Butler may raise their hand to vote.

                            - If the Master has their hand down, signaling that they are not voting, or if the Master lowers their hand before their vote is tallied, the Butler must lower their hand too.

                            - It is not the Storyteller's responsibility to monitor the Butler. They're responsible for their own voting. Deliberately voting when they shouldn't is considered cheating.

                            - Dead players may only raise their hand to vote if they have a vote token. If the Butler chooses a dead player as their Master, this still applies.

                            - The Butler is never forced to vote.

                            - The Butler's vote may be tallied by the Storyteller before or after their Master's. Seating position is not important.
                            """,
                    """
                            - The Butler chooses Filip to be their Master. Tomorrow, if Filip raises his hand to vote on an execution, then the Butler may too. If not, then the Butler may not raise their hand.

                            - A nomination is in progress. The Butler and their Master both have their hands raised to vote. As the Storyteller is counting votes, the Master lowers their hand at the last second. The Butler must lower their hand immediately.

                            - The Butler is dead. Because dead players have no ability, the Butler may vote with their vote token at any time.
                            """, "The Yogscast"),
            entry(Role.SAINT,
                    "Wisdom begets peace. Patience begets wisdom. Fear not, for the time shall come when fear too shall pass. Let us pray, and may the unity of our vision make saints of us all.",
                    """
                            The Saint ends the game if they are executed.

                            - If the Saint dies by execution, the game ends. Good loses and evil wins.

                            - If the Saint dies in any way other than execution—such as the Demon killing them—then the game continues.
                            """,
                    """
                            There are seven players alive and nominations are in progress. The Saint gets four votes and is about to die. Then, the Baron is nominated but only gets three votes. No more nominations occur today. The Saint is executed, and evil wins.
                            """, "The Yogscast"),
            entry(Role.RECLUSE,
                    "Garn git ya darn grub ya mitts ofma lorn yasee. Grr. Natsy pikkins yonder southwise ye begittin afta ya! Git! Me harvy no so widda licks and demmons no be fightin' hadsup ne'er ma kin. Git, assay!",
                    """
                            The Recluse might appear to be an evil character, but is actually good.

                            - Whenever the Recluse's alignment is detected, the Storyteller chooses whether the Recluse registers as good or evil.

                            - Whenever the Recluse is targeted by an ability that affects specific Minions or Demons, the Storyteller chooses whether the Recluse registers as that specific Minion or Demon.

                            - The Recluse may register as either good or evil, or as an Outsider, Minion, or Demon, at different parts of the same night. The Storyteller chooses whatever is most interesting.

                            - A Recluse that registers as a particular Minion or Demon does not have this character's ability. For example, a Recluse that registers as a Poisoner does not wake at night and cannot poison a player.
                            """,
                    """
                            - The Slayer uses their ability on the Recluse. The Storyteller decides that the Recluse registers as the Imp, so the Recluse dies. The Slayer believes that they just killed the Imp.

                            - The Empath, who neighbours the Recluse and the Monk, learns she is neighbouring one evil player. The next night, the Empath learns they are neighbouring no evil players.

                            - The Investigator learns that either the Recluse or the Saint is the Scarlet Woman.

                            - The Recluse is executed. The Undertaker learns that the Imp was executed.

                            - The Recluse neighbours the Imp and a Minion. Because showing a "2" to the Chef might be too revealing, the Chef learns true information, a "0,” instead.
                            """, "The Yogscast"),
            entry(Role.DRUNK,
                    "I’m only a *hic* social drinker, my dear. Admittedly, I am a heavy *burp* socializer.",
                    """
                            The Drunk player thinks that they are a Townsfolk, and has no idea that they are actually the Drunk.

                            - During setup, the Drunk's token does not go in the bag. Instead, a Townsfolk character token goes in the bag, and the player who draws that token is secretly the Drunk for the whole game. The Storyteller knows. The player does not.

                            - The Drunk has no ability. Whenever their Townsfolk ability would affect the game in some way, it doesn't. However, the Storyteller pretends that the player is the Townsfolk they think they are. If that character would wake at night, the Drunk wakes to act as if they are that Townsfolk. If that Townsfolk would gain information, the Storyteller may give them false information instead—and the Storyteller is encouraged to do so.
                            """,
                    """
                            - The Drunk, who thinks they are the Soldier, is attacked by the Imp. The Drunk dies.

                            - The Drunk, who thinks they are the Empath, wakes and learns a "0,” even though they are sitting next to one evil player. The next night, they learn a "1.".

                            - The Drunk, who thinks they are the Ravenkeeper, is killed at night. They choose the Saint, but learn that this player is the Poisoner.

                            - The Fortune Teller is executed. That night, the Drunk, who thinks they are Undertaker, learns that the Drunk died today.
                            """, "The Yogscast"),

            //BMR

            entry(Role.GOON,
                    "Yes boss. I explained fings real good to dat geezer. He don't want me explain it again. Nah boss, I don't need no doctor - it's only a knife wound. Be right come mornin'",
                    """
                            The Goon is immune to other characters at night, but keeps changing allegiances.

                            - Each night, the first time a player wakes to use their ability and chooses the Goon, that player becomes drunk immediately. Their ability does not work tonight, nor the next day.

                            - Later on the same night, if another player wakes to use their ability and chooses the Goon, their ability works as normal.

                            - The Goon cannot make a player drunk unless the player chose the Goon. The Storyteller choosing the Goon due to an ability, such as the Grandmother’s, doesn’t count.

                            - As soon as the Goon makes a player drunk, the Goon changes alignment to match theirs. The Goon still changes alignment, and makes the player drunk, if the player choosing the Goon was already drunk or poisoned.

                            - If chosen by the Assassin, the Goon dies but still turns evil.
                            """,
                    """
                            - The Courtier chooses the Goon. The Goon turns good, and the Courtier becomes drunk.

                            - The Shabaloth attacks the Goon, then the Gossip. Since the Shabaloth became drunk as soon as they chose the Goon, neither player dies tonight, and the Goon turns evil. The next night, the Shabaloth attacks the Gambler then the Goon. The Gambler dies, then the Shabaloth becomes drunk again. The Goon is still alive and still evil.

                            - The Chambermaid chooses the Goon and the Minstrel, and learns a "1" because the Chambermaid is drunk.

                            - The Tea Lady neighbours the good Goon and the Tinker. The Tinker is executed, but does not die. The next day, the Goon is evil. The Tinker is executed again and dies.
                            """, "The Yogscast"),
            entry(Role.LUNATIC,
                    "I am the night... I think.",
                    """
                            The Lunatic thinks that they are the Demon.

                            - Much like the Drunk, the Lunatic does not know their real character or real alignment. They are woken each night to attack as if they were the Demon that is in play, but their choices have no effect because they have no Demon ability.

                            - The Lunatic wakes during the first night to learn three bluffs and the appropriate number of Minions, but this information may be wrong.

                            - The real Demon knows which players the Lunatic chose to attack each night.
                            """,
                    """
                            - The Lunatic, thinking they are the Shabaloth, wakes each night to choose two players. The chosen players do not die.

                            - The Lunatic, thinking they are the Zombuul, does not wake often at night. The real Zombuul, who is pretending to be the Lunatic's Minion, often attacks the same players the Lunatic chooses, to keep up the illusion that the Lunatic is the Demon.
                            """, "The Yogscast"),
            entry(Role.TINKER,
                    "I think I see the problem. Luckily, I have an idea! This catapult will shoot twice as far with just a minor adjustment...",
                    """
                            The Tinker can die at any time, for no reason.

                            - The Storyteller may kill the Tinker at any time.

                            - The Tinker cannot die from their ability while protected from death, as normal.
                            """,
                    """
                            - During the night, the Tinker dies, even though the Demon attacked a different player.

                            - The Tea Lady sits next to the Tinker and another good player, protecting the Tinker from death. The Tinker cannot die from their ability.

                            - The Tinker is attacked by the Demon. The Tinker does not die because they are protected by the Innkeeper. Later that night, the Innkeeper dies, so the Storyteller chooses to kill the Tinker too.
                            """, "mezz (JEI)"),
            entry(Role.MOONCHILD,
                    "Scorpio looks sideways at the lovers, and you have a choice to make. With silver cross my palm, and your fate shall be revealed. With steel cross my throat, and by the stars you shall regret it.",
                    """
                            The Moonchild curses someone upon death, killing them too.

                            - The Moonchild must choose a player within a minute or two of learning that they are dead, whether by an execution or at dawn when the Storyteller declares who died at night. The Moonchild can take their time and get advice from the group before making this decision.

                            - If the Moonchild chooses a good player, that player dies tonight. If they choose an evil player, nothing happens.

                            - As always, play along if an evil player is bluffing as the Moonchild and pretends to use their ability.

                            - It is not the Storyteller’s responsibility to prompt the Moonchild to choose a player. The Moonchild must do this shortly after they learn that they are dead. Deliberately not doing so is considered cheating.

                            - If the Moonchild is sober and healthy at night but was drunk or poisoned when they chose a player today, that player dies. If the Moonchild is drunk or poisoned at night but was sober and healthy when they chose a player today, that player doesn’t die.

                            - The Moonchild kills the Goon if the Goon was good when the Moonchild chose them, regardless of the Goon’s alignment at night.
                            """,
                    """
                            - The Pukka kills the Moonchild. The next morning, the Moonchild chooses a player, who is the Exorcist. That night, the Exorcist dies.

                            - The Pacifist is in play. The Moonchild is executed but remains alive. The Moonchild does not choose a player, because the Moonchild did not die.

                            - The Shabaloth eats the Moonchild. The Moonchild chooses the Assassin, who remains alive. The Shabaloth regurgitates the Moonchild. A few nights later, the Shabaloth eats the Moonchild again. This time, the Moonchild chooses the Gossip, who dies.
                            """, "The Yogscast"),
            //S&V
            entry(Role.MUTANT,
                    "I am not a freak! I am a human being! Have mercy!",
                    """
                            The Mutant is killed if they try to reveal who they are.

                            - “Madness” is a term that means “you are trying to convince the group of something.” So, if the Mutant player is mad about being the Mutant, this means they are trying to convince people that they are the Mutant. If they are mad about being an Outsider, this means they are trying to convince people that they are an Outsider.

                            - This can be by verbally hinting who they are, or by their silence when questioned. It is always up to the Storyteller to decide what the Mutant is doing. If you think they are trying to convince the group they are an Outsider in any way, you can execute them—even outside the nomination phase, or at night. If you do, no other executions may happen today by normal means, since there is only one execution per day.

                            - Mutant at night, you may execute them that night, even if an execution happened today. Declare they have died, and continue the night phase as normal. An execution may still happen the next day.
                            """,
                    """
                            - Ten seconds into the first day, the Mutant says to the group that they’re the Mutant. The Storyteller declares that the Mutant is executed immediately. There is no nomination for an execution today, since there can be a maximum of one execution per day.

                            - A Witch privately talks to the Storyteller and says that Evin, who is playing the Mutant, told them they are the Klutz. The Storyteller chooses to execute the Mutant immediately.

                            - The Mutant tells the group that they are a Townsfolk, but does not say which one. When questioned if they are the Mutant, they stay silent. After a minute or so of silence, the Storyteller executes the Mutant.

                            - The Mutant says they are the Oracle, gives some bogus Oracle information, then says "By the way, I am definitely not the Mutant" while giving a subtle wink. The Storyteller chooses to execute the Mutant immediately.
                            """, "The Yogscast"),
            entry(Role.SWEETHEART,
                    "I will never forget her. Never.",
                    """
                            The Sweetheart, when they die, causes someone to be drunk for the rest of the game.

                            - The Storyteller chooses which player becomes drunk.

                            - This ability works while the Sweetheart is dead.
                            """,
                    """
                            - The Sweetheart dies. The Mathematician is now drunk, and may get false information at night.

                            - The Sweetheart dies. The Mutant is now drunk. The Mutant may safely come out as the Mutant, but they do not know this.

                            - The Sweetheart dies. The Demon is now drunk, so their attack at night won’t kill anyone.
                            """, "The Yogscast"),
            entry(Role.BARBER,
                    "Did you know that barbery and surgery were once the same profession? No? Well, now you do.",
                    """
                            The Barber allows the Demon to swap any 2 player's characters.

                            - The players’ alignments stay the same when they swap characters. Each player learns which character they become.

                            - The Demon may choose not to swap players.

                            - If a player becomes a new character, they gain the new ability, even if it was a “you start knowing” ability or a “once per game” ability that the original character already used.

                            - If there is more than one living Demon, the Storyteller chooses which Demon makes the swap.

                            - The Demon may choose themself to swap.

                            - The Demon may not choose another Demon player to swap.

                            - If a player dies and then becomes the Barber, the Demon may not swap two players’ characters tonight.
                            """,
                    """
                            - The Barber dies. The Demon considers swapping the Clockmaker and the Juggler, but then does nothing.

                            - The Barber dies. The Demon swaps the alive Snake Charmer with the dead Barber. Now, there is an alive Barber and a dead Snake Charmer.

                            - The Barber dies. The Vortox swaps themself with an alive Witch.

                            - The Barber dies. The Vigormortis swaps themself with a dead Sweetheart. The old Vigormortis is now the evil Sweetheart. Because the Pit-Hag created a good Demon during the previous night, the game continues.
                            """, "Autumn Wind"),
            entry(Role.KLUTZ,
                    "Oops.",
                    """
                            The Klutz might accidentally lose the game for their team, unless they are clever.

                            - When the Klutz dies, they must declare a player. They may take a few minutes to do so—after all, it’s a big decision, and other players may give advice on who to choose, but it is always the Klutz’s decision. If they choose an evil player, the game ends immediately and the good team loses. If they choose a good player, nothing happens and the game continues.

                            - It is not the Storyteller’s responsibility to prompt the Klutz to declare they are the Klutz and choose a player. The Klutz must do this shortly after they learn that they are dead. Deliberately not doing so is considered cheating.
                            """,
                    """
                            - The Klutz dies by execution. After much yelling and confusion, the Klutz chooses a player—who is secretly the Seamstress. Night falls, and the game continues.

                            - The Demon kills Dave, the Klutz. In the morning, when the Storyteller informs the group that Dave is dead, Dave says "Ok everybody, I was the Klutz" and after discussion for a few minutes, Dave publicly chooses the player that is the Demon. The game ends immediately and evil rejoices.
                            """, "The Yogscast"),
            //Kickstarter
            entry(Role.DAMSEL,
                    "Don't touch ze hair, honey.",
                    """
                            The Damsel needs to avoid being found by the Minions.

                            - If a Minion guesses that you are the Damsel, and does so publicly (so that all players know that they are a Minion), evil wins.

                            - No matter how many Minions are in play, they only get one guess, total. If a Minion makes a guess and is wrong, future guesses by this Minion or by other Minions don’t count.

                            - If the Demon pretends to be a Minion making a guess, that doesn’t count as a guess. Minions may still make a guess and win.

                            - Minions may make a guess at any time.

                            - If the Damsel dies, they are no longer at risk of being guessed by a Minion, since the Damsel loses their ability when dead.

                            - There may not be a Huntsman in play. But if there is, and the Huntsman chooses the Damsel at night, the Damsel becomes a not-in-play Townsfolk, and is no longer the Damsel. The Damsel learns which Townsfolk and has that Townsfolk ability from then on.
                            """,
                    """
                            - Marianna is the Damsel. She is bluffing as the Lycanthrope. The Witch guesses that Marianna is the Damsel. Evil wins.

                            - Doug is the Damsel. The Boomdandy guesses that Julian is the Damsel. Nothing happens, and the game continues. The Goblin guesses that Doug is the Damsel. Nothing happens, and the game continues.
                            """, "The Yogscast"),
            entry(Role.GOLEM,
                    "Golem help? Golem smash! Golem help.",
                    """
                            The Golem kills the player they nominate.

                            - When the Golem nominates a player, that player immediately dies. The nomination process continues.

                            - If the Golem nominates the Demon, nothing happens. The Storyteller doesn’t confirm or deny that the Golem nominated, and continues with the voting process as normal. The Storyteller may say “nothing happens” if clarity is asked for.

                            - After the Golem has nominated once, whether or not the nominee dies, the Golem may not nominate again this game. It is the player’s responsibility to refrain from nominating, not the Storyteller’s. Deliberately nominating when they shouldn’t is considered cheating.
                            """,
                    """
                            - The Golem nominates the Poppy Grower. The Poppy Grower dies. The Golem may not nominate again this game.

                            - The Golem nominates the Recluse. The Recluse registers as the Demon. Nothing happens, and the Storyteller begins counting votes for the Recluse to be executed. The Golem may not nominate again this game.
                            """, "The Yogscast"),
            entry(Role.HERETIC,
                    "After the hail has smashed the roof and splintered the glass of the Cathedral windows, it melts again into the earth, like a dying lamb in the desert sun. Such is the parable of the madman.",
                    """
                            The Heretic turns a win into a loss, and a loss into a win.

                            - If the game ends due to the good team winning, then all good players lose, and all evil players win.

                            - If the game ends due to the evil team winning, then all evil players lose, and all good players win.

                            - The Heretic’s ability applies to all victory conditions, including the game ending due to just two players being alive, the Demon dying, or an ability ending the game.

                            - The Heretic’s ability functions even when the Heretic is dead, but not when the Heretic is drunk or poisoned.
                            """,
                    """
                            - On the first day, the Heretic publicly claims to be the Heretic. That night, the Demon kills themself. Evil wins.

                            - The Heretic does not reveal their character until the final day, when 3 players are alive. They convince the good team to execute a good player, leaving 2 players alive, one of which is the Demon. Good wins.

                            - The Heretic is dead. The Saint is executed. Good wins.

                            - The Heretic is poisoned. The Assassin kills the Demon. Good wins.

                            - There are 3 players alive. The Demon is executed. Because there is a Heretic in play, evil wins.
                            """, "The Yogscast"),
            entry(Role.PUZZLEMASTER,
                    "When one begins to think that some thing is merely some other thing, one is usually on the brink of an error. Patience, patience. Don’t confuse just and should with is and isn’t.",
                    """
                            The Puzzlemaster tries to figure out who is drunk.

                            - A player is drunk for the whole game. It will most often be a Townsfolk, but could be an Outsider. This player does not know that they are drunk.

                            - Once per game, the Puzzlemaster may guess which player it is. They may guess publicly, or privately. Whatever their guess, the Storyteller privately tells the Puzzlemaster the name of one player. If the Puzzlemaster guessed correctly, they learn which player the Demon is. If the Puzzlemaster guessed incorrectly, they learn a different player instead.

                            - The Puzzlemaster isn’t told if they guessed correctly or not.

                            - Only the player made drunk by the Puzzlemaster counts as a successful guess. Players drunk by other means don’t count.

                            - If the Puzzlemaster dies, the drunk player is still drunk. A dead Puzzlemaster may not make a guess, as they don’t have that part of their ability.
                            """,
                    """
                            - Alex is the Demon. Sarah is the Empath who has been made drunk by the Puzzlemaster and is getting false information. The Puzzlemaster publicly guesses that Sarah is drunk, and is told “Alex is the Demon” privately by the Storyteller.

                            - Lewis is the Demon. Ben is dead and is drunk due to the Puzzlemaster. Marianna is alive and drunk due to the Sailor. The Puzzlemaster privately guesses Marianna, and is told “Evin is the Demon” privately by the Storyteller.
                            """, "Autumn Wind"),
            entry(Role.SNITCH,
                    "It was John.",
                    """
                            The Snitch accidentally gives information to the evil team.

                            - The Minions learn three not-in-play characters at the start of the game, just like the Demon does.

                            - These characters may be the same three that the Demon learns, or different characters.

                            - Each Minion may learn different characters to each other. Or they may all learn the same three characters.
                            """,
                    """
                            - On the first night, the Demon and its two Minions all learn that the Sage, Innkeeper, and Golem are not in play.

                            - On the first night, the Demon learns that the Fool, Monk, and Saint are not in play. The Mastermind learns that the Fool, Monk, and Saint are not in play. The Witch learns that the Fool, Flowergirl, and Barber are not in play. The Fearmonger learns that the Noble, Amnesiac, and Heretic are not in play.

                            - On the fourth night, the Pit-Hag creates a Snitch. All Minions learn three not-in-play characters.
                            """, "The Yogscast"),
            //Carousel
            entry(Role.HATTER,
                    "One Hat. Too Hat. Three Hat. Tea Hat. Fore Hat. Thrive Hat. Six Hat. Sticks Hat.",
                    """
                            The Hatter allows the evil players to change characters.

                            - Each player with a Minion or Demon character may choose to become any character of the same type as their current character.

                            - They may choose not to change characters.

                            - If a player becomes a new character, they gain the new ability, even if it was a "you start knowing" ability or a once per game ability that had already been used.

                            - Once a player has changed character, their previous character ability has no further effect on the game.

                            - If a player dies then becomes the Hatter, the evil players do not change characters tonight.

                            - Once a character has been chosen, a second player cannot choose the same character. If it is already in play, the player with that character must choose a new character.
                            """,
                    """
                            - The Hatter dies. The Ojo chooses to become the No Dashii and the Devil's Advocate chooses to become the Scarlet Woman.

                            - The Vigormortis has killed their Pit-Hag, then later kills the Hatter. Both players choose to stay the same characters, so that they don't lose the effects of the Vigormortis.

                            - The Assassin kills a player. The Hatter is executed. That night, the Assassin becomes the Mastermind and the Mastermind shakes their head no to stay the Mastermind. The Storyteller shakes their head no because the Mastermind character has been chosen already, and gestures for the Mastermind player to choose again, so they choose Assassin. The new Assassin then uses their new ability to kill a player.
                            """, "The Yogscast"),
            entry(Role.HERMIT,
                    "In the lost and forgotten places of the earth, the soul’s light beckons.",
                    """
                            The Hermit isn’t really here.

                            - The Hermit has the abilities of all the other Outsiders on the Script, all at once. They do not have the abilities of Outsiders that are not on the script.

                            - If a custom script has more than 4 Outsiders, the Hermit has all these Outsider abilities.

                            - If one of the Outsider abilities continues after death, such as the Recluse’s, the Hermit keeps that ability when they die, but does not keep their other Outsider abilities.

                            - A Hermit with the Drunk ability does not know that they are the Hermit, and their other Outsider abilities function as normal. A Hermit with the Recluse ability can register as a different character etc.

                            - If an Outsider has a jinx, that jinx applies to the Hermit too.

                            - The Hermit may remove the Hermit from play during setup, resulting in one less Outsider than normal. If this happens, the Hermit may still be a bluff given to the Demon.
                            """,
                    """
                            - Marianna is the Hermit, and has the Klutz, Butler, and Recluse abilities. Each night, she chooses a player as her master due to the Butler ability. The Ravenkeeper dies and learns that she is the Lord of Typhon, due to the Recluse ability. Marianna is executed, and chooses a player due to the Klutz ability. She chooses the Wizard. Good loses.

                            - Lewis is the Hermit and has the Drunk, Mutant, and Sweetheart abilities. Thinking that he is the Exorcist, he chooses a player each night. Lewis says that he thinks he might be the Drunk, so the Storyteller executes him due to the Mutant ability. Due to the Sweetheart ability, the Alchemist is now drunk.
                            """, "Autumn Wind"),
            entry(Role.OGRE,
                    "<grunt><grin></grunt>",
                    """
                            The Ogre is someone's best friend.

                            - The Ogre's chosen player does not change, even if the Ogre is drunk or poisoned when they chose.

                            - The Ogre becomes the same alignment as their chosen player immediately on the first night, even if the Ogre is drunk or poisoned.

                            - The Ogre is not told their alignment at the beginning of the game.

                            - If the Ogre changes alignment by other means, the Ogre learns their new alignment, as normal.

                            - If an Ogre is created mid-game, the Ogre chooses a player that night, and becomes their alignment.
                            """,
                    """
                            - On the first night, the Ogre chooses the Summoner. The Ogre becomes evil, and stays evil for the rest of the game.

                            - On the first night, the Ogre chooses the Banshee. The Ogre stays good. On the third night, the Mezepheles turns the Banshee evil. The Ogre remains good.
                            """, "The Yogscast"),
            entry(Role.PLAGUE_DOCTOR,
                    "Pleauze shtay shtill. Thinks nid tiime for hillink. Myrhh-myrhh.",
                    """
                            The Plague Doctor brings an extra Minion ability into play.

                            - The Storyteller chooses which Minion ability is gained.

                            - This ability is in effect for the rest of the game.

                            - Nothing else changes for the Storyteller – they don’t become evil, they don’t become a player, they are not a legitimate player to be targeted by other abilities, and they cannot vote or nominate.

                            - If the Plague Doctor is drunk or poisoned when they die, the Storyteller doesn’t gain a Minion ability, even when the Plague Doctor becomes sober and healthy.
                            """,
                    """
                            - The Plague Doctor dies. The Storyteller gains the Poisoner ability and chooses a player to poison each night for the rest of the game.

                            - The Plague Doctor is executed and the Storyteller gains the Cerenovus ability. That night, the Pit-Hag turns the Witch into the Cerenovus. There are now two Cerenovus abilities in play – the Storyteller’s and the new Cerenovus’.

                            - The Plague Doctor has died and the Storyteller has had the Organ Grinder ability for two days. The Plague Doctor is made drunk by the Minstrel. The Storyteller still has the Organ Grinder ability as they gained it when the Plague Doctor died and the Plague Doctor now being drunk does not affect that.
                            """, "The Yogscast"),
            entry(Role.POLITICIAN,
                    "I'm glad you asked that question. Truly, I am. But I think the REAL question here is...",
                    """
                            The Politician changes teams if they are losing.

                            - When the game ends, if the Politician was responsible for good losing, then the Politician turns evil and wins too.

                            - The player needs to be very influential when determining who wins. Simply spreading false information or voting for good players is usually not enough – they need to be the player that was more responsible for the good team losing than any other good player, and preferably more responsible than any one evil player too. The Storyteller is the judge of whether the Politician’s actions qualify.

                            - The Politician may still win with the good team, as normal.

                            - A drunk or poisoned Politician can not change teams.
                            """,
                    """
                            - The Politician has been trying to execute the Demon all game, without success. With just three players alive, the Politician convinces the group to not execute, since someone is claiming to be the Mayor. There is no execution, and evil wins because a Minion was bluffing as the Mayor. The Politician wins too.

                            - On the final day, the Politician votes for the Empath, and tells the group that the Empath is evil. The Saint is executed instead. The Storyteller judges that the good team lost due to their own actions as a group, not just the bad advice of the Politician. Good loses, and the Politician loses.

                            - The Politician, believing that evil is winning, bluffs as the Atheist. The Storyteller is executed. Evil wins, and the Politician wins too.
                            """, "The Yogscast"),
            entry(Role.ZEALOT,
                    "I enjoy talking to you. Your mind appeals to me. It resembles my own mind except that you happen to be insane.",
                    """
                            The Zealot votes.

                            - If there are 5 or more players alive, the Zealot must vote for every nomination. If there are 4 or fewer players alive, the Zealot can choose whether they vote or not.

                            - The Zealot can vote like a normal dead player when dead.

                            - A Zealot must vote even if they think they might be drunk or poisoned.

                            - It is not the Storyteller's responsibility to monitor the Zealot. They're responsible for their own voting. Deliberately not voting when they should is considered cheating.
                            """,
                    """
                            - There are 7 players alive. The Zealot votes for the Alsaahir, the Summoner, Ogre, and the Banshee. The next day, there are 5 players alive. The Zealot votes for the Yaggababble, and the High Priestess. The next day, there are 3 players alive. The Zealot votes for the Yaggababble, but chooses not to vote for the High Priestess.

                            - There are 9 players alive. The Zealot is dead. The Zealot doesn't vote for 3 days, and uses their vote token when just 3 players are alive to vote for the Farmer.
                            """, "The Yogscast"),


            // --- MINIONS ---


            //TB
            entry(Role.POISONER,
                    "Add compound Alpha to compound Beta... NOT TOO MUCH!",
                    """
                            The Poisoner secretly disrupts character abilities.

                            - Each night, the Poisoner chooses someone to poison for that night and the entire next day.

                            - A poisoned player has no ability, but the Storyteller pretends they do. They do not affect the game in any real way. However, to keep up the illusion that the poisoned player is not poisoned, the Storyteller wakes them at the appropriate time and goes through the motions as if they were not poisoned. If their ability gives them information, the Storyteller may give them false information.

                            - If a poisoned player uses a "once per game" ability while poisoned, they cannot use their ability again.
                            """,
                    """
                            - During the night, the Poisoner poisons the Slayer. The next day, the Slayer tries to slay the Imp. Nothing happens. The Slayer now has no ability.

                            - The poisoned Empath, who neighbours two evil players, learns a "0.” The next night, the Empath, no longer poisoned, learns the correct information: a "2.”

                            - The Investigator is poisoned. They learn that one of two players is the Baron, even though neither is a Minion. (Or even the right players, but the wrong Minion type.)

                            - The Undertaker is poisoned. Even though the Imp died today, they learn that the Virgin died. A few days later, a poisoned Saint dies, and the game continues.

                            - The Poisoner poisons the Mayor, then becomes the Imp. The Mayor is no longer poisoned because there is no Poisoner in play.
                            """, "The Yogscast"),
            entry(Role.SPY,
                    "Any brewmaster worth their liquor, knows no concoction pours trouble quicker, than one where spies seem double.",
                    """
                            The Spy might appear to be a good character, but is actually evil. They also see the Grimoire, so they know the characters (and status) of all players.

                            - If any character has an ability that would detect or affect a good player, then the Spy might register as good to that character. If any character has an ability that detects Townsfolk or Outsiders, then the Spy might register as a specific Townsfolk or Outsider to that player. It is the Storyteller's choice as to what the Spy registers as, even as many characters or both alignments during the same night.

                            - A Spy that registers as a particular Townsfolk or Outsider does not have this character's ability. For example, a Spy that registers as a Slayer cannot slay the Demon.
                            """,
                    """
                            - The Washerwoman learns that either Abdallah or Douglas is the Ravenkeeper. Abdallah is the Monk, and Douglas is the Spy registering as the Ravenkeeper.

                            - The Spy neighbours the Imp and the Empath. The Chef learns a "1" because the Spy is registering as evil. Later that night, the Empath learns a "0" because the Spy is now registering as good.

                            - The Spy nominates the Virgin and is executed by the Virgin’s ability, because the Storyteller chooses that the Spy registers as a Townsfolk. That night, the Undertaker learns that the Drunk died today, because the Spy is now registering as the Drunk.
                            """, "The Yogscast"),
            entry(Role.BARON,
                    "This town has gone to the dogs, what? Cheap foreign labor... that's the ticket. Stuff them in the mine, I say. A bit of hard work never hurt anyone, and a clip'o'the ears to any brigand who says otherwise. It's all about the bottom line, what?",
                    """
                            The Baron changes the number of Outsiders present in the game.

                            - This change happens during setup, and it does not revert if the Baron dies. A change in characters during setup, regardless of what happens during the game, is shown on character sheets and tokens in square brackets at the end of a character's description—like [this].

                            - The added Outsiders always replace Townsfolk, not other character types.
                            """,
                    """
                            - The game is being set up for seven players, with five Townsfolk, one Minion, and one Demon. Because the Minion is the Baron, the Storyteller removes two Townsfolk tokens and adds a Saint and a Butler token. In total, three Townsfolk, two Outsider, one Minion, and one Demon tokens go in the bag for the players to draw from.

                            - The game is being set up for fifteen players, with nine Townsfolk, two Outsiders, three Minions, and one Demon. Because the Baron is in play, the Storyteller must add a Drunk and a Recluse. So, they remove the Monk token and add a Recluse token. Then, instead of adding the Drunk character token, they add the Drunk's “Is the Drunk” reminder token to the Grimoire... because this game, one player isn’t a Townsfolk—they are an Outsider: the Drunk. All these character tokens go into the bag for the players to draw from.
                            """, "The Yogscast"),
            entry(Role.SCARLET_WOMAN,
                    "You have shown me the secrets of the Council of the Purple Flame. We have lain together in fire and in lust and in beastly commune, and I am forever your servant. But tonight, my dear, I am your master.",
                    """
                            The Scarlet Woman becomes the Demon when the Demon dies.

                            - If there are five or more players just before the Demon dies—that is, four or more players left alive after the Demon dies—then the Scarlet Woman immediately becomes the Demon, and the game continues as if nothing happened.

                            - Travellers do not count as players when seeing if the Scarlet Woman's ability triggers.

                            - If less than five players are alive when the Demon is executed, then the game ends and good wins.

                            - If five or more players are alive when the Imp kills themself at night, the Scarlet Woman must become the new Imp.

                            - If the Scarlet Woman becomes the Demon, they are that Demon in every way. Good wins if they are executed. They attack each night. They register as the Demon.
                            """,
                    """
                            - There are five players alive: the Imp, the Scarlet Woman, the Baron, and two Townsfolk. The Imp is executed. The Scarlet Woman becomes the Imp, and the game continues.

                            - Brianna is the Scarlet Woman. The Fortune Teller chooses Brianna and Alex, and learns a "no.” Later, the Imp dies, so Brianna becomes the Imp. The Fortune Teller chooses Brianna and Alex again, and learns a "yes."
                            """, "The Yogscast"),

            //BMR
            entry(Role.GODFATHER,
                    "Normally, it's just business. But when you insult my daughter, you insult me. And when you insult me, you insult my family. You really should be more careful - it would be a shame if you had an unfortunate accident.",
                    """
                            The Godfather takes revenge when the town kills Outsiders.

                            - Whenever an Outsider is executed and dies, the Godfather chooses one player to die that night.

                            - The Godfather only kills if an Outsider dies during the day. Outsiders that die at night don’t count.

                            - If the Godfather is in play, this adds or removes one Outsider from play.

                            - At the start of the game, the Godfather learns which Outsiders are in play.

                            - If two Outsiders died today, the Godfather still only kills one player tonight.
                            """,
                    """
                            - The Godfather learns that the Lunatic and the Moonchild are in play, so the Godfather bluffs as the Tinker. During the third day, the Lunatic dies by execution. That night, the Demon kills the Minstrel, and the Godfather kills the Pacifist.

                            - The Tinker is executed but remains alive, because they were protected by the Devil's Advocate. The Godfather does not act that night. The next day, the Tinker dies due to their own ability. That night, the Demon kills a player, and the Godfather kills themself to appear like a good player.
                            """, "The Yogscast"),
            entry(Role.DEVILS_ADVOCATE,
                    "My client, should the objection be overruled, pleads innocent by virtue of the prosecution's non-observance of statute 27.B - incorrect or misleading conjugation of a verb. The fact that nine of the jury died last night is simply prima facie, which is, as Wills vs Thule set precedent for, further reason to acquit.",
                    """
                            The Devil's Advocate saves players from execution.

                            - Each night, the Devil’s Advocate chooses a player to protect from death by execution. The next day, if that player is executed, the execution succeeds but the player remains alive.

                            - The Devil’s Advocate cannot choose the same player two nights in a row, whether or not that player was saved from execution today, and they cannot choose a Zombuul that registers as dead.
                            """,
                    """
                            - At night, the Devil's Advocate protects themself. The next day, the Devil's Advocate is executed but remains alive.

                            - The Devil's Advocate protects the Zombuul. The Zombuul is executed but remains alive, so their life token is not flipped. The next day, the Zombuul is executed again and registers as dead.

                            - The Devil's Advocate protects the Grandmother. The Grandmother is executed but remains alive. Later, the Devil's Advocate protects the Tinker. The Tinker is executed, but the Storyteller kills the Tinker anyway, due to the Tinker ability. Later, the Devil's Advocate protects the Moonchild, who is executed that day—the execution succeeds, but the Moonchild remains alive.
                            """, "The Yogscast"),
            entry(Role.ASSASSIN,
                    "...",
                    """
                            The Assassin kills who the Demon cannot.

                            - Once per game at night, the Assassin can kill a player. This player dies, even if they are protected from death in any way, such as from an ability.

                            - The Assassin ability is affected by drunkenness and poisoning, as normal.

                            - If the Assassin attacks the Goon, the Goon dies and turns evil.
                            """,
                    """
                            - For the first three nights the Assassin wakes, but chooses not to act. During the fourth night, they choose to kill the Fool. Even though the Fool still has their ability, the Fool dies and stays dead.

                            - The Tea Lady neighbours two good players. The Assassin chooses to kill one of the Tea Lady’s neighbours, who dies even though they were protected by the Tea Lady.

                            - The Minstrel is in play. The Mastermind dies by execution. That night, the Assassin chooses to kill the Moonchild, but they do not die, because the Assassin is drunk due to the Minstrel.

                            - The Assassin, who was drunk due to the Courtier, chooses to kill the Goon. The Assassin has no ability, so the Goon remains alive but turns evil.
                            """, "The Yogscast"),
            entry(Role.MASTERMIND,
                    "The tentacles of that monster are nailed to the doors of the church. Mothers and children are dancing in the street. Excellent. Everything is proceeding exactly as I have planned.",
                    """
                            The Mastermind can still win after the Demon is dead.

                            - If the Demon dies by execution, the game continues. The players do not learn that the Demon died. The following day, if a good player is executed—whether or not they die from it—then evil wins. If an evil player is executed or nobody is executed, then the good team wins.

                            - A dead Demon does not get to attack. They lose their ability, as normal. During this extra night and day, other characters’ abilities function as normal.

                            - If the Demon dies and just two players are left alive, the game still continues for another day—evil does not win from two players being alive, and good did not win by killing the Demon. The Mastermind ability says “play for one more day,” and abilities override standard game rules.
                            """,
                    """
                            - The Shabaloth dies. The next day, the Professor is executed and dies. Evil wins.

                            - The Po dies. The next day, the Godfather is executed, but remains alive because they were protected by the Devil's Advocate. However, since an evil player was executed, good wins.

                            - The Zombuul is executed and appears to die. The Mastermind’s ability does not trigger yet, because the Zombuul's execution did not make the game end. When the Zombuul is executed a second time and dies for real, the Mastermind’s ability triggers, and the game continues for one more day.

                            - There are three players alive. The Demon dies. The following day, with just two players alive, good decides not to execute. When night falls, just two players are left alive but the Demon is dead, so good wins.
                            """, "The Yogscast"),
            //S&V
            entry(Role.EVIL_TWIN,
                    "I'm not Sara! I'm Clara! SHE is Sara! Sara is the evil one! Not me!",
                    """
                            The Evil Twin mirrors a good character, so that the players don't know which twin is good and which twin is evil.

                            - The Evil Twin is paired with a good player, chosen by the Storyteller, called the Good Twin.

                            - On the first night, the Evil Twin and Good Twin both wake, make eye contact, and learn each other’s character.

                            - If the Good Twin is executed, evil wins. If the Evil Twin is executed, the game continues. A dead Evil Twin has no ability, so evil doesn’t win if the Good Twin is later executed.

                            - Good cannot win while both twins are alive. Even if the Demon is killed, the game continues. Good will need to kill the Evil Twin as well as the Demon to win.

                            - If a good player is turned into an Evil Twin, they are still a good player, with an evil player becoming their twin. It doesn’t matter which twin is which character, what matters is their alignment—the good team can execute the evil player safely, but if they execute the good player, evil wins.

                            - If both Twins are the same alignment, the Storyteller chooses a new Twin.
                            """,
                    """
                            - Both twins are claiming to be the Oracle. The Evil Twin is executed. The game continues.

                            - The Pit-Hag turns the good Sage, who is also the Good Twin, into the Mutant. Both twins try to convince the group that they are the Mutant. The Storyteller immediately executes the Mutant, who is also the Good Twin. The game ends and evil wins.

                            - The Good Twin and the Evil Twin are both loudly claiming to be the Artist. Both players approach the Storyteller to ask a question in private. The good players, confused, execute the Demon. The game continues, with no death during the night from now on.

                            - The Pit-Hag turns a good player into the Evil Twin, who remains good. The group executes the good-aligned Evil Twin. Evil wins.
                            """, "The Betweenlands Team"),
            entry(Role.WITCH,
                    "Three drops of goat's blood. A lock of hair, torn in anger. The name is spoken, the shadow cast. Walk left foot first down that brambled path, and don't look back.",
                    """
                            The Witch curses players, so that they die if they nominate.

                            - Each night, the Witch chooses a player to curse. That player dies if they nominate any player on the next day, although their nomination still counts.

                            - The Witch’s curse lasts only for one day, but the Witch may curse the same player again and again each night.

                            - As soon as just three players are left alive, the Witch’s curse is immediately removed, and the Witch acts no more.
                            """,
                    """
                            - At night, the Witch curses the Sage. During the next day, the Sage nominates the Dreamer. The Storyteller immediately declares that the player of the Sage dies. The players still vote to execute the Dreamer, who dies too.

                            - The Witch curses themself. During the next day, the Witch nominates the Demon, and dies. The players do not vote to execute the Demon, and nominations continue.

                            - The Witch curses the Klutz. The Fang Gu attacks the Klutz, so the Klutz becomes the Fang Gu. The new Fang Gu is now cursed by the Witch, and they nominate. The new Fang Gu dies, and good wins.

                            - The Witch curses the Savant. Later that night, after the Demon kills a player, only three players are alive, so the curse is removed. The Savant may nominate safely.
                            """, "The Yogscast"),
            entry(Role.CERENOVUS,
                    "Reality is merely an opinion. Specifically, my opinion.",
                    """
                            The Cerenovus encourages players to pretend to be different characters than they actually are.

                            - The Cerenovus chooses Townsfolk or Outsiders that players are mad about being. They must try to convince the group that they actually are this character tomorrow, or else die.

                            - Simply hinting is not enough to avoid death. The player must make a decent effort to convince the group. Mad players are never literally forced to say things they don’t want to—but if the Storyteller doesn’t hear them make an effort, they pay the price.

                            - Mad evil players might be executed this way, but “might” means you can choose not to, to prevent evil from winning by this strategy.

                            - Like the Mutant, an execution penalty counts as the one execution allowed per day.
                            """,
                    """
                            - The Cerenovus makes the Barber mad about being the Savant. Tomorrow, the Barber claims to be the Savant, talks to the Storyteller, and tells the group two facts that they made up. When asked whether they are mad, the Barber says "no" emphatically, so avoids being executed.

                            - The dead Artist is made mad about being the Sage. The next day, they say nothing about being the Sage. The Artist is executed.

                            - The Cerenovus makes the Flowergirl mad about being the Clockmaker. The Flowergirl says to the group that they are the Clockmaker and learned a "2,” but hints privately to other players that they are mad. The Storyteller overhears this and executes the Flowergirl.
                            """, "The Yogscast"),
            entry(Role.PIT_HAG,
                    "Round about the cauldron go; In the poison'd entrails throw; Toad, that under cold stone; Days and nights has thirty-one; Sweated venom sleeping got; Boil thou first in the charmed pot.",
                    """
                            The Pit-Hag changes players into different characters.

                            - Each night, the Pit-Hag chooses a player and a character to turn that player into.

                            - They can’t create duplicate characters. If the character is already in play, nothing happens.
                            """,
                    """
                            - The Pit-Hag turns the Clockmaker into the Mutant.

                            - The Pit-Hag tries to turn the Savant into the Sage, but nothing happens because a Sage is already in play.

                            - The Pit-Hag turns the Flowergirl into the Evil Twin. Now, there is a good Evil Twin, so the Evil Twin and an evil player are woken to learn each other's character.

                            - During the final night, the Pit-Hag turns the Oracle into a good No Dashii. The Storyteller kills the evil Demon only, so that only one Demon is alive during the final day.
                            """, "The Yogscast"),
            //Kickstarter
            entry(Role.BOOMDANDY,
                    "Tick... Tick... Tick... TOCK.",
                    """
                            The Boomdandy explodes when executed, killing most other players.

                            - If the Boomdandy is executed, the Storyteller kills other players, one at a time, until only three are left alive.

                            - The Demon will be one of the remaining three players (otherwise, the game would be over).

                            - Afterward, there is no further nomination or execution today. Instead, the Storyteller counts down from ten and all players point at the player they want to die. When the countdown ends, the Storyteller counts the number of fingers pointed at each player. If it is a tie, the day ends (and evil probably wins due to the Demon killing that night).

                            - Even dead players who have no vote token may point.

                            - Players may change who they are pointing at up until the countdown ends, at which point their decision is final.

                            - The Boomdandy only explodes due to an execution. Deaths by other means, such as via a Golem or a Psychopath, don’t count. If the Boomdandy is executed but doesn’t die (due to a Devil’s Advocate etc.), they still explode.

                            - If a character can’t die, such as the Fool or the Sailor, the Storyteller may rule that four players remain alive after a Boomdandy explosion.
                            """,
                    """
                            Amy is the Boomdandy. She is executed. She explodes, killing all players except for the Po, the Widow, and the Fortune Teller. Frantically, all players start pointing at each other, and talking about who should be pointed at. After counting down, the Storyteller calls “Freeze” and the Widow has the most number of fingers pointed at them, and dies. Evil wins.
                            """, "The Yogscast"),
            entry(Role.FEARMONGER,
                    "Beware of gazing long into the Abyss, lest the Abyss also gaze into you.",
                    """
                            The Fearmonger creates paranoia about who nominates whom.

                            - During the first night, when the Fearmonger selects a player, all players learn this.

                            - During other nights, each time the Fearmonger selects a new player, all players learn this. If the Fearmonger selects the same player as previously, the players learn nothing.

                            - The players only learn that the Fearmonger has acted, not which player was selected.

                            - If the Fearmonger nominates their chosen player, and that nomination results in their execution, the chosen player loses, their team loses, and the game ends.

                            - Only the currently chosen player is susceptible to the Fearmonger’s ability. Previously chosen players don’t count.

                            - If the chosen player is executed but does not die, the chosen player’s team still loses.
                            """,
                    """
                            - On the first night, the Fearmonger chooses the Butler. All players learn the Fearmonger has chosen a new player. During the day, the Fearmonger nominates the Butler, and the Butler is executed. Evil wins.

                            - At night, the Fearmonger chooses the Juggler. The Flowergirl nominates the Juggler, and the Juggler is executed. The game continues.

                            - The Fearmonger chooses the Empath. The next night, the Fearmonger chooses the Soldier. The Fearmonger nominates and executes the Empath. The game continues because the Fearmonger has selected the Soldier, not the Empath.

                            - The Fearmonger accidentally chooses the Baron, due to the Poppy Grower being in play. The Fearmonger nominates and executes the Baron. Good wins.
                            """, "The Yogscast"),
            entry(Role.MARIONETTE,
                    "Words, words. They're all we have to go on.",
                    """
                            The Marionette doesn't know that they are a Minion.

                            - The Marionette draws either a Townsfolk or an Outsider token from the bag, but is secretly the Marionette.

                            - The Marionette neighbors the Demon. There are no players sitting in between the Marionette and the Demon.

                            - The Demon knows which player is the Marionette.

                            - On the first night, the Marionette does not wake to learn the other evil players, and the other Minions do not learn the Marionette.

                            - The good ability that the Marionette thinks they have doesn’t work, but the Storyteller pretends it does. It is just as if this player is the Drunk.

                            - The Marionette registers as evil, and as a Minion.
                            """,
                    """
                            - Marianna is the Marionette, but thinks she is the Undertaker. She wakes each night to learn who was executed that day, but her information is often wrong. Halfway through the game, the Demon tells her that she is the Marionette.

                            - Lachlan is the Demon. He tells Sarah that she is the Marionette. Lachlan is lying. There is no Marionette.

                            - The Demon tells Ben that he is the Marionette. Ben thinks he is the Fortune Teller, but he isn’t. Ben doesn’t believe the Demon, and executes them. Good wins.
                            """, "The Yogscast"),
            entry(Role.MEZEPHELES,
                    "That which issues from the heart alone, will bend the hearts of others to your own.",
                    """
                            The Mezepheles offers good players a choice: to turn evil or not.

                            - On the first night, the Mezepheles learns a secret word from the Storyteller.

                            - If a good player says this word, either publicly or privately, they turn evil that night. The Storyteller needs to hear this player actually say the word before turning them evil.

                            - The Mezepheles does not learn if a player turns evil. The good player learns if they turn evil, but not until that night.

                            - If the Mezepheles is sober and healthy at night, the good player turns evil even if the Mezepheles was drunk or poisoned when the good player spoke the secret word. If the Mezepheles is drunk or poisoned at night when a player would turn evil, the player stays good—the Mezepheles has “used their ability” and may not turn a player evil later on.
                            """,
                    """
                            - The Mezepheles tells the Barber that the secret word is “Rumplestiltskin”. The Barber publicly says “This reminds me of the fairy tale where the Miller’s daughter has to guess Rumplestiltskin’s name”. The Barber turns evil that night.

                            - The Mezepheles tells the Mayor the secret word. The Mayor, wanting to stay good, tells the group who the Mezepheles is.

                            - The Mezepheles privately tells the Noble the secret word – “Constantinople”. The Noble visits the Storyteller and says “Constantinople” in private. That night, the Courtier makes the Mezepheles drunk. The Noble stays good.
                            """, "fish0016054 (Fish's Undead Rising) + edits by Autumn Wind"),
            entry(Role.PSYCHOPATH,
                    "Surprise!",
                    """
                            The Psychopath kills in broad daylight.

                            - During the day, if the Psychopath declares that they are the Psychopath and publicly chooses a player, that player dies. This can only be done once per day, and only before the Storyteller has called for nominations.

                            - The Psychopath does not need to use this ability if they don’t want to.

                            - The Psychopath can be nominated and voted for normally. If the Psychopath is executed, they might not die. They play Roshambo (Paper-Rock-Scissors) with the player that nominated them. The nominator needs to win for the Psychopath to die. Drawing or losing means the Psychopath lives.

                            - If the Psychopath is executed, this still counts as the one execution for the day. No more players may be nominated or executed today.

                            - If the Psychopath dies by other means, such as the Demon attacking them, they do not play Roshambo. They die.
                            """,
                    """
                            - The Psychopath chooses to kill the Sailor. The Sailor is sober, so does not die. The Psychopath may not use their ability again today.

                            - The Psychopath has been nominated by the Barber, and is executed. In Roshambo, the Barber has rock and the Psychopath has rock, so the Psychopath lives. The next day, the Saint nominates and executes the Psychopath. The Saint has paper and the Psychopath has scissors, so the Psychopath lives. The next day, the Barber nominates and executes the Psychopath again. The Barber has rock and the Psychopath has scissors, so the Psychopath dies.
                            """, "The Yogscast"),
            //Carousel
            entry(Role.BOFFIN,
                    "Stellar hydrogen, vast, inert; carbon, oxygen, neon gases, all ruined. Molecular chaos, entropy, yields new cosmic phenomena, rebirth from atomic chaos, dense matter collapsing. All in a teeny little bottle.",
                    """
                            The Boffin replicates a good ability.

                            - While the Boffin is alive, the Demon has a single Townsfolk ability or Outsider ability.

                            - If the Demon is drunk or poisoned, the Demon keeps this good ability. If the Boffin is drunk or poisoned, the Demon temporarily loses this good ability.

                            - If the Demon dies and has an ability that functions while dead, such as the Sweetheart, the Demon keeps this ability.

                            - If a new Demon is created, such as via a Scarlet Woman or a Barber, this new Demon has an ability from the Boffin. This ability may be different to the previous Demon's ability.

                            - If there are multiple Demons alive, only one alive Demon has an ability from the Boffin.

                            - If the Demon has an ability that modifies the setup, such as a Choirboy, these changes are made during setup, as normal.

                            - Both the Demon and the Boffin learn which good ability the Demon has. The Storyteller may wake these players independently, or together.

                            - The not-in-play character may be 1 of the Demon's 3 bluffs.

                            - The Demon also wakes at night at the time that the good character would normally wake.
                            """,
                    """
                            - The Imp has the Virgin ability. The Alsaahir nominates the Imp, and is immediately executed.

                            - The Lord of Typhon has the Chambermaid ability. Each night, the Lord of Typhon wakes, chooses two players, and learns how many woke tonight. On the 4th night, the Boffin is drunk, so the Demon has no Chambermaid ability, so does not wake.

                            - The Kazali has the Banshee ability. The Kazali dies at night, and the Scarlet Woman becomes the Kazali. The dead Kazali may nominate and vote twice per day.
                            """, "The Yogscast"),
            entry(Role.GOBLIN,
                    "You don’t want to insult the goblins. You really, really don’t. On a completely different note… can I have another piece of cake?",
                    """
                            The Goblin takes revenge if the town knowingly executes them.

                            - If the Goblin is executed, evil wins.

                            - ...but for this to happen the Goblin needs to tell the group that they are the Goblin when they are nominated, but before votes happen, and to do so in a way that everyone hears. The good players need to know the risk.

                            - If the Goblin is executed without telling the group that they are the Goblin when nominated, the Goblin dies and the game continues as normal.

                            - The Goblin must have claimed to be the Goblin today for their ability to work. Telling the group yesterday, or even every previous day, doesn't count.

                            - Any player may claim to be the Goblin when nominated.
                            """,
                    """
                            - Abdallah is the Goblin. Alex nominates Abdallah, and Abdallah claims to be the Goblin. Votes are counted, and Abdallah is about to die. Other nominations occur later today, but Abdallah has the most votes and is executed. Evil wins.

                            - Lewis is the Artist, and claims to be the Goblin when nominated. He is executed, and the game continues.

                            - Doug is the Goblin. He claimed to be the Goblin yesterday and the day before, but not today. He is executed. The game continues.
                            """, "The Yogscast"),
            entry(Role.HARPY,
                    "So fair a day I never did see, nor so fowl a presence hanging over me.",
                    """
                            The Harpy creates discord and distrust between good players.

                            - At night, the Harpy player chooses one player at a time, not two at once.

                            - A player chosen by the Harpy is affected by the ability until the next Harpy choice.

                            - If the Storyteller decides to kill players with the Harpy ability, they do not need to kill both. The Storyteller can decide to kill only one, or none.

                            - The Harpy can choose a dead player. If so, the Storyteller can kill just the living player, since dead players can not die again.

                            - The order of deaths due to the Harpy ability can be chosen by the Storyteller, should that be important.
                            """,
                    """
                            - The Harpy chooses the Monk and the Engineer. The Monk claims to be the Investigator who saw the Engineer and campaigns for them to be executed. When challenged, they are emphatic in their claims that the Engineer is most likely evil due to their information, and so avoid death.

                            - The Harpy chooses the Oracle and the dead Alchemist. The Oracle claims that they trust the Alchemist because their Oracle information indicates that they were not evil. The Storyteller declares that the Oracle dies.

                            - The Farmer is chosen by the Harpy. As they don't have any information themselves to claim in order to imply that the other player is evil, they make a concerted effort to find information that might clear each of the other living people, leaving their target as the remaining Demon candidate and therefore evil by implication.
                            """, "The Yogscast"),
            entry(Role.ORGAN_GRINDER,
                    "Round and round the handles go. The more you dance the less you know.",
                    """
                            The Organ Grinder makes voting secret.

                            - When a player is nominated, players vote with eyes closed.

                            - The Storyteller does not count the votes out loud, and does not reveal how many players voted once voting is complete.

                            - The Storyteller doesn’t reveal which player is “about to die”.

                            - After nominations have closed, the Storyteller reveals which player is executed, as normal.

                            - Dead players may vote once if they have a vote token. Their vote token is removed at the end of the day instead of after the vote.

                            - If the Organ Grinder is drunk, the vote happens with eyes open, as normal. The Storyteller makes no comment as to whether the Organ Grinder is dead or alive. That night, the Organ Grinder chooses to become sober or drunk again.
                            """,
                    """
                            There are 8 players alive. The Noble is nominated. All players close eyes to vote and the Noble gets 5 votes. The Imp is nominated. All players close eyes to vote and the Imp gets 7 votes. The Pixie is nominated. All players close eyes to vote and the Pixie gets 4 votes. After nominations close, the Storyteller declares that Doug (the Imp) is executed and dies, and that good has won.
                            """, "starzz4every1 (Pinterest) + fez by Autumn Wind"),
            entry(Role.SUMMONER,
                    "Hail the guardians of the north; by my intellect, thou art cut. Hail the guardians of the east; by my will, thou art dominated. Hail the guardians of the south; by that which lies beyond, the mystery is revealed. Hail the guardians of the west; a shield in the darkness.",
                    """
                            The Summoner creates a Demon.

                            - The Summoner may choose any player to become the Demon, even themselves.

                            - The new Demon does not learn which players are Minions, or vice versa. The evil players will need to talk amongst themselves to figure this out.

                            - Even though there is no Demon in play for two days, the game does not end. However, if the Summoner becomes unable to create a Demon (due to dying, becoming drunk on night 3 etc.) good wins.

                            - The newly created Demon acts on the same night that it is created.
                            """,
                    """
                            - On the third night, the Summoner chooses the Snitch player, and the Lleech. The Snitch becomes the evil Lleech, and chooses a player to poison, and a player to kill.

                            - On the first day, the Summoner is executed. Good wins.

                            - On the third night, the Summoner turns the Alchemist into the Leviathan. At dawn, all players learn that Leviathan is in play, and that it is day three of five.
                            """, "The Yogscast"),
            entry(Role.VIZIER,
                    "An excellent decision, as always, sire. Such a petty crime as bumping into the Bishop indeed deserves your ‘justice’ and ‘mercy’. Take a stroll in the gardens. Visit the gallery and peruse the sculptures of Von Strauf. Relax, sire. Leave everything… to me.",
                    """
                            The Vizier executes players without the town’s consent.

                            - On the first day, all players learn that the Vizier is in play, and which player it is.

                            - During the day, the Vizier can not die by any means.

                            - After a vote is tallied, if the Vizier chooses to execute the nominee (and at least one good player voted), they are executed immediately. This counts as the 1 execution allowed each day.

                            - After a vote is tallied, if the Vizier chooses to execute the nominee (and no good players voted), nothing happens.

                            - Even if the vote tally is less than 50% of the living players, the Vizier may still execute. Even if another player has more votes than the current player, the Vizier may still execute.

                            - The Vizier does not have to force an execution each day.
                            """,
                    """
                            - The King has been nominated. Five people vote, but the Vizier does not use their ability. The Boomdandy is nominated and eight people vote. The Vizier uses their ability and the Boomdandy is executed immediately.

                            - The Demon has seven votes against them, and is “about to die”. The Vizier nominates Bill, the Barber. Two evil players and one good player vote. The Vizier declares that Bill is executed. The Demon survives today.

                            - The town nominates and executes the Vizier. The Vizier does not die. That night, The Demon kills the Vizier.
                            """, "The Yogscast"),
            entry(Role.WIDOW,
                    "More wine? Château d’Ergot ’07 is a very special vintage. My yes, very special indeed.",
                    """
                            The Widow sees the Grimoire and poisons a character of their choice.

                            - The Widow acts on their first night only, poisoning one player.

                            - The player that the Widow poisons is poisoned until the Widow dies.

                            - On the same night that the Widow acts, one good player learns that the Widow is in play, but not which player is the Widow, and not which player is poisoned.
                            """,
                    """
                            - The Widow sees the Grimoire and points to the Sailor. The Sailor is poisoned this game. The Sailor is sober, but dies when executed.

                            - On the third night, the Pit-Hag turns themselves into the Widow. That night, the Recluse learns that a Widow is in play.

                            - The Empath is poisoned due to the Widow. The Widow becomes drunk due to the Innkeeper. The Empath is no longer poisoned. The Innkeeper dies. The Widow is now sober and the Empath is poisoned again.
                            """, "Me! (Mod Creator)"),
            entry(Role.WIZARD,
                    "Every man and every woman is a star. Love is the law, love under will.",
                    """
                            The Wizard makes a wish.

                            - This wish is limited only by the player’s imagination. It can be anything at all.

                            - If the Storyteller tells the group that the Wizard has made a wish, they need not do so immediately, and can do so at any point later on.

                            - Many wishes have a price. The price changes the game in some way, or changes the wish in some way. It can be anything at all, and is decided by the Storyteller. The Storyteller may or may not tell the Wizard what the price is. The purpose of the price is to rebalance a wish that is unfair for the good team on a mechanical level.

                            - Many wishes leave a clue. The clue can be anything at all, is decided by the Storyteller, and is declared publicly. The purpose of a clue is to rebalance a wish that is unfair to the good team on an informational level.

                            - When the Wizard dies, the wish may or may not still be in effect, depending on the nature of the wish and the nature of the price.

                            - If the Wizard makes a wish that the Storyteller doesn’t understand, or feels like it would be impossible to implement, the Storyteller may ask the Wizard to wish again, or cancel the wish.
                            """,
                    """
                            - The Wizard wishes to see the Grimoire. The Storyteller grants this wish, and there is no price and no clue.

                            - The Wizard wishes that all good players are drunk. The Storyteller grants the wish. Later, they declare that the Wizard has made a wish and that “Things are wrong” but provide no further context. For the rest of the game, the Storyteller makes all information false.

                            - The Wizard wishes that they become a Demon. The Storyteller grants the wish. Later, they declare that the Wizard has made a wish, and that “The student has become the master.” The Storyteller kills the Lord of Typhon and turns the Wizard into the Ojo.

                            - The Wizard wishes to win the game. The Storyteller grants the wish, and tells the Wizard that the evil team will win the game at the end of the day. The Storyteller declares that the Wizard has made a wish, and that either Ben, Amy, or Lewis is the Demon. The group executes Ben, who is the Demon, and good wins.

                            - The Wizard wishes that all players have 5 lives, and all reminder tokens for the script are added to characters. The Storyteller judges that this wish is too awkward, confusing and boring, declines the wish, and asks the Wizard to wish again.
                            """, "The Yogscast"),
            entry(Role.WRAITH,
                    "Ra'āb ina pān ṣilli ša dāri. Rigim qallu ina šūri, šītu ša šunātīka iredde, u napšutka idlul ina pān maṣṣartī dāriti.",
                    """
                            The Wraith knows and shares what happens at night.

                            - The Wraith may visit any other player at any point during the night, for as long or as short a time as they wish.

                            - They may try to remain hidden or make their presence clear.

                            - The Storyteller wakes the Wraith when other evil players also wake, such as when the Demon kills a player, an evil Townsfolk uses their ability, or a Cult Leader learns that they are evil.

                            - When several players are woken together, they may communicate if they wish.

                            - If a good player catches the Wraith visiting them, there is no mechanical effect.

                            - A dead Wraith may not visit other players at night. A drunk or poisoned Wraith is told by the Storyteller that they may not visit other players that night.
                            """,
                    """
                            During the first night the Wraith visits other players, and notices which good players wake and which do not. During the second night, the Wraith enters one of their houses and hears that Doug was woken and chose two players tonight. During the 3rd night, when both the Wraith and the Demon wake together, the Wraith tells the Demon to kill Doug, whom the Wraith believes is the Fortune Teller.
                            """, "imonk (itch.io) + edits by Autumn Wind"),
            entry(Role.XAAN,
                    "Down they fall. One by one. By two, by three, by five.",
                    """
                            The Xaan poisons all Townsfolk.

                            - The Xaan poisons all Townsfolk players for one night then one day. The night that this happens equals the number of Outsiders in play. For example, if there are 2 Outsiders, the Xaan poisons on night 2.

                            - There can be any number of Outsiders in play, but usually 1 to 4. This can be the normal number of Outsiders if the Xaan was not in play, or something different. This overrides other characters that add or remove Outsiders, such as the Baron.

                            - If the number of Outsiders changes during the game, the Xaan poisons on the night corresponding to the number of Outsiders during setup.

                            - The Xaan needs to be alive in order to poison.
                            """,
                    """
                            - There are 3 Outsiders in play, due to the Xaan. On night 3, the Exorcist chooses the Demon but nothing happens, the Acrobat chooses the Drunk but nothing happens, and the Seamstress gets false information.

                            - There is 1 Outsider in play. It is an 11 player game. On the first night, the Xaan poisons all 7 Townsfolk. On the second night, the Pit-Hag creates a Hatter. Even though there are 2 Outsiders in play, all players are healthy tonight.

                            - There are no Outsiders in play. The Xaan never poisons anyone. The Xaan bluffs as the Zealot, and the good team believes that all Townsfolk are poisoned on night 1, but they are not.
                            """, "Autumn Wind"),

            // --- DEMONS ---

            //TB
            entry(Role.IMP,
                    "We must keep our wits sharp and our sword sharper. Evil walks among us, and will stop at nothing to destroy us good, simple folk, bringing our fine town to ruin. Trust no-one. But, if you must trust someone, trust me.",
                    """
                            The Imp kills a player each night, and can make copies of itself... for a terrible price.

                            - On each night except the first, the Imp chooses a player to kill. Because most characters act after the Demon, that player will probably not get to use their ability tonight.

                            - The Imp, because they're a Demon, knows which players are their Minions, and knows three not-in-play good characters that they can safely bluff as.

                            - If the Imp dies, the game ends and good wins. However, if the Imp kills themself at night, they die and an alive Minion becomes an Imp. This new Imp does not act that same night, but is now the Imp in every other way—they kill each night, and lose if they die.
                            """,
                    """
                            - It is the first night. The Imp learns that Evin and Sarah are the Minions. The Imp also learns that the Monk, Chef, and Librarian are not in play. The Imp bluffs as the Chef, then bluffs as Mayor halfway through the game. Eventually, the Imp is executed and good wins.

                            - During the night, the Imp wakes and chooses a player, who dies. The next night, the Imp chooses themselves to die. The Imp dies, and the Poisoner becomes the Imp.
                            """, "The Yogscast"),

            //BMR

            entry(Role.PUKKA,
                    "You truly have been kind welcoming me into your beautiful home. I am so sorry I accidentally scratched you. A little thing. No matter. But please, take this golden toothpick as a humble token of my regret.",
                    """
                            The Pukka poisons its victims, who die at a later time.

                            - When the Pukka attacks, their victim is poisoned immediately. The next night, just after the Pukka attacks again, that player dies.

                            - Unlike other Demons, the Pukka acts during the first night.

                            - The Exorcist prevents the Pukka from waking to poison a player. The Innkeeper prevents the Pukka from killing a poisoned player, then that player is no longer poisoned.

                            - If the Pukka is drunk and chooses a player, that player does not become poisoned, so does not die the following night.

                            - If the Pukka was sober when they chose a player the previous night, but is drunk at night, that player does not die. But when the Pukka sobers up, the poison resumes and kills the player at night.
                            """,
                    """
                            - The Pukka poisons the Chambermaid. The Chambermaid gets false information. The next night, the Chambermaid dies.

                            - The Pukka poisons the Fool. The next day, the Fool is executed and dies because they have no ability. The next night, nobody dies and the Pukka poisons the Gossip. The next night, the Pukka is drunk and tries to poison the Tinker, but does not. The next night, the Gossip dies because the Pukka is sober.

                            - The Pukka poisons the Pacifist. The next night, the Exorcist chooses the Pukka to not wake tonight. The Pacifist dies, but the Pukka does not wake to attack tonight.

                            - The Moonchild is executed, dies, and chooses the Courtier. That night, the Pukka chooses the Moonchild. The Courtier does not die because the Moonchild is poisoned.
                            """, "The Yogscast"),
            entry(Role.SHABALOTH,
                    "Blarg f'taag nm mataan! No sho gumtha m'sik na yuuu. Fluuuuuuuuurg h-sikkkh.",
                    """
                            The Shabaloth eats two players per night, but may vomit one of them back up the following night.

                            - Unlike most Demons, the Shabaloth attacks twice per night. The night after the attack, the Storyteller may decide that one of the players attacked by the Shabaloth comes back to life.

                            - This can be an alive player that was killed, or a dead player that was attacked.

                            - The regurgitated player regains their ability, even a “once per game” ability already used. If they had a “first night only” or “start knowing” ability, they may use it again.
                            """,
                    """
                            - The Shabaloth attacks the Gossip, then the Gambler. The Gossip dies, but the Gambler, who was protected by the Innkeeper, remains alive.

                            - The Shabaloth attacks the alive Courtier and the dead Exorcist. The Courtier dies. The next night, the Storyteller decides that the Exorcist becomes alive again. The Exorcist doesn't act tonight--they normally act before the Shabaloth.

                            - The Shabaloth attacks the Tea Lady’s neighbour, then the Tea Lady. The Tea Lady’s neighbour, who is protected by the Tea Lady, doesn’t die, but then the Tea Lady dies.
                            """, "The Yogscast"),
            entry(Role.PO,
                    "Would you like a flower? I'm so lonely.",
                    """
                            The Po can choose to attack nobody at night, but goes on a rampage the following night.

                            - The Po attacks one player per night, like many other Demons. However, if the Po chooses to attack nobody, then they may attack three players the following night.

                            - If the Po was drunk or poisoned when they chose nobody last night, they still choose three players tonight.

                            - A Po must choose three players when prompted to do so. They cannot choose no one again.

                            - The Po only gets three attacks if they chose nobody. The Po does not get three attacks if they chose to attack someone the previous night, but that player did not die.

                            - The Po doesn’t act on the first night, but this night does not count as a night where the Po “chose no one.”

                            - If the Exorcist selects the Po, the Po does not act, but this night does not count as a night where the Po “chose no one.” However, if the Po chose no one the night before the Exorcist chose the Po, the Po chooses three players the night after the Exorcist chose the Po, because their last choice was no one.
                            """,
                    """
                            - On the second night, the Po attacks one player. On the third night, the Po chooses to attack nobody. On the fourth night, the Po attacks three players.

                            - The Po chooses to attack nobody, but is drunk. The next night, the Po is poisoned. They choose three players, but none of them die. The following night, the Po is sober and healthy and attacks a player, who dies.

                            - The Po attacks the Moonchild, then the Goon, then the Grandmother. Only the Moonchild dies, because the Po became drunk when they attacked the Goon.
                            """, "Autumn Wind"),
            entry(Role.ZOMBUUL,
                    "I do not. Understand. Your ways. Fellow human. Show me. The dirt. Where the holy. Lay. Sleeping. I too. Must sleep. Soon.",
                    """
                            The Zombuul secretly remains alive while in the grave.

                            - When the Zombuul would die for any reason, they actually don’t die, but the Storyteller acts as if they died. The second time the Zombuul dies, they die for real and good wins.

                            - The seemingly dead Zombuul counts as a dead player in almost every way. The player’s life token on the Town Square flips to indicate their death. The next time they vote, they lose their vote token. They cannot nominate, they may vote with the Voudon, they’re not an alive neighbor for the Tea Lady, and so on. The only differences are that the game continues, the Zombuul still attacks, and the game continues if just two other players are alive.

                            - The Zombuul only wakes at night to attack if nobody died that day. If a dead player is executed, the player can’t die again, so the Zombuul would still wake.

                            - If a drunk or poisoned Zombuul dies, good wins. If a “dead” Zombuul becomes drunk or poisoned, do not announce that the player is alive.
                            """,
                    """
                            - The Zombuul is executed and appears to die. They cannot attack tonight. A few days later, only two players appear alive on the Town Square. The good team is fairly certain that one of the dead players is the Zombuul, and the game continues until one more player dies.

                            - Nobody died today. That night, the Zombuul attacks. The next day, the Tinker dies. That night, the Zombuul does not wake.
                            """, "The Yogscast"),
            //S&V
            entry(Role.FANG_GU,
                    "Your walls and your weapons are but smoke in dreams.",
                    """
                            The Fang Gu possesses Outsiders.

                            - The first time a Fang Gu attacks and kills an Outsider, the Fang Gu dies, and the Outsider becomes a Fang Gu and turns evil.

                            - This can only happen once per game. If the new Fang Gu attacks an Outsider, the Outsider dies as normal.

                            - The new Fang Gu counts as the Demon, and good wins if they die. They do not learn which players are Minions.

                            - There is an extra Outsider in play.

                            - If the Fang Gu attacks an Outsider but that Outsider does not die, that Outsider does not become an evil Fang Gu and the Fang Gu does not die.
                            """,
                    """
                            The Fang Gu attacks the Artist, who dies. The next night, the Fang Gu attacks the Sweetheart, who becomes the Fang Gu while the old Fang Gu dies. The Sweetheart does not make a player drunk, because they did not die. The next night, the new Fang Gu attacks the Klutz, who dies.
                            """, "The Yogscast"),
            entry(Role.VIGORMORTIS,
                    "All doors are one door. All keys are one key. All cups are one cup, but whosoever drinketh of the water that I give shall never thirst, but the water shall be in him a well springing up into everlasting life.",
                    """
                            The Vigormortis kills their own Minions, but those Minions keep their ability.

                            - Every time the Vigormortis kills a Minion, they die but keep their ability for as long as the Vigormortis remains alive. The Witch, Cerenovus, and Pit-Hag still act each night.

                            - Somewhat like the No Dashii, the dead Minion’s closest clockwise or closest counterclockwise Townsfolk becomes poisoned, even if they are dead. If the Vigormortis dies or otherwise loses their ability, then those players become healthy again. One Townsfolk per Minion will always be poisoned this way, as neighboring Outsiders, Minions, or Demons are skipped. The Storyteller chooses which of the two Townsfolk is poisoned.

                            - All Minions killed by the Vigormortis keep their ability and poison a Townsfolk, not just the most recent.

                            - If a dead Minion becomes a non-Minion character, they no longer poison a Townsfolk and have no ability. If a dead Minion becomes drunk or poisoned, they lose their ability until they become sober and healthy again.
                            """,
                    """
                            - The Vigormortis kills the Witch. The player that the Witch cursed tonight remains cursed. The next day, when the cursed player nominates, they die.

                            - The Vigormortis kills the Evil Twin. The Evil Twin neighbours a Klutz and a Flowergirl. The Sage is the next neighbour to the Klutz. The Storyteller chooses that the Sage is poisoned.

                            - The Vigormortis kills the Pit-Hag. The Pit-Hag changes a Savant into a Witch. The Vigormortis kills the Witch, who curses and kills a player. The Pit-Hag turns the dead Witch into the Oracle, who now has no ability. The Pit-Hag turns the Vigormortis into a Vortox. The Pit-Hag now has no ability.
                            """, "The Yogscast"),
            entry(Role.NO_DASHII,
                    "By the sins of Arnoch, I feel thy laden stench. By the curs-ed sun and her foul legion of tiny grinning gods, I corrupt thee. By the blessed night and the hidden depths of the horrid and unholy sea, I end thy squalid life upon this plane.",
                    """
                            The No Dashii poisons their neighboring Townsfolk.

                            - The No Dashii’s closest clockwise and counterclockwise Townsfolk neighbors are poisoned, regardless of whether they are alive or dead. If a No Dashii dies or otherwise loses their ability, then those two players become healthy. Two Townsfolk players will always be poisoned this way, as neighboring Outsiders, Minions, or Demons are skipped.

                            - If a new player becomes the No Dashii, or a poisoned Townsfolk changes into a non-Townsfolk character, the players who are poisoned may change immediately based on who the neighbors of the No Dashii are.
                            """,
                    """
                            - At the start of the game, the No Dashii neighbors a Town Crier and a Snake Charmer. They are both poisoned. A few days later, they are both dead, and the closest alive neighbors to the No Dashii are an unpoisoned Clockmaker and an unpoisoned Barber.

                            - Clockwise from the No Dashii sits a Philosopher, a Mathematician, then a Sage. Anticlockwise from the No Dashii sits a Witch, a Mutant, then a Seamstress. The Philosopher and the Seamstress are poisoned.
                            """, "The Yogscast"),
            entry(Role.VORTOX,
                    "Black is White. Right is Wrong. Left is Right. Up is Long. Down is Sight. Short is Blind. Follow me. Answers find.",
                    """
                            The Vortox makes all information false.

                            - Anytime a Townsfolk player gets information from their ability, they get false information. Even if they are drunk or poisoned, it must be false.

                            - The Vortox does not affect information gained by other means, such as when the Storyteller explains the rules, or when a player’s character or alignment changes.

                            - When night falls, if nobody was executed today, evil wins.
                            """,
                    """
                            - The Vortox kills the Sage. The Sage learns two players, both of which are not Demons.

                            - Nobody voted or nominated today, but the Mutant is executed. That night, both the Flowergirl and the Town Crier learn a "yes.”

                            - The Savant is in play, and learns two pieces of information each day. Both are false. That night, the Dreamer chooses a player who is the Savant, and learns that player is either the Philosopher or the No Dashii.

                            - The Pit-Hag turns the Juggler into the Witch. The Juggler learns that they are now the good Witch, because this information comes from the Pit-Hag's ability, not a Townsfolk's ability.

                            - Today, a player died from the Witch and 5 nominations happened, but nobody was executed. Evil wins.
                            """, "The Yogscast"),
            //Kickstarter
            entry(Role.AL_HADIKHIA,
                    "Alsukut min dhahab.",
                    """
                            The Al-Hadikhia puts three players in a dilemma — who will choose to die, so that others can live?

                            - The Al-Hadikhia may choose three players per night. Everyone learns which three were chosen. Each player makes their choice before the next player is revealed.

                            - All players must be silent when the Al-Hadikhia acts at night. This period lasts from when the Storyteller first declares that a player has been chosen, until the Storyteller says that it ends.

                            - If the Al-Hadikhia chooses no one, no announcement is made and nobody dies to the Al-Hadikhia tonight.

                            - At night, the Storyteller asks players out loud if they choose to live. If they nod their head, they live. If they shake their head, they die. Players may be brought back to life this way.

                            - If all players choose to live, then they all die instead. If a player chose to die but did not die, they count as alive for this calculation.
                            """,
                    """
                            - The Al-Hadikhia chooses Evin, Lachlan, and Sarah. Evin chooses to die. Lachlan chooses to die. Sarah chooses to live. In the morning, Evin and Lachlan are dead, and Sarah is alive.

                            - The Al-Hadikhia chooses Alex, Lewis, and Doug, who is dead. Alex chooses life. Lewis chooses life. Doug chooses life, so is now alive. Since all players are now alive, all three players die.
                            """, "The Yogscast"),
            entry(Role.LEGION,
                    "We are the chill wind on a winter’s day. We are the shadow in the moonless night. We are the poison in your tea and the whisper in your ear. We are everywhere.",
                    """
                            Legion is many Demons.

                            - The recommended number of good and evil players is the reverse of the normal. For example, for a ten player game, there are roughly seven Legion and three good players.

                            - The players that are not Legion may be Townsfolk or Outsiders, in any combination.

                            - If at least one good player voted for the nomination, and that player is “about to die”, then the execution happens as normal. If only evil players vote for a nomination, the vote tally for that nominee is zero.

                            - Each Legion registers as a Minion as well as a Demon.

                            - The Storyteller chooses which player dies at night.

                            - If only one good player remains alive, the Storyteller may declare that evil wins, since good cannot win.

                            - The Storyteller can decide not to give Legion players bluffs.
                            """,
                    """
                            - The only good players are the Fortune Teller and the Slayer. 6 Legion and the Slayer vote to execute the Fortune Teller. They are executed, and evil wins.

                            - 4 players are alive. 3 Legion and no good players vote to execute Julian. Julian is not executed. Alex, who has 2 votes, 1 of which is a good player, is executed instead. Evil wins.
                            """, "The Yogscast"),
            entry(Role.LEVIATHAN,
                    "To the last, I grapple with thee. From Hell’s heart, I stab at thee. For hate’s sake, I spit my last breath at thee.",
                    """
                            The Leviathan doesn't kill.

                            - All players know the Leviathan is in play, even if the Leviathan is created mid-game.

                            - Any number of evil players may be executed, but if more than one good player is executed, evil wins. It doesn’t matter which characters were executed, only the alignment of the player at the time they were executed.

                            - When the fifth day ends and night begins, if the Leviathan is still alive, evil wins.

                            - All types of execution count, even if the player doesn’t die. A player executed due to the Virgin, or due to revealing that they are the Mutant, is still executed. An executed player who lives due to the Pacifist is still executed.
                            """,
                    """
                            - On the first day, the Monk nominates the Virgin, and is executed. On the second day, the Courtier is executed. Evil wins.

                            - On the second day, the Scarlet Woman is executed. On the third day, the Poisoner is executed. On the fifth day, the Soldier is executed. Evil wins.
                            """, "The Yogscast"),
            entry(Role.LLEECH,
                    "Tasty, tasty, tasty, tasty, tasty, tasty, tasty, tasty brai- I mean pie! Yes. Tasty pie. That’s what I meant to say.",
                    """
                            The Lleech lives if their host lives, and dies if their host dies.

                            - On the first night, the Lleech chooses a player, who is poisoned for the rest of the game.

                            - If this player is alive, the Lleech cannot die. If the Lleech is executed, the Storyteller tells the group that the player lives, but not why.

                            - If the player that the Lleech chose dies, the Lleech dies as well. If this means that only one or two players are left alive, good still wins, because the Demon is dead.

                            - From the second night onwards, players that the Lleech attacks die but are not poisoned.

                            - If a Lleech is created mid-game, they poison a player that night. They must choose an alive player.
                            """,
                    """
                            - The Lleech poisons the Noble. The Noble learns false information. The Lleech is executed, but does not die. The next day, the Noble is executed. The Noble and the Lleech die. Good wins.

                            - The Lleech poisons the Farmer. The Lleech is made drunk by the Courtier. The poisoned Farmer dies, and the game continues because the Lleech is also drunk. The drunk Lleech is executed and dies, and good wins.
                            """, "The Yogscast"),
            entry(Role.RIOT,
                    "Larga vida a la revolución! Mi revolucion!",
                    """
                            Riot kills everybody in a panic.

                            - On the 3rd day, each player that is nominated dies immediately. Even though they are dead, they nominate again.

                            - The player that was nominated must nominate again immediately or lose their chance to do so. The Storyteller counts down “3... 2... 1...” to let the player know how long they have to nominate, should they wish to. If they don’t, the Storyteller nominates instead.

                            - The good team wins if all Riot players are dead. If the last Riot dies and only two players are alive, they do not nominate, and the good team wins.

                            - If nobody nominates on the 3rd day, the Storyteller makes the 1st nomination instead.

                            - Minions may change into Riot as the nomination phase begins on the 3rd day.
                            """,
                    """
                            Alex nominates Lewis. Lewis dies and nominates Ben. Ben dies and nominates Marianna. Marianna dies and nominates Lachlan. Lachlan dies. All Riot players are dead. Good wins.
                            """, "The Yogscast"),
            //Carousel
            entry(Role.KAZALI,
                    "Gon(z)a7les6. Take cau8tun. The mech4an4ion is iNvert10d. E99ors insy6tum. Reco{7}fig.",
                    """
                            The Kazali chooses their own Minions.

                            - If a Kazali is created mid game, the Kazali does not choose new Minion players.

                            - The Storyteller can give the Minions’ original good characters as bluffs to the Demon, since they are not in play.

                            - The Kazali acts at a time that is technically both during setup and during the first night.

                            - The Storyteller may keep the Kazali awake, or put the Kazali to sleep, when waking the Minions to tell them which Minion that they are.

                            - Only Minions that are on the script may be chosen. Duplicate Minion characters are not allowed.
                            """,
                    """
                            There are 15 players and no Minions in play yet. The Kazali wakes and chooses that Doug becomes the evil Organ Grinder, that Amy becomes the evil Vizier, and that Lewis becomes the evil Goblin.
                            """, "The Yogscast"),
            entry(Role.LIL_MONSTA,
                    """
                            Step 1: Be cute.
                            Step 2: World domination.
                            Step 3: Bweakfast.""",
                    """
                            Lil’ Monsta isn’t a player, and is instead babysat by a Minion.

                            - Each night, all Minions wake together and decide amongst themselves who babysits the Lil’ Monsta. If they can not reach a unanimous decision, the Storyteller decides.

                            - The player with the Lil’ Monsta token “is the Demon”. Good wins if they die. They register as a Demon for characters like the Fortune Teller etc.

                            - If a good player babysits Lil’ Monsta, they “are the Demon” but they remain good. A dead player babysitting Lil’ Monsta ends the game because the Demon “is dead”.

                            - Minions babysitting Lil’ Monsta keep their Minion ability.

                            - Lil’ Monsta isn’t a player, so can’t be drunk or poisoned.
                            """,
                    """
                            The Poisoner and the Widow wake. They each say the other, then themselves, then eventually agree on the Widow, who receives Lil’ Monsta’s token. The next night, they both choose that the Poisoner babysits Lil’ Monsta instead.
                            """, "The Yogscast"),
            entry(Role.LORD_OF_TYPHON,
                    "In the shadowed and forgotten corners of the cosmos, where the stars whisper secrets to the void, lies a truth so profound that the merest glimpse of it unravels the sanity of mortal minds.",
                    """
                            The Lord of Typhon is surrounded by Minions.

                            - All evil characters sit next to each other in a continuous line.

                            - All evil characters must be in the line at setup.

                            - Evil Townsfolk and Evil Outsiders may be part of the line, but do not have to be.

                            - The Lord of Typhon must have an evil character on both sides. They cannot sit at the end of the line of evil characters.

                            - The evil team starts with one additional Minion when the Lord of Typhon is in play.

                            - Any number of Outsiders might be in play.

                            - Like the Marionette, the Storyteller decides which players are Minions during setup. The Storyteller also decides which player is which Minion.

                            - If a Lord of Typhon is created mid game, the Lord of Typhon does not need to sit in a line with the evil characters.
                            """,
                    """
                            - There are two Minions: the Organ Grinder and the Mezepheles. In between them, neighboring them both, sits the Lord of Typhon. The number of Outsiders is normal.

                            - The Vizier neighbors the Harpy, who neighbors the Lord of Typhon, who neighbors the Goblin. There are ten players, and two Outsiders in play, due to the Lord of Typhon ability.

                            - The Fearmonger neighbors the Boomdandy, who neighbors the Lord of Typhon, who neighbors the Poisoner, who neighbors the Mastermind. There are 15 players, but zero Outsiders in play, since the Lord of Typhon removed one Outsider.
                            """, "The Yogscast"),
            entry(Role.OJO,
                    "Like a bonfire on a moonless night... I see you, mortal.",
                    """
                            The Ojo chooses specifically which character dies.

                            - Unlike other Demons, the Ojo must choose a character, not a player. The Storyteller may need to remind the player of this. We recommend that all players have their character sheet handy during the night phase.

                            - The Ojo can kill evil characters, if they wish.

                            - If there are multiple copies of a particular character in play, and the Ojo chooses that character to die, only one of those characters dies.

                            - If the Ojo chooses a character that is not in play, the Storyteller will almost always kill a living good player. It is possible, but uncommon, for the Storyteller to choose a dead player or an evil player to die.
                            """,
                    """
                            The Ojo chooses the Plague Doctor. The Plague Doctor dies. The next night, the Ojo chooses the Poppy Grower. The Poppy Grower dies. The next night, the Ojo chooses the Empath. There is no Empath in play, so the Storyteller chooses that the Shugenja dies instead.
                            """, "The Yogscast"),
            entry(Role.YAGGABABBLE,
                    "Murders inside the Rue Morgue? Фальшивые новости! Hounds on the Baskerville moor? Фальшивые новости! Death while sailing the Nile? Фальшивые новости!",
                    """
                            The Yaggababble kills by talking.

                            - The phrase that the Yaggababble says can be any length, but is usually 2 to 5 words long.

                            - If the Yaggababble says this phrase, the Storyteller may kill a player any time afterwards, until dawn.

                            - The Yaggababble may say this phrase as a standalone sentence, or part of another sentence.

                            - The Yaggababble may say this phrase multiple times per day. If so, the Storyteller may kill multiple players.

                            - The Storyteller chooses which players die.

                            - The Storyteller may choose to kill fewer players than the number of times the phrase was said.

                            - If the Yaggababble is drunk or poisoned, players cannot die, even if the Yaggababble was sober and healthy when they said their phrase. If the Yaggababble is sober and healthy, players might die, even if the Yaggababble was drunk or poisoned when they said their phrase.

                            - It is rare for the Yaggababble to kill during the day.
                            """,
                    """
                            - The Yaggababble's phrase is "that sounds fishy". The Yaggababble says this once during the first day. That night, a player dies. The next day, the Yaggababble says "that sounds fishy" three times. That night, three players die.

                            - The Yaggababble has said their phrase twice today. A Witch is in play. When the Heretic nominates, the Heretic dies, even though they were not cursed by the Witch. The Golem nominates the Demon, and the Golem dies. Both players died due to the Yaggababble's ability.
                            """, "The Yogscast"),

            // --- TRAVELERS ---

            entry(Role.THIEF,
                    "I aint done nuffink. I weren't even in dat alley last night! It weren't me what stole Mayor Bruno's briefcase wiv all dem fancy dockoments innit. Besides, it was too 'eavy to carry far.",
                    """
                            The Thief steals votes from a player, making their vote count negatively.

                            - When a player chosen by the Thief votes, the vote tally goes down by one instead of up by one. This happens every time that player votes that day.

                            - The player with the negative vote changes back to having a positive vote immediately if the Thief dies, including if the Thief is exiled, because the Thief loses their ability.

                            - Exiles are never affected by abilities, so the player with the negative vote can support exiles unaffected by the Thief’s ability.

                            - Since the Storyteller counts the number of votes out loud as they move their hand around the circle, all players will know which player the Thief chose.
                            """,
                    """
                            - The Thief chooses Marianna. The next day, while tallying the first vote, the Storyteller counts "1... 2... 3... 2... 3... 4... 5.” The nominated player now has five votes for their execution, and the nomination process continues.

                            - The Thief chooses Abdallah. Abdallah votes for an execution, and instead of the tally being six, it is four. Since 10 players are alive, the nominee is not executed today. Later that day, the players are voting to exile the Gunslinger. Abdallah votes to exile, and his vote counts as positive.
                            """, "TheCatghost"),
            entry(Role.BUREAUCRAT,
                    "Sign here please. And here. And here. Aaaaaaaaand here. This should all be sorted and tallied by the end of the day, assuming everyone's signatures are legible. We haven't had a mix-up in the paperwork for ages. Yesterday noon, if memory serves...",
                    """
                            The Bureaucrat gives extra votes to a player of their choice.

                            - When a player chosen by the Bureaucrat votes, that vote counts as three votes. This happens every time that player votes that day.

                            - The player with the triple vote loses it immediately if the Bureaucrat dies, including if the Bureaucrat is exiled, because the Bureaucrat loses their ability.

                            - Exiles are never affected by abilities, so the player with the triple vote can only support exiles once, not three times.

                            - Since the Storyteller counts the number of votes out loud as they move their hand around the circle, all players will know which player the Bureaucrat chose.
                            """,
                    """
                            - The Bureaucrat chooses Evin. The next day, when the first vote is being tallied, the Storyteller counts "1... 2... 3... 4-5-6... 7.” The nominated player now has seven votes for their execution, and the nomination process continues.

                            - The Bureaucrat chooses Filip. The next day, Filip has a triple vote, which he uses during four nominations.

                            - The Bureaucrat chooses Douglas, who is dead. The next day, Douglas uses his vote token to vote, and his vote counts as triple.
                            """, "Minecraft Magic Dungeons team"),
            entry(Role.GUNSLINGER,
                    "It's time someone took matters into their own hands. That someone... is me.",
                    """
                            The Gunslinger kills players who vote.

                            - Each day, after the first vote for execution has been tallied, the Gunslinger may publicly choose a player that just voted to die immediately. The Gunslinger does not have to kill a player—it is entirely up to them. Whether they use their ability or not, the Gunslinger cannot kill any further players that day.

                            - It is the Gunslinger’s responsibility to speak up and let the Storyteller know that they wish to use their ability.

                            - Since exiles are not affected by character abilities in any way, the Gunslinger cannot use their ability to kill a player that supports an exile.
                            """,
                    """
                            - The Imp has been nominated. There are 10 players alive, and five votes for the Imp, so the Imp is about to die. The Gunslinger chooses a voting player to die. They die, and the nomination continues, with the Imp still about to die.

                            - The players exile the Thief. Then, the Butler is nominated for execution and gets one vote. This is the first nomination for execution, since the Thief’s exile does not count. The Gunslinger chooses to kill the single voting player. Later that day, the Saint is nominated and six players vote. The Gunslinger cannot use their ability now because this is not the first vote for execution today.
                            """, "theregoestimmy (Revolver Bow Replacement dev)"),
            entry(Role.SCAPEGOAT,
                    "Good evening! Thank you for inviting me to the ball. I'm not from around here, but you sure seem like a friendly bunch, by golly. I'm sure we'll get along just dandy. What's all that rope for?",
                    """
                            The Scapegoat is executed instead of an ally.

                            - If the Scapegoat is evil, they might die instead of an evil player dying. If the Scapegoat is good, they might die instead of a good player dying. When exactly this happens is up to the Storyteller. This can only happen due to an execution, not death by other means such as a Demon or Slayer.

                            - The Scapegoat being killed still counts as an execution, so no more nominations occur today.

                            - As always, players do not learn the alignment of the Scapegoat when they die.
                            """,
                    """
                            - The Fortune Teller is about to be executed, but the Storyteller chooses to execute the good Scapegoat instead. The Fortune Teller lives and the Scapegoat dies. That night, the Undertaker learns that a Scapegoat was executed today.

                            - The Poisoner is about to be executed, but the Storyteller chooses to execute the evil Scapegoat instead. The Storyteller could have let the Poisoner die as normal, but chose not to.

                            - The Spy is about to be executed. The good Scapegoat dies instead.
                            """, "Autumn Wind"),
            entry(Role.BEGGAR,
                    "Alms for the poor, good Sir? Spare a coin, Madam? Thank you. God bless! You're a right kind soul and no mistake! I'll have some swanky nosh tonight, I will!",
                    """
                            The Beggar can not vote unless someone gives them a token to use, but they learn if the player that does so is good or evil.

                            - The Beggar cannot raise their hand to vote at all unless they have a vote token.

                            - When they do vote, they lose one vote token. If they have more than one, they may only use one at a time.

                            - Only a dead player may give their vote token to the Beggar, after which that dead player cannot vote. Each dead player decides for themself whether to give the Beggar their vote token. No one, including the Beggar, may move a player’s vote token on their behalf.

                            - When a player gives their vote token to the Beggar, the Beggar learns whether that player is good or evil.

                            - The Beggar can still nominate freely, and can still vote for an exile freely, because exiles are not affected by abilities.

                            - If the Beggar dies, they gain one vote token to use while dead, just like any other character would. However, the Beggar loses all their previously acquired vote tokens.

                            - If the Beggar would become drunk or poisoned, they do not.

                            - The ability to donate vote tokens is unique to the Beggar ability. Players may not give their vote token to a player that is not the Beggar, whether or not a Beggar is in play.
                            """,
                    """
                            - The Beggar cannot vote. On the fourth day, the Monk gives her vote token to the Beggar. The Beggar may now vote (once), and learns that the player is good. The Beggar is evil and tells the group that the Monk player is evil.

                            - The good Beggar has three vote tokens. The Recluse gives the Beggar their vote token, and the Beggar learns that they are evil. That day, the Beggar dies, and loses all their vote tokens except for one.
                            """, "AppleRoar (Coins mod dev)"),
            entry(Role.APPRENTICE,
                    "For years have I traveled, studying the ways of The Craft. Which craft, you ask? Simply that of the simple folk. Nothing to worry about. Not yet.",
                    """
                            The Apprentice has either a Townsfolk or a Minion ability.

                            - A good Apprentice gains a Townsfolk ability. An evil Apprentice gains a Minion ability. They have this ability until they die.

                            - The Apprentice learns their ability on their first night, and they may act that night if the character whose ability they gain would do so.

                            - Only abilities listed on the character sheet may be gained.

                            - If the Apprentice gains an ability that normally only functions on the first night of the game, such as the Grandmother’s, it functions on the Apprentice’s first night instead.

                            - The Apprentice does not literally become the character whose ability they gain. They are the Apprentice, a Traveller, so they may be exiled but not executed, and they do not count toward the number of alive players to see if evil wins due to just two players being alive. Also, other characters’ abilities that detect characters would detect the Apprentice as the Apprentice.
                            """,
                    """
                            - The evil Apprentice gains the Assassin ability. That night, they kill the Fool.

                            - The good Apprentice gains the Chambermaid ability. From now on, they learn who wakes at night. Later, the Gambler guesses that the Apprentice is the Tea Lady. The Gambler dies, because the Apprentice is not the Tea Lady, but the Apprentice.
                            """, "tterrag1098 (Chisel mod dev)"),
            entry(Role.MATRON,
                    "Miss Featherbottom, be quiet. Master Rutherford, a teacup needs just the four fingers, please. I know you are a father of nine, but age, or lack there-of as the case may be, is never an excuse for poor manners.",
                    """
                            The Matron chooses which players sit where.

                            - The Matron may swap two players’ seating positions, up to three times per day. The new seating order is permanent, unless changed again by the Matron.

                            - The same player may be moved multiple times.

                            - Some players may find moving difficult due to a physical disability or impediment. In these cases, they are immune to the Matron’s ability and can stay put.

                            - With the Matron in play, players may not talk privately except with their immediate neighbors while sitting down. Players may not leave their seat to whisper something to any player, and may not even talk about the game to each other when going to the bathroom, and so on. Players should self-police this.

                            - If the Matron swaps just one or two sets of players, they may not swap another set of players later that day.
                            """,
                    """
                            - The evil Matron rearranges the seating order so that she is sitting next to the Tea Lady. This way, the two of them can whisper to each other, and the Tea Lady's ability does not work.

                            - The good Matron swaps the seating position of the player they think is the Demon, so that player is far away from the player they think is the Minion. They may not whisper to each other now.
                            """, "Autumn Wind"),
            entry(Role.JUDGE,
                    "I find the defendant guilty of the crimes of murder, fraud, arson, larceny, impersonating an officer of the law, practicing medicine without a license, slander, regicide, and littering.",
                    """
                            The Judge can determine if an execution succeeds or not, regardless of who voted.

                            - The Judge can decide to pardon a player that they think is innocent, to condemn a player that they think is guilty, or vice versa.

                            - If the nominee is pardoned, then they are not executed today, and none of the votes for them count. If the nominee is condemned, then they are executed immediately, regardless of how many votes they received, and regardless of whether another player was about to die by execution. Then the day ends, because there can normally only be one execution per day.

                            - The Judge may use their ability during or after the votes are tallied. However, once a new player has been nominated, then the Judge may only use their ability on this new nominee. The Judge may only use their ability once, and only if a different player made a nomination.
                            """,
                    """
                            - The Slayer was about to die, but the Po is nominated and every alive player votes, so now the Po is about to die. The evil Judge decides that the Po’s execution fails. So, as before, the Slayer is about to die, and the nomination process continues.

                            - The good Judge nominates the Professor. Nobody votes, but the Judge may not use their ability. A Traveller's exile is voted on. Once again, the Judge may not use their ability. The Grandmother nominates the Goon. Even though the Goon got only one vote, the Judge decides that the Goon is executed immediately.
                            """, "Autumn Wind"),
            entry(Role.VOUDON,
                    "Bien venu. Sit down. Breathe deep. Enter the land of the dead. See with their eyes. Speak with their voice. Yon sel lang se janm ase.",
                    """
                            The Voudon gives the voting power to the dead instead of the living.

                            - The dead and the Voudon may vote as many times per day as they wish. They do not need a vote token to vote, and do not lose their vote token when they do so. Alive players cannot vote. It is not the case that they may put their hand up but the votes don’t count—their hands must stay down during voting.

                            - The number of votes required to execute a player is no longer half or more of the alive players. The player with the most votes is executed each day, but even a single vote is enough to execute a player if no other player gets more votes.

                            - The Voudon does not alter who can make nominations. As normal, alive players may make nominations, and dead players may not. Since Travellers are exiled, not executed, all players, alive or dead, may support exiling the Voudon or other Travellers.

                            - If a player is about to die and then the Voudon is exiled, that player is still about to die and nominations continue, but alive players vote as normal. If a later nomination gets more votes and it tallies to half or more of the alive players, this new player is about to die instead.
                            """,
                    """
                            - There are 12 players alive, and three dead. An alive Innkeeper nominates the Moonchild. Of the four players that can vote, three do. All other nominations today get fewer than three votes, so the Moonchild dies.

                            - It is the first day. Only the Voudon can vote, but does not. The players call for the Voudon to be exiled. Five players support the exile, and seven oppose. The Voudon lives.

                            - Two dead players vote for the Mastermind to be executed. Then, the Voudon, the dead Fool and the apparently dead Zombuul all vote for the Gossip. The Gossip is executed.
                            """, "Monumenta Team"),
            entry(Role.BISHOP,
                    "In nomine Patris, et Filii, et Spiritus Sancti… Nos mos Dei. Deus vult de nobis.",
                    """
                            The Bishop prevents players from nominating at all. Instead, the Storyteller does all nominating.

                            - The Storyteller makes nominations during the nomination process instead of the players, and the Storyteller may nominate as few or as many players as they wish. To make things fair, they must nominate at least one player whose alignment is opposite the Bishop’s alignment each day.

                            - The Bishop does not alter who can and cannot vote. Each player may do so normally.

                            - Since Travellers are exiled, not executed, any player may call for the Bishop or another Traveller to be exiled.
                            """,
                    """
                            - The Bishop is good. On the first day, the Storyteller nominates the Demon, a Minion, and two Townsfolk. On the second day, the Storyteller nominates a Minion and Outsider.

                            - The Bishop is evil. The Storyteller has nominated nobody. However, the Storyteller must nominate at least one good player today, so they choose the Minstrel. The next day, the Storyteller nominates four good players and the Demon. The Bishop is exiled that day, and now the players may continue the nomination process normally.
                            """, "JoPe158"),
            entry(Role.BARISTA,
                    "A cup of coffee with no cream, Monsieur? I’m terribly sorry, but we’re fresh out of cream — how about with no milk?",
                    """
                            The Barista either makes people sober & healthy, or allows them to act twice as much as normal.

                            - The Storyteller chooses which player the Barista affects each night, and which one of the two Barista abilities is in effect. The Barista does not know who or what the Storyteller chooses, but the affected player does.

                            - If the affected player is acting twice, then they do so at the normal time. If they would normally wake at night, they act, go to sleep, then wake to act again. If they have already used a “once per game” ability, they may use that ability again. If they have a “once per game” ability but have not used it yet, they may use it twice before dusk.

                            - If the Barista makes a player sober and healthy, their drunkenness and poisoning, if any, is removed, and they may not become drunk or poisoned until dusk. This player must get true information, even if a Vortox is in play.
                            """,
                    """
                            - The Barista makes the Sage sober and healthy.

                            - The Klutz acts twice. They die and must choose two players. If either is evil, evil wins. The next night, the Barista makes the Witch act twice. Two players are cursed.
                            """, "TheWandererRaven (Raven Coffee dev)"),
            entry(Role.HARLOT,
                    "Enchanté, Sailor. You look like you need someone to really listen to your troubles. I'm a good listener. Very, very good.",
                    """
                            The Harlot learns the character of whoever agrees to reveal it, but at great risk for them both.

                            - Each night, the Harlot chooses a player. That player has a decision to make: do they reveal their character to the Harlot? If they do, the Storyteller may decide that both this player and the Harlot die tonight.

                            - The Harlot only learns the character of the chosen player, not that player’s alignment.

                            - The Harlot may discuss during the day which character they would like to pick at night, and other players may offer to be picked, but they may go back on their word and choose differently when night comes.
                            """,
                    """
                            - The good Harlot wakes and chooses the Philosopher, who chooses to reveal. The next night, the Harlot chooses the No Dashii, who chooses not to reveal. The next night, the Harlot chooses the Mutant, who chooses to reveal. The Storyteller decides that the Harlot and Mutant die tonight.

                            - The evil Harlot chooses the Sage, who reveals. The next day, the Harlot says the Sage is actually the Witch.
                            """, "Autumn Wind"),
            entry(Role.BUTCHER,
                    "It tastes like chicken. More please.",
                    """
                            The Butcher allows a second execution to occur per day.

                            - After the first executed player has died, the Butcher may nominate a second player for execution. The Butcher may nominate a player that has already been nominated today, and the Butcher may make a nomination even if the Butcher already made a nomination earlier today.

                            - If a player is executed, even if they do not die, then the Butcher may use their ability. The players may choose to vote or not to vote, so there is no guarantee that this extra nomination will cause an execution—it still needs to get enough votes—but this second nomination does not need to exceed the vote tally of the previous nominations.

                            - If no execution occurs today, then the Butcher may not use their ability at all today.
                            """,
                    """
                            - The Witch is executed and dies. The Butcher then nominates the Sage, who gets enough votes to be executed. The Sage dies too.

                            - The Bone Collector is exiled, and then the Harlot is exiled. There are no executions today. The Butcher does not get to nominate again, because exiles are not executions.

                            - The Butcher nominates the Town Crier, but the Town Crier is not executed. The Mathematician gets more votes and is executed today. The game continues, and the Butcher nominates the Town Crier again. This time, enough hands are raised, and the Town Crier is executed.
                            """, "jmods (Butchery dev)"),
            entry(Role.DEVIANT,
                    "Twas the lady's quip, forsooth.",
                    """
                            The Deviant can avoid being Exiled - as long as the Deviant player has been amusing today.

                            - The Deviant can amuse the group in any way they choose. Generally, verbal means such as jokes, funny stories, or witty remarks will suffice.

                            - The Storyteller is the judge of whether the Deviant was funny or not.
                            """,
                    """
                            - The evil Deviant cracks a few jokes, and gets a few laughs, but the players nevertheless decide to exile them. Even though there are enough votes, the Storyteller decides to keep the Deviant alive.

                            - On the third day, the Deviant was slightly funny, and cannot be exiled. On the fourth day, the Deviant was not very funny, and is successfully exiled.
                            """, "igalaxy (Collar Trinkets dev)"),
            entry(Role.BONE_COLLECTOR,
                    "I collect many things. Hair. Teeth. Clothes. Fragments of poems. The dreams of lost lovers. My secret arts are not for you to know but my fee is a mere pittance. Bring me the blood of a noblewoman who died of heartbreak under a full moon, and you shall have your answers.",
                    """
                            The Bone Collector temporarily gives dead players their ability back.

                            - The Bone Collector must choose a dead player. The chosen player remains dead, but they get their ability to use. If their ability was a “you start knowing” or a “once per game” ability—such as the Virgin, Slayer, Clockmaker, Seamstress, or Juggler—they may use it again, even if it was already used, until dusk falls.

                            - When the Bone Collector chooses a player, that player does not learn they were selected by the Bone Collector, although they find out soon enough when they are woken to use their ability.

                            - If the Bone Collector dies, that player no longer has the ability they regained due to the Bone Collector.
                            """,
                    """
                            - The Bone Collector gives the dead Flowergirl her ability back. That night, the Flowergirl learns that the Demon did indeed vote today. The following night, the Flowergirl once again has no ability.

                            - The Bone Collector chooses the dead Witch. The Witch wakes and curses the Clockmaker. The Clockmaker nominates the following day, and dies.

                            - At night, the Bone Collector chooses the dead Butcher. The following day, after an execution has occurred, the Storyteller prompts the Butcher to nominate again.

                            - During the day, the dead Juggler guesses five players’ characters. That night, the Bone Collector gives the Juggler their ability back. The Juggler learns a "3".
                            """, "Minecraft Bone item + edits by Autumn Wind"),
            entry(Role.CACKLEJACK,
                    "Wire α To wire β. LigHt oN. BuZZer off. GAzOinks! Arms STra1ght. FingER 2 nose. hOooLd stiLL. BoiNgo-banGo! Ha-ha-ha!",
                    """
                            The Cacklejack causes character-changing chaos.

                            - The Storyteller chooses one player to change character each night. This player can be evil or good, dead or alive.

                            - The Storyteller also chooses which character they become. This may be an in-play or a not-in-play character.

                            - Each day, the Cacklejack chooses a player that is immune - the Storyteller must choose a different player to change character that night.

                            - The Cacklejack may choose publicly or privately, but must choose during the day.

                            - Each time a player’s character changes, this is treated as a new instance of that character ability. For example, if a player becomes the Chef on Night 3, they immediately learn how many pairs of evil players there are.
                            """,
                    """
                            - During the day, the Cacklejack chooses Evin. That night, Amy changes from the Acrobat into the Clockmaker. Amy learns a 2.

                            - During the day, the Cacklejack chooses Abdallah. That night, Julian changes from the Drunk into the Flowergirl. Julian does not learn that he is now sober.

                            - During the day, the Cacklejack chooses Alex. That night, Lachlan changes from the Leviathan into the Shabaloth. Lachlan has fun.
                            """, "The Yogscast"),
            entry(Role.GANGSTER,
                    "I like your shoes. It would be such a shame if you had a little accident, and they got ruined. Now that you mention it, I like your cufflinks too.",
                    """
                            The Gangster encourages their neighbors to kill each other.

                            - The Gangster may kill one of their two living neighbors. Their dead neighbors are skipped over, and do not count.

                            - To use their ability, the Gangster and one of their living neighbors must agree to kill the other living neighbor. The Storyteller must hear and confirm this agreement. The Gangster cannot kill without the Storyteller present.

                            - Each day, the Gangster may say whatever they want, and offer any encouraging words they want to either player. Once an agreement has been reached, then the Gangster may not use their ability again today, even if that player didn’t die due to an ability protecting them.

                            - The Gangster’s two living neighbors are always one clockwise, and one counter-clockwise.

                            - If both living neighbors want to kill the other, the Gangster decides who dies.
                            """,
                    """
                            - The Gangster neighbors the Saint and the Baron. The Gangster asks the Baron if they want to kill the Saint. The Baron agrees and the Saint dies.

                            - The Gangster neighbours the Chambermaid and the Poppy Grower, but they are both dead. The Gangster’s two living neighbours are the Engineer and the Po. The Gangster talks with the Po and offers to kill the Engineer. The Po declines. The Gangster talks with the Engineer and the Engineer asks the Gangster to kill the Po. The Gangster agrees, and the Po dies. Good wins.

                            - The Gangster neighbours the Fool and the Sage. The Sage and the Gangster kill the Fool but the Fool doesn’t die, because of the Fool's ability. The Gangster may not use their ability again today.
                            """, "sbom_xela (Alex's Mobs)"),
            entry(Role.GNOME,
                    "Four the score or seven beers, no shows are goes for me and my. A prank to crack the cranks and planks o' the floor foundation length, so incontravertabubbilly mini. The large essays down streams of joyce, no greater than is scene, not inherdt, Ha-urrumph.o.",
                    """
                            The Gnome protects one player on their team.

                            - The Gnome starts as the same alignment as one other player - their "amigo". The Storyteller publicly announces which player this is.

                            - When their amigo is nominated, it is the Gnome's responsibility to speak up. The Storyteller may not prompt them to use their ability.

                            - If their amigo changes alignment, the Gnome's alignment does not change.

                            - The Gnome may use their ability any number of times over the course of the game, including zero. Their amigo may still only be nominated once per day.

                            - When the Gnome uses their ability, and the Storyteller confirms it, the nominator dies immediately. Voting for execution still occurs.

                            - Regardless of what the group wants, it is always the individual player's decision whether they wish to nominate or not, and always the Gnome player's decision on whether they wish to use their ability or not. If the Storyteller feels that a player is being pressured into nominating or using their ability when they don't want to, the Storyteller may not recognize that nomination or ability use.
                            """,
                    """
                            - The Gnome starts the game at the same time as the rest of the players, and is good. Amy is the Alsaahir. Before the first night, the Storyteller announces that the Gnome is the same alignment as Amy. The Engineer nominates Amy on day 3. The Gnome does not use their ability.

                            - The Gnome enters the game on the 2nd day, and is evil. Lewis is the Demon. At this time, the Storyteller announces that the Gnome is the same alignment as Lewis. On the 2nd day, the Boffin nominates Lewis, and is killed by the Gnome. On the 3rd day, the Zealot nominates Lewis, and is killed by the Gnome. On the 4th day, the Village Idiot nominates Lewis, and the Gnome does not use their ability.
                            """, "Terramity Team"),

            // --- FABLED ---

            entry(Role.ANGEL,
                    "Let those who are without sin dare to raise their hand to my chosen, for I shall strike such fools down with the fury and righteousness of a thousand storms.",
                    """
                            Use the Angel to help new players have fun when there are one or two new players in a group of veterans.

                            - Being the only new player in a group can be overwhelming. Being protected by the Angel encourages all players to keep new players alive for as long as possible, which means new players have more fun and contribute to the game more.

                            - All players know who is protected by the Angel, but not their alignment or character. Whoever is the single player most responsible for killing a protected player suffers some consequence. For example, if the Demon kills a protected player, the Demon suffers a penalty. If a protected player is executed, the player who suffers a penalty will probably be the one who nominated the protected player.
                            """,
                    """
                            - The Angel protects Sarah. The Demon attacks and kills her. As punishment, the Demon cannot attack on the next night.

                            - Ben is the Demon and is protected by the Angel. The players do not execute him until the final day, at which point they may execute him without penalty.
                            """, "Autumn Wind"),
            entry(Role.BUDDHIST,
                    "You throw thorns. Falling in my silence, they become flowers.",
                    """
                            Use the Buddhist to help new players have fun when there are one or two veterans in a group of new players.

                            - When experienced players find themselves in a game full of beginners, the veterans will often dominate the game due to their enthusiasm and knowledge.

                            - Players affected by the Buddhist cannot talk at all for the first two minutes of each day. They may not whisper in private, and may not talk to each other. They simply listen.

                            - This is not a punishment for being talkative. Being talkative is great! Blood on the Clocktower is a talking game, and the more, the merrier. That said, forcing the veterans to stay silent temporarily each day allows the new players to find their own voices, to come up with their own theories, and to take action on their own. It is about fun for everybody.

                            - It is common for a player to say “I am a Buddhist” or for the Storyteller to say to them “You are a Buddhist.” This doesn’t mean that their character is the Buddhist. It is a pleasant shorthand for saying “You are affected by the Buddhist ability.” This is similar to saying “You are a Revolutionary.”
                            """,
                    """
                            - Lachlan and Lewis are veterans in a game of mostly new players. To encourage the new players to talk, the Storyteller puts the Buddhist in play. Lachlan and Lewis may not talk for the first two minutes each day, after which, they may talk freely.

                            - Evin is affected by the Buddhist. He is a Minion and simply listens to what people are saying for the first two minutes, allowing him to bluff as a not-in-play character later on.
                            """, "Autumn Wind"),
            entry(Role.DEUS_EX_FIASCO,
                    "It’s not a bug, it’s a feature. It’s not an error, it’s a tweak. It’s not broken, it’s quirky.",
                    """
                            Use the Deus ex Fiasco to neutralize mistakes and increase your confedence when running a difficult script.

                            - The Deus ex Fiasco must be announced at the start of the game. It may never be added partway through the game. Hypothetically, if the Storyteller makes a misteak mid-game, and adds the Deus ex Fiasco afterwards, all players would know that the mistake was real and the Deus ex Fiasco would not work.

                            - The Storyteller must make a mistake. This can be an accidental mistake, or a deliberate mistake. The players are not told which.

                            - If the Storyteller has made an accidental mistake, they do not have to make additional mistakes. If the game is appoaching the final day and the Storyteller has not made an accidental mistake, they must make a deliberate mistake before the game ends.

                            - All mistakes, whether deliberate or accidental, must be corrected. The Storyteller may need to break the rules in order to fix a mistake. Any time after a mistake is made the Storyteller must inform the group that a mistake has been made and corrected. The exact nature of the mistake is not revealed to the group, but may need to be revealed to an affected player in private.

                            - Players are welcome to bluff that the Storyteller has made a mistake when they haven’t, or to bluff that a mistake was corrected when it wasn’t.

                            - If needed, the Storyteller may make several accidental mistakes, several deliberate mistakes, or some combination of the two.
                            """,
                    """
                            - During the first day, Doug talks privately with the Storyteller and says that he drew the Drunk token from the bag. The Storyteller looks in the Grimoire and notices that Doug’s token is the Drunk. The Storyteller tells Doug that he is now the Mayor, replaces the Drunk token with the Mayor token in the Grimoire, and secretly makes the Ravenkeeper the Drunk instead. Later that day, the Storyteller declares that a mistack has been madde.

                            - The Empath is sitting next to the Monk and the poisoned Recluse. The Storyteller, forgetting that the Recluse is poisoned, accidentally gives the Empath a “1”. Later that night, the Storyteller wakes the Empath again and gives a “0”. The following day, when the Empath privately asks what happened, the Storyteller explains that the first number was a mistake, and the second number is correct. To avoid revealing too much, the Storyteller waits until the next day to inform the group that a mistake was made.

                            - The Imp was executed today. The Storyteller wakes the sober & healthy Undertaker, and deliberately (and incorrectly) shows the Recluse token. Later that night, the Storyteller again wakes the Undertaker and shows the Imp token. In the morning, the Storyteller tells the group that a mistake was made in the night, but corrected.

                            - The Yaggababble is executed and dies. The Storyteller accidentally declares that the game is over and good has wun. However, the Scarlet Woman is still alive. The Storyteller declares that a mistake has been made, and that the game continus.

                            - Ben is the Shabaloth. At night, Ben kills Amy and Doug. In the morning, the Storyteller deliberately declares that Ben and Lewis have died. After 30 seconds or so, the Storyteller says “Whoops, I had my Grimoire facing the wrong way. Ben and Lewis should not have died. They are still alive. Amy and Doug died during the night. Sorry, my mistake.”

                            - The Storyteller forgot to wake the Poisoner last night. The Storyteller has a private chat with the Poisoner, tells them this, and tells them that every night from now on, they can choose as many players as they want, and the Storyteller will choose which of these players are poisoned.

                            - The Chambermaid wakes and chooses a dead player, even though the Chambermaid must only choose living players. The Storyteller notices this, but does not correct the Chambermaid. Instead, the Storyteller deliberately gives false information to the Chambermaid. The next day, the Storyteller requests a private chat with the Chambermaid, tells them that they made a mistake by allowing them to choose a dead player, and prompts them to choose two living players immediately. The Storyteller gives correct information this time.
                            """, "(stock image)"),
            entry(Role.DJINN,
                    "نحن لسنا هنا.\n" +
                            "انت لست حقيقي.\n" +
                            "كل شيء هو وهم.\n" +
                            "أسئلتك هي جبل نار في يوم صافٍ.",
                    """
                            Add the Djinn to all games with a jinx icon on the script. The Djinn resolves jinxes by creating a unique rule.

                            - When creating a character list using the Script Tool, some character combinations will be marked as unusual. These two characters are jinxed—they have abilities that clash or contradict each other in some way. The Djinn creates a special rule that allows these characters to work well together. Some jinxed characters even work better with the Djinn in play!

                            - The Djinn’s special rule is described by the Script Tool online, and is printed out automatically when you create a script with a character combination that is jinxed.

                            - There are many different Djinn special rules. Each is tailored to a specific pair of jinxed characters.

                            - If there are jinxed characters on the character sheet, even if there are no jinxed characters in play, the Storyteller tells all players what the Djinn’s special rule is at the start of the game.

                            - The Djinn may have several special rules at once. If there are multiple pairs of jinxed characters on the character sheet, the players learn all the Djinn’s special rules.
                            """,
                    """
                            - The Pit-Hag and the Heretic are Jinxed. At the start of the game, the Storyteller reads out the Djinn's special rule: “A Pit-Hag cannot create a Heretic.” Later in the game, the Pit-Hag tries to create a Heretic. The Storyteller shakes their head, and the Pit-Hag must choose another character to create.

                            - The Spy and the Magician are Jinxed. At the start of the game, the Storyteller reads out the Djinn's special rule: "When the Spy sees the grimoire, the Demon and the Magician's character tokens are removed." There is no Spy and no Magician in play, but the Storyteller reads this aloud anyway so that the good team doesn't know which Minion is in play.
                            """, "Tejty (Genie Lamp mod)"),
            entry(Role.DOOMSAYER,
                    "And on the Seventh Day, there shall be a great flood and a pestilence upon the People of the Village of the Ravens! The dead shall rise and the living shall repent! O Woe! O Unholy day! Only by great sacrifice shall they prevail! So sayeth the Sages of Nostros and so sayeth I.",
                    """
                            Use the Doomsayer to make large games take less time.

                            - The Doomsayer allows players to sacrifice their allies in order to gain information, which shortens the game.

                            - Only alive players may use the Doomsayer ability, and each may do so only once per game. It is their responsibility to remember to not use it again.

                            - If a player says something like “I use the Doomsayer ability,” then the Storyteller chooses which player to kill, but they must kill an alive player of the same alignment as the player who used the Doomsayer ability. So, if a good player uses the ability, then a good player dies. If an evil player uses the ability, then an evil player dies.

                            - Once three players are left alive, the Doomsayer ability may no longer be used.
                            """,
                    """
                            - The Monk uses the Doomsayer ability, and the Washerwoman dies. Later that day, the Poisoner uses the Doomsayer ability, and the Baron dies.

                            - An evil Thief uses the Doomsayer ability, and the Scarlet Woman dies. Later, the Spy uses the Doomsayer ability, and the good Gunslinger dies. Later, the Demon uses the Doomsayer ability, and the Spy dies.
                            """, "Autumn Wind"),
            entry(Role.DUCHESS,
                    "We shall entertain between the hours of 6 and 7 precisely. Tea at 6:15. Scones at 6:45. Do not be late. Formal wear applies, as always.",
                    """
                            Add the Duchess if your script has too little information or too much misinformation.

                            - Sometimes, you may want to create a character list using the Script Tool that has hardly any good characters that gain information directly. Whilst having an abundance of abilities and a lack of information can be fun for some players, other players like something more. The Duchess adds regular information to such a game.

                            - Each player that visits the Duchess learns how many visitors are evil, including themself. However, one visitor of the Storyteller’s choice will get false information.

                            - Players that visit the Duchess still get to use their ability normally. The Duchess does not make their ability give false information.

                            - The players decide amongst themselves which players will be the three players to visit. If exactly three visitors cannot be decided upon, then the Duchess does not act tonight.
                            """,
                    """
                            - The Soldier, Pacifist, and Sage visit the Duchess. The Soldier and Pacifist learn a "0.” The Sage learns a "1.”

                            - The Mutant, Butler, and Po visit the Duchess. The Mutant learns a "1,” the Butler learns a "2," and the Po learns a "1.”

                            - The Mastermind, Imp, and Minstrel visit the Duchess. The Mastermind learns a "2,” the Imp learns a "1," and the Minstrel learns a "2.”
                            """, "The Yogscast"),
            entry(Role.FERRYMAN,
                    "When righteous dreams come, they have the weight of truth.",
                    """
                            Use the Ferryman to create a fun and inclusive climax to the game even if new players have used their vote tokens.

                            - If you are running a game for newer players who don’t yet grasp the strategy of when to use their dead votes, or have used them when they forgot they were dead, you can add the Ferryman. This will ensure everyone gets a say in the final day’s critical votes.

                            - All dead players regain their vote tokens on the final day, regardless of alignment or when they voted.

                            - If a dead player still has their vote token, they do not get a second one from the Ferryman.

                            - The final day is the day that the Storyteller thinks is most likely to be the last day of the game – the day where, if the Demon is not executed, evil will win. This most likely means the day with only 3 living players remaining.

                            - If vote tokens are used on the final day, they aren’t returned.
                            """,
                    """
                            - Most of the group is new. Two players, Amy and Doug, forgot they were dead in the excitement of voting. The Storyteller puts the Ferryman in play. Later in the game, when there are three players left alive, the Storyteller declares that it is the final day. Amy and Doug regain their vote tokens.

                            - It is the start of the final day. 17 players are dead and three players are alive. 10 dead players have used their vote tokens. In order to create a more fun and engaging final day, the Storyteller adds the Ferryman and those dead players regain their vote tokens. After a riotously entertaining final day, the Storyteller is celebrated for maximizing the players’ enjoyment.
                            """, "Autumn Wind"),
            entry(Role.FIBBIN,
                    "Tee-hee-hee.\n" +
                            "Tee. Hee. Hee.",
                    """
                            Add the Fibbin if your script has too much information or no possibility of misinformation.

                            - If you create a character list and it has no characters that cause drunkenness, poisoning, or other ways for information to be false, then you may want to add the Fibbin. Whilst it is not necessary, even a minor chance of a good player’s information being incorrect can drastically help the evil players bluff.

                            - The Fibbin does not make an ability fail in the way that drunkenness and poisoning do. It only affects abilities that provide information from the Storyteller signaling to a player during the night or telling them something.

                            - If the game ends before you have given a good player incorrect information, that’s okay.

                            - Some characters get false information due to their ability. The Fibbin can make this information true.
                            """,
                    """
                            - On the first night, all players get correct information. On the second night, the Empath learns they are neighbouring one evil player, but both their neighbours are actually good. For the rest of the game, all good players get correct information.

                            - The Virgin is nominated by a Townsfolk. This Townsfolk is executed immediately because the Fibbin cannot make an ability malfunction. Later, the Monk protects a player. Again, the Monk's ability cannot fail due to the Fibbin ability. Later, the Ravenkeeper dies at night and gets false information, because information from an ability can be affected by the Fibbin ability.
                            """, "Autumn Wind"),
            entry(Role.FIDDLER,
                    "I'll wager mi lyef ye cannae best me in a fiddle contest, ye boss-eyed snook! We'll go out on the lash, get the pub jammers an' have a right craic. I'll be layin' ma boots into ya come mornin' ye rumbly muppet.",
                    """
                            Use the Fiddler to decide a winner if the game must end due to time constraints or a stalemate.

                            - Sometimes there won’t be enough time to finish a game. Maybe the venue you are playing at needs to close. Maybe some players need to leave unexpectedly and the game cannot continue without them. Maybe the Townsfolk refuse to execute and the Demon refuses to kill.

                            - The Storyteller can add and activate the Fiddler at any time. To do so, all players close their eyes while the Demon chooses a good player to challenge to a fiddle contest. Then, after a minute or two, all players will raise their hands to vote on which of these two players wins. The game ends, and the winning player’s entire team wins too.

                            - Like an exile, this group decision on who wins the game is not affected by abilities, and the dead may vote normally. The Thief cannot steal votes, the Voudon has no effect, and so on.

                            - Players cannot use their abilities once the Fiddler has been activated. The Slayer cannot choose to slay a player, the Artist cannot ask their question, and so on.

                            - If this fiddle contest is a tie, evil wins.
                            """,
                    """
                            - The game begins but will need to end in 45 minutes due to a freak lightning storm approaching the neighbourhood, so the Storyteller adds the Fiddler. After 40 minutes, the Fiddler activates. The players choose the good player to win, so good wins.

                            - There are just four players left alive. Each day, nobody nominates. Each night, the Demon chooses a dead player to kill. Since this could go on indefinitely, the Storyteller adds the Fiddler so that the game can end.
                            """, "xerca0 (Music Maker Mod)"),
            entry(Role.HELLS_LIBRARIAN,
                    "Shhhhhh. Please be quiet. It is best not to disturb the Librarian. I've heard it has a temper.",
                    """
                            Use the Hell’s Librarian to allow a softly-spoken Storyteller to be heard when needed.

                            - As the Storyteller, you’ll find the Hell’s Librarian useful when it is difficult to get the group’s attention. Maybe you need to explain a game rule? Or get attention for a crucial final-day vote? It can also be used to prevent players from talking about their characters before the game begins or from narrating what they are doing at night. Players instinctively stay quieter during the pre-game period and at night, so you may never need it.

                            - It is best to give the players fair warning before you bring the hammer down. Like the Angel, the threat of a mysterious penalty is more important than the actual penalty. The purpose of this character is to make games run smoothly, not to punish minor infringements.
                            """,
                    """
                            The Storyteller is attempting to explain the voting rules to a few new players. The group is loud and is not listening to the Storyteller’s requests for silence, so the Storyteller declares that the Hell's Librarian is in play. Two players continue to loudly talk, even though they know the possible penalty. The Storyteller decides that one dies and the other loses their vote for today. All players are silent whilst the rules are explained.
                            """, "The Yogscast"),
            entry(Role.REVOLUTIONARY,
                    "United we feigned.\n" +
                            "Divided, we stalled.",
                    """
                            Use the Revolutionary to help disadvantaged players participate.

                            - If a player has an intellectual disability, is unable to understand the rules of the game, is blind or deaf, or is unable to communicate or participate as normal, they may still play by teaming up with a player that they trust.

                            - These two players are the same alignment and sit next to each other so they can whisper or signal to each other throughout the game. The experienced player can help the disadvantaged player in whatever way is needed, talking on their behalf or suggesting what to do.

                            - The Revolutionary is also useful for couples or good friends who wish to play, but are uncomfortable with lying to or mistrusting each other, even in a game.

                            - Once per game, the Storyteller can make either player register as a different character, alignment, or both.

                            - The Storyteller may wake both players at night, instead of just the player due to wake, if that helps understanding.

                            - If an ability would change a Revolutionary’s alignment, this ability has no effect or it changes both Revolutionaries’ alignment, Storyteller’s choice.
                            """,
                    """
                            - Mathew is deaf. He teams up with Davo, via the Revolutionary. Mathew draws the Poisoner, and Davo draws the Imp. They can scheme in private, using sign language, so that Mathew can still participate.

                            - Hannah is 12 years old. She is keen to play, but does not understand many of the intricacies of how the characters work. She gets the Ravenkeeper and teams up with her father, who is the Fortune Teller.
                            """, "Autumn Wind"),
            entry(Role.SENTINEL,
                    "Name, please.\n" +
                            "Papers, please.\n" +
                            "Weapons, please.",
                    """
                            Add the Sentinel to your script to keep the number of Outsiders in play mysterious.

                            - The official character lists are carefully constructed so that the number of Outsiders is never completely known, which lets evil players safely bluff as Outsiders. Many of the games you create using the Script Tool will not have this luxury. If, for one reason or another, the number of Outsiders in a game will become certain, the Storyteller can add a Sentinel. This will confuse matters and help the evil team either bluff as Outsiders or make existing Outsiders look suspicious.

                            - Games with a Sentinel in play might have one more Outsider than normal. They may have one less. They may have the normal amount. It is up to the Storyteller.
                            """,
                    """
                            - There are seven players in this game. There are no characters on the character list that add Outsiders. The Demon bluffs as the Saint. A Sentinel is in play, so the good players are not sure if there is actually a Saint or not.

                            - There are nine players in this game. Even though a Baron is on the character list, the good players know no Baron is in play because the Witch just killed a player, so there should be just two Outsiders in play. However, the Outsiders cannot be trusted because a Sentinel is in play. Indeed, there is one less Outsider than normal in this game, and the Witch is bluffing as the Butler.
                            """, "The Yogscast"),
            entry(Role.SPIRIT_OF_IVORY,
                    "The Wasteland calls. Bones rise to flesh, then fall to dust. The great spirit grows. The great spirit watches. The great spirit guides. The human listens, or the human is no more.",
                    """
                            Add the Spirit of Ivory to your script to keep the number of evil players fair and balanced.

                            - When creating character lists using the Script Tool, it is a good idea to include no more than one character that adds evil characters. If two or more players turn evil, then the evil team can win simply by revealing who they are and winning due to their voting majority. Adding the Spirit of Ivory prevents too many players turning evil, creating a more fun and fair game for the good players.

                            - With a Spirit of Ivory in play, only one more player than normal can ever be evil. If a second player would become evil, they stay good instead.

                            - The normal number of evil players is printed on the Traveller sheet and on the Setup sheet.
                            """,
                    """
                            The Fang Gu attacks an Outsider and creates an evil player. The Devil's Advocate chooses the Goon at night. Normally, the Goon would turn evil, but the Goon remains good because there is already 1 more evil character than normal in play.
                            """, "Autumn Wind"),
            entry(Role.TOYMAKER,
                    "It buzzes! It walks down stairs! It keeps you warm at night! It tastes like sugar! The kiddies love it! Introducing... the brand new... Warm'o-buzzy-wuzzy-walk'a'bot-thingy-contraption! Fun for all ages!",
                    """
                            Use the Toymaker to make small games take more time.

                            - If you created a character list using the Teensyville option in the Script Tool, then you may want to use the Toymaker. Games set in Teensyville have only six Townsfolk, two Outsiders, two Minions, and two Demons on the list, and they specifically cater to five or six players.

                            - With the Toymaker in play, the Demon learns three not-in-play characters at the start of the game, and the Minion(s) and Demon learn who each other are. Once per game, the Demon must voluntarily choose to attack nobody tonight. If the Demon is about wake to attack a player and this would end the game, but the Demon has not yet chosen to attack nobody, then the Storyteller does not wake the Demon—they are forced to attack nobody tonight.

                            - You may use the Toymaker in games of Trouble Brewing with five or six players, but it is not necessary.
                            """,
                    """
                            - On the second night, when five players are alive, the Imp chooses not to attack, which allows it to act during the final night. On the third night, when four players are alive, it kills a player.

                            - On the second night, the Imp kills a player. On the third night, when just three players are alive, the Imp cannot attack because it is the final night.
                            """, "The Yogscast"),

            // --- LORIC ---

            entry(Role.BIG_WIG,
                    "Vanity asks ‘Is it popular?’\n" +
                            "Cowardice asks ‘Is it safe?’\n" +
                            "Conscience asks ‘Is it right?’\n" +
                            "Who among us will ask:\n" +
                            "‘Is it true?’",
                    """
                            The Big Wig gives nominees a defence lawyer.

                            - When nominated, that player must choose a player to speak on their behalf. They may choose living or dead players.

                            - Other players are not allowed to speak during this period. This includes the nominated player. If necessary, the Storyteller may use the Hell’s Librarian to enforce this.

                            - If the chosen player is mad that the nominee is evil, or not mad enough that the nominee is good, the Storyteller might kill that player.

                            - Being mad that ‘the nominee should not be executed’ might be similar enough to being mad that ‘the nominee is good’ to avoid being killed by the Storyteller.

                            - The Storyteller will make it obvious when the period of silence begins. It ends when voting begins.

                            - The player chosen by the nominee may vote for the nominee.
                            """,
                    """
                            - Alex is claiming to be the Sailor, and is nominated. Alex chooses Evin to speak on his behalf. Evin makes a convincing argument that Alex actually is the Sailor, and that Julian should be executed instead. Evin lives.

                            - Marianna is the Hermit, but has not told many people that she is the Hermit. When nominated, she chooses Alex to defend her. Alex says that Marianna is probably a Minion and that it doesn’t matter if Marianna dies or not. Alex dies due to the Big Wig ability.
                            """, "The Yogscast"),
            entry(Role.BOOTLEGGER,
                    "I've got the latest shipment from home, a brew I'd like to call 'Barrowfog'. Wanna try?",
                    """
                            Add the Bootlegger to include homebrew characters or rules.

                            - The Bootlegger allows Storytellers to use characters they, or others, have created that are not official characters or allows them to use non-standard rules in the game.

                            - If there are homebrew characters on the character sheet, or homebrew rules in effect, the Storyteller tells all players what they are before play begins.

                            - The Bootlegger allows for multiple characters or rules to be in effect at once.

                            - As long as there is at least one homebrew character on the current script, this Loric will be in play and can only be removed by switching to a script that does not contain any homebrew characters.

                            - The Bootlegger is designed for use in the official app only.

                            - Bootlegger, despite many claims to the contrary, defeated Homebrewy McHomebrewface, “the people’s choice”, in a hotly contested poll to decide the Loric's name.
                            """,
                    """
                            - The character sheet contains the homebrew character the Peasant. The Storyteller announces that the Bootlegger is in play and then explains how the Peasant works.

                            - The Storyteller has a homebrew or house rule. The Storyteller announces that the Bootlegger is in play and explains what the homebrew rule is and how it will affect the game.
                            """, "The Yogscast"),
            entry(Role.GARDENER,
                    "Oh now, this won't do. We've got the monkshood mixed in with the wolfsbane and the hemlock is smothering the hellebore! Oh dear me, we'd better start over. Fetch my shears.",
                    """
                            Use the Gardener to assign characters to particular players.

                            - After the Storyteller has put the Gardener into play, they can manually assign and edit which characters are going to be given to which seated players before sending them out.

                            - If a player has an issue with a particular character, you may use the Gardener to affect setup so that player doesn’t draw the relevant token.

                            - The Gardener can also be useful if a particular player has drawn evil many times over a single session.

                            - The Gardener is designed for use in the official app only.
                            """,
                    """
                            - The Vizier is on the script and Ida doesn’t like playing as outed evil. To ensure that Ida doesn’t receive the Vizier token, the Storyteller puts the Gardener into play and assigns Ida directly as the Poisoner instead.

                            - Robin has been evil every game today. The Storyteller puts the Gardener into play and assigns Robin directly as the Chef instead.
                            """, "(stock image)"),
            entry(Role.GOD_OF_UG,
                    "Blessed are my children, for they see the beauty in simple things.",
                    """
                            The God of Ug makes players speak one-syllable words only.

                            - When the player wearing the Ug hat votes, their vote counts as two votes. Use a physical hat if you can.

                            - Any word that has one syllable counts. Shenanigans with hyphenated words don’t count. Saying a single multisyllabic word is enough to lose the Ug hat.

                            - The Storyteller chooses who first wears the Ug hat. If a player says a multisyllabic word, or if the player takes too long to speak, or if the player isn’t having any fun, or if the player has been wearing the hat for too long, the Storyteller chooses a new player to wear the hat.

                            - Any player may wear the Ug hat. They may refuse it if they want. A player may have the Ug hat more than once.

                            - If the Storyteller is not present, the honor system is used. Any player who catches the player wearing the Ug hat saying multisyllabic words may say so, and the Ug hat is removed.

                            - The Storyteller may choose a new player each day to start with the Ug hat, or keep the same player as the previous day.
                            """,
                    """
                            - Amy is wearing the Ug hat. When nominated, she says, “It not me. I think Ben has been the talk with the bad guy. He bad.” Amy keeps the Ug hat.

                            - Lewis has the Ug hat. In a private conversation with Julian and Alex, Lewis says, “I think that you are both good. I am the bird thing that can see one guy when I die. I want the Demon to kill me so that I get the guy and know things to tell you in the day that is next.” The Storyteller gives Abdallah the Ug hat, because “Demon” is a multisyllabic word.
                            """, "The Yogscast"),
            entry(Role.HINDU,
                    "चत्वारो मृत्युमध्ये पतन्ति,\n" +
                            "चत्वारो यात्री पुनरुद्गताः।\n" +
                            "चत्वारो धर्मे स्थितचित्तवृत्तेः,\n" +
                            "चत्वार एषां न पुनः क्षयः॥",
                    """
                            The Hindu gives players that die early a new life.

                            - The first four players to die become Travellers.

                            - It doesn’t matter how the players died.

                            - The Storyteller chooses which Traveller the player becomes. This is different to the normal rule that players choose their Traveller. The players’ alignment stays the same as it was before death.

                            - If the script has five recommended Travellers, the Storyteller chooses from those.

                            - Death is never simultaneous. For example, if the Shabaloth kills two players at night, it kills one, then the other.

                            - If the Demon is one of the first four players to die, the game ends and good wins.
                            """,
                    """
                            - Julian is a Princess. He is executed and becomes the Gangster.

                            - Lachlan is the Sweetheart. He dies and becomes the Cacklejack. Nobody is drunk because the Sweetheart is no longer in play. Evin is the Engineer. He dies and becomes the Judge. That night, the Po kills Marianna, Abdallah, and Lewis. Marianna becomes the Gnome and Abdallah becomes the Harlot.
                            """, "Autumn Wind"),
            entry(Role.KNAVES,
                    "Every wall is a door. Every meal is a feast.",
                    """
                            The Knaves are two Storytellers.

                            - When the Knaves is in play, there are two Storytellers. When players get their information from character abilities, one Storyteller gives true information, and the other Storyteller gives false information.

                            - The players do not know which Storyteller is which. Each player chooses which Storyteller will give them information each time that they communicate.

                            - Storytellers always tell the truth when it comes to game rules, and non-character ability information, such as declaring who has been nominated, giving Demon bluffs, the Minion info step etc.

                            - If a player is drunk or poisoned, a Storyteller can tell the truth or lie.

                            - Once per game, usually when the good players start to figure out which Storyteller is which, the truth-telling Storyteller can become the lying Storyteller, and vice versa.
                            """,
                    """
                            - There are two Storytellers, Ben and Lachlan. The Chef wakes and chooses Lachlan, who is the lying Storyteller. The Chef learns a "3", even though there are only 2 pairs of evil players.

                            - Later that game, the Storytellers switch. The Savant visits Ben and Lachlan and chooses Lachlan. The Savant learns 2 pieces of true information.
                            """, "The Yogscast"),
            entry(Role.POPE,
                    "...Pulcherrimae.",
                    """
                            The Pope creates duplicate character claims.

                            - A Townsfolk or an Outsider, or both, have multiple copies in play.

                            - There may be one character that has multiple copies in play, or multiple characters that have multiple copies in play.

                            - There may be 2 characters that are the same, or as many as the Storyteller has tokens to accommodate.

                            - These characters might be a part of the 3 bluffs given to the Demon.

                            - The Storyteller will need multiple copies of the game, or at least some way to access identical character tokens, in order to run this character... for now.

                            - Duplicate Outsider characters may cause an unusual number of Outsiders. Duplicate Townsfolk characters may not.
                            """,
                    """
                            - Doug, Ben, and Sarah are all Empaths. Doug learns a “1”. Ben learns a “0”. Sarah learns a “2”.

                            - Lewis and Abdallah are Generals. Evin is the Drunk who thinks he is a General. Alex and Julian are Zealots, and Marianna is the Xaan bluffing as a Zealot.
                            """, "u/Cosix101"),
            entry(Role.STORM_CATCHER,
                    "At dawn, the temple’s long shadow creeps to the fountain. At dusk, the obelisk blocks the red glare, cooling warm water under the archway. All lines converge here. A storm is coming, and this, this pebbled and lush and holy place between the apple trees, is the eye.",
                    """
                            Use the Storm Catcher to focus the game on a particular good character.

                            - If you want to construct a script based around the actions or information of one particular good character, if you want to have this character in every game (or at least have an evil player bluffing as this character), you can use the Storm Catcher. Your chosen character will play a big part in the game, will be the focus of a lot of group discussion, and will probably live until the final day.

                            - The Storyteller declares that one character can’t die, unless by execution. This character may be in play, or not in play. If it is in play, this good player lives as long as the good players want them to, since evil players cannot kill them. If it is not in play, all evil players learn this, so any evil player can easily bluff as this character. (They don’t have to, but they can.)
                            """,
                    """
                            - The Storyteller has built a script based on the General, so says that “the Storm Catcher favours the General”. The General is in play. At night, the Imp and the Godfather both attack the General, who does not die. After 5 days of information and discussion, the town decides to execute the General. They die.

                            - The Storyteller wants a game based around the Empath’s information, so declares that “The Storm Catcher favours the Empath”. There is no Empath in play. Because the evil players learn this, the Poisoner chooses to bluff as the Empath.
                            """, "Autumn Wind"),
            entry(Role.TOR,
                    "With thunder as my voice and lightning as my blade, I, the eternal guardian, feast upon the fools who dare approach the forbidden gate. Behold, my sacred goal! To purge the beetle from the belly of the rocky earth, to ensnare it in a net of stars on the hilltop where heaven meets earth.",
                    """
                            Tor removes all knowledge of who is who.

                            - Players do not know which character they are.

                            - Players do not draw tokens from the bag. The Storyteller puts them directly in the Grimoire at the start of the game.

                            - Character abilities work as normal. Players are woken and prompted to use their ability if needed.

                            - When a player dies, they learn their character and alignment. Even if they are drunk or poisoned, this information is correct.

                            - The Demon and Minions do not know each other.
                            """,
                    """
                            Julian, Alex, Evin, Lachlan, Sarah, Marianna and Amy do not know which character they are. Evin wakes each night and learns a thumbs up, but does not know why. Sarah wakes each night to choose a player, but does not know why. Amy wakes each night to choose a player, but does not know why. Sarah is executed and learns that she is the good Monk. Lachlan, guessing that he might be the Slayer, publicly says that he is the Slayer, and chooses Amy. Amy dies. Evin was the General, Lachlan was the Slayer and Amy was the Imp.
                            """, "Autumn Wind"),
            entry(Role.VENTRILOQUIST,
                    "Well, folks, gather ‘round! This here’s my pal Charlie, and he’s got a mouth on him that’d make a mule blush. But don’t worry folks, I do all the talkin’... or do I?",
                    """
                            The Ventriloquist rewards players for lying about who they are.

                            - Being "mad as a fresh character" means to claim to be a character that is different to a character that you have previously claimed to be.

                            - A player benefits from the Ventriloquist only if they are mad as a fresh character during the time that they are nominated.

                            - It doesn’t matter what their real character is. Players may be mad as a fake character then later mad as their real character, mad as their real character then later mad as a fake character, or mad as two fake characters.

                            - If the player does not die, they learn this after the execution happens. They don’t learn whether it was due to the Ventriloquist or not.

                            - The Storyteller judges whether the player is mad or not, and might let them die even if they were convincingly mad.

                            - This can protect the Demon, but the Storyteller will not protect the Demon if that means that evil wins.

                            - A player is only protected from dying on the same day that they were mad as a fresh character.
                            """,
                    """
                            - Abdallah is the Gambler. On the 1st day, he is mad that he is the Gambler. When nominated, he is mad that he is the Hatter. Later that day, Abdallah is executed and does not die.

                            - Marianna is the Professor. She has been claiming to be the Empath all game. When nominated, she claims to be the Huntsman. When executed, Marianna does not die.

                            - Julian is the Hermit. Julian is quiet for 3 days. When nominated, Julian is mad as the Knight, and says that Evin and Alex are not the Demon. When executed, Julian dies, because he was not mad as a fresh character.
                            """, "Autumn Wind"),
            entry(Role.ZENOMANCER,
                    "The universe is a verb not a noun, they say, and it is turtles, turtles all the way down. Turtles all the way down, my friend, turtles all the way down.",
                    """
                            The Zenomancer gives mini quests.

                            - One or more players may be given goals by the Storyteller. These goals are given privately. They may be given goals at the beginning of the game, or at some time during the game. Different players may be given goals at different times.

                            - A goal can be anything. It can be something to do with the game, or something beyond the game.

                            - The Storyteller is the judge of when a goal is achieved. When this happens, the player learns one piece of information about the game. This is true even if they are drunk or poisoned, since this is due to the Zenomancer, not their character.

                            - Goals may clash, or may not be achieved.

                            - Usually, 1 to 3 goals will be given throughout the game, and most goals will be given at the beginning of the game.

                            - Players may bluff that they have been given goals, or bluff that they have achieved a goal.

                            - Any player may be given a goal, even Travellers and dead players.
                            """,
                    """
                            - Alex is the Pit-Hag. He is given the goal “create an evil Outsider”. Alex turns the Witch into the Sweetheart, and learns that Lewis is the drunk Sage.

                            - Evin is given the goal “convince Sarah that she is the Marionette”. The Storyteller judges that Sarah is adequately confused about whether she is the Marionette or not. Evin learns that there is no Marionette in play. Meanwhile, Ben has the goal “nominate and execute a Minion”. This does not happen, and Ben does not get his Zenomancer information.

                            - Julian has the goal to “steal all the comfy pillows from the couch, without touching them”. Amy has the same goal. Amy convinces Marianna to engage Julian in a private chat while Abdallah steals the pillows and gives them to her. Amy learns that the Demon is a Fang Gu.
                            """, "Autumn Wind")

    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Helper to make the map initialization cleaner
    private static Map.Entry<Role, RoleDetailData> entry(Role role, String flavor, String summary, String examples, String artist) {
        return Map.entry(role, new RoleDetailData(flavor, summary, examples, artist));
    }

    public static RoleDetailData get(Role role) {
        return DETAILS_MAP.getOrDefault(role, new RoleDetailData("Not found.", "No summary available.", "No examples available.", "Unknown"));
    }
}
