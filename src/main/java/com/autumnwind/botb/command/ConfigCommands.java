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

            source.sendFeedback(() -> Text.literal("Set home for seat " + seat + " to " + blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting seat home: " + e.getMessage()));
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

            source.sendFeedback(() -> Text.literal("Set town square seat for seat " + seat + " to " + blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting town square seat: " + e.getMessage()));
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

            source.sendFeedback(() -> Text.literal("Set Town Square to " + blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting Town Square: " + e.getMessage()));
            return 0;
        }
    }

    static int setDeathCommand(ServerCommandSource source, int seat, String command) {
        try {
            ServerConfig.DEATH_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Set death command for seat " + seat + " to: " + command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting death command: " + e.getMessage()));
            return 0;
        }
    }

    static int setReviveCommand(ServerCommandSource source, int seat, String command) {
        try {
            ServerConfig.REVIVE_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Set revive command for seat " + seat + " to: " + command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting revive command: " + e.getMessage()));
            return 0;
        }
    }

    static int setSeatAssignmentCommand(ServerCommandSource source, int seat, String command) {
        try {
            ServerConfig.SEAT_ASSIGNMENT_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Set seat assignment command for seat " + seat + " to: " + command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting seat assignment command: " + e.getMessage()));
            return 0;
        }
    }

    static int setDuskCommand(ServerCommandSource source, String command) {
        try {
            ServerConfig.DUSK_COMMAND = command;
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Set dusk command to: " + command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting dusk command: " + e.getMessage()));
            return 0;
        }
    }

    static int setDawnCommand(ServerCommandSource source, String command) {
        try {
            ServerConfig.DAWN_COMMAND = command;
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Set dawn command to: " + command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting dawn command: " + e.getMessage()));
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

            source.sendFeedback(() -> Text.literal("Set switch position for seat " + seat + " to " + blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting switch position: " + e.getMessage()));
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

            source.sendFeedback(() -> Text.literal("Set vote indicator position for seat " + seat + " to " + blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting vote indicator position: " + e.getMessage()));
            return 0;
        }
    }

    static int setExecutionCommand(ServerCommandSource source, int seat, String command) {
        try {
            ServerConfig.EXECUTION_COMMANDS.put(seat, command);
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Set execution command for seat " + seat + " to: " + command)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting execution command: " + e.getMessage()));
            return 0;
        }
    }

    static int setVoteTimePerPlayer(ServerCommandSource source, int millis) {
        try {
            ServerConfig.VOTE_TIME_PER_PLAYER = millis;
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Set vote time per player to " + millis + " ms")
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting vote time per player: " + e.getMessage()));
            return 0;
        }
    }

    static int setExecutionSoundDelay(ServerCommandSource source, int delay) {
        try {
            ServerConfig.EXECUTION_SOUND_DELAY = delay;
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Set execution sound delay to " + delay + " ms")
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting execution sound delay: " + e.getMessage()));
            return 0;
        }
    }

    static int setExecutionDeathTitleDelay(ServerCommandSource source, int delay) {
        try {
            ServerConfig.EXECUTION_DEATH_TITLE_DELAY = delay;
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Set execution death title delay to " + delay + " ms")
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting execution death title delay: " + e.getMessage()));
            return 0;
        }
    }

    static int setExecutionSurvivedSoundDelay(ServerCommandSource source, int delay) {
        try {
            ServerConfig.EXECUTION_SURVIVED_SOUND_DELAY = delay;
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Set execution survived sound delay to " + delay + " ms")
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting execution survived sound delay: " + e.getMessage()));
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

            source.sendFeedback(() -> Text.literal("Set execution position to " + blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting execution position: " + e.getMessage()));
            return 0;
        }
    }

    static int setAnvilHeight(ServerCommandSource source, int height) {
        try {
            ServerConfig.ANVIL_HEIGHT = height;
            ServerConfig.save();

            if (height <= 0) {
                source.sendFeedback(() -> Text.literal("Anvil disabled (height set to " + height + ")")
                        .formatted(Formatting.GREEN), true);
            } else {
                source.sendFeedback(() -> Text.literal("Set anvil height to " + height + " blocks above execution position")
                        .formatted(Formatting.GREEN), true);
            }
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting anvil height: " + e.getMessage()));
            return 0;
        }
    }

    static int setLockInExecutionPosition(ServerCommandSource source, boolean lock) {
        try {
            ServerConfig.LOCK_IN_EXECUTION_POSITION = lock;
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Lock in execution position set to " + lock)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting lock in execution position: " + e.getMessage()));
            return 0;
        }
    }

    static int setTimeDawn(ServerCommandSource source, int time) {
        try {
            ServerConfig.TIME_DAWN = time;
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Dawn time set to " + time)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting dawn time: " + e.getMessage()));
            return 0;
        }
    }

    static int setTimeEvening(ServerCommandSource source, int time) {
        try {
            ServerConfig.TIME_EVENING = time;
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Evening time set to " + time)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting evening time: " + e.getMessage()));
            return 0;
        }
    }

    static int setTimeDusk(ServerCommandSource source, int time) {
        try {
            ServerConfig.TIME_DUSK = time;
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Dusk time set to " + time)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting dusk time: " + e.getMessage()));
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

            source.sendFeedback(() -> Text.literal("Set clock center to " + blockPos.toShortString())
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting clock center: " + e.getMessage()));
            return 0;
        }
    }

    static int setClockHandScale(ServerCommandSource source, float scale) {
        try {
            ServerConfig.CLOCK_HAND_SCALE = scale;
            ServerConfig.save();

            source.sendFeedback(() -> Text.literal("Set clock hand scale to " + scale)
                    .formatted(Formatting.GREEN), true);
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("Error setting clock hand scale: " + e.getMessage()));
            return 0;
        }
    }

    static int setNameMaxLength(ServerCommandSource source, int length) {
        try {
            ServerConfig.MAX_NAME_LENGTH = length;
            ServerConfig.save();
            source.sendFeedback(() -> Text.literal("Custom names are now limited to " + length + " characters")
                    .formatted(Formatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error setting name length: " + e.getMessage()));
            return 0;
        }
    }
}
