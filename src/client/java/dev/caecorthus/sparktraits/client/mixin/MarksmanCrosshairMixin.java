package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.VigilanteVeteranTraitService;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps police-trait gun crosshair feedback aligned with the extended shot range.
 * 让警察系天赋的枪械准星提示与扩大后的射程保持一致。
 */
@Mixin(value = CrosshairRenderer.class, remap = false)
public abstract class MarksmanCrosshairMixin {
    @Inject(method = "getVisibleGunTarget", at = @At("HEAD"), cancellable = true)
    private static void sparktraits$extendMarksmanCrosshairRange(
            PlayerEntity user,
            double range,
            CallbackInfoReturnable<HitResult> cir
    ) {
        double extendedRange = VigilanteVeteranTraitService.gunRange(user, range);
        if (extendedRange == range) {
            return;
        }
        cir.setReturnValue(ProjectileUtil.getCollision(
                user,
                entity -> entity instanceof PlayerEntity player
                        && GameFunctions.isPlayerAliveAndSurvival(player)
                        && !entity.isInvisible(),
                extendedRange
        ));
    }
}
