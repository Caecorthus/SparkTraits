package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.CorruptCopTraitService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Boosts only the sideways input used to build LivingEntity's travel vector.
 * 仅放大 LivingEntity 构造移动向量时使用的左右平移输入。
 */
@Mixin(LivingEntity.class)
public abstract class ArrogantAsfSidewaysSpeedMixin {
    @ModifyArg(
            method = "tickMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;<init>(DDD)V"),
            index = 0
    )
    private double sparktraits$tripleArrogantAsfSidewaysSpeed(double sideways) {
        if (!((Object) this instanceof PlayerEntity player)) {
            return sideways;
        }
        return CorruptCopTraitService.lateralSidewaysInput(player, sideways);
    }
}
