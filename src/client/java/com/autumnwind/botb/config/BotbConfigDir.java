package com.autumnwind.botb.config;

import com.autumnwind.botb.BloodOnTheBlocktower;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** The mod's client config folder, {@code config/botb}. The per-world server config lives with its world instead. */
public final class BotbConfigDir {

    private BotbConfigDir() {}

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("botb");

    /** A file inside the folder, creating the folder if it doesn't exist yet. */
    public static Path resolve(String fileName) {
        try {
            Files.createDirectories(DIR);
        } catch (IOException e) {
            BloodOnTheBlocktower.LOGGER.error("Could not create config folder {}", DIR, e);
        }
        return DIR.resolve(fileName);
    }
}
