package com.autumnwind.botb.gui;

import com.autumnwind.botb.hud.GameEndAnimationHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
            client.getWindow().handle(),
            GLFW.GLFW_CURSOR,
            GLFW.GLFW_CURSOR_DISABLED
        );

        // Hide HUD like F1 key (hides hotbar, items, hands)
        if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        GameEndAnimationHUD.render(context, Minecraft.getInstance());
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
            client.getWindow().handle(),
            GLFW.GLFW_CURSOR,
            GLFW.GLFW_CURSOR_NORMAL
        );

        // Restore HUD visibility
        if (client.gui.hud.isHidden()) client.gui.hud.toggle();
    }
}
