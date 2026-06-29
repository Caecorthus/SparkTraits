package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.CorruptCopTraitService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Boosts only Arrogant ASF's strafe input, leaving forward, vertical, jump, and gravity behavior unchanged.
 * 仅放大“展示豪度”的左右平移输入，前后、纵向、跳跃与重力行为保持不变。
 */
@Mixin(PlayerEntity.class)
public abstract class ArrogantAsfMovementInputMixin {
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3d sparktraits$tripleArrogantAsfSidewaysInput(Vec3d movementInput) {
        return CorruptCopTraitService.lateralMovementInput((PlayerEntity) (Object) this, movementInput);
    }
}
