package com.autumnwind.botb.world;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;

/** Sets the overworld time of day through the dimension clock, the way /time set does. */
public final class WorldTime {

    private WorldTime() {}

    public static void setOverworldTime(MinecraftServer server, long ticks) {
        ServerLevel overworld = server.overworld();
        Holder<WorldClock> clock = overworld.dimensionTypeRegistration().value().defaultClock().orElse(null);
        if (clock == null) return;
        server.clockManager().setTotalTicks(clock, ticks);
    }
}
