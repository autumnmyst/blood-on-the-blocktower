package com.autumnwind.botb.hud;

import com.autumnwind.botb.event.KeyInputHandler;
import com.autumnwind.botb.states.ClientState;
import com.autumnwind.botb.util.PendingRoleAssignment;
import com.autumnwind.botb.util.Role;
import com.autumnwind.botb.util.RoleType;
import com.autumnwind.botb.util.ScriptRole;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Handles the role assignment animation when a player receives their role.
 * The role icon starts large and centered, then shrinks and moves to the RoleHUD position.
 * A big title with the role name displays and fades out after the animation.
 */
public class RoleAssignmentAnimation {

    // Animation state
    private static boolean isAnimating = false;
    private static long animationStartTime = 0;
    private static Role animatingRole = null;
    private static PendingRoleAssignment animatingAssignment = null;
    private static boolean animatingIsGood = true;

    // Animation timing constants
    private static final int ANIMATION_DURATION_MS = 1200; // Icon animation duration
    private static final int HOLD_DURATION_MS = 400; // Time to hold at center before moving
    private static final int MOVE_DURATION_MS = 800; // Time to animate the move
    private static final int TITLE_FADE_DURATION_MS = 800; // Title fade out after icon animation
    private static final int TOTAL_DURATION_MS = ANIMATION_DURATION_MS + TITLE_FADE_DURATION_MS;

    // Size constants - end position matches RoleHUD full mode (role box with description)
    private static final int START_ICON_SIZE = 96; // Large centered icon
    private static final int END_ICON_SIZE = 48; // Size in full HUD role box
    private static final int END_X = 10 + 10; // x + x_padding from RoleHUD.renderRoleBox
    private static final int END_Y = 50 + 10; // y + y_padding from RoleHUD.renderRoleBox

    /**
     * Starts the role assignment animation with a PendingRoleAssignment.
     * Supports both official and custom roles.
     * @param assignment The role assignment
     */
    public static void startAnimation(PendingRoleAssignment assignment) {
        if (assignment == null) {
            return;
        }
        // Don't animate for no role (unless it's a custom role)
        if (!assignment.isCustomRole() && assignment.role() == Role.NO_ROLE) {
            return;
        }

        animatingAssignment = assignment;
        animatingRole = assignment.role(); // For backwards compat
        animatingIsGood = assignment.isFinalGood();
        animationStartTime = System.currentTimeMillis();
        isAnimating = true;
    }

    /**
     * Starts the role assignment animation for an official role.
     * @param role The role being assigned
     * @param isGood Whether the role alignment is good
     */
    public static void startAnimation(Role role, boolean isGood) {
        // Role receive animation always plays (not affected by grimoireAnimationsEnabled)
        if (role == null || role == Role.NO_ROLE) {
            return; // Don't animate for no role
        }

        animatingRole = role;
        animatingAssignment = null; // No full assignment
        animatingIsGood = isGood;
        animationStartTime = System.currentTimeMillis();
        isAnimating = true;
    }

    /**
     * Gets the icon for the animating role (supports both official and custom roles).
     */
    private static Identifier getAnimatingIcon() {
        if (animatingAssignment != null) {
            ScriptRole sr = animatingAssignment.getScriptRole();
            if (sr != null) return sr.getIcon();
        }
        if (animatingRole != null) return animatingRole.getIcon();
        return null;
    }

    /**
     * Gets the display name for the animating role.
     */
    private static String getAnimatingDisplayName() {
        if (animatingAssignment != null) {
            return animatingAssignment.getDisplayName();
        }
        if (animatingRole != null) return animatingRole.getDisplayName();
        return "Unknown";
    }

    /**
     * Gets the role type for the animating role.
     */
    private static RoleType getAnimatingRoleType() {
        if (animatingAssignment != null) {
            return animatingAssignment.getRoleType();
        }
        if (animatingRole != null) return animatingRole.getType();
        return RoleType.NONE;
    }

    /**
     * Checks if the animating role is default good.
     */
    private static boolean isAnimatingRoleDefaultGood() {
        if (animatingAssignment != null) {
            return animatingAssignment.isRoleDefaultGood();
        }
        if (animatingRole != null) return animatingRole.isDefaultGood();
        return true;
    }

    /**
     * Stops the animation, resets state, and shows the keybind hint (if hints enabled).
     */
    private static void stopAnimation() {
        isAnimating = false;
        animatingRole = null;
        animatingAssignment = null;

        // Show overlay message with the keybind to hide role HUD (only if hints enabled)
        if (ClientState.hintsEnabled) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.inGameHud != null) {
                String keyName = KeyInputHandler.toggleShowRole.getBoundKeyLocalizedText().getString();
                Text message = Text.literal("Press [")
                        .append(Text.literal(keyName).formatted(Formatting.YELLOW))
                        .append(Text.literal("] to hide role HUD"));
                client.inGameHud.setOverlayMessage(message, false);
            }
        }
    }

    /**
     * Checks if the animation is currently playing (including title fade).
     * Note: Does not modify state - state is only modified in render().
     */
    public static boolean isAnimating() {
        if (!isAnimating || (animatingRole == null && animatingAssignment == null)) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - animationStartTime;
        return elapsed < TOTAL_DURATION_MS;
    }

    /**
     * Checks if the RoleHUD should be hidden during the animation.
     * RoleHUD should be hidden only during the icon animation, not the title fade.
     */
    public static boolean shouldHideRoleHUD() {
        if (!isAnimating || (animatingRole == null && animatingAssignment == null)) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - animationStartTime;
        return elapsed < ANIMATION_DURATION_MS;
    }

    /**
     * Renders the role assignment animation if active.
     * Should be called from the HUD render callback.
     */
    public static void render(DrawContext drawContext, MinecraftClient client) {
        if (!isAnimating || (animatingRole == null && animatingAssignment == null)) {
            return;
        }

        long elapsed = System.currentTimeMillis() - animationStartTime;

        // End animation cleanly after total duration
        if (elapsed >= TOTAL_DURATION_MS) {
            stopAnimation();
            return;
        }

        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();

        // Render icon animation (only during icon phase)
        if (elapsed < ANIMATION_DURATION_MS) {
            renderIconAnimation(drawContext, elapsed, screenWidth, screenHeight);
        }

        // Render title (visible throughout, fades out after icon animation)
        renderTitle(drawContext, client, elapsed, screenWidth, screenHeight);
    }

    /**
     * Renders the animated icon moving from center to HUD position.
     */
    private static void renderIconAnimation(DrawContext drawContext, long elapsed, int screenWidth, int screenHeight) {
        int currentSize;
        int currentX;
        int currentY;
        float alpha;

        if (elapsed < HOLD_DURATION_MS) {
            // Hold phase - stay centered at full size with fade in
            currentSize = START_ICON_SIZE;
            currentX = (screenWidth - currentSize) / 2;
            currentY = (screenHeight - currentSize) / 2 - 20; // Slightly above center

            // Fade in during hold phase
            float fadeProgress = (float) elapsed / HOLD_DURATION_MS;
            alpha = easeOutCubic(fadeProgress);
        } else {
            // Move phase - shrink and move to HUD position
            long moveElapsed = elapsed - HOLD_DURATION_MS;
            float progress = Math.min(1.0f, (float) moveElapsed / MOVE_DURATION_MS);

            // Apply easing to the movement
            float easedProgress = easeOutCubic(progress);

            // Interpolate size
            currentSize = (int) lerp(START_ICON_SIZE, END_ICON_SIZE, easedProgress);

            // Calculate start and end positions
            int startX = (screenWidth - START_ICON_SIZE) / 2;
            int startY = (screenHeight - START_ICON_SIZE) / 2 - 20;

            // Interpolate position
            currentX = (int) lerp(startX, END_X, easedProgress);
            currentY = (int) lerp(startY, END_Y, easedProgress);

            alpha = 1.0f;
        }

        // Render the animated role icon
        Identifier icon = getAnimatingIcon();
        if (icon == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        drawContext.drawTexture(icon, currentX, currentY, 0, 0,
                currentSize, currentSize, currentSize, currentSize);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    /**
     * Renders the role name as a big centered title.
     */
    private static void renderTitle(DrawContext drawContext, MinecraftClient client, long elapsed, int screenWidth, int screenHeight) {
        // Calculate title alpha - full during icon animation, fades out after
        float titleAlpha;
        if (elapsed < ANIMATION_DURATION_MS) {
            // During icon animation - fade in with icon during hold phase
            if (elapsed < HOLD_DURATION_MS) {
                float fadeProgress = (float) elapsed / HOLD_DURATION_MS;
                titleAlpha = easeOutCubic(fadeProgress);
            } else {
                titleAlpha = 1.0f;
            }
        } else {
            // After icon animation - fade out with easing, but cap progress to avoid boundary issues
            long fadeElapsed = elapsed - ANIMATION_DURATION_MS;
            float fadeProgress = Math.min(0.95f, (float) fadeElapsed / TITLE_FADE_DURATION_MS);
            titleAlpha = 1.0f - easeOutCubic(fadeProgress);
        }

        // Skip if nearly transparent
        if (titleAlpha < 0.05f) {
            return;
        }

        // Get role name
        String roleName = getAnimatingDisplayName();
        Text titleText = Text.literal(roleName);

        // Determine role color based on alignment override
        int roleColor;
        boolean isDefaultGood = isAnimatingRoleDefaultGood();
        if (animatingIsGood && !isDefaultGood) {
            // Forced good (evil role made good) - townsfolk blue
            roleColor = RoleType.TOWNSFOLK.getColor();
        } else if (!animatingIsGood && isDefaultGood) {
            // Forced evil (good role made evil) - minion red
            roleColor = RoleType.MINION.getColor();
        } else {
            // No alignment override - use role's type color
            roleColor = getAnimatingRoleType().getColor();
        }

        // Calculate position - centered below the initial icon position
        int titleY = (screenHeight / 2) + START_ICON_SIZE / 2;

        // Draw the title with scaling (like Minecraft titles)
        drawContext.getMatrices().push();

        // Scale up for big title effect
        float scale = 3.0f;
        int textWidth = client.textRenderer.getWidth(titleText);
        float scaledWidth = textWidth * scale;
        float titleX = (screenWidth - scaledWidth) / 2;

        drawContext.getMatrices().translate(titleX, titleY, 0);
        drawContext.getMatrices().scale(scale, scale, 1.0f);

        // Apply alpha to role color
        int alphaInt = (int) (titleAlpha * 255);
        int color = (roleColor & 0x00FFFFFF) | (alphaInt << 24);

        // Draw text with default shadow
        drawContext.drawTextWithShadow(client.textRenderer, titleText, 0, 0, color);

        drawContext.getMatrices().pop();
    }

    /**
     * Ease-out cubic function for smooth deceleration.
     */
    private static float easeOutCubic(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
    }

    /**
     * Linear interpolation.
     */
    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }
}
