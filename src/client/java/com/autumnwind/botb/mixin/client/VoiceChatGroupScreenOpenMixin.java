package com.autumnwind.botb.mixin.client;

import de.maxhenkel.voicechat.gui.group.GroupScreen;
import de.maxhenkel.voicechat.gui.group.JoinGroupScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.permissions.Permissions;

/**
 * Mixin to prevent non-operators from opening the Simple Voice Chat settings screen
 */
@Mixin(Gui.class)
public class VoiceChatGroupScreenOpenMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void restrictVoiceChatGroupScreenOpening(Screen screen, CallbackInfo ci) {
        if (screen instanceof GroupScreen || screen instanceof JoinGroupScreen) {
            LocalPlayer player = minecraft.player;
            if (player != null && !player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                player.sendOverlayMessage(Component.translatable("message.blood-on-the-blocktower.client.groups_menu_operators_only"));
                ci.cancel(); // Don't open the screen
            }
        }
    }
}
