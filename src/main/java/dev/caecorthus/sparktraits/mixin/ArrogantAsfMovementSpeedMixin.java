package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.caecorthus.sparktraits.impl.CorruptCopTraitService;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Applies Arrogant ASF after Wathe's in-round PlayerEntity speed override has produced the final speed.
 * 在 Wathe 局内 PlayerEntity 速度覆盖算出最终速度之后，再应用展示豪度的加速。
 */
@Mixin(value = PlayerEntity.class, priority = 900)
public abstract class ArrogantAsfMovementSpeedMixin implements ArrogantAsfMovementState {
    @Unique
    private boolean sparktraits$arrogantAsfPureSidewaysInput;

    @Override
    public void sparktraits$setArrogantAsfPureSidewaysInput(boolean pureSidewaysInput) {
        sparktraits$arrogantAsfPureSidewaysInput = pureSidewaysInput;
    }

    @Override
    public boolean sparktraits$isArrogantAsfPureSidewaysInput() {
        return sparktraits$arrogantAsfPureSidewaysInput;
    }

    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    private float sparktraits$tripleArrogantAsfWatheMovementSpeed(float original) {
        return CorruptCopTraitService.lateralMovementSpeed(
                (PlayerEntity) (Object) this,
                original,
                sparktraits$arrogantAsfPureSidewaysInput
        );
    }
}
