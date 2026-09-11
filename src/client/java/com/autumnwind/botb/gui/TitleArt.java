package com.autumnwind.botb.gui;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.mixin.client.AbstractTextureAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.autumnwind.botb.mixin.client.AbstractTextureAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * The mod's title artwork, shared by the title screen logo and the credits header.
 *
 * <p>Callers give the width they want and get the artwork itself at that width. The padding
 * baked into the PNG is not part of the deal, so nothing has to compensate for it.
 */
public final class TitleArt {

    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(BloodOnTheBlocktower.MOD_ID, "textures/botb_title.png");

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
    public static void draw(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        draw(context, x, y, width, height, 0xFFFFFFFF);
    }

    /** Same, tinted by an ARGB color, which is how the title screen fades the art in. */
    public static void draw(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
        // Linear sampling, reapplied each draw because a resource reload rebuilds the
        // texture object with the default nearest sampler, which makes scaled edges wobble.
        GpuSampler linear = RenderSystem.getSamplerCache()
                .getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false);
        ((AbstractTextureAccessor) Minecraft.getInstance().getTextureManager().getTexture(TEXTURE)).botb$setSampler(linear);
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, width, height, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT, color);
    }

    /** Draw the artwork centered horizontally on {@code centerX}, in its natural proportions. */
    public static void drawCentered(GuiGraphicsExtractor context, int centerX, int y, int width) {
        drawCentered(context, centerX, y, width, 0xFFFFFFFF);
    }

    public static void drawCentered(GuiGraphicsExtractor context, int centerX, int y, int width, int color) {
        draw(context, centerX - width / 2, y, width, heightFor(width), color);
    }
}
