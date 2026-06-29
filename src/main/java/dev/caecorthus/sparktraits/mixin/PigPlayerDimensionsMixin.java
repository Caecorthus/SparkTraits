package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.PigTraitService;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives Pig players vanilla pig-sized collision without touching other entities.
 * 仅让猪天赋玩家使用原版猪碰撞体积，不影响其他实体。
 */
@Mixin(PlayerEntity.class)
public abstract class PigPlayerDimensionsMixin {
    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void sparktraits$pigDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (PigTraitService.shouldUsePigDimensions(player, pose)) {
            cir.setReturnValue(PigTraitService.pigDimensions());
        }
    }
}
