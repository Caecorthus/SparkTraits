package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.RaycastShapeScope;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;

/** Scopes every ray block-shape query on both sides, keeping the ray's own shape context for other mods.
 *  双端为所有射线方块形状查询设置作用域，保留射线自身的形状上下文，不影响其他模组。 */
@Mixin(RaycastContext.class)
public abstract class RaycastShapeScopeMixin {
    @WrapMethod(method = "getBlockShape")
    private VoxelShape sparktraits$scopeRayBlockShape(BlockState state, BlockView world, BlockPos pos,
                                                      Operation<VoxelShape> original) {
        return RaycastShapeScope.query(() -> original.call(state, world, pos));
    }
}
