package com.autumnwind.botb.mixin.client;

import de.maxhenkel.voicechat.voice.client.GroupChatManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hide the default Simple Voice Chat group icons
 * We have our own custom sidebar implementation
 */
@Mixin(GroupChatManager.class)
public class GroupChatRenderMixin {

    @Inject(method = "renderIcons", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hideGroupIcons(GuiGraphicsExtractor drawContext, CallbackInfo ci) {
        // Cancel the rendering of default group icons
        ci.cancel();
    }
}
