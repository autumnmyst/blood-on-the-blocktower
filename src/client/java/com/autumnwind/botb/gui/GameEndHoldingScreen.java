package com.autumnwind.botb.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A blank holding screen shown during the game end animation black screen phase.
 * Prevents player movement and interaction while hiding the mouse cursor.
 */
public class GameEndHoldingScreen extends Screen {

    public GameEndHoldingScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        super.init();
        Minecraft client = Minecraft.getInstance();

        // Lock and hide the mouse cursor using GLFW
        GLFW.glfwSetInputMode(
            client.getWindow().getWindow(),
            GLFW.GLFW_CURSOR,
            GLFW.GLFW_CURSOR_DISABLED
        );

        // Hide HUD like F1 key (hides hotbar, items, hands)
        client.options.hideGui = true;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Don't render anything - the animation overlay handles all visuals
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game - let animation continue
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Prevent closing with ESC
    }

    @Override
    public void removed() {
        super.removed();
        Minecraft client = Minecraft.getInstance();

        // Restore normal mouse cursor when screen closes
        GLFW.glfwSetInputMode(
            client.getWindow().getWindow(),
            GLFW.GLFW_CURSOR,
            GLFW.GLFW_CURSOR_NORMAL
        );

        // Restore HUD visibility
        client.options.hideGui = false;
    }
}
