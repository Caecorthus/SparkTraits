package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.VigilanteVeteranTraitService;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends only Marksman Vigilantes' revolver targeting range.
 * 只扩大精确枪手义警的左轮手枪锁定射程。
 */
@Mixin(value = RevolverItem.class, remap = false)
public abstract class MarksmanRevolverMixin {
    @Inject(method = "getGunTarget", at = @At("HEAD"), cancellable = true)
    private static void sparktraits$extendMarksmanRevolverRange(
            PlayerEntity user,
            CallbackInfoReturnable<HitResult> cir
    ) {
        double range = VigilanteVeteranTraitService.gunRange(user, VigilanteVeteranTraitService.REVOLVER_RANGE);
        if (range == VigilanteVeteranTraitService.REVOLVER_RANGE) {
            return;
        }
        cir.setReturnValue(ProjectileUtil.getCollision(
                user,
                entity -> entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player),
                range
        ));
    }
}
