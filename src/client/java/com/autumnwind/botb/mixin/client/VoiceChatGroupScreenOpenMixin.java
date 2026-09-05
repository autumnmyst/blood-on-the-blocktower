package com.autumnwind.botb.mixin.client;

import de.maxhenkel.voicechat.gui.group.GroupScreen;
import de.maxhenkel.voicechat.gui.group.JoinGroupScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to prevent non-operators from opening the Simple Voice Chat settings screen
 */
@Mixin(MinecraftClient.class)
public class VoiceChatGroupScreenOpenMixin {

    @Shadow
    public ClientPlayerEntity player;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void restrictVoiceChatGroupScreenOpening(Screen screen, CallbackInfo ci) {
        if (screen instanceof GroupScreen || screen instanceof JoinGroupScreen) {
            if (player != null && !player.hasPermissionLevel(2)) {
                player.sendMessage(Text.literal("Only server operators can access the groups menu"), true);
                ci.cancel(); // Don't open the screen
            }
        }
    }
}
