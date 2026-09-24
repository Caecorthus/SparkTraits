package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minecarts write both velocities without calling Entity.pushAwayFrom.
 *  矿车直接修改双方速度，不调用 Entity.pushAwayFrom。 */
@Mixin(AbstractMinecartEntity.class)
public abstract class LastEscapeMinecartPushMixin {
    @Inject(method = "pushAwayFrom(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void sparktraits$escapeMinecartPush(Entity other, CallbackInfo ci) {
        if (LastEscapeService.blocksEntityCollision((Entity) (Object) this, other)) ci.cancel();
    }
}
