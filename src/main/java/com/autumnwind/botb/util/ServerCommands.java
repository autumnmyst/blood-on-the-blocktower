package com.autumnwind.botb.util;

import net.minecraft.server.MinecraftServer;

/** Runs commands from the server console, silently. */
public final class ServerCommands {

    private ServerCommands() {}

    /** Runs {@code command} as the player with the given UUID, via {@code /execute as}. */
    public static void runAs(MinecraftServer server, String playerUuid, String command) {
        run(server, "execute as " + playerUuid + " run " + command);
    }

    /** Runs {@code command} as the server console with feedback suppressed. */
    public static void run(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }
}
