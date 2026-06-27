package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.VigilanteVeteranTraitService;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extends only police-trait revolver targeting range and recoil.
 * 只扩大警察系天赋的左轮手枪锁定射程与后坐力效果。
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

    @ModifyConstant(
            method = {"use", "method_7836"},
            constant = @Constant(floatValue = 4.0f),
            remap = false
    )
    private float sparktraits$reduceNikoRevolverRecoil(float recoil, World world, PlayerEntity user, Hand hand) {
        return VigilanteVeteranTraitService.adjustedRevolverRecoil(recoil, user);
    }
}
