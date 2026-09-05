package com.autumnwind.botb.mixin;

import com.autumnwind.botb.util.CustomNames;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shows a player's custom name (/botb setName) wherever the game asks a player entity for its
 * name: chat, death and join messages, the nametag over the head, and the mod's own screens.
 * Applies on both sides. The Mojang profile name is untouched, so scoreboard teams, skins,
 * and command targets keep working.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerNameMixin {

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void botb$customName(CallbackInfoReturnable<Text> cir) {
        Text custom = CustomNames.text(((PlayerEntity) (Object) this).getUuid());
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
