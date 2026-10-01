package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.RaycastShapeScope;
import dev.doctor4t.wathe.block.DoorPartBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Movement only: rays keep the closed door, so the escaping player cannot see, aim or target through it.
 *  仅限移动：射线仍保留关闭的门，逃脱者无法隔门观察、瞄准或选取目标。 */
@Mixin(DoorPartBlock.class)
public abstract class LastEscapeDoorMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void sparktraits$passDoor(BlockState state, BlockView world, BlockPos pos, ShapeContext context,
                                     CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityShapeContext shape && shape.getEntity() instanceof PlayerEntity player
                && LastEscapeService.isActive(player)
                && !RaycastShapeScope.isRaycast()) cir.setReturnValue(VoxelShapes.empty());
    }
}
