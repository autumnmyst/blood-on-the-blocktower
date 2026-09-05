package com.autumnwind.botb.command;

import com.autumnwind.botb.setup.SetupStick;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;

/** The /botb command tree. Handler bodies live in {@link ConfigCommands} and {@link GameCommands}. */
public final class BotbCommands {

    private BotbCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("botb")
                    .then(CommandManager.literal("setup")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(context -> SetupStick.startCommand(context.getSource()))
                            .then(CommandManager.literal("world")
                                    .executes(context -> SetupStick.worldCommand(context.getSource()))
                            )
                            .then(CommandManager.literal("help")
                                    .executes(context -> SetupStick.helpCommand(context.getSource()))
                            )
                            .then(CommandManager.literal("skip")
                                    .executes(context -> SetupStick.skipCommand(context.getSource()))
                            )
                            .then(CommandManager.literal("back")
                                    .executes(context -> SetupStick.backCommand(context.getSource()))
                            )
                            .then(CommandManager.literal("finish")
                                    .executes(context -> SetupStick.finishCommand(context.getSource()))
                            )
                    )
                    .then(CommandManager.literal("setSeatHome")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("seat", IntegerArgumentType.integer(1))
                                    // /botb setSeatHome <seat>
                                    .executes(context -> ConfigCommands.setSeatHome(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "seat"),
                                            null
                                    ))
                                    // /botb setSeatHome <seat> <xyz>
                                    .then(CommandManager.argument("pos", Vec3ArgumentType.vec3(true))
                                            .executes(context -> ConfigCommands.setSeatHome(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    Vec3ArgumentType.getVec3(context, "pos")
                                            ))
                                    )
                            )
                    )
                    // --- Teleport Command ---
                    .then(CommandManager.literal("teleportToSeat")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("seat", IntegerArgumentType.integer(1))
                                    .executes(context -> GameCommands.teleportToSeat(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "seat")
                                    ))
                            )
                    )
                    // --- Set Town Square Command ---
                    .then(CommandManager.literal("setTownSquare")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(context -> ConfigCommands.setTownSquare(context.getSource(), null))
                            .then(CommandManager.argument("pos", Vec3ArgumentType.vec3(true))
                                    .executes(context -> ConfigCommands.setTownSquare(
                                            context.getSource(),
                                            Vec3ArgumentType.getVec3(context, "pos")
                                    ))
                            )
                    )
                    // --- Set Town Square Seat Command ---
                    .then(CommandManager.literal("setTownSquareSeat")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("seat", IntegerArgumentType.integer(1))
                                    // /botb setTownSquareSeat <seat>
                                    .executes(context -> ConfigCommands.setTownSquareSeat(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "seat"),
                                            null
                                    ))
                                    // /botb setTownSquareSeat <seat> <xyz>
                                    .then(CommandManager.argument("pos", Vec3ArgumentType.vec3(true))
                                            .executes(context -> ConfigCommands.setTownSquareSeat(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    Vec3ArgumentType.getVec3(context, "pos")
                                            ))
                                    )
                            )
                    )
                    // --- Set Death Command ---
                    .then(CommandManager.literal("setDeathCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermissionLevel(4))
                            .then(CommandManager.argument("seat", IntegerArgumentType.integer(1))
                                    .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                            .executes(context -> ConfigCommands.setDeathCommand(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    StringArgumentType.getString(context, "command")
                                            ))
                                    )
                            )
                    )
                    // --- Set Revive Command ---
                    .then(CommandManager.literal("setReviveCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermissionLevel(4))
                            .then(CommandManager.argument("seat", IntegerArgumentType.integer(1))
                                    .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                            .executes(context -> ConfigCommands.setReviveCommand(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    StringArgumentType.getString(context, "command")
                                            ))
                                    )
                            )
                    )
                    // --- Set Seat Assignment Command ---
                    .then(CommandManager.literal("setSeatAssignmentCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermissionLevel(4))
                            .then(CommandManager.argument("seat", IntegerArgumentType.integer(1))
                                    .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                            .executes(context -> ConfigCommands.setSeatAssignmentCommand(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    StringArgumentType.getString(context, "command")
                                            ))
                                    )
                            )
                    )
                    // --- Set Dusk Command ---
                    .then(CommandManager.literal("setDuskCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermissionLevel(4))
                            .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                    .executes(context -> ConfigCommands.setDuskCommand(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "command")
                                    ))
                            )
                    )
                    // --- Set Dawn Command ---
                    .then(CommandManager.literal("setDawnCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermissionLevel(4))
                            .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                    .executes(context -> ConfigCommands.setDawnCommand(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "command")
                                    ))
                            )
                    )
                    // --- Set Switch Position ---
                    .then(CommandManager.literal("setSwitchPosition")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("seat", IntegerArgumentType.integer(1))
                                    .executes(context -> ConfigCommands.setSwitchPosition(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "seat"),
                                            null
                                    ))
                                    .then(CommandManager.argument("pos", Vec3ArgumentType.vec3(true))
                                            .executes(context -> ConfigCommands.setSwitchPosition(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    Vec3ArgumentType.getVec3(context, "pos")
                                            ))
                                    )
                            )
                    )
                    // --- Set Vote Indicator Position ---
                    .then(CommandManager.literal("setVoteIndicatorPosition")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("seat", IntegerArgumentType.integer(1))
                                    .executes(context -> ConfigCommands.setVoteIndicatorPosition(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "seat"),
                                            null
                                    ))
                                    .then(CommandManager.argument("pos", Vec3ArgumentType.vec3(true))
                                            .executes(context -> ConfigCommands.setVoteIndicatorPosition(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    Vec3ArgumentType.getVec3(context, "pos")
                                            ))
                                    )
                            )
                    )
                    // --- Set Execution Command ---
                    .then(CommandManager.literal("setExecutionCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermissionLevel(4))
                            .then(CommandManager.argument("seat", IntegerArgumentType.integer(1))
                                    .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                            .executes(context -> ConfigCommands.setExecutionCommand(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    StringArgumentType.getString(context, "command")
                                            ))
                                    )
                            )
                    )
                    // --- Set Execution Delays ---
                    .then(CommandManager.literal("setExecution")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.literal("soundDelay")
                                    .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                                            .executes(context -> ConfigCommands.setExecutionSoundDelay(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "delay")
                                            ))
                                    )
                            )
                            .then(CommandManager.literal("deathTitleDelay")
                                    .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                                            .executes(context -> ConfigCommands.setExecutionDeathTitleDelay(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "delay")
                                            ))
                                    )
                            )
                            .then(CommandManager.literal("survivedSoundDelay")
                                    .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                                            .executes(context -> ConfigCommands.setExecutionSurvivedSoundDelay(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "delay")
                                            ))
                                    )
                            )
                    )
                    // --- Set Execution Position ---
                    .then(CommandManager.literal("setExecutionPosition")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(context -> ConfigCommands.setExecutionPosition(context.getSource(), null))
                            .then(CommandManager.argument("pos", Vec3ArgumentType.vec3(true))
                                    .executes(context -> ConfigCommands.setExecutionPosition(
                                            context.getSource(),
                                            Vec3ArgumentType.getVec3(context, "pos")
                                    ))
                            )
                    )
                    // --- Set Anvil Height ---
                    .then(CommandManager.literal("setAnvilHeight")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("height", IntegerArgumentType.integer())
                                    .executes(context -> ConfigCommands.setAnvilHeight(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "height")
                                    ))
                            )
                    )
                    // --- Lock In Execution Position ---
                    .then(CommandManager.literal("lockInExecutionPosition")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.literal("true")
                                    .executes(context -> ConfigCommands.setLockInExecutionPosition(context.getSource(), true))
                            )
                            .then(CommandManager.literal("false")
                                    .executes(context -> ConfigCommands.setLockInExecutionPosition(context.getSource(), false))
                            )
                    )
                    // --- Set Time Commands ---
                    .then(CommandManager.literal("setTime")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.literal("dawn")
                                    .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 24000))
                                            .executes(context -> ConfigCommands.setTimeDawn(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "time")
                                            ))
                                    )
                            )
                            .then(CommandManager.literal("evening")
                                    .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 24000))
                                            .executes(context -> ConfigCommands.setTimeEvening(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "time")
                                            ))
                                    )
                            )
                            .then(CommandManager.literal("dusk")
                                    .then(CommandManager.argument("time", IntegerArgumentType.integer(0, 24000))
                                            .executes(context -> ConfigCommands.setTimeDusk(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "time")
                                            ))
                                    )
                            )
                    )
                    // --- Set Clock Center Command ---
                    .then(CommandManager.literal("setClockCenter")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(context -> ConfigCommands.setClockCenter(context.getSource(), null))
                            .then(CommandManager.argument("pos", Vec3ArgumentType.vec3(true))
                                    .executes(context -> ConfigCommands.setClockCenter(
                                            context.getSource(),
                                            Vec3ArgumentType.getVec3(context, "pos")
                                    ))
                            )
                    )
                    // --- Set Name Max Length Command ---
                    .then(CommandManager.literal("setNameMaxLength")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("length", IntegerArgumentType.integer(1, 64))
                                    .executes(context -> ConfigCommands.setNameMaxLength(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "length")
                                    ))
                            )
                    )
                    // --- Set Clock Hand Scale Command ---
                    .then(CommandManager.literal("setClockHandScale")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("scale", FloatArgumentType.floatArg(0.1f, 10.0f))
                                    .executes(context -> ConfigCommands.setClockHandScale(
                                            context.getSource(),
                                            FloatArgumentType.getFloat(context, "scale")
                                    ))
                            )
                    )
                    // --- End Game Command ---
                    .then(CommandManager.literal("endGame")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.literal("good")
                                    .executes(context -> GameCommands.endGame(context.getSource(), true))
                            )
                            .then(CommandManager.literal("evil")
                                    .executes(context -> GameCommands.endGame(context.getSource(), false))
                            )
                    )
                    // --- Reset Game (soft) Command (revive all, unassign players but keep storyteller grimoire) ---
                    .then(CommandManager.literal("resetGame")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(context -> GameCommands.resetGame(context.getSource()))
                    )
                    // --- Reset Game Hard Command (full reset including grimoire) ---
                    .then(CommandManager.literal("resetGameHard")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(context -> GameCommands.resetGameHard(context.getSource()))
                    )
                    // --- Set Name Command (no permission requirement - players can set their own name) ---
                    .then(CommandManager.literal("setName")
                            .executes(context -> GameCommands.setPlayerName(context.getSource(), ""))
                            .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                    .executes(context -> GameCommands.setPlayerName(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "name")
                                    ))
                            )
                    )
            );
        });
    }
}
