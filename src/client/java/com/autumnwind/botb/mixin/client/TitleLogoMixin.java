package com.autumnwind.botb.mixin.client;

import com.autumnwind.botb.gui.TitleArt;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swaps the vanilla "Minecraft" wordmark on the title screen for the mod's own title art. The
 * "Java Edition" banner and the Minceraft easter egg go with it, because one call draws the
 * whole logo block.
 */
@Mixin(LogoDrawer.class)
public class TitleLogoMixin {

    /**
     * Drawn width. Matching the vanilla wordmark keeps the splash text, which is pinned to
     * width/2+123, sitting off the top right corner of the art the way it always has.
     */
    private static final int DRAW_WIDTH = 256;

    /**
     * Lift applied to the incoming y. Vanilla fills y through y+51 with the wordmark and the
     * edition banner, so centering the taller art on that band keeps it clear of the buttons,
     * which start at height/4+48 and so sit at y=108 on the shortest GUI the game allows.
     */
    private static final int Y_OFFSET = -15;

    @Shadow
    @Final
    private boolean ignoreAlpha;

    @Inject(
            method = "draw(Lnet/minecraft/client/gui/DrawContext;IFI)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void drawBotbTitle(DrawContext context, int screenWidth, float alpha, int y, CallbackInfo ci) {
        // Vanilla tints the logo by the fade alpha, so the title fades in with the panorama.
        context.setShaderColor(1.0F, 1.0F, 1.0F, this.ignoreAlpha ? 1.0F : alpha);
        TitleArt.drawCentered(context, screenWidth / 2, y + Y_OFFSET, DRAW_WIDTH);
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        ci.cancel();
    }
}
