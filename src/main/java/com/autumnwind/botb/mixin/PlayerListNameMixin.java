package com.autumnwind.botb.mixin;

import com.autumnwind.botb.util.CustomNames;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts a player's custom name in the tab list. Vanilla sends this name to every client in the
 * player-list packet, so the tab list follows the custom name without any client-side work.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class PlayerListNameMixin {

    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
    private void botb$customListName(CallbackInfoReturnable<Text> cir) {
        Text custom = CustomNames.text(((ServerPlayerEntity) (Object) this).getUuid());
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
