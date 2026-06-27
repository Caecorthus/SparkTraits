package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.VigilanteVeteranTraitService;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.DerringerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends only police-trait derringer targeting range.
 * 只扩大警察系天赋的德林加锁定射程。
 */
@Mixin(value = DerringerItem.class, remap = false)
public abstract class MarksmanDerringerMixin {
    @Inject(method = "getGunTarget", at = @At("HEAD"), cancellable = true)
    private static void sparktraits$extendMarksmanDerringerRange(
            PlayerEntity user,
            CallbackInfoReturnable<HitResult> cir
    ) {
        double range = VigilanteVeteranTraitService.gunRange(user, VigilanteVeteranTraitService.DERRINGER_RANGE);
        if (range == VigilanteVeteranTraitService.DERRINGER_RANGE) {
            return;
        }
        cir.setReturnValue(ProjectileUtil.getCollision(
                user,
                entity -> entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player),
                range
        ));
    }
}
