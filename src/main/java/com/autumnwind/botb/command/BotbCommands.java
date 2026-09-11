package com.autumnwind.botb.command;

import com.autumnwind.botb.setup.SetupStick;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

/** The /botb command tree. Handler bodies live in {@link ConfigCommands} and {@link GameCommands}. */
public final class BotbCommands {

    private BotbCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("botb")
                    .then(Commands.literal("setup")
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> SetupStick.startCommand(context.getSource()))
                            .then(Commands.literal("help")
                                    .executes(context -> SetupStick.helpCommand(context.getSource()))
                            )
                            .then(Commands.literal("skip")
                                    .executes(context -> SetupStick.skipCommand(context.getSource()))
                            )
                            .then(Commands.literal("back")
                                    .executes(context -> SetupStick.backCommand(context.getSource()))
                            )
                            .then(Commands.literal("finish")
                                    .executes(context -> SetupStick.finishCommand(context.getSource()))
                            )
                    )
                    .then(Commands.literal("setSeatHome")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.argument("seat", IntegerArgumentType.integer(1))
                                    // /botb setSeatHome <seat>
                                    .executes(context -> ConfigCommands.setSeatHome(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "seat"),
                                            null
                                    ))
                                    // /botb setSeatHome <seat> <xyz>
                                    .then(Commands.argument("pos", Vec3Argument.vec3(true))
                                            .executes(context -> ConfigCommands.setSeatHome(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    Vec3Argument.getVec3(context, "pos")
                                            ))
                                    )
                            )
                    )
                    // --- Teleport Command ---
                    .then(Commands.literal("teleportToSeat")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.argument("seat", IntegerArgumentType.integer(1))
                                    .executes(context -> GameCommands.teleportToSeat(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "seat")
                                    ))
                            )
                    )
                    // --- Set Town Square Command ---
                    .then(Commands.literal("setTownSquare")
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> ConfigCommands.setTownSquare(context.getSource(), null))
                            .then(Commands.argument("pos", Vec3Argument.vec3(true))
                                    .executes(context -> ConfigCommands.setTownSquare(
                                            context.getSource(),
                                            Vec3Argument.getVec3(context, "pos")
                                    ))
                            )
                    )
                    // --- Set Town Square Seat Command ---
                    .then(Commands.literal("setTownSquareSeat")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.argument("seat", IntegerArgumentType.integer(1))
                                    // /botb setTownSquareSeat <seat>
                                    .executes(context -> ConfigCommands.setTownSquareSeat(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "seat"),
                                            null
                                    ))
                                    // /botb setTownSquareSeat <seat> <xyz>
                                    .then(Commands.argument("pos", Vec3Argument.vec3(true))
                                            .executes(context -> ConfigCommands.setTownSquareSeat(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    Vec3Argument.getVec3(context, "pos")
                                            ))
                                    )
                            )
                    )
                    // --- Set Death Command ---
                    .then(Commands.literal("setDeathCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermission(4))
                            .then(Commands.argument("seat", IntegerArgumentType.integer(1))
                                    .then(Commands.argument("command", StringArgumentType.greedyString())
                                            .executes(context -> ConfigCommands.setDeathCommand(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    StringArgumentType.getString(context, "command")
                                            ))
                                    )
                            )
                    )
                    // --- Set Revive Command ---
                    .then(Commands.literal("setReviveCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermission(4))
                            .then(Commands.argument("seat", IntegerArgumentType.integer(1))
                                    .then(Commands.argument("command", StringArgumentType.greedyString())
                                            .executes(context -> ConfigCommands.setReviveCommand(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    StringArgumentType.getString(context, "command")
                                            ))
                                    )
                            )
                    )
                    // --- Set Seat Assignment Command ---
                    .then(Commands.literal("setSeatAssignmentCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermission(4))
                            .then(Commands.argument("seat", IntegerArgumentType.integer(1))
                                    .then(Commands.argument("command", StringArgumentType.greedyString())
                                            .executes(context -> ConfigCommands.setSeatAssignmentCommand(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    StringArgumentType.getString(context, "command")
                                            ))
                                    )
                            )
                    )
                    // --- Set Dusk Command ---
                    .then(Commands.literal("setDuskCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermission(4))
                            .then(Commands.argument("command", StringArgumentType.greedyString())
                                    .executes(context -> ConfigCommands.setDuskCommand(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "command")
                                    ))
                            )
                    )
                    // --- Set Dawn Command ---
                    .then(Commands.literal("setDawnCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermission(4))
                            .then(Commands.argument("command", StringArgumentType.greedyString())
                                    .executes(context -> ConfigCommands.setDawnCommand(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "command")
                                    ))
                            )
                    )
                    // --- Set Switch Position ---
                    .then(Commands.literal("setSwitchPosition")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.argument("seat", IntegerArgumentType.integer(1))
                                    .executes(context -> ConfigCommands.setSwitchPosition(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "seat"),
                                            null
                                    ))
                                    .then(Commands.argument("pos", Vec3Argument.vec3(true))
                                            .executes(context -> ConfigCommands.setSwitchPosition(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    Vec3Argument.getVec3(context, "pos")
                                            ))
                                    )
                            )
                    )
                    // --- Set Vote Indicator Position ---
                    .then(Commands.literal("setVoteIndicatorPosition")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.argument("seat", IntegerArgumentType.integer(1))
                                    .executes(context -> ConfigCommands.setVoteIndicatorPosition(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "seat"),
                                            null
                                    ))
                                    .then(Commands.argument("pos", Vec3Argument.vec3(true))
                                            .executes(context -> ConfigCommands.setVoteIndicatorPosition(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    Vec3Argument.getVec3(context, "pos")
                                            ))
                                    )
                            )
                    )
                    // --- Set Execution Command ---
                    .then(Commands.literal("setExecutionCommand")
                            // Stored commands run with the server's console-level source, so only
                            // full admins may set them; a storyteller (level 2) could otherwise
                            // schedule "op @s" or "stop" for the next dusk
                            .requires(source -> source.hasPermission(4))
                            .then(Commands.argument("seat", IntegerArgumentType.integer(1))
                                    .then(Commands.argument("command", StringArgumentType.greedyString())
                                            .executes(context -> ConfigCommands.setExecutionCommand(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "seat"),
                                                    StringArgumentType.getString(context, "command")
                                            ))
                                    )
                            )
                    )
                    // --- Set Execution Delays ---
                    .then(Commands.literal("setExecution")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.literal("soundDelay")
                                    .then(Commands.argument("delay", IntegerArgumentType.integer(0))
                                            .executes(context -> ConfigCommands.setExecutionSoundDelay(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "delay")
                                            ))
                                    )
                            )
                            .then(Commands.literal("deathTitleDelay")
                                    .then(Commands.argument("delay", IntegerArgumentType.integer(0))
                                            .executes(context -> ConfigCommands.setExecutionDeathTitleDelay(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "delay")
                                            ))
                                    )
                            )
                            .then(Commands.literal("survivedSoundDelay")
                                    .then(Commands.argument("delay", IntegerArgumentType.integer(0))
                                            .executes(context -> ConfigCommands.setExecutionSurvivedSoundDelay(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "delay")
                                            ))
                                    )
                            )
                    )
                    // --- Set Execution Position ---
                    .then(Commands.literal("setExecutionPosition")
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> ConfigCommands.setExecutionPosition(context.getSource(), null))
                            .then(Commands.argument("pos", Vec3Argument.vec3(true))
                                    .executes(context -> ConfigCommands.setExecutionPosition(
                                            context.getSource(),
                                            Vec3Argument.getVec3(context, "pos")
                                    ))
                            )
                    )
                    // --- Set Anvil Height ---
                    .then(Commands.literal("setAnvilHeight")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.argument("height", IntegerArgumentType.integer())
                                    .executes(context -> ConfigCommands.setAnvilHeight(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "height")
                                    ))
                            )
                    )
                    // --- Lock In Execution Position ---
                    .then(Commands.literal("lockInExecutionPosition")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.literal("true")
                                    .executes(context -> ConfigCommands.setLockInExecutionPosition(context.getSource(), true))
                            )
                            .then(Commands.literal("false")
                                    .executes(context -> ConfigCommands.setLockInExecutionPosition(context.getSource(), false))
                            )
                    )
                    // --- Set Time Commands ---
                    .then(Commands.literal("setTime")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.literal("dawn")
                                    .then(Commands.argument("time", IntegerArgumentType.integer(0, 24000))
                                            .executes(context -> ConfigCommands.setTimeDawn(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "time")
                                            ))
                                    )
                            )
                            .then(Commands.literal("evening")
                                    .then(Commands.argument("time", IntegerArgumentType.integer(0, 24000))
                                            .executes(context -> ConfigCommands.setTimeEvening(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "time")
                                            ))
                                    )
                            )
                            .then(Commands.literal("dusk")
                                    .then(Commands.argument("time", IntegerArgumentType.integer(0, 24000))
                                            .executes(context -> ConfigCommands.setTimeDusk(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "time")
                                            ))
                                    )
                            )
                    )
                    // --- Set Clock Center Command ---
                    .then(Commands.literal("setClockCenter")
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> ConfigCommands.setClockCenter(context.getSource(), null))
                            .then(Commands.argument("pos", Vec3Argument.vec3(true))
                                    .executes(context -> ConfigCommands.setClockCenter(
                                            context.getSource(),
                                            Vec3Argument.getVec3(context, "pos")
                                    ))
                            )
                    )
                    // --- Set Vote Time Per Player Command ---
                    .then(Commands.literal("setVoteTimePerPlayer")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.argument("millis", IntegerArgumentType.integer(0))
                                    .executes(context -> ConfigCommands.setVoteTimePerPlayer(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "millis")
                                    ))
                            )
                    )
                    // --- Set Name Max Length Command ---
                    .then(Commands.literal("setNameMaxLength")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.argument("length", IntegerArgumentType.integer(1, 64))
                                    .executes(context -> ConfigCommands.setNameMaxLength(
                                            context.getSource(),
                                            IntegerArgumentType.getInteger(context, "length")
                                    ))
                            )
                    )
                    // --- Set Clock Hand Scale Command ---
                    .then(Commands.literal("setClockHandScale")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.argument("scale", FloatArgumentType.floatArg(0.1f, 10.0f))
                                    .executes(context -> ConfigCommands.setClockHandScale(
                                            context.getSource(),
                                            FloatArgumentType.getFloat(context, "scale")
                                    ))
                            )
                    )
                    // --- End Game Command ---
                    .then(Commands.literal("endGame")
                            .requires(source -> source.hasPermission(2))
                            .then(Commands.literal("good")
                                    .executes(context -> GameCommands.endGame(context.getSource(), true))
                            )
                            .then(Commands.literal("evil")
                                    .executes(context -> GameCommands.endGame(context.getSource(), false))
                            )
                    )
                    // --- Reset Game (soft) Command (revive all, unassign players but keep storyteller grimoire) ---
                    .then(Commands.literal("resetGame")
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> GameCommands.resetGame(context.getSource()))
                    )
                    // --- Reset Game Hard Command (full reset including grimoire) ---
                    .then(Commands.literal("resetGameHard")
                            .requires(source -> source.hasPermission(2))
                            .executes(context -> GameCommands.resetGameHard(context.getSource()))
                    )
                    // --- Set Name Command (no permission requirement - players can set their own name) ---
                    .then(Commands.literal("setName")
                            .executes(context -> GameCommands.setPlayerName(context.getSource(), ""))
                            .then(Commands.argument("name", StringArgumentType.greedyString())
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
