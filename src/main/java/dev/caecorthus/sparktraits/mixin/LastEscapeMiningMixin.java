package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;

@Mixin(value = ServerPlayerInteractionManager.class, priority = 2000)
public abstract class LastEscapeMiningMixin {
    @Shadow @Final protected ServerPlayerEntity player;
    @Shadow protected ServerWorld world;
    @Shadow private boolean mining;
    @Shadow private boolean failedToMine;
    @Shadow private BlockPos miningPos;
    @Shadow private BlockPos failedMiningPos;

    @WrapMethod(method = "update")
    private void sparktraits$cancelHeldMining(Operation<Void> original) {
        if (LastEscapeService.isActive(player)) {
            if (mining) world.setBlockBreakingInfo(player.getId(), miningPos, -1);
            if (failedToMine) world.setBlockBreakingInfo(player.getId(), failedMiningPos, -1);
            mining = false;
            failedToMine = false;
        }
        original.call();
    }

    @WrapMethod(method = "interactItem")
    private net.minecraft.util.ActionResult sparktraits$blockItemUse(ServerPlayerEntity actor,
            net.minecraft.world.World targetWorld, net.minecraft.item.ItemStack stack,
            net.minecraft.util.Hand hand, Operation<net.minecraft.util.ActionResult> original) {
        return LastEscapeService.isActive(actor) ? net.minecraft.util.ActionResult.FAIL
                : original.call(actor, targetWorld, stack, hand);
    }

    @WrapMethod(method = "interactBlock")
    private net.minecraft.util.ActionResult sparktraits$blockBlockUse(ServerPlayerEntity actor,
            net.minecraft.world.World targetWorld, net.minecraft.item.ItemStack stack,
            net.minecraft.util.Hand hand, net.minecraft.util.hit.BlockHitResult hit,
            Operation<net.minecraft.util.ActionResult> original) {
        return LastEscapeService.isActive(actor) ? net.minecraft.util.ActionResult.FAIL
                : original.call(actor, targetWorld, stack, hand, hit);
    }

    @WrapMethod(method = "tryBreakBlock")
    private boolean sparktraits$blockBreak(BlockPos pos, Operation<Boolean> original) {
        return !LastEscapeService.isActive(player) && original.call(pos);
    }
}
