package com.autumnwind.botb.clockhands;

import com.autumnwind.botb.networking.ClockHandsStateS2CPayload;
import com.autumnwind.botb.states.ClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Client-side state manager for clock hands animations.
 * Tracks current animation state and handles smooth interpolation.
 */
public class ClockHandsState {

    // Current mode (matches ClockHandsStateS2CPayload constants)
    private static int currentMode = ClockHandsStateS2CPayload.MODE_HIDDEN;

    // Clock configuration
    private static BlockPos clockCenter = null;
    private static float scale = 1.0f;

    // Hour hand state
    private static float hourHandCurrentAngle = 0.0f;
    private static float hourHandTargetAngle = 0.0f;
    private static float hourHandStartAngle = 0.0f;
    private static boolean hourHandVisible = false;

    // Minute hand state
    private static float minuteHandCurrentAngle = 0.0f;
    private static float minuteHandTargetAngle = 0.0f;
    private static float minuteHandStartAngle = 0.0f;
    private static boolean minuteHandVisible = false;

    // Fade animation state
    private static float fadeAlpha = 0.0f;
    private static boolean fadingIn = false;
    private static boolean fadingOut = false;
    private static long fadeStartTime = 0;

    // Swivel animation state (for nomination)
    private static boolean swiveling = false;
    private static long swivelStartTime = 0;
    private static boolean hourSwivelClockwise = true;
    private static boolean minuteSwivelClockwise = false;

    // Tick animation state (for voting)
    private static boolean ticking = false;
    private static long tickStartTime = 0;

    /**
     * Called when a new clock hands state is received from the server.
     */
    public static void onStateReceived(ClockHandsStateS2CPayload payload) {
        int newMode = payload.mode();
        BlockPos newClockCenter = payload.clockCenter();
        float newScale = payload.scale();
        Vec3d hourTargetPos = payload.hourHandTargetPos();
        Vec3d minuteTargetPos = payload.minuteHandTargetPos();
        boolean shouldFadeIn = payload.fadeIn();
        boolean shouldSwivel = payload.swivel();

        // Update clock configuration
        clockCenter = newClockCenter;
        scale = newScale;

        // Can't render without a clock center
        if (clockCenter == null) {
            currentMode = ClockHandsStateS2CPayload.MODE_HIDDEN;
            hourHandVisible = false;
            minuteHandVisible = false;
            fadeAlpha = 0.0f;
            return;
        }

        // Handle mode transitions
        if (newMode == ClockHandsStateS2CPayload.MODE_HIDDEN) {
            // Start fade out. Keep currentMode at the previous value so the renderer
            // continues to pick the correct texture (e.g. exile vs nomination) for the
            // duration of the fade-out. The mode flip to HIDDEN happens in tick() when
            // the fade actually completes.
            if (fadeAlpha > 0) {
                startFadeOut();
            } else {
                currentMode = newMode;
            }
            return;
        }

        // Calculate center position for angle calculation
        double centerX = clockCenter.getX() + 0.5;
        double centerZ = clockCenter.getZ() + 0.5;

        // Update hour hand
        if (hourTargetPos != null) {
            float newTargetAngle = ClockHandsAnimator.calculateAngle(centerX, centerZ, hourTargetPos.x, hourTargetPos.z);

            if (!hourHandVisible || shouldFadeIn) {
                // First appearance or new nomination
                if (shouldSwivel) {
                    // Start from opposite direction
                    hourHandStartAngle = ClockHandsAnimator.oppositeAngle(newTargetAngle);
                    hourHandCurrentAngle = hourHandStartAngle;
                } else {
                    hourHandCurrentAngle = newTargetAngle;
                }
            } else {
                // Update target for smooth tracking
                hourHandStartAngle = hourHandCurrentAngle;
            }

            hourHandTargetAngle = newTargetAngle;
            hourHandVisible = true;
        } else {
            hourHandVisible = false;
        }

        // Update minute hand
        if (minuteTargetPos != null) {
            float newTargetAngle = ClockHandsAnimator.calculateAngle(centerX, centerZ, minuteTargetPos.x, minuteTargetPos.z);

            if (!minuteHandVisible || shouldFadeIn) {
                // First appearance or new nomination
                if (shouldSwivel) {
                    // Start from opposite direction
                    minuteHandStartAngle = ClockHandsAnimator.oppositeAngle(newTargetAngle);
                    minuteHandCurrentAngle = minuteHandStartAngle;
                } else {
                    minuteHandCurrentAngle = newTargetAngle;
                }
            } else if ((currentMode == ClockHandsStateS2CPayload.MODE_VOTING && newMode == ClockHandsStateS2CPayload.MODE_VOTING)
                    || (currentMode == ClockHandsStateS2CPayload.MODE_EXILE && newMode == ClockHandsStateS2CPayload.MODE_EXILE)) {
                // Vote tick or exile support tick - start tick animation
                minuteHandStartAngle = minuteHandCurrentAngle;
                startTick();
            } else {
                // Update target for smooth tracking
                minuteHandStartAngle = minuteHandCurrentAngle;
            }

            minuteHandTargetAngle = newTargetAngle;
            minuteHandVisible = true;
        } else {
            minuteHandVisible = false;
        }

        // Handle fade in
        if (shouldFadeIn && fadeAlpha < 1.0f) {
            startFadeIn();
        }

        // Handle swivel animation
        if (shouldSwivel && hourHandVisible && minuteHandVisible) {
            // Calculate swivel directions so the hands rotate toward each other and cross
            // Both hands start at opposite angles (pointing away from their targets)
            float hourStart = ClockHandsAnimator.oppositeAngle(hourHandTargetAngle);
            float minuteStart = ClockHandsAnimator.oppositeAngle(minuteHandTargetAngle);

            // Find the angular difference between starting positions (short path)
            float diff = ClockHandsAnimator.normalizeAngle(minuteStart - hourStart);

            // If diff > 0, minute starts clockwise from hour
            // To cross: hour goes clockwise (+), minute goes counter-clockwise (-)
            // If diff < 0, minute starts counter-clockwise from hour
            // To cross: hour goes counter-clockwise (-), minute goes clockwise (+)
            hourSwivelClockwise = (diff > 0);
            minuteSwivelClockwise = (diff <= 0);

            startSwivel();
        } else if (shouldSwivel) {
            // Only one hand visible, just do normal swivel
            startSwivel();
        }

        currentMode = newMode;
    }

    /**
     * Called every frame to update animations.
     * @param deltaTime Time since last frame in seconds
     */
    public static void tick(float deltaTime) {
        long currentTime = System.currentTimeMillis();

        // Update fade animation
        if (fadingIn) {
            long elapsed = currentTime - fadeStartTime;
            float progress = (float) elapsed / ClockHandsAnimator.FADE_IN_MS;
            if (progress >= 1.0f) {
                fadeAlpha = 1.0f;
                fadingIn = false;
            } else {
                fadeAlpha = ClockHandsAnimator.easeOutCubic(progress);
            }
        } else if (fadingOut) {
            long elapsed = currentTime - fadeStartTime;
            float progress = (float) elapsed / ClockHandsAnimator.FADE_OUT_MS;
            if (progress >= 1.0f) {
                fadeAlpha = 0.0f;
                fadingOut = false;
                hourHandVisible = false;
                minuteHandVisible = false;
                // Now that the fade-out is fully complete, flip mode to HIDDEN. Doing this
                // earlier (in updateState) would switch the rendered texture mid-fade.
                currentMode = ClockHandsStateS2CPayload.MODE_HIDDEN;
            } else {
                fadeAlpha = 1.0f - ClockHandsAnimator.easeOutCubic(progress);
            }
        }

        // Update swivel animation
        if (swiveling) {
            long elapsed = currentTime - swivelStartTime;
            float progress = (float) elapsed / ClockHandsAnimator.NOMINATION_SWIVEL_MS;
            if (progress >= 1.0f) {
                // Animation complete
                hourHandCurrentAngle = hourHandTargetAngle;
                minuteHandCurrentAngle = minuteHandTargetAngle;
                swiveling = false;
            } else {
                // Ease-out animation - hands rotate in opposite directions to cross each other
                float easedProgress = ClockHandsAnimator.easeOutCubic(progress);
                if (hourHandVisible) {
                    if (hourSwivelClockwise) {
                        hourHandCurrentAngle = ClockHandsAnimator.lerpAngleClockwise(hourHandStartAngle, hourHandTargetAngle, easedProgress);
                    } else {
                        hourHandCurrentAngle = ClockHandsAnimator.lerpAngleCounterClockwise(hourHandStartAngle, hourHandTargetAngle, easedProgress);
                    }
                }
                if (minuteHandVisible) {
                    if (minuteSwivelClockwise) {
                        minuteHandCurrentAngle = ClockHandsAnimator.lerpAngleClockwise(minuteHandStartAngle, minuteHandTargetAngle, easedProgress);
                    } else {
                        minuteHandCurrentAngle = ClockHandsAnimator.lerpAngleCounterClockwise(minuteHandStartAngle, minuteHandTargetAngle, easedProgress);
                    }
                }
            }
        }

        // Update tick animation (voting)
        if (ticking) {
            long elapsed = currentTime - tickStartTime;
            float progress = (float) elapsed / ClockHandsAnimator.VOTE_TICK_MS;
            if (progress >= 1.0f) {
                minuteHandCurrentAngle = minuteHandTargetAngle;
                ticking = false;
            } else {
                // Fast ease-out for clock-like tick, always moving clockwise (visually)
                // Note: lerpAngleCounterClockwise produces clockwise visual movement due to coordinate system
                float easedProgress = ClockHandsAnimator.easeOutQuad(progress);
                minuteHandCurrentAngle = ClockHandsAnimator.lerpAngleCounterClockwise(minuteHandStartAngle, minuteHandTargetAngle, easedProgress);
            }
        }

        // Update target angles by tracking actual player positions during nomination or exile
        if ((currentMode == ClockHandsStateS2CPayload.MODE_NOMINATION || currentMode == ClockHandsStateS2CPayload.MODE_EXILE) && clockCenter != null) {
            updateTargetAnglesFromPlayers();
        }

        // Smooth tracking when not in special animation
        if (!swiveling && !ticking) {
            // Smoothly interpolate towards target (for player movement tracking)
            float trackingSpeed = 8.0f * deltaTime; // Adjust for smoothness
            if (hourHandVisible) {
                hourHandCurrentAngle = ClockHandsAnimator.lerpAngle(hourHandCurrentAngle, hourHandTargetAngle,
                        ClockHandsAnimator.clamp(trackingSpeed, 0, 1));
            }
            if (minuteHandVisible) {
                minuteHandCurrentAngle = ClockHandsAnimator.lerpAngle(minuteHandCurrentAngle, minuteHandTargetAngle,
                        ClockHandsAnimator.clamp(trackingSpeed, 0, 1));
            }
        }
    }

    /**
     * Updates target angles based on current player positions.
     * Called each tick during NOMINATION or EXILE mode to track moving players.
     */
    private static void updateTargetAnglesFromPlayers() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        double centerX = clockCenter.getX() + 0.5;
        double centerZ = clockCenter.getZ() + 0.5;

        if (currentMode == ClockHandsStateS2CPayload.MODE_EXILE) {
            // For exile: track exile target (minute hand) only during CALL_FOR_EXILE phase
            // During EXILE_SUPPORT phase, ticking handles movement (like voting)
            if (!ClientState.exileSupportInProgress) {
                UUID exileTargetUuid = ClientState.currentExileTarget;
                if (exileTargetUuid != null && minuteHandVisible) {
                    AbstractClientPlayerEntity target = findPlayerByUuid(client, exileTargetUuid);
                    if (target != null) {
                        minuteHandTargetAngle = ClockHandsAnimator.calculateAngle(centerX, centerZ,
                                target.getX(), target.getZ());
                    }
                }
            }
        } else {
            // For nomination: track nominator (hour hand) and nominee (minute hand)
            UUID nominatorUuid = ClientState.currentNominator;
            if (nominatorUuid != null && hourHandVisible) {
                AbstractClientPlayerEntity nominator = findPlayerByUuid(client, nominatorUuid);
                if (nominator != null) {
                    hourHandTargetAngle = ClockHandsAnimator.calculateAngle(centerX, centerZ,
                            nominator.getX(), nominator.getZ());
                }
            }

            UUID nomineeUuid = ClientState.currentNominee;
            if (nomineeUuid != null && minuteHandVisible) {
                AbstractClientPlayerEntity nominee = findPlayerByUuid(client, nomineeUuid);
                if (nominee != null) {
                    minuteHandTargetAngle = ClockHandsAnimator.calculateAngle(centerX, centerZ,
                            nominee.getX(), nominee.getZ());
                }
            }
        }
    }

    /**
     * Finds a player entity by UUID in the client world.
     */
    private static AbstractClientPlayerEntity findPlayerByUuid(MinecraftClient client, UUID uuid) {
        if (client.world == null) return null;
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player.getUuid().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    private static void startFadeIn() {
        fadingIn = true;
        fadingOut = false;
        fadeStartTime = System.currentTimeMillis();
    }

    private static void startFadeOut() {
        fadingOut = true;
        fadingIn = false;
        fadeStartTime = System.currentTimeMillis();
    }

    private static void startSwivel() {
        swiveling = true;
        swivelStartTime = System.currentTimeMillis();
    }

    private static void startTick() {
        ticking = true;
        tickStartTime = System.currentTimeMillis();
    }

    // Getters for the renderer

    public static int getCurrentMode() {
        return currentMode;
    }

    public static BlockPos getClockCenter() {
        return clockCenter;
    }

    public static float getScale() {
        return scale;
    }

    public static float getHourHandAngle() {
        return hourHandCurrentAngle;
    }

    public static float getMinuteHandAngle() {
        return minuteHandCurrentAngle;
    }

    public static boolean isHourHandVisible() {
        return hourHandVisible && fadeAlpha > 0;
    }

    public static boolean isMinuteHandVisible() {
        return minuteHandVisible && fadeAlpha > 0;
    }

    public static float getFadeAlpha() {
        return fadeAlpha;
    }

    public static boolean shouldRender() {
        return clockCenter != null && fadeAlpha > 0 && (hourHandVisible || minuteHandVisible);
    }
}
