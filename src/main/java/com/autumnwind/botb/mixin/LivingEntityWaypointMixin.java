package com.autumnwind.botb.mixin;

import com.autumnwind.botb.world.PlayerWaypoints;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityWaypointMixin {

    @Inject(method = "isTransmittingWaypoint", at = @At("HEAD"), cancellable = true)
    private void botb$transmitWhileInvisible(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player && PlayerWaypoints.alwaysTransmits(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onAttributeUpdated", at = @At("TAIL"))
    private void botb$retrackAfterRangeChange(Holder<Attribute> attribute, CallbackInfo ci) {
        if (attribute.is(Attributes.WAYPOINT_TRANSMIT_RANGE) && (Object) this instanceof ServerPlayer player
                && PlayerWaypoints.alwaysTransmits(player)) {
            PlayerWaypoints.transmitRangeChanged(player);
        }
    }

    @Inject(method = "makeWaypointConnectionWith", at = @At("HEAD"), cancellable = true)
    private void botb$filterLocatorBar(ServerPlayer receiver, CallbackInfoReturnable<Optional<WaypointTransmitter.Connection>> cir) {
        if ((Object) this instanceof ServerPlayer source && PlayerWaypoints.hides(source, receiver)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
