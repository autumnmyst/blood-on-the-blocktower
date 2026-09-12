package com.autumnwind.botb.mixin;

import com.autumnwind.botb.world.PlayerWaypoints;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityWaypointMixin {

    @Inject(method = "makeWaypointConnectionWith", at = @At("HEAD"), cancellable = true)
    private void botb$filterLocatorBar(ServerPlayer receiver, CallbackInfoReturnable<Optional<WaypointTransmitter.Connection>> cir) {
        if ((Object) this instanceof ServerPlayer source && PlayerWaypoints.hides(source, receiver)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
