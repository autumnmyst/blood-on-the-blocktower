package com.autumnwind.botb.config;

import com.autumnwind.botb.states.ClientState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.util.FetchLimits;
import com.autumnwind.botb.util.FloatingRoleIconMode;

public class PlayerConfig {
    private static final Path CONFIG_FILE = BotbConfigDir.resolve("player.properties");

    // Called when the game starts
    public static void load() {
        Properties props = new Properties();
        if (Files.exists(CONFIG_FILE)) {
            try (var in = Files.newInputStream(CONFIG_FILE)) {
                props.load(in);
                // Read the value from the file, defaulting to "true" if not found
                ClientState.isRoleHudVisible = Boolean.parseBoolean(props.getProperty("hudVisible", "true"));

                // Load volume multipliers (0.0 to 2.0, where 1.0 = 100%)
                ClientState.volumeNominations = Float.parseFloat(props.getProperty("volumeNominations", "1.0"));
                ClientState.volumeDawnDusk = Float.parseFloat(props.getProperty("volumeDawnDusk", "1.0"));
                ClientState.volumeDoorbell = Float.parseFloat(props.getProperty("volumeDoorbell", "1.0"));
                ClientState.volumeRoleReceive = Float.parseFloat(props.getProperty("volumeRoleReceive", "1.0"));
                ClientState.volumeFinalReveal = Float.parseFloat(props.getProperty("volumeFinalReveal", "1.0"));

                // Load grimoire settings
                ClientState.fadeOutOfGroupHeads = Boolean.parseBoolean(props.getProperty("fadeOutOfGroupHeads", "true"));
                ClientState.grimoireAnimationsEnabled = Boolean.parseBoolean(props.getProperty("grimoireAnimationsEnabled", "true"));

                // Load hints settings
                ClientState.hintsEnabled = Boolean.parseBoolean(props.getProperty("hintsEnabled", "true"));

                // Load floating role icon mode
                ClientState.floatingRoleIconMode = FloatingRoleIconMode.fromName(
                        props.getProperty("floatingRoleIconMode", "AFTER_END"));

                // Limits for images and almanac pages fetched on a script's behalf
                FetchLimits.timeoutMs = Math.max(1000,
                        Integer.parseInt(props.getProperty("fetchTimeoutMs", "10000")));
                FetchLimits.maxBytes = Math.max(64 * 1024,
                        Integer.parseInt(props.getProperty("fetchMaxBytes", String.valueOf(8 * 1024 * 1024))));
                FetchLimits.maxImageDimension = Math.max(64,
                        Integer.parseInt(props.getProperty("fetchMaxImageDimension", "2048")));
            } catch (IOException | RuntimeException e) {
                // A missing or hand-edited value (e.g. a non-numeric volume) falls back to
                // the defaults already in ClientState rather than crashing client startup
                BloodOnTheBlocktower.LOGGER.warn("Could not read player config, using defaults", e);
            }
        } else {
            // If the file doesn't exist, use defaults and save a new one
            save();
        }
    }

    // Called when the setting changes
    public static void save() {
        Properties props = new Properties();
        props.setProperty("hudVisible", String.valueOf(ClientState.isRoleHudVisible));

        // Save volume settings
        props.setProperty("volumeNominations", String.valueOf(ClientState.volumeNominations));
        props.setProperty("volumeDawnDusk", String.valueOf(ClientState.volumeDawnDusk));
        props.setProperty("volumeDoorbell", String.valueOf(ClientState.volumeDoorbell));
        props.setProperty("volumeRoleReceive", String.valueOf(ClientState.volumeRoleReceive));
        props.setProperty("volumeFinalReveal", String.valueOf(ClientState.volumeFinalReveal));

        // Save grimoire settings
        props.setProperty("fadeOutOfGroupHeads", String.valueOf(ClientState.fadeOutOfGroupHeads));
        props.setProperty("grimoireAnimationsEnabled", String.valueOf(ClientState.grimoireAnimationsEnabled));

        // Save hints settings
        props.setProperty("hintsEnabled", String.valueOf(ClientState.hintsEnabled));

        // Save floating role icon mode
        props.setProperty("floatingRoleIconMode", ClientState.floatingRoleIconMode.name());

        // Save fetch limits
        props.setProperty("fetchTimeoutMs", String.valueOf(FetchLimits.timeoutMs));
        props.setProperty("fetchMaxBytes", String.valueOf(FetchLimits.maxBytes));
        props.setProperty("fetchMaxImageDimension", String.valueOf(FetchLimits.maxImageDimension));

        try (var out = Files.newOutputStream(CONFIG_FILE)) {
            props.store(out, "Blood on the Blocktower Configuration");
        } catch (IOException e) {
            BloodOnTheBlocktower.LOGGER.warn("Could not write player config", e);
        }
    }
}
