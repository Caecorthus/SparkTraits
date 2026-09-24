package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
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

@Mixin(DoorPartBlock.class)
public abstract class LastEscapeDoorMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void sparktraits$passDoor(BlockState state, BlockView world, BlockPos pos, ShapeContext context,
                                     CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityShapeContext shape && shape.getEntity() instanceof PlayerEntity player
                && LastEscapeService.isActive(player)) cir.setReturnValue(VoxelShapes.empty());
    }
}
