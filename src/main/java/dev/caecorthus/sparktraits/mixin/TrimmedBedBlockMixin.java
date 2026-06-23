package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.ConsciencePoisonerService;
import dev.caecorthus.sparktraits.impl.ConscienceScorpionBed;
import dev.doctor4t.wathe.block.TrimmedBedBlock;
import dev.doctor4t.wathe.block_entity.TrimmedBedBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrimmedBedBlock.class)
public abstract class TrimmedBedBlockMixin {
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void sparktraits$placeConscienceScorpion(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (world.isClient
                || player.isCreative()
                || !player.getStackInHand(Hand.MAIN_HAND).isOf(WatheItems.SCORPION)
                || !ConsciencePoisonerService.isConsciencePoisoner(player, GameWorldComponent.KEY.get(world))) {
            return;
        }

        TrimmedBedBlockEntity head = ConsciencePoisonerService.resolveBedHead(world, pos);
        if (!(head instanceof ConscienceScorpionBed bed)) {
            return;
        }

        // Blue scorpions are a separate trap layer, so normal scorpions can still coexist.
        // 蓝蝎子是独立陷阱层，因此可以和普通蝎子同时存在。
        if (!bed.sparktraits$hasConscienceScorpion()) {
            bed.sparktraits$setConscienceScorpion(true, player.getUuid());
            player.getStackInHand(Hand.MAIN_HAND).decrement(1);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                NbtCompound extra = new NbtCompound();
                GameRecordManager.putBlockPos(extra, "pos", head.getPos());
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(WatheItems.SCORPION), null, extra);
            }
        }
        cir.setReturnValue(ActionResult.SUCCESS);
    }
}
