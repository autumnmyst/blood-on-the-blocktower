package com.autumnwind.botb.mixin;

import com.autumnwind.botb.util.CustomNames;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts a player's custom name in the tab list. Vanilla sends this name to every client in the
 * player-list packet, so the tab list follows the custom name without any client-side work.
 */
@Mixin(ServerPlayer.class)
public abstract class PlayerListNameMixin {

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void botb$customListName(CallbackInfoReturnable<Component> cir) {
        Component custom = CustomNames.text(((ServerPlayer) (Object) this).getUUID());
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
