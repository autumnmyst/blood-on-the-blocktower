package com.autumnwind.botb.command;

import com.autumnwind.botb.config.ServerConfig;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Handlers for the /botb set* commands that write the world-scoped {@link ServerConfig}. */
final class ConfigCommands {

    private ConfigCommands() {}

    static int setSeatHome(CommandSourceStack source, int seat, Vec3 pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrException().blockPosition();
            } else {
                blockPos = BlockPos.containing(pos);
            }

            ServerConfig.SEAT_HOMES.put(seat, blockPos);
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.seat_home", seat, blockPos.toShortString())
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.seat_home", e.getMessage()));
            return 0;
        }
    }

    static int setTownSquareSeat(CommandSourceStack source, int seat, Vec3 pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrException().blockPosition();
            } else {
                blockPos = BlockPos.containing(pos);
            }

            ServerConfig.TOWN_SQUARE_SEATS.put(seat, blockPos);
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.town_square_seat", seat, blockPos.toShortString())
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.town_square_seat", e.getMessage()));
            return 0;
        }
    }

    static int setTownSquare(CommandSourceStack source, Vec3 pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrException().blockPosition();
            } else {
                blockPos = BlockPos.containing(pos);
            }

            ServerConfig.TOWN_SQUARE = blockPos;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.town_square", blockPos.toShortString())
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.town_square", e.getMessage()));
            return 0;
        }
    }

    static int setDeathCommand(CommandSourceStack source, int seat, String command) {
        try {
            ServerConfig.DEATH_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.death_command", seat, command)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.death_command", e.getMessage()));
            return 0;
        }
    }

    static int setReviveCommand(CommandSourceStack source, int seat, String command) {
        try {
            ServerConfig.REVIVE_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.revive_command", seat, command)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.revive_command", e.getMessage()));
            return 0;
        }
    }

    static int setSeatAssignmentCommand(CommandSourceStack source, int seat, String command) {
        try {
            ServerConfig.SEAT_ASSIGNMENT_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.seat_assignment_command", seat, command)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.seat_assignment_command", e.getMessage()));
            return 0;
        }
    }

    static int setDuskCommand(CommandSourceStack source, String command) {
        try {
            ServerConfig.DUSK_COMMAND = command;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.dusk_command", command)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.dusk_command", e.getMessage()));
            return 0;
        }
    }

    static int setDawnCommand(CommandSourceStack source, String command) {
        try {
            ServerConfig.DAWN_COMMAND = command;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.dawn_command", command)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.dawn_command", e.getMessage()));
            return 0;
        }
    }

    static int setSwitchPosition(CommandSourceStack source, int seat, Vec3 pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrException().blockPosition();
            } else {
                blockPos = BlockPos.containing(pos);
            }

            ServerConfig.SEAT_SWITCH_POSITIONS.put(seat, blockPos);
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.switch_position", seat, blockPos.toShortString())
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.switch_position", e.getMessage()));
            return 0;
        }
    }

    static int setVoteIndicatorPosition(CommandSourceStack source, int seat, Vec3 pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrException().blockPosition();
            } else {
                blockPos = BlockPos.containing(pos);
            }

            ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.put(seat, blockPos);
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.vote_indicator_position", seat, blockPos.toShortString())
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.vote_indicator_position", e.getMessage()));
            return 0;
        }
    }

    static int setExecutionCommand(CommandSourceStack source, int seat, String command) {
        try {
            ServerConfig.EXECUTION_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.execution_command", seat, command)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.execution_command", e.getMessage()));
            return 0;
        }
    }

    static int setVoteTimePerPlayer(CommandSourceStack source, int millis) {
        try {
            ServerConfig.VOTE_TIME_PER_PLAYER = millis;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.vote_time_per_player", millis)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.vote_time_per_player", e.getMessage()));
            return 0;
        }
    }

    static int setExecutionSoundDelay(CommandSourceStack source, int delay) {
        try {
            ServerConfig.EXECUTION_SOUND_DELAY = delay;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.execution_sound_delay", delay)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.execution_sound_delay", e.getMessage()));
            return 0;
        }
    }

    static int setExecutionDeathTitleDelay(CommandSourceStack source, int delay) {
        try {
            ServerConfig.EXECUTION_DEATH_TITLE_DELAY = delay;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.execution_death_title_delay", delay)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.execution_death_title_delay", e.getMessage()));
            return 0;
        }
    }

    static int setExecutionSurvivedSoundDelay(CommandSourceStack source, int delay) {
        try {
            ServerConfig.EXECUTION_SURVIVED_SOUND_DELAY = delay;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.execution_survived_sound_delay", delay)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.execution_survived_sound_delay", e.getMessage()));
            return 0;
        }
    }

    static int setExecutionPosition(CommandSourceStack source, Vec3 pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrException().blockPosition();
            } else {
                blockPos = BlockPos.containing(pos);
            }

            ServerConfig.EXECUTION_POSITION = blockPos;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.execution_position", blockPos.toShortString())
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.execution_position", e.getMessage()));
            return 0;
        }
    }

    static int setAnvilHeight(CommandSourceStack source, int height) {
        try {
            ServerConfig.ANVIL_HEIGHT = height;
            ServerConfig.save();

            if (height <= 0) {
                source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.anvil_disabled", height)
                        .withStyle(ChatFormatting.GREEN), true);
            } else {
                source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.anvil_height", height)
                        .withStyle(ChatFormatting.GREEN), true);
            }
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.anvil_height", e.getMessage()));
            return 0;
        }
    }

    static int setLockInExecutionPosition(CommandSourceStack source, boolean lock) {
        try {
            ServerConfig.LOCK_IN_EXECUTION_POSITION = lock;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.lock_in_execution_position", lock)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.lock_in_execution_position", e.getMessage()));
            return 0;
        }
    }

    static int setTimeDawn(CommandSourceStack source, int time) {
        try {
            ServerConfig.TIME_DAWN = time;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.dawn_time", time)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.dawn_time", e.getMessage()));
            return 0;
        }
    }

    static int setTimeEvening(CommandSourceStack source, int time) {
        try {
            ServerConfig.TIME_EVENING = time;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.evening_time", time)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.evening_time", e.getMessage()));
            return 0;
        }
    }

    static int setTimeDusk(CommandSourceStack source, int time) {
        try {
            ServerConfig.TIME_DUSK = time;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.dusk_time", time)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.dusk_time", e.getMessage()));
            return 0;
        }
    }

    static int setClockCenter(CommandSourceStack source, Vec3 pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrException().blockPosition();
            } else {
                blockPos = BlockPos.containing(pos);
            }

            ServerConfig.CLOCK_CENTER = blockPos;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.clock_center", blockPos.toShortString())
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.clock_center", e.getMessage()));
            return 0;
        }
    }

    static int setClockHandScale(CommandSourceStack source, float scale) {
        try {
            ServerConfig.CLOCK_HAND_SCALE = scale;
            ServerConfig.save();

            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.clock_hand_scale", scale)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.clock_hand_scale", e.getMessage()));
            return 0;
        }
    }

    static int setNameMaxLength(CommandSourceStack source, int length) {
        try {
            ServerConfig.MAX_NAME_LENGTH = length;
            ServerConfig.save();
            source.sendSuccess(() -> Component.translatable("message.blood-on-the-blocktower.config.name_max_length", length)
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.blood-on-the-blocktower.config.error.name_length", e.getMessage()));
            return 0;
        }
    }
}
