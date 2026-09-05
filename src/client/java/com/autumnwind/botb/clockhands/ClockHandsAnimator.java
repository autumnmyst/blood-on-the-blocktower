package com.autumnwind.botb.clockhands;

/**
 * Animation constants and utility functions for clock hands.
 */
public class ClockHandsAnimator {

    // Animation durations in milliseconds
    public static final int FADE_IN_MS = 300;
    public static final int FADE_OUT_MS = 500;
    public static final int NOMINATION_SWIVEL_MS = 800;
    public static final int VOTE_TICK_MS = 200;

    // Y offset above clock center (in blocks)
    public static final float Y_OFFSET = 0.1f;

    /**
     * Ease-out cubic function for smooth deceleration.
     * @param t Progress from 0.0 to 1.0
     * @return Eased value from 0.0 to 1.0
     */
    public static float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0 - t, 3);
    }

    /**
     * Ease-out quadratic function for slightly less dramatic deceleration.
     * @param t Progress from 0.0 to 1.0
     * @return Eased value from 0.0 to 1.0
     */
    public static float easeOutQuad(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    /**
     * Linear interpolation between two values.
     * @param start Starting value
     * @param end Ending value
     * @param t Progress from 0.0 to 1.0
     * @return Interpolated value
     */
    public static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    /**
     * Linear interpolation for angles, handling wraparound.
     * @param startAngle Starting angle in radians
     * @param endAngle Ending angle in radians
     * @param t Progress from 0.0 to 1.0
     * @return Interpolated angle in radians
     */
    public static float lerpAngle(float startAngle, float endAngle, float t) {
        // Normalize angles to [-PI, PI]
        startAngle = normalizeAngle(startAngle);
        endAngle = normalizeAngle(endAngle);

        float diff = endAngle - startAngle;

        // Take the shorter path (or longer path for swivel)
        if (diff > Math.PI) {
            diff -= 2 * Math.PI;
        } else if (diff < -Math.PI) {
            diff += 2 * Math.PI;
        }

        return startAngle + diff * t;
    }

    /**
     * Linear interpolation for angles, ALWAYS moving clockwise.
     * Clockwise means angles increase: 0 → PI/2 → PI → -PI/2 → 0
     * @param startAngle Starting angle in radians
     * @param endAngle Ending angle in radians
     * @param t Progress from 0.0 to 1.0
     * @return Interpolated angle in radians
     */
    public static float lerpAngleClockwise(float startAngle, float endAngle, float t) {
        // Normalize angles to [-PI, PI]
        startAngle = normalizeAngle(startAngle);
        endAngle = normalizeAngle(endAngle);

        float diff = endAngle - startAngle;

        // For clockwise motion, we want a positive diff (increasing angle)
        // If diff is negative or zero, add 2*PI to go the long way around clockwise
        if (diff <= 0) {
            diff += (float) (2 * Math.PI);
        }

        return normalizeAngle(startAngle + diff * t);
    }

    /**
     * Linear interpolation for angles, ALWAYS moving counter-clockwise.
     * Counter-clockwise means angles decrease: 0 → -PI/2 → PI → PI/2 → 0
     * @param startAngle Starting angle in radians
     * @param endAngle Ending angle in radians
     * @param t Progress from 0.0 to 1.0
     * @return Interpolated angle in radians
     */
    public static float lerpAngleCounterClockwise(float startAngle, float endAngle, float t) {
        // Normalize angles to [-PI, PI]
        startAngle = normalizeAngle(startAngle);
        endAngle = normalizeAngle(endAngle);

        float diff = endAngle - startAngle;

        // For counter-clockwise motion, we want a negative diff (decreasing angle)
        // If diff is positive or zero, subtract 2*PI to go the long way around counter-clockwise
        if (diff >= 0) {
            diff -= (float) (2 * Math.PI);
        }

        return normalizeAngle(startAngle + diff * t);
    }

    /**
     * Normalize an angle to [-PI, PI] range.
     * @param angle Angle in radians
     * @return Normalized angle
     */
    public static float normalizeAngle(float angle) {
        while (angle > Math.PI) {
            angle -= 2 * Math.PI;
        }
        while (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    /**
     * Calculate the angle from the clock center to a target position (in XZ plane).
     * @param centerX Clock center X coordinate
     * @param centerZ Clock center Z coordinate
     * @param targetX Target X coordinate
     * @param targetZ Target Z coordinate
     * @return Angle in radians, where 0 = North (+Z direction)
     */
    public static float calculateAngle(double centerX, double centerZ, double targetX, double targetZ) {
        double dx = targetX - centerX;
        double dz = targetZ - centerZ;
        // atan2 gives angle from positive X axis, but we want angle from positive Z axis
        // So we use atan2(dx, dz) which gives 0 when pointing +Z (North)
        return (float) Math.atan2(dx, dz);
    }

    /**
     * Calculate the opposite angle (180 degrees away).
     * @param angle Original angle in radians
     * @return Opposite angle in radians
     */
    public static float oppositeAngle(float angle) {
        return normalizeAngle(angle + (float) Math.PI);
    }

    /**
     * Clamp a value between min and max.
     * @param value Value to clamp
     * @param min Minimum value
     * @param max Maximum value
     * @return Clamped value
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
