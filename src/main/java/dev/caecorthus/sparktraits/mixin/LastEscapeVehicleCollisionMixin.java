package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Both boat and minecart collidesWith overrides delegate here, bypassing Entity's hook.
 *  船和矿车的碰撞覆写均委托至此，绕过 Entity 的钩子。 */
@Mixin(BoatEntity.class)
public abstract class LastEscapeVehicleCollisionMixin {
    @Inject(method = "canCollide(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void sparktraits$escapeVehicleCollision(Entity first, Entity second,
                                                           CallbackInfoReturnable<Boolean> cir) {
        if (LastEscapeService.blocksEntityCollision(first, second)) cir.setReturnValue(false);
    }
}
