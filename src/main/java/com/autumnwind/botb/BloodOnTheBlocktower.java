package com.autumnwind.botb;

import com.autumnwind.botb.command.BotbCommands;
import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.event.PlayerEvents;
import com.autumnwind.botb.item.ModItems;
import com.autumnwind.botb.networking.ModPackets;
import net.fabricmc.api.ModInitializer;
import com.autumnwind.botb.setup.SetupStick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import com.autumnwind.botb.daytime.ElectionManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.autumnwind.botb.networking.ModPayloads;
import com.autumnwind.botb.networking.StateBroadcaster;

public class BloodOnTheBlocktower implements ModInitializer {
    public static final String MOD_ID = "blood-on-the-blocktower";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** This side's mod version from fabric.mod.json, or "unknown" outside a loaded mod. */
    public static String version() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Blood on the Blocktower server is loading!");

        // The server configuration is world-scoped, so it loads with each starting
        // server (in singleplayer, that's every time a world is opened)
        ServerLifecycleEvents.SERVER_STARTING
                .register(ServerConfig::load);
        ServerLifecycleEvents.SERVER_STOPPING
                .register(server -> SetupStick.onServerStopping());

        // Register packets
        ModPayloads.registerPayloads();
        ModPackets.registerC2SReceivers();

        // Register mod items
        ModItems.registerItems();

        PlayerEvents.register();

        // Tick scheduler for vote piston power delays
        ElectionManager.registerTickScheduler();

        // Broadcast lobby player/storyteller counts when they change (joins, leaves, op changes)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) {
                StateBroadcaster.syncLobbyCounts(server);
            }
        });

        // --- Register Command ---
        BotbCommands.register();
    }

}
