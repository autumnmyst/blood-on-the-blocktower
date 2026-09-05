package com.autumnwind.botb.timer;

/**
 * Client-side state for the timer system
 */
public class ClientTimerState {
    public static boolean isActive = false;
    public static boolean isPaused = false;
    public static int remainingSeconds = 0;
    public static int totalSeconds = 0;

    /**
     * Updates the timer state from server sync
     */
    public static void updateTimerState(boolean active, boolean paused, int remaining, int total) {
        isActive = active;
        isPaused = paused;
        remainingSeconds = remaining;
        totalSeconds = total;
    }

    /**
     * Checks if a timer is currently running or paused
     */
    public static boolean hasActiveTimer() {
        return isActive;
    }
}
