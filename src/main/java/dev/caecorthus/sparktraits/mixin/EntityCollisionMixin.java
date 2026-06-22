package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.LastStandService;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disables entity-to-entity collision while Last Stand is waiting to revive a player.
 * 当背水一战玩家处于等待复活状态时，禁用实体之间的碰撞体积。
 */
@Mixin(Entity.class)
public abstract class EntityCollisionMixin {
    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void sparktraits$disableLastStandPendingCollidable(CallbackInfoReturnable<Boolean> cir) {
        if (LastStandService.shouldDisablePendingCollision((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "collidesWith", at = @At("HEAD"), cancellable = true)
    private void sparktraits$disableLastStandPendingCollision(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (LastStandService.shouldDisablePendingCollision((Entity) (Object) this, other)) {
            cir.setReturnValue(false);
        }
    }
}
