package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops Last Stand pending players from pushing or being pushed by living entities.
 * 阻止等待复活中的背水一战玩家推动生物实体，或被生物实体推动。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityCollisionMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void sparktraits$disableLastStandPendingPushable(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (!SparkTraitsServerConnection.isUnconfirmedClientEntity(entity)
                && LastStandService.shouldDisablePendingCollision(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pushAway", at = @At("HEAD"), cancellable = true)
    private void sparktraits$disableLastStandPendingPushAway(Entity entity, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!SparkTraitsServerConnection.isUnconfirmedClientEntity(self)
                && LastStandService.shouldDisablePendingCollision(self, entity)) {
            ci.cancel();
        }
    }
}
