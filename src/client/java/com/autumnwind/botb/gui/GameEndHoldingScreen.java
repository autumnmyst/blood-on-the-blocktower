package com.autumnwind.botb.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * A blank holding screen shown during the game end animation black screen phase.
 * Prevents player movement and interaction while hiding the mouse cursor.
 */
public class GameEndHoldingScreen extends Screen {

    public GameEndHoldingScreen() {
        super(Text.empty());
    }

    @Override
    protected void init() {
        super.init();
        MinecraftClient client = MinecraftClient.getInstance();

        // Lock and hide the mouse cursor using GLFW
        GLFW.glfwSetInputMode(
            client.getWindow().getHandle(),
            GLFW.GLFW_CURSOR,
            GLFW.GLFW_CURSOR_DISABLED
        );

        // Hide HUD like F1 key (hides hotbar, items, hands)
        client.options.hudHidden = true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Don't render anything - the animation overlay handles all visuals
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't pause the game - let animation continue
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Prevent closing with ESC
    }

    @Override
    public void removed() {
        super.removed();
        MinecraftClient client = MinecraftClient.getInstance();

        // Restore normal mouse cursor when screen closes
        GLFW.glfwSetInputMode(
            client.getWindow().getHandle(),
            GLFW.GLFW_CURSOR,
            GLFW.GLFW_CURSOR_NORMAL
        );

        // Restore HUD visibility
        client.options.hudHidden = false;
    }
}
