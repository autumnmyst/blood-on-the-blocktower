package com.autumnwind.botb.command;

import com.autumnwind.botb.config.ServerConfig;
import java.util.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Handlers for the /botb set* commands that write the world-scoped {@link ServerConfig}. */
final class ConfigCommands {

    private ConfigCommands() {}

    static int setSeatHome(ServerCommandSource source, int seat, Vec3d pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrThrow().getBlockPos();
            } else {
                blockPos = BlockPos.ofFloored(pos);
            }

            ServerConfig.SEAT_HOMES.put(seat, blockPos);
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.seat_home", seat, blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.seat_home", e.getMessage()));
            return 0;
        }
    }

    static int setTownSquareSeat(ServerCommandSource source, int seat, Vec3d pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrThrow().getBlockPos();
            } else {
                blockPos = BlockPos.ofFloored(pos);
            }

            ServerConfig.TOWN_SQUARE_SEATS.put(seat, blockPos);
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.town_square_seat", seat, blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.town_square_seat", e.getMessage()));
            return 0;
        }
    }

    static int setTownSquare(ServerCommandSource source, Vec3d pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrThrow().getBlockPos();
            } else {
                blockPos = BlockPos.ofFloored(pos);
            }

            ServerConfig.TOWN_SQUARE = blockPos;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.town_square", blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.town_square", e.getMessage()));
            return 0;
        }
    }

    static int setDeathCommand(ServerCommandSource source, int seat, String command) {
        try {
            ServerConfig.DEATH_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.death_command", seat, command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.death_command", e.getMessage()));
            return 0;
        }
    }

    static int setReviveCommand(ServerCommandSource source, int seat, String command) {
        try {
            ServerConfig.REVIVE_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.revive_command", seat, command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.revive_command", e.getMessage()));
            return 0;
        }
    }

    static int setSeatAssignmentCommand(ServerCommandSource source, int seat, String command) {
        try {
            ServerConfig.SEAT_ASSIGNMENT_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.seat_assignment_command", seat, command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.seat_assignment_command", e.getMessage()));
            return 0;
        }
    }

    static int setDuskCommand(ServerCommandSource source, String command) {
        try {
            ServerConfig.DUSK_COMMAND = command;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.dusk_command", command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.dusk_command", e.getMessage()));
            return 0;
        }
    }

    static int setDawnCommand(ServerCommandSource source, String command) {
        try {
            ServerConfig.DAWN_COMMAND = command;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.dawn_command", command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.dawn_command", e.getMessage()));
            return 0;
        }
    }

    static int setSwitchPosition(ServerCommandSource source, int seat, Vec3d pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrThrow().getBlockPos();
            } else {
                blockPos = BlockPos.ofFloored(pos);
            }

            ServerConfig.SEAT_SWITCH_POSITIONS.put(seat, blockPos);
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.switch_position", seat, blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.switch_position", e.getMessage()));
            return 0;
        }
    }

    static int setVoteIndicatorPosition(ServerCommandSource source, int seat, Vec3d pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrThrow().getBlockPos();
            } else {
                blockPos = BlockPos.ofFloored(pos);
            }

            ServerConfig.SEAT_VOTE_INDICATOR_POSITIONS.put(seat, blockPos);
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.vote_indicator_position", seat, blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.vote_indicator_position", e.getMessage()));
            return 0;
        }
    }

    static int setExecutionCommand(ServerCommandSource source, int seat, String command) {
        try {
            ServerConfig.EXECUTION_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.execution_command", seat, command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.execution_command", e.getMessage()));
            return 0;
        }
    }

    static int setVoteTimePerPlayer(ServerCommandSource source, int millis) {
        try {
            ServerConfig.VOTE_TIME_PER_PLAYER = millis;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.vote_time_per_player", millis)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.vote_time_per_player", e.getMessage()));
            return 0;
        }
    }

    static int setExecutionSoundDelay(ServerCommandSource source, int delay) {
        try {
            ServerConfig.EXECUTION_SOUND_DELAY = delay;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.execution_sound_delay", delay)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.execution_sound_delay", e.getMessage()));
            return 0;
        }
    }

    static int setExecutionDeathTitleDelay(ServerCommandSource source, int delay) {
        try {
            ServerConfig.EXECUTION_DEATH_TITLE_DELAY = delay;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.execution_death_title_delay", delay)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.execution_death_title_delay", e.getMessage()));
            return 0;
        }
    }

    static int setExecutionSurvivedSoundDelay(ServerCommandSource source, int delay) {
        try {
            ServerConfig.EXECUTION_SURVIVED_SOUND_DELAY = delay;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.execution_survived_sound_delay", delay)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.execution_survived_sound_delay", e.getMessage()));
            return 0;
        }
    }

    static int setExecutionPosition(ServerCommandSource source, Vec3d pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrThrow().getBlockPos();
            } else {
                blockPos = BlockPos.ofFloored(pos);
            }

            ServerConfig.EXECUTION_POSITION = blockPos;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.execution_position", blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.execution_position", e.getMessage()));
            return 0;
        }
    }

    static int setAnvilHeight(ServerCommandSource source, int height) {
        try {
            ServerConfig.ANVIL_HEIGHT = height;
            ServerConfig.save();

            if (height <= 0) {
                source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.anvil_disabled", height)
                        .formatted(Formatting.GREEN), true);
            } else {
                source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.anvil_height", height)
                        .formatted(Formatting.GREEN), true);
            }
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.anvil_height", e.getMessage()));
            return 0;
        }
    }

    static int setLockInExecutionPosition(ServerCommandSource source, boolean lock) {
        try {
            ServerConfig.LOCK_IN_EXECUTION_POSITION = lock;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.lock_in_execution_position", lock)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.lock_in_execution_position", e.getMessage()));
            return 0;
        }
    }

    static int setTimeDawn(ServerCommandSource source, int time) {
        try {
            ServerConfig.TIME_DAWN = time;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.dawn_time", time)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.dawn_time", e.getMessage()));
            return 0;
        }
    }

    static int setTimeEvening(ServerCommandSource source, int time) {
        try {
            ServerConfig.TIME_EVENING = time;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.evening_time", time)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.evening_time", e.getMessage()));
            return 0;
        }
    }

    static int setTimeDusk(ServerCommandSource source, int time) {
        try {
            ServerConfig.TIME_DUSK = time;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.dusk_time", time)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.dusk_time", e.getMessage()));
            return 0;
        }
    }

    static int setClockCenter(ServerCommandSource source, Vec3d pos) {
        BlockPos blockPos;
        try {
            if (pos == null) {
                blockPos = source.getPlayerOrThrow().getBlockPos();
            } else {
                blockPos = BlockPos.ofFloored(pos);
            }

            ServerConfig.CLOCK_CENTER = blockPos;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.clock_center", blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.clock_center", e.getMessage()));
            return 0;
        }
    }

    static int setClockHandScale(ServerCommandSource source, float scale) {
        try {
            ServerConfig.CLOCK_HAND_SCALE = scale;
            ServerConfig.save();

            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.clock_hand_scale", scale)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.clock_hand_scale", e.getMessage()));
            return 0;
        }
    }

    static int setNameMaxLength(ServerCommandSource source, int length) {
        try {
            ServerConfig.MAX_NAME_LENGTH = length;
            ServerConfig.save();
            source.sendFeedback(() -> Text.translatable("message.blood-on-the-blocktower.config.name_max_length", length)
                    .formatted(Formatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.translatable("message.blood-on-the-blocktower.config.error.name_length", e.getMessage()));
            return 0;
        }
    }
}
