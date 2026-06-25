package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.KillerTraitService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds Thrust as a narrow extra knockback modifier for eligible killer weapons.
 * 将突刺作为合格杀手武器上的窄范围额外击退修饰。
 */
@Mixin(LivingEntity.class)
public abstract class KillerTraitLivingEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void sparktraits$updateThrustKnockback(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player) {
            KillerTraitService.updateThrustKnockback(player);
        }
    }
}
