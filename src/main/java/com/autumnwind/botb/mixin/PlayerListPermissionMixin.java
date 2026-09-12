package com.autumnwind.botb.mixin;

import com.autumnwind.botb.world.PlayerWaypoints;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListPermissionMixin {

    @Inject(method = "sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("TAIL"))
    private void botb$refreshLocatorBars(ServerPlayer player, CallbackInfo ci) {
        PlayerWaypoints.refresh(player.level().getServer());
    }
}
