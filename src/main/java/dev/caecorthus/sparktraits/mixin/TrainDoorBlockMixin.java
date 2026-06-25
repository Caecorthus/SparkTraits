package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.ChildishDoorSafetyService;
import dev.doctor4t.wathe.block.TrainDoorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Runs the Childish-only train door unstuck check after Wathe has toggled door collision.
 * 在 wathe 完成列车门碰撞切换后，只为幼稚体质执行防卡门检查。
 */
@Mixin(TrainDoorBlock.class)
public abstract class TrainDoorBlockMixin {
    @Inject(method = "onUse", at = @At("RETURN"))
    private void sparktraits$nudgeChildishPlayerAfterTrainDoorUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        ChildishDoorSafetyService.afterTrainDoorUse(state, world, pos, player, cir.getReturnValue());
    }
}
