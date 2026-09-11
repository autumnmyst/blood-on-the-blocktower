package com.autumnwind.botb.timer;

import com.autumnwind.botb.config.ServerConfig;
import com.autumnwind.botb.networking.TimerStateS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Server-side manager for the storyteller timer with boss bar display
 */
public class TimerManager {
    private static ServerBossEvent bossBar = null;
    private static Timer updateTimer = null;
    private static double remainingTime = 0.0; // Track precise time as double
    private static int totalSeconds = 0;
    private static boolean isPaused = false;
    private static long lastUpdateTime = 0;
    private static boolean syncDaylight = false;

    /**
     * Starts a new timer with the specified duration
     */
    public static void startTimer(MinecraftServer server, int durationSeconds, boolean shouldSyncDaylight) {
        // Stop any existing timer
        stopTimer(server);

        // Create new boss bar
        totalSeconds = durationSeconds;
        remainingTime = (double) durationSeconds;
        isPaused = false;
        lastUpdateTime = System.currentTimeMillis();
        syncDaylight = shouldSyncDaylight;

        // Set initial time to dawn if syncing
        if (syncDaylight) {
            server.overworld().setDayTime(ServerConfig.TIME_DAWN);
        }

        bossBar = new ServerBossEvent(
                formatTimerText((int) Math.ceil(remainingTime)),
                BossEvent.BossBarColor.GREEN,
                BossEvent.BossBarOverlay.PROGRESS
        );
        bossBar.setProgress(1.0f);

        // Add all players to boss bar
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            bossBar.addPlayer(player);
        }

        // Start update timer (50ms ticks for smooth updates). The tick body runs on the server
        // thread: it writes world time and updates the boss bar, whose player set the server
        // thread edits on join and leave, so touching it from the Timer thread would race.
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isPaused) {
                    server.execute(() -> updateTimerTick(server));
                }
            }
        }, 50, 50);

        // Broadcast initial state
        broadcastTimerState(server);
    }

    /**
     * Pauses the current timer
     */
    public static void pauseTimer(MinecraftServer server) {
        if (bossBar != null && !isPaused) {
            isPaused = true;
            lastUpdateTime = System.currentTimeMillis();
            broadcastTimerState(server);
        }
    }

    /**
     * Resumes a paused timer
     */
    public static void resumeTimer(MinecraftServer server) {
        if (bossBar != null && isPaused) {
            isPaused = false;
            lastUpdateTime = System.currentTimeMillis();
            broadcastTimerState(server);
        }
    }

    /**
     * Stops and clears the timer
     */
    public static void stopTimer(MinecraftServer server) {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }

        if (bossBar != null) {
            bossBar.removeAllPlayers();
            bossBar = null;
        }

        remainingTime = 0.0;
        totalSeconds = 0;
        isPaused = false;
        syncDaylight = false;

        // Broadcast cleared state
        broadcastTimerState(server);
    }

    /**
     * Updates the timer every tick
     */
    private static void updateTimerTick(MinecraftServer server) {
        if (bossBar == null) return;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        // Decrease remaining time
        double secondsElapsed = elapsed / 1000.0;
        remainingTime -= secondsElapsed;

        if (remainingTime <= 0) {
            // Timer finished
            remainingTime = 0.0;
            bossBar.setProgress(0.0f);
            bossBar.setName(formatTimerText(0));
            bossBar.setColor(BossEvent.BossBarColor.RED);
            stopTimer(server);
            return;
        }

        // Update boss bar
        int displaySeconds = (int) Math.ceil(remainingTime);
        float progress = (float) remainingTime / totalSeconds;
        bossBar.setProgress(Math.max(0.0f, Math.min(1.0f, progress)));
        bossBar.setName(formatTimerText(displaySeconds));

        // Update color based on progress (green -> yellow -> red)
        BossEvent.BossBarColor color;
        if (progress > 0.5f) {
            color = BossEvent.BossBarColor.GREEN;
        } else if (progress > 0.25f) {
            color = BossEvent.BossBarColor.YELLOW;
        } else {
            color = BossEvent.BossBarColor.RED;
        }
        bossBar.setColor(color);

        // Update daylight if sync is enabled
        if (syncDaylight && !isPaused) {
            // Calculate how far through the timer we are (0.0 to 1.0)
            double timerProgress = 1.0 - (remainingTime / totalSeconds);
            // Interpolate between dawn and evening time
            long dawnTime = ServerConfig.TIME_DAWN;
            long eveningTime = ServerConfig.TIME_EVENING;
            long targetTime = dawnTime + (long) ((eveningTime - dawnTime) * timerProgress);
            server.overworld().setDayTime(targetTime);
        }

        // Broadcast state update periodically (every ~100ms, which is 2 ticks at 50ms)
        broadcastTimerState(server);
    }

    /**
     * Formats timer text as min:sec
     */
    private static Component formatTimerText(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return Component.literal(String.format("%d:%02d", minutes, secs));
    }

    /**
     * Broadcasts timer state to all players
     */
    private static void broadcastTimerState(MinecraftServer server) {
        boolean isActive = bossBar != null;
        int displaySeconds = (int) Math.ceil(remainingTime);
        TimerStateS2CPayload payload = new TimerStateS2CPayload(
                isActive,
                isPaused,
                displaySeconds,
                totalSeconds
        );

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /**
     * Adds a player to the boss bar when they join
     */
    public static void addPlayer(ServerPlayer player) {
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    /**
     * Removes a player from the boss bar when they leave
     */
    public static void removePlayer(ServerPlayer player) {
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }
}
