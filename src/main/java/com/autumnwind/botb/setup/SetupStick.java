package com.autumnwind.botb.setup;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.item.ModItems;
import com.autumnwind.botb.networking.SetupHudS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.autumnwind.botb.daytime.ElectionManager;
import net.minecraft.world.GameRules;
import com.autumnwind.botb.world.VoteIndicators;

/**
 * Guided map setup with the "BotB Setup Stick". The storyteller MB1s blocks to fill in each
 * map position instead of typing the commands. The commands stay as the backup and are
 * listed, with current values, by /botb setup help.
 *
 * <p>Flow: Town Square, execution spot, clock center, then each seat's town square seat, lever
 * and vote indicator (these sit together), then every seat's home in one pass (homes are far
 * from the square). MB1 sets the current step, MB2 goes back a step, Shift + MB1 skips a step
 * keeping whatever is configured, and Shift + MB2 moves on from the seats to the homes. Setting
 * the last home finishes and removes the stick.
 *
 * <p>The step being set, its current value and the controls live in an on-screen box
 * ({@code SetupHUD}, fed by {@link SetupHudS2CPayload}); chat only gets a summary as each
 * group finishes: the three town square positions, then one line per seat, then one per home.
 *
 * <p>"Standing" spots are read as the block adjacent to the clicked face, so clicking a floor
 * block gives the air block a player stands in, matching what the commands record from a
 * player's feet. Lever and vote indicator positions are the clicked block itself.
 */
public final class SetupStick {

    private SetupStick() {}

    private enum Step {
        TOWN_SQUARE("Town Square", "where you teleport at dawn", true),
        EXECUTION("Execution spot", "where the executed player stands", true),
        CLOCK_CENTER("Clock center", "floor block under the clock center", true),
        TOWN_SQUARE_SEAT("Seat %d town square seat", "floor block where the player sits and votes from", true),
        SWITCH("Seat %d lever", "lever to toggle vote", false),
        VOTE_INDICATOR("Seat %d vote indicator", "block that shows vote state", false),
        SEAT_HOME("Home %d", "floor block teleported to when sent home/visiting", true);

        final String title;
        final String target;
        final boolean standing;

        Step(String title, String target, boolean standing) {
            this.title = title;
            this.target = target;
            this.standing = standing;
        }

        /** The seat loop: town square seat, lever, vote indicator. */
        boolean isSeatStep() {
            return this == TOWN_SQUARE_SEAT || this == SWITCH || this == VOTE_INDICATOR;
        }
    }

    private static final class Session {
        Step step = Step.TOWN_SQUARE;
        int seat = 1;
        boolean seatStarted = false; // something was set or skipped for the current seat
        int seatCount = 0;           // seats to walk homes for, fixed when the seat loop ends
        long lastClickMs = 0;        // holding left-click re-fires the attack event; only the first counts
    }

    private static final long CLICK_COOLDOWN_MS = 400;

    /**
     * Left-click keeps firing while the button is held. When the final click removes the stick,
     * the next firing sees an empty hand and would break the block, so attacks stay swallowed
     * for a moment after the stick was last held. Tracked on both sides, since the client
     * decides on its own whether to break in creative mode.
     */
    private static final long EMPTY_HAND_GRACE_MS = 1500;
    private static final Map<UUID, Long> LAST_STICK_ATTACK_MS = new HashMap<>();

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    /** Drops every walkthrough and click-grace record; sessions don't outlive the server. */
    public static void onServerStopping() {
        SESSIONS.clear();
        LAST_STICK_ATTACK_MS.clear();
    }

    // ========== Commands ==========

    /**
     * /botb setup (also the Storyteller Tools "World Setup" button): sets the
     * gamerules and hands over the stick and starts (or restarts) the walkthrough.
     */
    public static int startCommand(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Only a player can run map setup."));
            return 0;
        }
        GameRules rules = source.getServer().getGameRules();
        rules.get(GameRules.DO_IMMEDIATE_RESPAWN).set(true, source.getServer());
        rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, source.getServer());
        rules.get(GameRules.DO_MOB_SPAWNING).set(false, source.getServer());
        rules.get(GameRules.KEEP_INVENTORY).set(true, source.getServer());
        giveStick(player);
        reportGamerules(player);
        SESSIONS.put(player.getUuid(), new Session());
        sendControls(player);
        prompt(player);
        return 1;
    }

    /** /botb setup help: the map positions and the other map settings, with current values. */
    public static int helpCommand(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Only a player can view map setup help."));
            return 0;
        }
        sendPositionsSummary(player);
        sendSettings(player);
        return 1;
    }

    /** /botb setup skip: keeps the current value for this step and moves on. */
    public static int skipCommand(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        Session session = player != null ? SESSIONS.get(player.getUuid()) : null;
        if (session == null) {
            source.sendError(Text.literal("No map setup in progress. Run /botb setup first."));
            return 0;
        }
        skip(player, session);
        return 1;
    }

    /** /botb setup back: returns to the previous step so it can be clicked again. */
    public static int backCommand(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        Session session = player != null ? SESSIONS.get(player.getUuid()) : null;
        if (session == null) {
            source.sendError(Text.literal("No map setup in progress. Run /botb setup first."));
            return 0;
        }
        back(player, session);
        return 1;
    }

    /** /botb setup finish: ends the walkthrough early, removing the stick. */
    public static int finishCommand(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || SESSIONS.remove(player.getUuid()) == null) {
            source.sendError(Text.literal("No map setup in progress. Run /botb setup first."));
            return 0;
        }
        finish(player);
        return 1;
    }

    // ========== Item events ==========

    /** MB1 on a block while holding the stick: sets the current step (Shift: skips it). */
    public static ActionResult onAttackBlock(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction side) {
        long now = System.currentTimeMillis();
        if (!player.getStackInHand(hand).isOf(ModItems.SETUP_STICK)) {
            Long lastHeld = LAST_STICK_ATTACK_MS.get(player.getUuid());
            if (lastHeld != null && now - lastHeld < EMPTY_HAND_GRACE_MS) return ActionResult.SUCCESS;
            return ActionResult.PASS;
        }
        LAST_STICK_ATTACK_MS.put(player.getUuid(), now);
        if (world.isClient()) return ActionResult.SUCCESS; // the server does the work
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
        if (!serverPlayer.hasPermissionLevel(2)) {
            send(serverPlayer, Text.literal("Only operators can use the Setup Stick.").formatted(Formatting.RED));
            return ActionResult.SUCCESS;
        }

        Session session = SESSIONS.get(serverPlayer.getUuid());
        if (session == null) {
            SESSIONS.put(serverPlayer.getUuid(), session = new Session());
            session.lastClickMs = System.currentTimeMillis();
            sendControls(serverPlayer);
            prompt(serverPlayer);
            return ActionResult.SUCCESS;
        }

        if (now - session.lastClickMs < CLICK_COOLDOWN_MS) return ActionResult.SUCCESS;
        session.lastClickMs = now;

        if (serverPlayer.isSneaking()) {
            skip(serverPlayer, session);
            return ActionResult.SUCCESS;
        }

        BlockPos target = session.step.standing ? pos.offset(side) : pos;
        apply(session, target);
        ServerConfig.save();
        advance(serverPlayer, session);
        return ActionResult.SUCCESS;
    }

    /** MB2 on a block while holding the stick: back a step (Shift: seats to homes, or homes to finish). */
    public static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (!player.getStackInHand(hand).isOf(ModItems.SETUP_STICK)) return ActionResult.PASS;
        if (world.isClient()) return ActionResult.SUCCESS;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            rightClick(serverPlayer);
        }
        return ActionResult.SUCCESS;
    }

    /** MB2 in the air while holding the stick: back a step (Shift: seats to homes, or homes to finish). */
    public static TypedActionResult<ItemStack> onUseItem(PlayerEntity player, World world, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(ModItems.SETUP_STICK)) return TypedActionResult.pass(stack);
        if (world.isClient()) return TypedActionResult.success(stack);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            rightClick(serverPlayer);
        }
        return TypedActionResult.success(stack);
    }

    private static void rightClick(ServerPlayerEntity player) {
        if (!player.hasPermissionLevel(2)) {
            send(player, Text.literal("Only operators can use the Setup Stick.").formatted(Formatting.RED));
            return;
        }
        Session session = SESSIONS.get(player.getUuid());
        if (session == null) {
            send(player, Text.literal("Run ").formatted(Formatting.GRAY)
                    .append(command("/botb setup", "/botb setup"))
                    .append(Text.literal(" or MB1 a block to start map setup.").formatted(Formatting.GRAY)));
            return;
        }
        if (player.isSneaking()) {
            if (session.step.isSeatStep()) {
                startHomes(player, session);
            } else {
                SESSIONS.remove(player.getUuid());
                finish(player);
            }
        } else {
            back(player, session);
        }
    }

    // ========== Walkthrough ==========

    private static void apply(Session session, BlockPos pos) {
        if (session.step.isSeatStep()) session.seatStarted = true;
        switch (session.step) {
            case TOWN_SQUARE -> ServerConfig.TOWN_SQUARE = pos;
            case EXECUTION -> ServerConfig.EXECUTION_POSITION = pos;
            case CLOCK_CENTER -> ServerConfig.CLOCK_CENTER = pos;
            case SEAT_HOME -> ServerConfig.SEAT_HOMES.put(session.seat, pos);
            case TOWN_SQUARE_SEAT -> ServerConfig.TOWN_SQUARE_SEATS.put(session.seat, pos);
            case SWITCH -> ServerConfig.SEAT_SWITCH_POSITIONS.put(session.seat, pos);
            case VOTE_INDICATOR -> ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.put(session.seat, pos);
        }
    }

    private static BlockPos current(Session session) {
        return switch (session.step) {
            case TOWN_SQUARE -> ServerConfig.TOWN_SQUARE;
            case EXECUTION -> ServerConfig.EXECUTION_POSITION;
            case CLOCK_CENTER -> ServerConfig.CLOCK_CENTER;
            case SEAT_HOME -> ServerConfig.SEAT_HOMES.get(session.seat);
            case TOWN_SQUARE_SEAT -> ServerConfig.TOWN_SQUARE_SEATS.get(session.seat);
            case SWITCH -> ServerConfig.SEAT_SWITCH_POSITIONS.get(session.seat);
            case VOTE_INDICATOR -> ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(session.seat);
        };
    }

    private static void skip(ServerPlayerEntity player, Session session) {
        if (session.step.isSeatStep()) session.seatStarted = true;
        advance(player, session);
    }

    private static void advance(ServerPlayerEntity player, Session session) {
        switch (session.step) {
            case TOWN_SQUARE -> session.step = Step.EXECUTION;
            case EXECUTION -> session.step = Step.CLOCK_CENTER;
            case CLOCK_CENTER -> {
                sendTownSquareSummary(player);
                session.step = Step.TOWN_SQUARE_SEAT;
                session.seat = 1;
                session.seatStarted = false;
            }
            case TOWN_SQUARE_SEAT -> session.step = Step.SWITCH;
            case SWITCH -> session.step = Step.VOTE_INDICATOR;
            case VOTE_INDICATOR -> {
                sendSeatSummary(player, session.seat);
                session.seat++;
                session.seatStarted = false;
                session.step = Step.TOWN_SQUARE_SEAT;
            }
            case SEAT_HOME -> {
                sendHomeSummary(player, session.seat);
                if (session.seat >= session.seatCount) {
                    SESSIONS.remove(player.getUuid());
                    finish(player);
                    return;
                }
                session.seat++;
            }
        }
        prompt(player);
    }

    /** Ends the seat loop and walks the homes for every seat touched, in one pass. */
    private static void startHomes(ServerPlayerEntity player, Session session) {
        session.seatCount = session.seatStarted ? session.seat : session.seat - 1;
        if (session.seatCount == 0) {
            SESSIONS.remove(player.getUuid());
            finish(player);
            return;
        }
        if (session.seatStarted) sendSeatSummary(player, session.seat);
        session.step = Step.SEAT_HOME;
        session.seat = 1;
        prompt(player);
    }

    /** Steps back one position so it can be clicked again; the old value is overwritten on the next click. */
    private static void back(ServerPlayerEntity player, Session session) {
        switch (session.step) {
            case TOWN_SQUARE -> {
                send(player, Text.literal("Already at the first step.").formatted(Formatting.YELLOW));
                return;
            }
            case EXECUTION -> session.step = Step.TOWN_SQUARE;
            case CLOCK_CENTER -> session.step = Step.EXECUTION;
            case TOWN_SQUARE_SEAT -> {
                if (session.seat == 1) {
                    session.step = Step.CLOCK_CENTER;
                } else {
                    session.seat--;
                    session.seatStarted = true;
                    session.step = Step.VOTE_INDICATOR;
                }
            }
            case SWITCH -> session.step = Step.TOWN_SQUARE_SEAT;
            case VOTE_INDICATOR -> session.step = Step.SWITCH;
            case SEAT_HOME -> {
                if (session.seat == 1) {
                    // Back into the seat loop, on the last seat's indicator
                    session.seat = session.seatCount;
                    session.seatStarted = true;
                    session.step = Step.VOTE_INDICATOR;
                } else {
                    session.seat--;
                }
            }
        }
        prompt(player);
    }

    /** Updates the on-screen box for the current step. */
    private static void prompt(ServerPlayerEntity player) {
        Session session = SESSIONS.get(player.getUuid());
        if (session == null) return;
        BlockPos existing = current(session);
        ServerPlayNetworking.send(player, new SetupHudS2CPayload(true, title(session),
                existing != null ? existing.toShortString() : "", session.step.target,
                session.step.isSeatStep() ? "Homes" : "Finish"));
    }

    private static void hideHud(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, SetupHudS2CPayload.hidden());
    }

    // ========== Chat summaries ==========

    private static void sendTownSquareSummary(ServerPlayerEntity player) {
        send(player, Text.literal("Town square setup:").formatted(Formatting.GOLD));
        line(player, "Town Square", posOrUnset(ServerConfig.TOWN_SQUARE));
        line(player, "Execution spot", posOrUnset(ServerConfig.EXECUTION_POSITION));
        line(player, "Clock center", posOrUnset(ServerConfig.CLOCK_CENTER));
    }

    private static void sendSeatSummary(ServerPlayerEntity player, int seat) {
        send(player, Text.literal("Seat " + seat + ": ").formatted(Formatting.GOLD)
                .append(labeled("seat", posOrUnset(ServerConfig.TOWN_SQUARE_SEATS.get(seat))))
                .append(Text.literal(", ").formatted(Formatting.GRAY))
                .append(labeled("lever", posOrUnset(ServerConfig.SEAT_SWITCH_POSITIONS.get(seat))))
                .append(Text.literal(", ").formatted(Formatting.GRAY))
                .append(labeled("indicator", posOrUnset(ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat)))));
    }

    private static void sendHomeSummary(ServerPlayerEntity player, int seat) {
        send(player, Text.literal("Home " + seat + ": ").formatted(Formatting.GOLD)
                .append(Text.literal(posOrUnset(ServerConfig.SEAT_HOMES.get(seat))).formatted(Formatting.WHITE)));
    }

    private static MutableText labeled(String label, String value) {
        return Text.literal(label + " ").formatted(Formatting.GRAY).append(Text.literal(value).formatted(Formatting.WHITE));
    }

    private static String title(Session session) {
        return String.format(session.step.title, session.seat);
    }

    // ========== Finish: remaining settings ==========

    private static void finish(ServerPlayerEntity player) {
        hideHud(player);
        takeStick(player);
        VoteIndicators.paintVoteIndicatorStacks(player.getServer(), true);
        ElectionManager.powerAllSeatPistons(player.getServer());
        send(player, Text.literal("World setup complete! Stick removed, vote indicators painted. ").formatted(Formatting.GREEN)
                .append(Text.literal("Run ").formatted(Formatting.GRAY))
                .append(Text.literal("/botb setup help").styled(style -> style
                        .withColor(Formatting.AQUA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/botb setup help"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to run")))))
                .append(Text.literal(" for the other map settings.").formatted(Formatting.GRAY)));
    }

    /** The map positions with their current values, as one summary line. */
    private static void sendPositionsSummary(ServerPlayerEntity player) {
        int seats = Math.max(Math.max(ServerConfig.SEAT_HOMES.size(), ServerConfig.TOWN_SQUARE_SEATS.size()),
                Math.max(ServerConfig.SEAT_SWITCH_POSITIONS.size(), ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.size()));
        send(player, Text.literal("Map positions: ").formatted(Formatting.GOLD)
                .append(Text.literal("Seats configured: " + seats + " (homes " + ServerConfig.SEAT_HOMES.size()
                        + ", town square seats " + ServerConfig.TOWN_SQUARE_SEATS.size()
                        + ", levers " + ServerConfig.SEAT_SWITCH_POSITIONS.size()
                        + ", indicators " + ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.size() + "). "
                        + "Town Square " + posOrUnset(ServerConfig.TOWN_SQUARE)
                        + ", execution " + posOrUnset(ServerConfig.EXECUTION_POSITION)
                        + ", clock center " + posOrUnset(ServerConfig.CLOCK_CENTER) + ".").formatted(Formatting.GRAY)));
    }

    /** The other map settings with their current values; each command is clickable. */
    private static void sendSettings(ServerPlayerEntity player) {
        send(player, Text.literal("Other map settings (click a command to fill it in):").formatted(Formatting.GOLD));
        setting(player, "/botb setClockHandScale <scale>", "/botb setClockHandScale ", "clock hand scale", String.valueOf(ServerConfig.CLOCK_HAND_SCALE));
        setting(player, "/botb setNameMaxLength <length>", "/botb setNameMaxLength ", "longest custom name players may set", String.valueOf(ServerConfig.MAX_NAME_LENGTH));
        setting(player, "/botb setAnvilHeight <blocks>", "/botb setAnvilHeight ", "anvil height above execution spot (0 = no anvil)", String.valueOf(ServerConfig.ANVIL_HEIGHT));
        setting(player, "/botb lockInExecutionPosition <true|false>", "/botb lockInExecutionPosition ", "lock the executed player in place", String.valueOf(ServerConfig.LOCK_IN_EXECUTION_POSITION));
        setting(player, "/botb setExecution soundDelay <ms>", "/botb setExecution soundDelay ", "execution sound delay", ServerConfig.EXECUTION_SOUND_DELAY + " ms");
        setting(player, "/botb setExecution survivedSoundDelay <ms>", "/botb setExecution survivedSoundDelay ", "survived-execution sound delay", ServerConfig.EXECUTION_SURVIVED_SOUND_DELAY + " ms");
        setting(player, "/botb setExecution deathTitleDelay <ms>", "/botb setExecution deathTitleDelay ", "execution death title delay", ServerConfig.EXECUTION_DEATH_TITLE_DELAY + " ms");
        setting(player, "/botb setTime dawn <ticks>", "/botb setTime dawn ", "dawn time", String.valueOf(ServerConfig.TIME_DAWN));
        setting(player, "/botb setTime evening <ticks>", "/botb setTime evening ", "evening time", String.valueOf(ServerConfig.TIME_EVENING));
        setting(player, "/botb setTime dusk <ticks>", "/botb setTime dusk ", "dusk time", String.valueOf(ServerConfig.TIME_DUSK));
        setting(player, "/botb setDuskCommand <command>", "/botb setDuskCommand ", "command run at dusk", orNone(ServerConfig.DUSK_COMMAND));
        setting(player, "/botb setDawnCommand <command>", "/botb setDawnCommand ", "command run at dawn", orNone(ServerConfig.DAWN_COMMAND));
        setting(player, "/botb setDeathCommand <seat> <command>", "/botb setDeathCommand ", "per-seat command on death", ServerConfig.DEATH_COMMANDS.size() + " seats set");
        setting(player, "/botb setReviveCommand <seat> <command>", "/botb setReviveCommand ", "per-seat command on revive", ServerConfig.REVIVE_COMMANDS.size() + " seats set");
        setting(player, "/botb setSeatAssignmentCommand <seat> <command>", "/botb setSeatAssignmentCommand ", "per-seat command when a seat is assigned", ServerConfig.SEAT_ASSIGNMENT_COMMANDS.size() + " seats set");
        setting(player, "/botb setExecutionCommand <seat> <command>", "/botb setExecutionCommand ", "per-seat command on execution", ServerConfig.EXECUTION_COMMANDS.size() + " seats set");
    }

    private static void setting(ServerPlayerEntity player, String usage, String suggest, String description, String value) {
        send(player, Text.literal("  ").append(command(usage, suggest))
                .append(Text.literal("\n    " + description + ": ").formatted(Formatting.GRAY))
                .append(Text.literal(value).formatted(Formatting.WHITE)));
    }

    private static MutableText command(String label, String suggest) {
        return Text.literal(label).styled(style -> style
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to put this command in chat"))));
    }

    private static String posOrUnset(BlockPos pos) {
        return pos != null ? pos.toShortString() : "unset";
    }

    private static String orNone(String value) {
        return value != null && !value.isBlank() ? value : "none";
    }

    // ========== Helpers ==========

    /** The "Map setup started" header and the stick's controls, one per line. */
    private static void sendControls(ServerPlayerEntity player) {
        send(player, Text.literal("Map setup started:").formatted(Formatting.GOLD));
        line(player, "MB1", "set position");
        line(player, "MB2", "go back");
        line(player, "Shift + MB1", "skip step");
        line(player, "Shift + MB2", "finish seats and set homes");
    }

    private static void line(ServerPlayerEntity player, String label, String value) {
        send(player, Text.literal("  " + label + ": ").formatted(Formatting.GRAY)
                .append(Text.literal(value).formatted(Formatting.WHITE)));
    }

    /** Lists the gamerules World Setup manages, one per line, red when not at the expected value. */
    private static void reportGamerules(ServerPlayerEntity player) {
        GameRules rules = player.getServer().getGameRules();
        send(player, Text.literal("Gamerules:").formatted(Formatting.GOLD));
        gamerule(player, "doImmediateRespawn", rules.getBoolean(GameRules.DO_IMMEDIATE_RESPAWN), true);
        gamerule(player, "doDaylightCycle", rules.getBoolean(GameRules.DO_DAYLIGHT_CYCLE), false);
        gamerule(player, "doMobSpawning", rules.getBoolean(GameRules.DO_MOB_SPAWNING), false);
        gamerule(player, "keepInventory", rules.getBoolean(GameRules.KEEP_INVENTORY), true);
    }

    private static void gamerule(ServerPlayerEntity player, String name, boolean value, boolean expected) {
        send(player, Text.literal("  " + name + ": ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(value)).formatted(value == expected ? Formatting.GREEN : Formatting.RED)));
    }

    private static void takeStick(ServerPlayerEntity player) {
        player.getInventory().remove(stack -> stack.isOf(ModItems.SETUP_STICK), -1, player.playerScreenHandler.getCraftingInput());
    }

    private static void giveStick(ServerPlayerEntity player) {
        boolean hasStick = player.getInventory().contains(stack -> stack.isOf(ModItems.SETUP_STICK));
        if (!hasStick) {
            player.giveItemStack(new ItemStack(ModItems.SETUP_STICK));
        }
    }

    private static void send(ServerPlayerEntity player, Text text) {
        player.sendMessage(text, false);
    }
}
