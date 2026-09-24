package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers direct pushes, including BoatEntity's super call, on both logical sides.
 *  在双端覆盖直接推挤，包括船的父类调用。 */
@Mixin(Entity.class)
public abstract class LastEscapeEntityPushMixin {
    @Inject(method = "pushAwayFrom(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void sparktraits$escapeDirectPush(Entity other, CallbackInfo ci) {
        if (LastEscapeService.blocksEntityCollision((Entity) (Object) this, other)) ci.cancel();
    }
}
