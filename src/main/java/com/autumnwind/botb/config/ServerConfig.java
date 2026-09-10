package com.autumnwind.botb.config;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.autumnwind.botb.util.CustomNames;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class ServerConfig {
    /**
     * The config is world-scoped: each save carries its own botb_server.json in its
     * world folder, so seat/lever/home layouts follow the build they describe. The
     * path is resolved when a server starts; null until then.
     */
    private static Path configFile = null;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Map of seat number to BlockPos
    public static final Map<Integer, BlockPos> SEAT_HOMES = new HashMap<>();

    // Town Square seat locations (for sending players to their town square seats)
    public static final Map<Integer, BlockPos> TOWN_SQUARE_SEATS = new HashMap<>();

    // Town Square location
    public static BlockPos TOWN_SQUARE = null;

    // Death and revive commands per seat
    public static final Map<Integer, String> DEATH_COMMANDS = new HashMap<>();
    public static final Map<Integer, String> REVIVE_COMMANDS = new HashMap<>();

    // Seat assignment commands per seat
    public static final Map<Integer, String> SEAT_ASSIGNMENT_COMMANDS = new HashMap<>();

    // Dusk and Dawn commands
    public static String DUSK_COMMAND = null;
    public static String DAWN_COMMAND = null;

    // Voting system - switch positions per seat
    public static final Map<Integer, BlockPos> SEAT_SWITCH_POSITIONS = new HashMap<>();

    // Voting system - vote indicator block positions per seat
    public static final Map<Integer, BlockPos> SEAT_VOTE_INDICATOR_POSITIONS = new HashMap<>();

    // Execution commands per seat
    public static final Map<Integer, String> EXECUTION_COMMANDS = new HashMap<>();

    // Execution delays (in milliseconds)
    public static int EXECUTION_SOUND_DELAY = 2100;
    public static int EXECUTION_SURVIVED_SOUND_DELAY = 3500; // Separate delay for failed executions
    public static int EXECUTION_DEATH_TITLE_DELAY = 3500;

    // Execution position and anvil settings
    public static BlockPos EXECUTION_POSITION = null;
    public static int ANVIL_HEIGHT = 70; // Height above execution position to spawn anvil (0 = no anvil)
    public static boolean LOCK_IN_EXECUTION_POSITION = true; // Whether to lock player in place during execution

    // Voting system - global settings
    public static String VOTE_INDICATOR_BLOCK_ON = "minecraft:shroomlight";
    public static String VOTE_INDICATOR_BLOCK_OFF = "minecraft:waxed_copper_block";
    public static String VOTE_INDICATOR_BLOCK_GHOST_ON = "minecraft:sea_lantern";
    public static String VOTE_INDICATOR_BLOCK_GHOST_OFF = "minecraft:waxed_oxidized_copper";
    public static String VOTE_INDICATOR_BLOCK_GHOST_USED = "minecraft:obsidian";
    public static String VOTE_INDICATOR_BLOCK_DOUBLE = "minecraft:verdant_froglight"; // Banshee double vote
    public static String VOTE_INDICATOR_BLOCK_UNSEATED = "minecraft:bedrock"; // Unseated player indicator
    public static int VOTE_TIME_PER_PLAYER = 1000; // milliseconds

    // Exile support indicator blocks (for traveler exile)
    public static String EXILE_SUPPORT_INDICATOR_BLOCK_ON = "minecraft:pearlescent_froglight";
    public static String EXILE_SUPPORT_INDICATOR_BLOCK_OFF = "minecraft:amethyst_block";

    // In-game time settings (Minecraft time: 0=dawn, 6000=noon, 12000=dusk, 18000=midnight)
    public static int TIME_DAWN = 0;           // Default: sunrise
    public static int TIME_EVENING = 13000;    // Default: just after sunset
    public static int TIME_DUSK = 18000;       // Default: midnight

    // Clock hands settings
    public static BlockPos CLOCK_CENTER = null;  // Center position for clock hands
    public static float CLOCK_HAND_SCALE = 4.0f; // Scale multiplier for clock hands

    // Custom names players have set via /botb setName, keyed by UUID string. Loaded into
    // CustomNames on startup, which the player name mixins read, so names survive rejoins
    // and restarts.
    public static final Map<String, String> CUSTOM_PLAYER_NAMES = new HashMap<>();
    public static int MAX_NAME_LENGTH = 16; // longest custom name /botb setName accepts

    private static class ConfigData {
        Map<String, String> seatHomes = new HashMap<>();
        Map<String, String> townSquareSeats = new HashMap<>();
        String townSquare = null;
        Map<String, String> deathCommands = new HashMap<>();
        Map<String, String> reviveCommands = new HashMap<>();
        Map<String, String> seatAssignmentCommands = new HashMap<>();
        String duskCommand = null;
        String dawnCommand = null;
        Map<String, String> seatSwitchPositions = new HashMap<>();
        Map<String, String> seatVoteIndicatorPositions = new HashMap<>();
        Map<String, String> executionCommands = new HashMap<>();
        String voteIndicatorBlockOn = "minecraft:shroomlight";
        String voteIndicatorBlockOff = "minecraft:waxed_copper_block";
        String voteIndicatorBlockGhostOn = "minecraft:sea_lantern";
        String voteIndicatorBlockGhostOff = "minecraft:waxed_oxidized_copper";
        String voteIndicatorBlockGhostUsed = "minecraft:obsidian";
        String voteIndicatorBlockDouble = "minecraft:verdant_froglight";
        String voteIndicatorBlockUnseated = "minecraft:bedrock";
        Integer voteTimePerPlayer = 1000;
        // Exile support indicator blocks
        String exileSupportIndicatorBlockOn = "minecraft:pearlescent_froglight";
        String exileSupportIndicatorBlockOff = "minecraft:amethyst_block";
        Integer executionSoundDelay = 2100;
        Integer executionSurvivedSoundDelay = 3500;
        Integer executionDeathTitleDelay = 3500;
        String executionPosition = null;
        Integer anvilHeight = 70;
        Boolean lockInExecutionPosition = true;
        Integer timeDawn = 0;
        Integer timeEvening = 13000;
        Integer timeDusk = 18000;
        // Clock hands settings
        String clockCenter = null;
        Float clockHandScale = 4.0f;
        Integer maxNameLength = 16;
        // UUID string → custom display name (set via /botb setName).
        Map<String, String> customPlayerNames = new HashMap<>();
        // Whisper rules, see WhisperSettings. Null on disk means "use defaults".
        WhisperSettingsData whisperSettings = null;
    }

    /**
     * Disk shape for {@link WhisperSettings}. Kept as a separate POJO so missing
     * fields in older config files deserialize as null and fall back to defaults
     * rather than throwing.
     */
    private static class WhisperSettingsData {
        Boolean allowWhispering;
        Boolean broadcast;
        String visual;       // "OFF" | "RUNES"
        Boolean audio;
        Double range;        // null or <=0 means unlimited
        Boolean vcEnforced;
    }

    /**
     * Resets every field to its built-in default. Runs before each load so values from
     * a previously opened world can't leak into one whose file doesn't set them.
     */
    private static void resetToDefaults() {
        SEAT_HOMES.clear();
        TOWN_SQUARE_SEATS.clear();
        TOWN_SQUARE = null;
        DEATH_COMMANDS.clear();
        REVIVE_COMMANDS.clear();
        SEAT_ASSIGNMENT_COMMANDS.clear();
        DUSK_COMMAND = null;
        DAWN_COMMAND = null;
        SEAT_SWITCH_POSITIONS.clear();
        SEAT_VOTE_INDICATOR_POSITIONS.clear();
        EXECUTION_COMMANDS.clear();
        EXECUTION_SOUND_DELAY = 2100;
        EXECUTION_SURVIVED_SOUND_DELAY = 3500;
        EXECUTION_DEATH_TITLE_DELAY = 3500;
        EXECUTION_POSITION = null;
        ANVIL_HEIGHT = 70;
        LOCK_IN_EXECUTION_POSITION = true;
        VOTE_INDICATOR_BLOCK_ON = "minecraft:shroomlight";
        VOTE_INDICATOR_BLOCK_OFF = "minecraft:waxed_copper_block";
        VOTE_INDICATOR_BLOCK_GHOST_ON = "minecraft:sea_lantern";
        VOTE_INDICATOR_BLOCK_GHOST_OFF = "minecraft:waxed_oxidized_copper";
        VOTE_INDICATOR_BLOCK_GHOST_USED = "minecraft:obsidian";
        VOTE_INDICATOR_BLOCK_DOUBLE = "minecraft:verdant_froglight";
        VOTE_INDICATOR_BLOCK_UNSEATED = "minecraft:bedrock";
        VOTE_TIME_PER_PLAYER = 1000;
        EXILE_SUPPORT_INDICATOR_BLOCK_ON = "minecraft:pearlescent_froglight";
        EXILE_SUPPORT_INDICATOR_BLOCK_OFF = "minecraft:amethyst_block";
        TIME_DAWN = 0;
        TIME_EVENING = 13000;
        TIME_DUSK = 18000;
        CLOCK_CENTER = null;
        CLOCK_HAND_SCALE = 4.0f;
        MAX_NAME_LENGTH = 16;
        CUSTOM_PLAYER_NAMES.clear();
        CustomNames.replaceAll(new HashMap<>());
        WhisperSettingsManager.set(WhisperSettings.DEFAULT);
    }

    /** Loads the starting server's world-scoped config, creating or migrating it as needed. */
    public static void load(MinecraftServer server) {
        configFile = server.getSavePath(WorldSavePath.ROOT).resolve("botb_server.json");
        resetToDefaults();

        if (Files.exists(configFile)) {
            try (var in = Files.newBufferedReader(configFile)) {
                ConfigData data = GSON.fromJson(in, ConfigData.class);

                if (data != null) {
                    // Load seat homes
                    SEAT_HOMES.clear();
                    if (data.seatHomes != null) {
                        data.seatHomes.forEach((seatStr, posStr) -> {
                            try {
                                int seat = Integer.parseInt(seatStr);
                                String[] parts = posStr.split(",");
                                int x = Integer.parseInt(parts[0]);
                                int y = Integer.parseInt(parts[1]);
                                int z = Integer.parseInt(parts[2]);
                                SEAT_HOMES.put(seat, new BlockPos(x, y, z));
                            } catch (Exception e) {
                                BloodOnTheBlocktower.LOGGER.warn("Failed to parse seat home entry: " + seatStr + " = " + posStr);
                            }
                        });
                    }

                    // Load town square seats
                    TOWN_SQUARE_SEATS.clear();
                    if (data.townSquareSeats != null) {
                        data.townSquareSeats.forEach((seatStr, posStr) -> {
                            try {
                                int seat = Integer.parseInt(seatStr);
                                String[] parts = posStr.split(",");
                                int x = Integer.parseInt(parts[0]);
                                int y = Integer.parseInt(parts[1]);
                                int z = Integer.parseInt(parts[2]);
                                TOWN_SQUARE_SEATS.put(seat, new BlockPos(x, y, z));
                            } catch (Exception e) {
                                BloodOnTheBlocktower.LOGGER.warn("Failed to parse town square seat entry: " + seatStr + " = " + posStr);
                            }
                        });
                    }

                    // Load town square
                    if (data.townSquare != null) {
                        try {
                            String[] parts = data.townSquare.split(",");
                            int x = Integer.parseInt(parts[0]);
                            int y = Integer.parseInt(parts[1]);
                            int z = Integer.parseInt(parts[2]);
                            TOWN_SQUARE = new BlockPos(x, y, z);
                        } catch (Exception e) {
                            BloodOnTheBlocktower.LOGGER.warn("Failed to parse town square: " + data.townSquare);
                        }
                    }

                    // Load death commands
                    DEATH_COMMANDS.clear();
                    if (data.deathCommands != null) {
                        data.deathCommands.forEach((seatStr, cmd) -> {
                            try {
                                DEATH_COMMANDS.put(Integer.parseInt(seatStr), cmd);
                            } catch (Exception e) {
                                BloodOnTheBlocktower.LOGGER.warn("Failed to parse death command for seat: " + seatStr);
                            }
                        });
                    }

                    // Load revive commands
                    REVIVE_COMMANDS.clear();
                    if (data.reviveCommands != null) {
                        data.reviveCommands.forEach((seatStr, cmd) -> {
                            try {
                                REVIVE_COMMANDS.put(Integer.parseInt(seatStr), cmd);
                            } catch (Exception e) {
                                BloodOnTheBlocktower.LOGGER.warn("Failed to parse revive command for seat: " + seatStr);
                            }
                        });
                    }

                    // Load seat assignment commands
                    SEAT_ASSIGNMENT_COMMANDS.clear();
                    if (data.seatAssignmentCommands != null) {
                        data.seatAssignmentCommands.forEach((seatStr, cmd) -> {
                            try {
                                SEAT_ASSIGNMENT_COMMANDS.put(Integer.parseInt(seatStr), cmd);
                            } catch (Exception e) {
                                BloodOnTheBlocktower.LOGGER.warn("Failed to parse seat assignment command for seat: " + seatStr);
                            }
                        });
                    }

                    // Load dusk and dawn commands
                    DUSK_COMMAND = data.duskCommand;
                    DAWN_COMMAND = data.dawnCommand;

                    // Load seat switch positions
                    SEAT_SWITCH_POSITIONS.clear();
                    if (data.seatSwitchPositions != null) {
                        data.seatSwitchPositions.forEach((seatStr, posStr) -> {
                            try {
                                int seat = Integer.parseInt(seatStr);
                                String[] parts = posStr.split(",");
                                int x = Integer.parseInt(parts[0]);
                                int y = Integer.parseInt(parts[1]);
                                int z = Integer.parseInt(parts[2]);
                                SEAT_SWITCH_POSITIONS.put(seat, new BlockPos(x, y, z));
                            } catch (Exception e) {
                                BloodOnTheBlocktower.LOGGER.warn("Failed to parse seat switch position entry: " + seatStr + " = " + posStr);
                            }
                        });
                    }

                    // Load seat vote indicator positions
                    SEAT_VOTE_INDICATOR_POSITIONS.clear();
                    if (data.seatVoteIndicatorPositions != null) {
                        data.seatVoteIndicatorPositions.forEach((seatStr, posStr) -> {
                            try {
                                int seat = Integer.parseInt(seatStr);
                                String[] parts = posStr.split(",");
                                int x = Integer.parseInt(parts[0]);
                                int y = Integer.parseInt(parts[1]);
                                int z = Integer.parseInt(parts[2]);
                                SEAT_VOTE_INDICATOR_POSITIONS.put(seat, new BlockPos(x, y, z));
                            } catch (Exception e) {
                                BloodOnTheBlocktower.LOGGER.warn("Failed to parse seat vote indicator position entry: " + seatStr + " = " + posStr);
                            }
                        });
                    }

                    // Load execution commands
                    EXECUTION_COMMANDS.clear();
                    if (data.executionCommands != null) {
                        data.executionCommands.forEach((seatStr, cmd) -> {
                            try {
                                EXECUTION_COMMANDS.put(Integer.parseInt(seatStr), cmd);
                            } catch (Exception e) {
                                BloodOnTheBlocktower.LOGGER.warn("Failed to parse execution command for seat: " + seatStr);
                            }
                        });
                    }

                    // Load vote indicator block settings
                    if (data.voteIndicatorBlockOn != null) {
                        VOTE_INDICATOR_BLOCK_ON = data.voteIndicatorBlockOn;
                    }
                    if (data.voteIndicatorBlockOff != null) {
                        VOTE_INDICATOR_BLOCK_OFF = data.voteIndicatorBlockOff;
                    }
                    if (data.voteIndicatorBlockGhostOn != null) {
                        VOTE_INDICATOR_BLOCK_GHOST_ON = data.voteIndicatorBlockGhostOn;
                    }
                    if (data.voteIndicatorBlockGhostOff != null) {
                        VOTE_INDICATOR_BLOCK_GHOST_OFF = data.voteIndicatorBlockGhostOff;
                    }
                    if (data.voteIndicatorBlockGhostUsed != null) {
                        VOTE_INDICATOR_BLOCK_GHOST_USED = data.voteIndicatorBlockGhostUsed;
                    }
                    if (data.voteIndicatorBlockDouble != null) {
                        VOTE_INDICATOR_BLOCK_DOUBLE = data.voteIndicatorBlockDouble;
                    }
                    if (data.voteIndicatorBlockUnseated != null) {
                        VOTE_INDICATOR_BLOCK_UNSEATED = data.voteIndicatorBlockUnseated;
                    }
                    if (data.voteTimePerPlayer != null) {
                        VOTE_TIME_PER_PLAYER = data.voteTimePerPlayer;
                    }
                    // Load exile support indicator blocks
                    if (data.exileSupportIndicatorBlockOn != null) {
                        EXILE_SUPPORT_INDICATOR_BLOCK_ON = data.exileSupportIndicatorBlockOn;
                    }
                    if (data.exileSupportIndicatorBlockOff != null) {
                        EXILE_SUPPORT_INDICATOR_BLOCK_OFF = data.exileSupportIndicatorBlockOff;
                    }
                    if (data.executionSoundDelay != null) {
                        EXECUTION_SOUND_DELAY = data.executionSoundDelay;
                    }
                    if (data.executionSurvivedSoundDelay != null) {
                        EXECUTION_SURVIVED_SOUND_DELAY = data.executionSurvivedSoundDelay;
                    }
                    if (data.executionDeathTitleDelay != null) {
                        EXECUTION_DEATH_TITLE_DELAY = data.executionDeathTitleDelay;
                    }

                    // Load execution position and anvil settings
                    if (data.executionPosition != null) {
                        try {
                            String[] parts = data.executionPosition.split(",");
                            int x = Integer.parseInt(parts[0]);
                            int y = Integer.parseInt(parts[1]);
                            int z = Integer.parseInt(parts[2]);
                            EXECUTION_POSITION = new BlockPos(x, y, z);
                        } catch (Exception e) {
                            BloodOnTheBlocktower.LOGGER.warn("Failed to parse execution position: " + data.executionPosition);
                        }
                    }
                    if (data.anvilHeight != null) {
                        ANVIL_HEIGHT = data.anvilHeight;
                    }
                    if (data.lockInExecutionPosition != null) {
                        LOCK_IN_EXECUTION_POSITION = data.lockInExecutionPosition;
                    }

                    // Load time settings
                    if (data.timeDawn != null) {
                        TIME_DAWN = data.timeDawn;
                    }
                    if (data.timeEvening != null) {
                        TIME_EVENING = data.timeEvening;
                    }
                    if (data.timeDusk != null) {
                        TIME_DUSK = data.timeDusk;
                    }

                    // Load clock hands settings
                    if (data.clockCenter != null) {
                        try {
                            String[] parts = data.clockCenter.split(",");
                            int x = Integer.parseInt(parts[0]);
                            int y = Integer.parseInt(parts[1]);
                            int z = Integer.parseInt(parts[2]);
                            CLOCK_CENTER = new BlockPos(x, y, z);
                        } catch (Exception e) {
                            BloodOnTheBlocktower.LOGGER.warn("Failed to parse clock center: " + data.clockCenter);
                        }
                    }
                    if (data.clockHandScale != null) {
                        CLOCK_HAND_SCALE = data.clockHandScale;
                    }
                    if (data.maxNameLength != null) {
                        MAX_NAME_LENGTH = data.maxNameLength;
                    }

                    // Load custom player names
                    CUSTOM_PLAYER_NAMES.clear();
        CustomNames.replaceAll(new HashMap<>());
                    if (data.customPlayerNames != null) {
                        CUSTOM_PLAYER_NAMES.putAll(data.customPlayerNames);
                        publishCustomNames();
                    }

                    // Load whisper settings. Any missing field (or no block at all)
                    // falls back to the corresponding default.
                    if (data.whisperSettings != null) {
                        WhisperSettings def = WhisperSettings.DEFAULT;
                        WhisperSettingsData ws = data.whisperSettings;
                        WhisperSettings.VisualMode visual = def.visual();
                        if (ws.visual != null) {
                            try { visual = WhisperSettings.VisualMode.valueOf(ws.visual); }
                            catch (IllegalArgumentException ignored) {}
                        }
                        boolean audio = ws.audio != null ? ws.audio : def.audio();
                        WhisperSettingsManager.set(new WhisperSettings(
                                ws.allowWhispering != null ? ws.allowWhispering : def.allowWhispering(),
                                ws.broadcast != null ? ws.broadcast : def.broadcast(),
                                visual,
                                audio,
                                ws.range != null ? ws.range : def.range(),
                                ws.vcEnforced != null ? ws.vcEnforced : def.vcEnforced()
                        ));
                    } else {
                        WhisperSettingsManager.set(WhisperSettings.DEFAULT);
                    }
                }
            } catch (IOException | JsonSyntaxException e) {
                BloodOnTheBlocktower.LOGGER.error("Failed to load server config", e);
            }
        } else {
            save(); // Create a default empty file
        }
    }

    /** Copies the saved custom names into the live map the name mixins read. */
    public static void publishCustomNames() {
        Map<UUID, String> names = new HashMap<>();
        for (Map.Entry<String, String> e : CUSTOM_PLAYER_NAMES.entrySet()) {
            try {
                names.put(UUID.fromString(e.getKey()), e.getValue());
            } catch (IllegalArgumentException ignored) {
                // malformed key in an old config; skip it
            }
        }
        CustomNames.replaceAll(names);
    }

    public static void save() {
        if (configFile == null) {
            BloodOnTheBlocktower.LOGGER.error("Server config save requested before any world was loaded");
            return;
        }

        ConfigData data = new ConfigData();

        // Convert seat homes
        data.seatHomes = SEAT_HOMES.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> {
                            BlockPos pos = entry.getValue();
                            return pos.getX() + "," + pos.getY() + "," + pos.getZ();
                        }
                ));

        // Convert town square seats
        data.townSquareSeats = TOWN_SQUARE_SEATS.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> {
                            BlockPos pos = entry.getValue();
                            return pos.getX() + "," + pos.getY() + "," + pos.getZ();
                        }
                ));

        // Convert town square
        if (TOWN_SQUARE != null) {
            data.townSquare = TOWN_SQUARE.getX() + "," + TOWN_SQUARE.getY() + "," + TOWN_SQUARE.getZ();
        }

        // Convert death commands
        data.deathCommands = DEATH_COMMANDS.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue
                ));

        // Convert revive commands
        data.reviveCommands = REVIVE_COMMANDS.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue
                ));

        // Convert seat assignment commands
        data.seatAssignmentCommands = SEAT_ASSIGNMENT_COMMANDS.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue
                ));

        // Save dusk and dawn commands
        data.duskCommand = DUSK_COMMAND;
        data.dawnCommand = DAWN_COMMAND;

        // Convert seat switch positions
        data.seatSwitchPositions = SEAT_SWITCH_POSITIONS.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> {
                            BlockPos pos = entry.getValue();
                            return pos.getX() + "," + pos.getY() + "," + pos.getZ();
                        }
                ));

        // Convert seat vote indicator positions
        data.seatVoteIndicatorPositions = SEAT_VOTE_INDICATOR_POSITIONS.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        entry -> {
                            BlockPos pos = entry.getValue();
                            return pos.getX() + "," + pos.getY() + "," + pos.getZ();
                        }
                ));


        // Convert execution commands
        data.executionCommands = EXECUTION_COMMANDS.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue
                ));

        // Save vote indicator block settings
        data.voteIndicatorBlockOn = VOTE_INDICATOR_BLOCK_ON;
        data.voteIndicatorBlockOff = VOTE_INDICATOR_BLOCK_OFF;
        data.voteIndicatorBlockGhostOn = VOTE_INDICATOR_BLOCK_GHOST_ON;
        data.voteIndicatorBlockGhostOff = VOTE_INDICATOR_BLOCK_GHOST_OFF;
        data.voteIndicatorBlockGhostUsed = VOTE_INDICATOR_BLOCK_GHOST_USED;
        data.voteIndicatorBlockDouble = VOTE_INDICATOR_BLOCK_DOUBLE;
        data.voteIndicatorBlockUnseated = VOTE_INDICATOR_BLOCK_UNSEATED;
        data.voteTimePerPlayer = VOTE_TIME_PER_PLAYER;
        // Save exile support indicator block settings
        data.exileSupportIndicatorBlockOn = EXILE_SUPPORT_INDICATOR_BLOCK_ON;
        data.exileSupportIndicatorBlockOff = EXILE_SUPPORT_INDICATOR_BLOCK_OFF;
        data.executionSoundDelay = EXECUTION_SOUND_DELAY;
        data.executionSurvivedSoundDelay = EXECUTION_SURVIVED_SOUND_DELAY;
        data.executionDeathTitleDelay = EXECUTION_DEATH_TITLE_DELAY;

        // Save execution position and anvil settings
        if (EXECUTION_POSITION != null) {
            data.executionPosition = EXECUTION_POSITION.getX() + "," + EXECUTION_POSITION.getY() + "," + EXECUTION_POSITION.getZ();
        }
        data.anvilHeight = ANVIL_HEIGHT;
        data.lockInExecutionPosition = LOCK_IN_EXECUTION_POSITION;

        // Save time settings
        data.timeDawn = TIME_DAWN;
        data.timeEvening = TIME_EVENING;
        data.timeDusk = TIME_DUSK;

        // Save clock hands settings
        if (CLOCK_CENTER != null) {
            data.clockCenter = CLOCK_CENTER.getX() + "," + CLOCK_CENTER.getY() + "," + CLOCK_CENTER.getZ();
        }
        data.clockHandScale = CLOCK_HAND_SCALE;
        data.maxNameLength = MAX_NAME_LENGTH;

        // Save custom player names
        data.customPlayerNames = new HashMap<>(CUSTOM_PLAYER_NAMES);

        // Save whisper settings
        WhisperSettings ws = WhisperSettingsManager.get();
        WhisperSettingsData wsd = new WhisperSettingsData();
        wsd.allowWhispering = ws.allowWhispering();
        wsd.broadcast = ws.broadcast();
        wsd.visual = ws.visual().name();
        wsd.audio = ws.audio();
        wsd.range = ws.range();
        wsd.vcEnforced = ws.vcEnforced();
        data.whisperSettings = wsd;

        // Write to a temp file and swap it in, so a crash mid-write can't leave a truncated
        // config that wipes the map layout on the next load
        Path tempFile = configFile.resolveSibling(configFile.getFileName() + ".tmp");
        try {
            try (var out = Files.newBufferedWriter(tempFile)) {
                GSON.toJson(data, out);
            }
            Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            BloodOnTheBlocktower.LOGGER.error("Failed to save server config", e);
        }
    }
}