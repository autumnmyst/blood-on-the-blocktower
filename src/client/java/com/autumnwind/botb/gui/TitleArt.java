package com.autumnwind.botb.gui;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The mod's title artwork, shared by the title screen logo and the credits header.
 *
 * <p>Callers give the width they want and get the artwork itself at that width. The padding
 * baked into the PNG is not part of the deal, so nothing has to compensate for it.
 */
public final class TitleArt {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/botb_title.png");

    /** Full size of the PNG on disk, which the artwork fills edge to edge. */
    private static final int TEXTURE_WIDTH = 1594;
    private static final int TEXTURE_HEIGHT = 541;

    private TitleArt() {
    }

    /** The height that holds the artwork's proportions at a given drawn width. */
    public static int heightFor(int width) {
        return Math.round((float) width * TEXTURE_HEIGHT / TEXTURE_WIDTH);
    }

    /** Draw the artwork with its top left corner at the given position. */
    public static void draw(GuiGraphics context, int x, int y, int width, int height) {
        // Linear sampling, reapplied each draw because a resource reload rebuilds the
        // texture object with the default nearest filter, which makes scaled edges wobble.
        Minecraft.getInstance().getTextureManager()
                .getTexture(TEXTURE).setFilter(true, false);
        RenderSystem.enableBlend();
        context.blit(TEXTURE, x, y, width, height,
                0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        RenderSystem.disableBlend();
    }

    /** Draw the artwork centered horizontally on {@code centerX}, in its natural proportions. */
    public static void drawCentered(GuiGraphics context, int centerX, int y, int width) {
        draw(context, centerX - width / 2, y, width, heightFor(width));
    }
}
