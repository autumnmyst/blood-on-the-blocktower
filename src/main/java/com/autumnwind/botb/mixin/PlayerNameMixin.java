package com.autumnwind.botb.mixin;

import com.autumnwind.botb.util.CustomNames;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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
@Mixin(Player.class)
public abstract class PlayerNameMixin {

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void botb$customName(CallbackInfoReturnable<Component> cir) {
        Component custom = CustomNames.text(((Player) (Object) this).getUUID());
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
