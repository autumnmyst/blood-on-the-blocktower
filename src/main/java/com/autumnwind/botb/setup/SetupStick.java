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
        TOWN_SQUARE("town_square", true),
        EXECUTION("execution", true),
        CLOCK_CENTER("clock_center", true),
        TOWN_SQUARE_SEAT("town_square_seat", true),
        SWITCH("switch", false),
        VOTE_INDICATOR("vote_indicator", false),
        SEAT_HOME("seat_home", true);

        final String key;
        final boolean standing;

        Step(String key, boolean standing) {
            this.key = "hud.blood-on-the-blocktower.setup.step." + key;
            this.standing = standing;
        }

        /** The step name, with the seat number filled in for the per-seat steps. */
        MutableText title(int seat) {
            return Text.translatable(key, seat);
        }

        /** What to click, with the floor word in bold so it's clear the click goes on the ground block. */
        MutableText target() {
            return Text.translatable(key + ".target", Text.translatable("hud.blood-on-the-blocktower.setup.floor").formatted(Formatting.BOLD));
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
            source.sendError(Text.translatable("message.blood-on-the-blocktower.setup.only_player_run"));
            return 0;
        }
        GameRules rules = source.getServer().getGameRules();
        rules.get(GameRules.DO_IMMEDIATE_RESPAWN).set(true, source.getServer());
        rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false, source.getServer());
        rules.get(GameRules.DO_MOB_SPAWNING).set(false, source.getServer());
        rules.get(GameRules.KEEP_INVENTORY).set(true, source.getServer());
        giveStick(player);
        reportGamerules(player);
        startSession(player);
        return 1;
    }

    /** /botb setup help: the map positions and the other map settings, with current values. */
    public static int helpCommand(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.setup.only_player_help"));
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
            source.sendError(Text.translatable("message.blood-on-the-blocktower.setup.not_in_progress"));
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
            source.sendError(Text.translatable("message.blood-on-the-blocktower.setup.not_in_progress"));
            return 0;
        }
        back(player, session);
        return 1;
    }

    /** /botb setup finish: ends the walkthrough early, removing the stick. */
    public static int finishCommand(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || SESSIONS.remove(player.getUuid()) == null) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.setup.not_in_progress"));
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
            send(serverPlayer, Text.translatable("message.blood-on-the-blocktower.setup.operators_only").formatted(Formatting.RED));
            return ActionResult.SUCCESS;
        }

        Session session = SESSIONS.get(serverPlayer.getUuid());
        if (session == null) {
            startSession(serverPlayer).lastClickMs = now;
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
        serverPlayer.sendMessage(Text.translatable("message.blood-on-the-blocktower.setup.step_set", session.step.title(session.seat)).formatted(Formatting.GREEN)
                .append(Text.literal(target.toShortString()).formatted(Formatting.WHITE)), true);
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
            send(player, Text.translatable("message.blood-on-the-blocktower.setup.operators_only").formatted(Formatting.RED));
            return;
        }
        Session session = SESSIONS.get(player.getUuid());
        if (session == null) {
            send(player, Text.translatable("message.blood-on-the-blocktower.setup.run_to_start", command("/botb setup", "/botb setup"))
                    .formatted(Formatting.GRAY));
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
                send(player, Text.translatable("message.blood-on-the-blocktower.setup.already_first_step").formatted(Formatting.YELLOW));
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
    /** Starts (or restarts) the walkthrough from the first step and shows the HUD. */
    private static Session startSession(ServerPlayerEntity player) {
        Session session = new Session();
        SESSIONS.put(player.getUuid(), session);
        send(player, Text.translatable("message.blood-on-the-blocktower.setup.started").formatted(Formatting.GOLD));
        prompt(player);
        return session;
    }

    private static void prompt(ServerPlayerEntity player) {
        Session session = SESSIONS.get(player.getUuid());
        if (session == null) return;
        BlockPos existing = current(session);
        ServerPlayNetworking.send(player, new SetupHudS2CPayload(true, session.step.title(session.seat),
                existing != null ? existing.toShortString() : "", session.step.target(),
                Text.translatable(session.step.isSeatStep() ? "hud.blood-on-the-blocktower.setup.control.homes" : "hud.blood-on-the-blocktower.setup.control.finish")));
    }

    private static void hideHud(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, SetupHudS2CPayload.hidden());
    }

    // ========== Chat summaries ==========

    private static void sendTownSquareSummary(ServerPlayerEntity player) {
        send(player, Text.translatable("message.blood-on-the-blocktower.setup.town_square_summary").formatted(Formatting.GOLD));
        line(player, Step.TOWN_SQUARE.title(0), posOrUnset(ServerConfig.TOWN_SQUARE));
        line(player, Step.EXECUTION.title(0), posOrUnset(ServerConfig.EXECUTION_POSITION));
        line(player, Step.CLOCK_CENTER.title(0), posOrUnset(ServerConfig.CLOCK_CENTER));
    }

    private static void sendSeatSummary(ServerPlayerEntity player, int seat) {
        send(player, Text.translatable("message.blood-on-the-blocktower.setup.seat_summary", seat).formatted(Formatting.GOLD)
                .append(labeled("message.blood-on-the-blocktower.setup.seat_label", posOrUnset(ServerConfig.TOWN_SQUARE_SEATS.get(seat))))
                .append(Text.literal(", ").formatted(Formatting.GRAY))
                .append(labeled("message.blood-on-the-blocktower.setup.lever_label", posOrUnset(ServerConfig.SEAT_SWITCH_POSITIONS.get(seat))))
                .append(Text.literal(", ").formatted(Formatting.GRAY))
                .append(labeled("message.blood-on-the-blocktower.setup.indicator_label", posOrUnset(ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.get(seat)))));
    }

    private static void sendHomeSummary(ServerPlayerEntity player, int seat) {
        send(player, Text.translatable("message.blood-on-the-blocktower.setup.home_summary", seat).formatted(Formatting.GOLD)
                .append(posOrUnset(ServerConfig.SEAT_HOMES.get(seat)).copy().formatted(Formatting.WHITE)));
    }

    private static MutableText labeled(String labelKey, Text value) {
        return Text.translatable(labelKey).formatted(Formatting.GRAY).append(value.copy().formatted(Formatting.WHITE));
    }

    // ========== Finish: remaining settings ==========

    private static void finish(ServerPlayerEntity player) {
        hideHud(player);
        takeStick(player);
        VoteIndicators.paintVoteIndicatorStacks(player.getServer(), true);
        ElectionManager.powerAllSeatPistons(player.getServer());
        send(player, Text.translatable("message.blood-on-the-blocktower.setup.complete").formatted(Formatting.GREEN)
                .append(Text.translatable("message.blood-on-the-blocktower.setup.run_for_settings", Text.literal("/botb setup help").styled(style -> style
                        .withColor(Formatting.AQUA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/botb setup help"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("message.blood-on-the-blocktower.setup.click_to_run")))))
                        .formatted(Formatting.GRAY)));
    }

    /** The map positions with their current values, as one summary line. */
    private static void sendPositionsSummary(ServerPlayerEntity player) {
        int seats = Math.max(Math.max(ServerConfig.SEAT_HOMES.size(), ServerConfig.TOWN_SQUARE_SEATS.size()),
                Math.max(ServerConfig.SEAT_SWITCH_POSITIONS.size(), ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.size()));
        send(player, Text.translatable("message.blood-on-the-blocktower.setup.positions").formatted(Formatting.GOLD)
                .append(Text.translatable("message.blood-on-the-blocktower.setup.positions_summary", seats, ServerConfig.SEAT_HOMES.size(),
                        ServerConfig.TOWN_SQUARE_SEATS.size(),
                        ServerConfig.SEAT_SWITCH_POSITIONS.size(),
                        ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.size(),
                        posOrUnset(ServerConfig.TOWN_SQUARE),
                        posOrUnset(ServerConfig.EXECUTION_POSITION),
                        posOrUnset(ServerConfig.CLOCK_CENTER)).formatted(Formatting.GRAY)));
    }

    /** The other map settings with their current values; each command is clickable. */
    private static void sendSettings(ServerPlayerEntity player) {
        send(player, Text.translatable("message.blood-on-the-blocktower.setup.other_settings").formatted(Formatting.GOLD));
        setting(player, "/botb setClockHandScale <scale>", "/botb setClockHandScale ", "message.blood-on-the-blocktower.setup.setting.clock_hand_scale", Text.literal(String.valueOf(ServerConfig.CLOCK_HAND_SCALE)));
        setting(player, "/botb setNameMaxLength <length>", "/botb setNameMaxLength ", "message.blood-on-the-blocktower.setup.setting.name_max_length", Text.literal(String.valueOf(ServerConfig.MAX_NAME_LENGTH)));
        setting(player, "/botb setVoteTimePerPlayer <ms>", "/botb setVoteTimePerPlayer ", "message.blood-on-the-blocktower.setup.setting.vote_time_per_player", ms(ServerConfig.VOTE_TIME_PER_PLAYER));
        setting(player, "/botb setAnvilHeight <blocks>", "/botb setAnvilHeight ", "message.blood-on-the-blocktower.setup.setting.anvil_height", Text.literal(String.valueOf(ServerConfig.ANVIL_HEIGHT)));
        setting(player, "/botb lockInExecutionPosition <true|false>", "/botb lockInExecutionPosition ", "message.blood-on-the-blocktower.setup.setting.lock_in_execution_position", Text.literal(String.valueOf(ServerConfig.LOCK_IN_EXECUTION_POSITION)));
        setting(player, "/botb setExecution soundDelay <ms>", "/botb setExecution soundDelay ", "message.blood-on-the-blocktower.setup.setting.execution_sound_delay", ms(ServerConfig.EXECUTION_SOUND_DELAY));
        setting(player, "/botb setExecution survivedSoundDelay <ms>", "/botb setExecution survivedSoundDelay ", "message.blood-on-the-blocktower.setup.setting.execution_survived_sound_delay", ms(ServerConfig.EXECUTION_SURVIVED_SOUND_DELAY));
        setting(player, "/botb setExecution deathTitleDelay <ms>", "/botb setExecution deathTitleDelay ", "message.blood-on-the-blocktower.setup.setting.execution_death_title_delay", ms(ServerConfig.EXECUTION_DEATH_TITLE_DELAY));
        setting(player, "/botb setTime dawn <ticks>", "/botb setTime dawn ", "message.blood-on-the-blocktower.setup.setting.time_dawn", Text.literal(String.valueOf(ServerConfig.TIME_DAWN)));
        setting(player, "/botb setTime evening <ticks>", "/botb setTime evening ", "message.blood-on-the-blocktower.setup.setting.time_evening", Text.literal(String.valueOf(ServerConfig.TIME_EVENING)));
        setting(player, "/botb setTime dusk <ticks>", "/botb setTime dusk ", "message.blood-on-the-blocktower.setup.setting.time_dusk", Text.literal(String.valueOf(ServerConfig.TIME_DUSK)));
        setting(player, "/botb setDuskCommand <command>", "/botb setDuskCommand ", "message.blood-on-the-blocktower.setup.setting.dusk_command", orNone(ServerConfig.DUSK_COMMAND));
        setting(player, "/botb setDawnCommand <command>", "/botb setDawnCommand ", "message.blood-on-the-blocktower.setup.setting.dawn_command", orNone(ServerConfig.DAWN_COMMAND));
        setting(player, "/botb setDeathCommand <seat> <command>", "/botb setDeathCommand ", "message.blood-on-the-blocktower.setup.setting.death_command", seatsSet(ServerConfig.DEATH_COMMANDS.size()));
        setting(player, "/botb setReviveCommand <seat> <command>", "/botb setReviveCommand ", "message.blood-on-the-blocktower.setup.setting.revive_command", seatsSet(ServerConfig.REVIVE_COMMANDS.size()));
        setting(player, "/botb setSeatAssignmentCommand <seat> <command>", "/botb setSeatAssignmentCommand ", "message.blood-on-the-blocktower.setup.setting.seat_assignment_command", seatsSet(ServerConfig.SEAT_ASSIGNMENT_COMMANDS.size()));
        setting(player, "/botb setExecutionCommand <seat> <command>", "/botb setExecutionCommand ", "message.blood-on-the-blocktower.setup.setting.execution_command", seatsSet(ServerConfig.EXECUTION_COMMANDS.size()));
    }

    private static void setting(ServerPlayerEntity player, String usage, String suggest, String descriptionKey, Text value) {
        send(player, Text.literal("  ").append(command(usage, suggest))
                .append(Text.literal("\n    ").append(Text.translatable(descriptionKey)).append(": ").formatted(Formatting.GRAY))
                .append(value.copy().formatted(Formatting.WHITE)));
    }

    private static MutableText command(String label, String suggest) {
        return Text.literal(label).styled(style -> style
                .withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("message.blood-on-the-blocktower.setup.click_to_suggest"))));
    }

    private static Text posOrUnset(BlockPos pos) {
        return pos != null ? Text.literal(pos.toShortString()) : Text.translatable("message.blood-on-the-blocktower.setup.unset");
    }

    private static Text orNone(String value) {
        return value != null && !value.isBlank() ? Text.literal(value) : Text.translatable("message.blood-on-the-blocktower.setup.none");
    }

    private static Text ms(long millis) {
        return Text.translatable("message.blood-on-the-blocktower.setup.ms", millis);
    }

    private static Text seatsSet(int count) {
        return Text.translatable("message.blood-on-the-blocktower.setup.seats_set", count);
    }

    // ========== Helpers ==========

    private static void line(ServerPlayerEntity player, Text label, Text value) {
        send(player, Text.literal("  ").append(label).append(": ").formatted(Formatting.GRAY)
                .append(value.copy().formatted(Formatting.WHITE)));
    }

    /** Lists the gamerules World Setup manages, one per line, red when not at the expected value. */
    private static void reportGamerules(ServerPlayerEntity player) {
        GameRules rules = player.getServer().getGameRules();
        send(player, Text.translatable("message.blood-on-the-blocktower.setup.gamerules").formatted(Formatting.GOLD));
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
