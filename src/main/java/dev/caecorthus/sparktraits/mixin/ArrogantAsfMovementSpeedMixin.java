package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.CorruptCopTraitService;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Applies Arrogant ASF after Wathe finishes computing horizontal player speed.
 * 在 Wathe 完成玩家横向速度计算后应用“展示豪度”倍率。
 */
@Mixin(PlayerEntity.class)
public abstract class ArrogantAsfMovementSpeedMixin {
    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    private float sparktraits$tripleArrogantAsfMovementSpeed(float original) {
        return CorruptCopTraitService.horizontalMovementSpeed((PlayerEntity) (Object) this, original);
    }
}
