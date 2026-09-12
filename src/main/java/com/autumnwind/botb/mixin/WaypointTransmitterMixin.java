package com.autumnwind.botb.mixin;

import com.autumnwind.botb.world.PlayerWaypoints;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WaypointTransmitter.class)
public interface WaypointTransmitterMixin {

    @Inject(method = "doesSourceIgnoreReceiver", at = @At("HEAD"), cancellable = true)
    private static void botb$rangeWhileInvisible(LivingEntity source, ServerPlayer receiver, CallbackInfoReturnable<Boolean> cir) {
        if (source instanceof ServerPlayer player && PlayerWaypoints.alwaysTransmits(player)) {
            cir.setReturnValue(PlayerWaypoints.outOfRange(player, receiver));
        }
    }
}
