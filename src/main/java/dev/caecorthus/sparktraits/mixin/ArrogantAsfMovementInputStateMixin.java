package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.CorruptCopTraitService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Records pure sideways input before LivingEntity hands the travel vector to speed calculation.
 * 在 LivingEntity 把移动向量交给速度计算前，记录这次输入是否为纯左右横移。
 */
@Mixin(LivingEntity.class)
public abstract class ArrogantAsfMovementInputStateMixin {
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void sparktraits$clearArrogantAsfMovementStateAtHead(CallbackInfo ci) {
        sparktraits$setArrogantAsfPureSidewaysInput(false);
    }

    @ModifyArgs(
            method = "tickMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;<init>(DDD)V")
    )
    private void sparktraits$captureArrogantAsfPureSidewaysInput(Args args) {
        double sideways = args.get(0);
        double forward = args.get(2);
        sparktraits$setArrogantAsfPureSidewaysInput(
                CorruptCopTraitService.isPureSidewaysInput(sideways, forward)
        );
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void sparktraits$clearArrogantAsfMovementStateAtTail(CallbackInfo ci) {
        sparktraits$setArrogantAsfPureSidewaysInput(false);
    }

    private void sparktraits$setArrogantAsfPureSidewaysInput(boolean pureSidewaysInput) {
        if ((Object) this instanceof PlayerEntity player && player instanceof ArrogantAsfMovementState state) {
            state.sparktraits$setArrogantAsfPureSidewaysInput(pureSidewaysInput);
        }
    }
}
