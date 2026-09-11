package com.autumnwind.botb.util;

import com.autumnwind.botb.BloodOnTheBlocktower;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public enum Role {
    NO_ROLE(RoleType.NONE, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/no_role.png")),

    // --- TOWNSFOLK ---

    //TB
    CHEF(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/chef.png")),
    INVESTIGATOR(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/investigator.png")),
    WASHERWOMAN(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/washerwoman.png")),
    LIBRARIAN(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/librarian.png")),
    EMPATH(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/empath.png")),
    FORTUNE_TELLER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/fortune_teller.png")),
    UNDERTAKER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/undertaker.png")),
    MONK(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/monk.png")),
    SLAYER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/slayer.png")),
    SOLDIER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/soldier.png")),
    RAVENKEEPER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/ravenkeeper.png")),
    VIRGIN(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/virgin.png")),
    MAYOR(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/mayor.png")),
    //BMR
    GRANDMOTHER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/grandmother.png")),
    SAILOR(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/sailor.png")),
    CHAMBERMAID(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/chambermaid.png")),
    INNKEEPER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/innkeeper.png")),
    GAMBLER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/gambler.png")),
    EXORCIST(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/exorcist.png")),
    GOSSIP(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/gossip.png")),
    COURTIER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/courtier.png")),
    PROFESSOR(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/professor.png")),
    MINSTREL(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/minstrel.png")),
    TEA_LADY(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/tea_lady.png")),
    FOOL(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/fool.png")),
    PACIFIST(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/pacifist.png")),
    //S&V
    CLOCKMAKER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/clockmaker.png")),
    DREAMER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/dreamer.png")),
    SNAKE_CHARMER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/snake_charmer.png")),
    MATHEMATICIAN(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/mathematician.png")),
    FLOWERGIRL(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/flowergirl.png")),
    TOWN_CRIER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/town_crier.png")),
    ORACLE(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/oracle.png")),
    SAVANT(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/savant.png")),
    SEAMSTRESS(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/seamstress.png")),
    PHILOSOPHER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/philosopher.png")),
    ARTIST(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/artist.png")),
    JUGGLER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/juggler.png")),
    SAGE(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/sage.png")),
    //Kickstarter
    NOBLE(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/noble.png")),
    PIXIE(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/pixie.png")),
    GENERAL(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/general.png")),
    KING(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/king.png")),
    LYCANTHROPE(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/lycanthrope.png")),
    ENGINEER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/engineer.png")),
    HUNTSMAN(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/huntsman.png")),
    ALCHEMIST(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/alchemist.png")),
    CANNIBAL(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/cannibal.png")),
    AMNESIAC(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/amnesiac.png")),
    FARMER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/farmer.png")),
    CHOIRBOY(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/choirboy.png")),
    MAGICIAN(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/magician.png")),
    POPPY_GROWER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/poppy_grower.png")),
    ATHEIST(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/atheist.png")),
    //Carousel
    STEWARD(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/steward.png")),
    KNIGHT(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/knight.png")),
    SHUGENJA(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/shugenja.png")),
    BOUNTY_HUNTER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/bounty_hunter.png")),
    HIGH_PRIESTESS(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/high_priestess.png")),
    BALLOONIST(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/balloonist.png")),
    PREACHER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/preacher.png")),
    VILLAGE_IDIOT(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/village_idiot.png")),
    CULT_LEADER(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/cult_leader.png")),
    ACROBAT(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/acrobat.png")),
    ALSAAHIR(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/alsaahir.png")),
    NIGHTWATCHMAN(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/nightwatchman.png")),
    FISHERMAN(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/fisherman.png")),
    PRINCESS(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/princess.png")),
    BANSHEE(RoleType.TOWNSFOLK, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/banshee.png")),

    // --- OUTSIDERS ---

    //TB
    BUTLER(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/butler.png")),
    SAINT(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/saint.png")),
    RECLUSE(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/recluse.png")),
    DRUNK(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/drunk.png")),
    //BMR
    GOON(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/goon.png")),
    LUNATIC(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/lunatic.png")),
    TINKER(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/tinker.png")),
    MOONCHILD(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/moonchild.png")),
    //S&V
    MUTANT(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/mutant.png")),
    BARBER(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/barber.png")),
    SWEETHEART(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/sweetheart.png")),
    KLUTZ(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/klutz.png")),
    //Kickstarter
    GOLEM(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/golem.png")),
    DAMSEL(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/damsel.png")),
    SNITCH(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/snitch.png")),
    HERETIC(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/heretic.png")),
    PUZZLEMASTER(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/puzzlemaster.png")),
    //Carousel
    HERMIT(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/hermit.png")),
    OGRE(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/ogre.png")),
    PLAGUE_DOCTOR(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/plague_doctor.png")),
    HATTER(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/hatter.png")),
    POLITICIAN(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/politician.png")),
    ZEALOT(RoleType.OUTSIDER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/zealot.png")),

    // --- MINIONS ---

    //TB
    POISONER(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/poisoner.png")),
    SPY(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/spy.png")),
    BARON(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/baron.png")),
    SCARLET_WOMAN(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/scarlet_woman.png")),
    //BMR
    GODFATHER(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/godfather.png")),
    DEVILS_ADVOCATE(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/devils_advocate.png")),
    ASSASSIN(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/assassin.png")),
    MASTERMIND(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/mastermind.png")),
    //S&V
    WITCH(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/witch.png")),
    CERENOVUS(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/cerenovus.png")),
    PIT_HAG(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/pit_hag.png")),
    EVIL_TWIN(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/evil_twin.png")),
    //Kickstarter
    MEZEPHELES(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/mezepheles.png")),
    FEARMONGER(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/fearmonger.png")),
    PSYCHOPATH(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/psychopath.png")),
    MARIONETTE(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/marionette.png")),
    BOOMDANDY(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/boomdandy.png")),
    //Carousel
    HARPY(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/harpy.png")),
    WIZARD(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/wizard.png")),
    WIDOW(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/widow.png")),
    XAAN(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/xaan.png")),
    WRAITH(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/wraith.png")),
    SUMMONER(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/summoner.png")),
    GOBLIN(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/goblin.png")),
    VIZIER(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/vizier.png")),
    ORGAN_GRINDER(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/organ_grinder.png")),
    BOFFIN(RoleType.MINION, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/boffin.png")),

    // --- DEMONS ---

    //TB
    IMP(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/tb/imp.png")),
    //BMR
    PUKKA(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/pukka.png")),
    SHABALOTH(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/shabaloth.png")),
    PO(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/po.png")),
    ZOMBUUL(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/bmr/zombuul.png")),
    //S&V
    FANG_GU(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/fang_gu.png")),
    VIGORMORTIS(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/vigormortis.png")),
    NO_DASHII(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/no_dashii.png")),
    VORTOX(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/snv/vortox.png")),
    //Kickstarter
    LEGION(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/legion.png")),
    LLEECH(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/lleech.png")),
    AL_HADIKHIA(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/al_hadikhia.png")),
    RIOT(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/riot.png")),
    LEVIATHAN(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/kickstarter/leviathan.png")),
    //Carousel
    YAGGABABBLE(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/yaggababble.png")),
    LIL_MONSTA(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/lil_monsta.png")),
    KAZALI(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/kazali.png")),
    OJO(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/ojo.png")),
    LORD_OF_TYPHON(RoleType.DEMON, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/carousel/lord_of_typhon.png")),

    // --- TRAVELERS ---

    //TB
    THIEF(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/thief.png")),
    BUREAUCRAT(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/bureaucrat.png")),
    GUNSLINGER(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/gunslinger.png")),
    SCAPEGOAT(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/scapegoat.png")),
    BEGGAR(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/beggar.png")),
    //BMR
    APPRENTICE(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/apprentice.png")),
    MATRON(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/matron.png")),
    JUDGE(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/judge.png")),
    VOUDON(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/voudon.png")),
    BISHOP(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/bishop.png")),
    //S&V
    BARISTA(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/barista.png")),
    HARLOT(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/harlot.png")),
    BUTCHER(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/butcher.png")),
    DEVIANT(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/deviant.png")),
    BONE_COLLECTOR(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/bone_collector.png")),
    //Carousel
    CACKLEJACK(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/cacklejack.png")),
    GANGSTER(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/gangster.png")),
    GNOME(RoleType.TRAVELER, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/traveler/gnome.png")),

    // --- FABLED ---

    ANGEL(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/angel.png")),
    BUDDHIST(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/buddhist.png")),
    DEUS_EX_FIASCO(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/deus_ex_fiasco.png")),
    DJINN(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/djinn.png")),
    DOOMSAYER(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/doomsayer.png")),
    DUCHESS(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/duchess.png")),
    FERRYMAN(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/ferryman.png")),
    FIBBIN(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/fibbin.png")),
    FIDDLER(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/fiddler.png")),
    HELLS_LIBRARIAN(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/hells_librarian.png")),
    REVOLUTIONARY(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/revolutionary.png")),
    SENTINEL(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/sentinel.png")),
    SPIRIT_OF_IVORY(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/spirit_of_ivory.png")),
    TOYMAKER(RoleType.FABLED, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/fabled/toymaker.png")),

    // --- LORIC ---

    BIG_WIG(RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/big_wig.png")),
    BOOTLEGGER(RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/bootlegger.png")),
    GARDENER(RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/gardener.png")),
    GOD_OF_UG(RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/god_of_ug.png")),
    HINDU(RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/hindu.png")),
    KNAVES(RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/knaves.png")),
    POPE(RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/pope.png")),
    STORM_CATCHER(RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/storm_catcher.png")),
    TOR(RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/tor.png")),
    VENTRILOQUIST(RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/ventriloquist.png")),
    ZENOMANCER(RoleType.LORIC, Identifier.of(BloodOnTheBlocktower.MOD_ID, "textures/roles/loric/zenomancer.png"));


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

    private final RoleType type;
    private final Identifier icon;
    private final String nameKey;
    private final String abilityKey;

    Role(RoleType type, Identifier icon) {
        this.type = type;
        this.icon = icon;
        this.nameKey = "role." + BloodOnTheBlocktower.MOD_ID + "." + getId();
        this.abilityKey = nameKey + ".ability";
    }

    /** The role's ability text from the lang file. */
    public String getDescription() {
        return Language.getInstance().get(abilityKey);
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

    /** The role's name from the lang file, in caps. */
    public String getDisplayName() {
        return Language.getInstance().get(nameKey);
    }

}
