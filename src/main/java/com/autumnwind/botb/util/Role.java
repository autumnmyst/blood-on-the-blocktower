package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum Role {
    NO_ROLE("null role used to unassign a player's role.", RoleType.NONE, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/no_role.png")),

    // --- TOWNSFOLK ---

    //TB
    CHEF("You start knowing how many pairs of evil players there are.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/chef.png")),
    INVESTIGATOR("You start knowing that 1 of 2 players is a particular Minion.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/investigator.png")),
    WASHERWOMAN("You start knowing that 1 of 2 players is a particular Townsfolk.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/washerwoman.png")),
    LIBRARIAN("You start knowing that 1 of 2 players is a particular Outsider. (Or that zero are in play.)", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/librarian.png")),
    EMPATH("Each night, you learn how many of your 2 alive neighbors are evil.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/empath.png")),
    FORTUNE_TELLER("Each night, choose 2 players: you learn if either is a Demon. There is a good player that registers as a Demon to you.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/fortune_teller.png")),
    UNDERTAKER("Each night*, you learn which character died by execution today.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/undertaker.png")),
    MONK("Each night*, choose a player (not yourself): they are safe from the Demon tonight.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/monk.png")),
    SLAYER("Once per game, during the day, publicly choose a player: if they are the Demon, they die.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/slayer.png")),
    SOLDIER("You are safe from the Demon.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/soldier.png")),
    RAVENKEEPER("If you die at night, you are woken to choose a player: you learn their character.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/ravenkeeper.png")),
    VIRGIN("The 1st time you are nominated, if the nominator is a Townsfolk, they are executed immediately.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/virgin.png")),
    MAYOR("If only 3 players live & no execution occurs, your team wins. If you die at night, another player might die instead.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/mayor.png")),
    //BMR
    GRANDMOTHER("You start knowing a good player & their character. If the Demon kills them, you die too.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/grandmother.png")),
    SAILOR("Each night, choose an alive player: either you or they are drunk until dusk. You can't die.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/sailor.png")),
    CHAMBERMAID("Each night, choose 2 alive players (not yourself): you learn how many woke tonight due to their ability.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/chambermaid.png")),
    INNKEEPER("Each night*, choose 2 players: they can't die tonight, but 1 is drunk until dusk.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/innkeeper.png")),
    GAMBLER("Each night*, choose a player & guess their character: if you guess wrong, you die.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/gambler.png")),
    EXORCIST("Each night*, choose a player (different to last night): the Demon, if chosen, learns who you are then doesn't wake tonight.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/exorcist.png")),
    GOSSIP("Each day, you may make a public statement. Tonight, if it was true, a player dies.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/gossip.png")),
    COURTIER("Once per game, at night, choose a character: they are drunk for 3 nights & 3 days.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/courtier.png")),
    PROFESSOR("Once per game, at night*, choose a dead player: if they are a Townsfolk, they are resurrected.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/professor.png")),
    MINSTREL("When a Minion dies by execution, all other players are drunk until dusk tomorrow.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/minstrel.png")),
    TEA_LADY("If both your alive neighbors are good, they can't die.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/tea_lady.png")),
    FOOL("The 1st time you die, you don't.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/fool.png")),
    PACIFIST("Executed good players might not die.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/pacifist.png")),
    //S&V
    CLOCKMAKER("You start knowing how many steps from the Demon to its nearest Minion.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/clockmaker.png")),
    DREAMER("Each night, choose a player (not yourself): you learn 1 good & 1 evil character, 1 of which is correct.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/dreamer.png")),
    SNAKE_CHARMER("Each night, choose an alive player: a chosen Demon swaps characters & alignments with you & is then poisoned.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/snake_charmer.png")),
    MATHEMATICIAN("Each night, you learn how many players' abilities worked abnormally (since dawn) due to another character's ability.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/mathematician.png")),
    FLOWERGIRL("Each night*, you learn if a Demon voted today.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/flowergirl.png")),
    TOWN_CRIER("Each night*, you learn if a Minion nominated today.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/town_crier.png")),
    ORACLE("Each night*, you learn how many dead players are evil.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/oracle.png")),
    SAVANT("Each night, you learn 2 things in private: 1 is true & 1 is false.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/savant.png")),
    SEAMSTRESS("Once per game, at night, choose 2 players (not yourself): you learn if they are the same alignment.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/seamstress.png")),
    PHILOSOPHER("Once per game, at night, choose a good character: gain that ability. If this character is in play, they are drunk.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/philosopher.png")),
    ARTIST("Once per game, at night, ask the Storyteller any yes/no question.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/artist.png")),
    JUGGLER("On your 1st day, publicly guess up to 5 players' characters. That night, you learn how many you got correct.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/juggler.png")),
    SAGE("If the Demon kills you, you learn that it is 1 of 2 players.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/sage.png")),
    //Kickstarter
    NOBLE("You start knowing 3 players, 1 and only 1 of which is evil.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/noble.png")),
    PIXIE("You start knowing 1 in-play Townsfolk. If you were mad that you were this character, you gain their ability when they die.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/pixie.png")),
    GENERAL("Each night, you learn which alignment the Storyteller believes is winning: good, evil, or neither.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/general.png")),
    KING("Each night, if the dead equal or outnumber the living, you learn 1 alive character. The Demon knows you are the King.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/king.png")),
    LYCANTHROPE("Each night*, choose an alive player. If good, they die & the Demon doesn't kill tonight. One good player registers as evil.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/lycanthrope.png")),
    ENGINEER("Once per game, at night, choose which Minions or which Demon is in play.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/engineer.png")),
    HUNTSMAN("Once per game, at night, choose a living player: the Damsel, if chosen, becomes a not-in-play Townsfolk. [+the Damsel]", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/huntsman.png")),
    ALCHEMIST("You have a Minion ability. When using this, the Storyteller may prompt you to choose differently.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/alchemist.png")),
    CANNIBAL("You have the ability of the recently killed executee. If they are evil, you are poisoned until a good player dies by execution.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/cannibal.png")),
    AMNESIAC("You do not know what your ability is. Each day, privately guess what it is: you learn how accurate you are.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/amnesiac.png")),
    FARMER("When you die at night, an alive good player becomes a Farmer.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/farmer.png")),
    CHOIRBOY("If the Demon kills the King, you learn which player is the Demon. [+the King]", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/choirboy.png")),
    MAGICIAN("The Demon thinks you are a Minion. Minions think you are a Demon.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/magician.png")),
    POPPY_GROWER("Minions & Demons do not know each other. If you die, they learn who each other are that night.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/poppy_grower.png")),
    ATHEIST("The Storyteller can break the game rules, and if executed, good wins, even if you are dead. [No evil characters]", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/atheist.png")),
    //Carousel
    STEWARD("You start knowing 1 good player.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/steward.png")),
    KNIGHT("You start knowing 2 players that are not the Demon.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/knight.png")),
    SHUGENJA("You start knowing if your closest evil player is clockwise or anti-clockwise. If equidistant, this info is arbitrary.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/shugenja.png")),
    BOUNTY_HUNTER("You start knowing 1 evil player. If the player you know dies, you learn another evil player tonight. [1 Townsfolk is evil]", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/bounty_hunter.png")),
    HIGH_PRIESTESS("Each night, learn which player the Storyteller believes you should talk to most.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/high_priestess.png")),
    BALLOONIST("Each night, you learn a player of a different character type than last night. [+0 or +1 Outsider]", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/balloonist.png")),
    PREACHER("Each night, choose a player: a Minion, if chosen, learns this. All chosen Minions have no ability.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/preacher.png")),
    VILLAGE_IDIOT("Each night, choose a player: you learn their alignment. [+0 to +2 Village Idiots. 1 of the extras is drunk]", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/village_idiot.png")),
    CULT_LEADER("Each night, you become the alignment of an alive neighbor. If all good players choose to join your cult, your team wins.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/cult_leader.png")),
    ACROBAT("Each night*, choose a player: if they are or become drunk or poisoned tonight, you die.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/acrobat.png")),
    ALSAAHIR("Each day, if you publicly guess which players are Minion(s) and which are Demon(s), good wins.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/alsaahir.png")),
    NIGHTWATCHMAN("Once per game, at night, choose a player: they learn you are the Nightwatchman.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/nightwatchman.png")),
    FISHERMAN("Once per game, at night, ask the Storyteller for some advice to help your team win.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/fisherman.png")),
    PRINCESS("On your 1st day, if you nominated & executed a player, the Demon doesn't kill tonight.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/princess.png")),
    BANSHEE("If the Demon kills you, all players learn this. From now on, you may nominate twice per day and vote twice per nomination.", RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/banshee.png")),

    // --- OUTSIDERS ---

    //TB
    BUTLER("Each night, choose a player (not yourself): tomorrow, you may only vote if they are voting too.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/butler.png")),
    SAINT("If you die by execution, your team loses.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/saint.png")),
    RECLUSE("You might register as evil & as a Minion or Demon, even if dead.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/recluse.png")),
    DRUNK("You do not know you are the Drunk. You think you are a Townsfolk character, but you are not.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/drunk.png")),
    //BMR
    GOON("Each night, the 1st player to choose you with their ability is drunk until dusk.You become their alignment.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/goon.png")),
    LUNATIC("You think you are a Demon, but you are not. The Demon knows who you are & who you choose at night.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/lunatic.png")),
    TINKER("You might die at any time.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/tinker.png")),
    MOONCHILD("When you learn that you died, publicly choose 1 alive player. Tonight, if it was a good player, they die.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/moonchild.png")),
    //S&V
    MUTANT("If you are \"mad\" about being an Outsider, you might be executed.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/mutant.png")),
    BARBER("If you died today or tonight, the Demon may choose 2 players (not another Demon) to swap characters.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/barber.png")),
    SWEETHEART("When you die, 1 player is drunk from now on.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/sweetheart.png")),
    KLUTZ("When you learn that you died, publicly choose 1 alive player: if they are evil, your team loses.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/klutz.png")),
    //Kickstarter
    GOLEM("You may only nominate once per game. When you do, if the nominee is not the Demon, they die.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/golem.png")),
    DAMSEL("All Minions know a Damsel is in play. If a Minion publicly guesses you (once), your team loses.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/damsel.png")),
    SNITCH("Each Minion gets 3 bluffs.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/snitch.png")),
    HERETIC("Whoever wins, loses & whoever loses, wins, even if you are dead.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/heretic.png")),
    PUZZLEMASTER("1 player is drunk, even if you die. If you guess (once) who it is, learn the Demon player, but guess wrong & get false info.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/puzzlemaster.png")),
    //Carousel
    HERMIT("You have all Outsider abilities. [-0 or -1 Outsider]", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/hermit.png")),
    OGRE("On your 1st night, choose a player (not yourself): you become their alignment (you don't know which) even if drunk or poisoned.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/ogre.png")),
    PLAGUE_DOCTOR("When you die, the Storyteller gains a Minion ability.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/plague_doctor.png")),
    HATTER("If you died today or tonight, the Minion & Demon players may choose new Minion & Demon characters to be.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/hatter.png")),
    POLITICIAN("If you were the player most responsible for your team losing, you change alignment & win, even if dead.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/politician.png")),
    ZEALOT("If there are 5 or more players alive, you must vote for every nomination.", RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/zealot.png")),

    // --- MINIONS ---

    //TB
    POISONER("Each night, choose a player: they are poisoned tonight and tomorrow day.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/poisoner.png")),
    SPY("Each night, you see the Grimoire. You might register as good & as a Townsfolk or Outsider, even if dead.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/spy.png")),
    BARON("There are extra Outsiders in play. [+2 Outsiders]", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/baron.png")),
    SCARLET_WOMAN("If there are 5 or more players alive & the Demon dies, you become the Demon.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/scarlet_woman.png")),
    //BMR
    GODFATHER("You start knowing which Outsiders are in play. If 1 died today, choose a player tonight: they die. [-1 or +1 Outsider]", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/godfather.png")),
    DEVILS_ADVOCATE("Each night, choose a living player (different to last night): if executed tomorrow, they don't die.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/devils_advocate.png")),
    ASSASSIN("Once per game, at night*, choose a player: they die, even if for some reason they could not.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/assassin.png")),
    MASTERMIND("If the Demon dies by execution (ending the game), play for 1 more day. If a player is then executed, their team loses.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/mastermind.png")),
    //S&V
    WITCH("Each night, choose a player: if they nominate tomorrow, they die. If just 3 players live, you lose this ability.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/witch.png")),
    CERENOVUS("Each night, choose a player & a good character: they are \"mad\" they are this character tomorrow, or might be executed.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/cerenovus.png")),
    PIT_HAG("Each night*, choose a player & a character they become (if not in play). If a Demon is made, deaths tonight are arbitrary.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/pit_hag.png")),
    EVIL_TWIN("You & an opposing player know each other. If the good player is executed, evil wins. Good can't win if you both live.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/evil_twin.png")),
    //Kickstarter
    MEZEPHELES("You start knowing a secret word. The 1st good player to say this word becomes evil that night.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/mezepheles.png")),
    FEARMONGER("Each night, choose a player: if you nominate & execute them, their team loses. All players know if you choose a new player.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/fearmonger.png")),
    PSYCHOPATH("Each day, before nominations, you may publicly choose a player: they die. If executed, you only die if you lose roshambo.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/psychopath.png")),
    MARIONETTE("You think you are a good character, but you are not. The Demon knows who you are. [You neighbor the Demon]", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/marionette.png")),
    BOOMDANDY("If you are executed, all but 3 players die. After a 10 to 1 countdown, the player with the most players pointing at them, dies.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/boomdandy.png")),
    //Carousel
    HARPY("Each night, choose 2 players: tomorrow, the 1st player is mad that the 2nd is evil, or one or both might die.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/harpy.png")),
    WIZARD("Once per game, choose to make a wish. If granted, it might have a price & leave a clue as to its nature.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/wizard.png")),
    WIDOW("On your 1st night, look at the Grimoire & choose a player: they are poisoned. 1 good player knows a Widow is in play.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/widow.png")),
    XAAN("On night X, all Townsfolk are poisoned until dusk. [X Outsiders]", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/xaan.png")),
    WRAITH("You may choose to visit other players at night. You visit other evil players when they wake.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/wraith.png")),
    SUMMONER("You get 3 bluffs. On the 3rd night, choose a player: they become an evil Demon of your choice. [No Demon]", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/summoner.png")),
    GOBLIN("If you publicly claim to be the Goblin when nominated & are executed that day, your team wins.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/goblin.png")),
    VIZIER("All players know you are the Vizier. You cannot die during the day. If good voted, you may choose to execute immediately.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/vizier.png")),
    ORGAN_GRINDER("All players keep their eyes closed when voting and the vote tally is secret. Each night, choose if you are drunk until dusk.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/organ_grinder.png")),
    BOFFIN("The Demon (even if drunk or poisoned) has a not-in-play good character's ability. You both know which.", RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/boffin.png")),

    // --- DEMONS ---

    //TB
    IMP("Each night*, choose a player: they die. If you kill yourself this way, a Minion becomes the Imp.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/imp.png")),
    //BMR
    PUKKA("Each night, choose a player: they are poisoned. The previously poisoned player dies then becomes healthy.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/pukka.png")),
    SHABALOTH("Each night*, choose 2 players: they die. A dead player you chose last night might be regurgitated.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/shabaloth.png")),
    PO("Each night*, you may choose a player: they die. If your last choice was no-one, choose 3 players tonight.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/po.png")),
    ZOMBUUL("Each night*, if no-one died today, choose a player: they die. The 1st time you die, you live but register as dead.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/zombuul.png")),
    //S&V
    FANG_GU("Each night*, choose a player: they die. The 1st Outsider this kills becomes an evil Fang Gu & you die instead. [+1 Outsider]", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/fang_gu.png")),
    VIGORMORTIS("Each night*, choose a player: they die. Minions you kill keep their ability & poison 1 Townsfolk neighbor. [-1 Outsider]", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/vigormortis.png")),
    NO_DASHII("Each night*, choose a player: they die. Your 2 Townsfolk neighbors are poisoned.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/no_dashii.png")),
    VORTOX("Each night*, choose a player: they die. Townsfolk abilities yield false info. Each day, if no-one is executed, evil wins.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/vortox.png")),
    //Kickstarter
    LEGION("Each night*, a player might die. Executions fail if only evil voted. You register as a Minion too. [Most players are Legion]", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/legion.png")),
    LLEECH("Each night*, choose a player: they die. You start by choosing a player: they are poisoned. You die if & only if they are dead.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/lleech.png")),
    AL_HADIKHIA("Each night*, you may choose 3 players (all players learn who): each silently chooses to live or die, but if all live, all die.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/al_hadikhia.png")),
    RIOT("On day 3, Minions become Riot & nominees die but nominate an alive player immediately. This must happen.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/riot.png")),
    LEVIATHAN("If more than 1 good player is executed, evil wins. All players know you are in play. After day 5, evil wins.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/leviathan.png")),
    //Carousel
    YAGGABABBLE("You start knowing a secret phrase. For each time you said it publicly today, a player might die.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/yaggababble.png")),
    LIL_MONSTA("Each night, Minions choose who babysits Lil' Monsta & \"is the Demon\". Each night*, a player might die. [+1 Minion]", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/lil_monsta.png")),
    KAZALI("Each night*, choose a player: they die. [You choose which players are which Minions. -? to +? Outsiders]", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/kazali.png")),
    OJO("Each night*, choose a character: they die. If they are not in play, the Storyteller chooses who dies.", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/ojo.png")),
    LORD_OF_TYPHON("Each night*, choose a player: they die. [Evil characters are in a line. You are in the middle. +1 Minion. -? to +? Outsiders]", RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/lord_of_typhon.png")),

    // --- TRAVELERS ---

    //TB
    THIEF("Each night, choose a player (not yourself): their vote counts negatively tomorrow.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/thief.png")),
    BUREAUCRAT("Each night, choose a player (not yourself): their vote counts as 3 votes tomorrow.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/bureaucrat.png")),
    GUNSLINGER("Each day, after the 1st vote has been tallied, you may choose a player that voted: they die.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/gunslinger.png")),
    SCAPEGOAT("If a player of your alignment is executed, you might be executed instead.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/scapegoat.png")),
    BEGGAR("You must use a vote token to vote. If a dead player gives you theirs, you learn their alignment. You are sober & healthy.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/beggar.png")),
    //BMR
    APPRENTICE("On your 1st night, you gain a Townsfolk ability (if good), or a Minion ability (if evil).", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/apprentice.png")),
    MATRON("Each day, you may choose up to 3 sets of 2 players to swap seats. Players may not leave their seats to talk in private.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/matron.png")),
    JUDGE("Once per game, if another player nominated, you may choose to force the current execution to pass or fail.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/judge.png")),
    VOUDON("Only you & the dead can vote. They don't need a vote token to do so. A 50% majority isn't required.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/voudon.png")),
    BISHOP("Only the Storyteller can nominate. At least 1 opposing player must be nominated each day.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/bishop.png")),
    //S&V
    BARISTA("Each night, until dusk, 1) a player becomes sober, healthy & gets true info, or 2) their ability works twice. They learn which.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/barista.png")),
    HARLOT("Each night*, choose a living player: if they agree, you learn their character, but you both might die.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/harlot.png")),
    BUTCHER("Each day, after the 1st execution, you may nominate again.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/butcher.png")),
    DEVIANT("If you were funny today, you cannot die by exile.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/deviant.png")),
    BONE_COLLECTOR("Once per game, at night*, choose a dead player: they regain their ability until dusk.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/bone_collector.png")),
    //Carousel
    CACKLEJACK("Each day, choose a player: a different player changes character tonight.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/cacklejack.png")),
    GANGSTER("Once per day, you may choose to kill an alive neighbor, if your other alive neighbor agrees.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/gangster.png")),
    GNOME("All players start knowing a player of your alignment. You may choose to kill anyone who nominates them.", RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/gnome.png")),

    // --- FABLED ---

    ANGEL("Something bad might happen to whoever is most responsible for the death of a new player.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/angel.png")),
    BUDDHIST("For the first 2 minutes of each day, veteran players may not talk.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/buddhist.png")),
    DEUS_EX_FIASCO("At least once per game, the Storyteller will make a mistake, correct it, and publicly admit to it.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/deus_ex_fiasco.png")),
    DJINN("Use the Djinn's special rule. All players know what it is.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/djinn.png")),
    DOOMSAYER("If 4 or more players live, each living player may publicly choose (once per game) that a player of their own alignment dies.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/doomsayer.png")),
    DUCHESS("Each day, 3 players may choose to visit you. At night*, each visitor learns how many visitors are evil, but 1 gets false info.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/duchess.png")),
    FERRYMAN("On the final day, all dead players regain their vote token.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/ferryman.png")),
    FIBBIN("Once per game, 1 good player might get incorrect information.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/fibbin.png")),
    FIDDLER("Once per game, the Demon secretly chooses an opposing player: all players choose which of these 2 players win.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/fiddler.png")),
    HELLS_LIBRARIAN("Something bad might happen to whoever talks when the Storyteller has asked for silence.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/hells_librarian.png")),
    REVOLUTIONARY("2 neighboring players are known to be the same alignment. Once per game, 1 of them registers falsely.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/revolutionary.png")),
    SENTINEL("There might be 1 extra or 1 fewer Outsider in play.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/sentinel.png")),
    SPIRIT_OF_IVORY("There can't be more than 1 extra evil player.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/spirit_of_ivory.png")),
    TOYMAKER("The Demon may choose not to attack & must do this at least once per game. Evil players get normal starting info.", RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/toymaker.png")),

    // --- LORIC ---

    BIG_WIG("Each nominee chooses a player: until voting, only they may speak & they are mad the nominee is good or they might die.", RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/big_wig.png")),
    BOOTLEGGER("This script has homebrew characters or rules.", RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/bootlegger.png")),
    GARDENER("The Storyteller assigns 1 or more players' characters.", RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/gardener.png")),
    GOD_OF_UG("One Ug hat. When wear Ug hat, must speak one sound at a time but vote twice. If fail, pass Ug hat.", RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/god_of_ug.png")),
    HINDU("The first 4 players to die are immediately reincarnated as Travellers of the same alignment.", RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/hindu.png")),
    KNAVES("There are 2 Storytellers: one lies & one tells the truth. Once per game, at dusk, they might switch.", RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/knaves.png")),
    POPE("There are duplicate good characters in play. They might also be bluffs.", RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/pope.png")),
    STORM_CATCHER("Name a good character. If in play, they can only die by execution, but evil players learn which player it is.", RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/storm_catcher.png")),
    TOR("Players don't know their character or alignment. They learn them when they die.", RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/tor.png")),
    VENTRILOQUIST("If a player is mad as a fresh character during their nomination, they might not die if executed today.", RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/ventriloquist.png")),
    ZENOMANCER("One or more players each have a goal. When achieved, that player learns a piece of true info.", RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/zenomancer.png"));


    /** A pre-filtered, unmodifiable list of all roles that can be assigned to a player. */
    public static final List<Role> SELECTABLE_ROLES;

    /**
     * Roles whose own rules let them appear multiple times during setup. Skipped by the
     * setup-time duplicate check (Village Idiot: "[+0 to +2 Village Idiots]", Legion: "[Most players are Legion]").
     */
    public static final Set<Role> ALLOWS_DUPLICATES_AT_SETUP =
            Collections.unmodifiableSet(EnumSet.of(VILLAGE_IDIOT, LEGION));

    static {
        // This block runs once when the class is loaded.
        SELECTABLE_ROLES = Arrays.stream(values())
                .filter(role -> role != NO_ROLE) // Filter out NO_ROLE
                .collect(Collectors.toUnmodifiableList()); // Store it in a permanent list
    }

    // networking
    public static final PacketCodec<ByteBuf, Role> PACKET_CODEC =
            PacketCodecs.STRING.xmap(
                    Role::valueOf, // Decode from string to enum
                    Role::name    // Encode from enum to string
            );

    private final String description;
    private final RoleType type;
    private final Identifier icon;

    Role(String description, RoleType type, Identifier icon) {
        this.description = description;
        this.type = type;
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    /** Script-file id: lowercase name without underscores, e.g. SCARLET_WOMAN -> scarletwoman. */
    public String getId() {
        return name().toLowerCase(Locale.ROOT).replace("_", "");
    }

    public RoleType getType() {
        return type;
    }

    public boolean isDefaultGood() {
        return type.isDefaultGood();
    }

    public Identifier getIcon() {
        return icon;
    }

    /**
     * Returns a formatted display name for this role with proper punctuation.
     * Converts enum names like FORTUNE_TELLER to "FORTUNE TELLER" (all caps).
     */
    public String getDisplayName() {
        // Handle special cases with punctuation
        return switch (this) {
            case DEVILS_ADVOCATE -> "DEVIL'S ADVOCATE";
            case PIT_HAG -> "PIT-HAG";
            case AL_HADIKHIA -> "AL-HADIKHIA";
            case LIL_MONSTA -> "LIL' MONSTA";
            case HELLS_LIBRARIAN -> "HELL'S LIBRARIAN";
            case NO_ROLE -> "No Role";
            default -> {
                // Convert FORTUNE_TELLER to "FORTUNE TELLER" (all caps with spaces)
                String name = this.name();
                StringBuilder result = new StringBuilder();

                for (char c : name.toCharArray()) {
                    if (c == '_') {
                        result.append(' ');
                    } else {
                        result.append(Character.toUpperCase(c));
                    }
                }

                yield result.toString();
            }
        };
    }

}
