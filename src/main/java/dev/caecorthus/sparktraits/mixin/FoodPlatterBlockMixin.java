package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.SparkTraitsDataComponentTypes;
import dev.caecorthus.sparktraits.impl.ConsciencePoisonedPlate;
import dev.caecorthus.sparktraits.impl.ConsciencePoisonerService;
import dev.doctor4t.wathe.block.FoodPlatterBlock;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Adds Conscience Poisoner's independent blue-poison layer to platters.
 *  给餐盘增加善良毒师独立的蓝毒层，不覆盖普通毒。 */
@Mixin(FoodPlatterBlock.class)
public abstract class FoodPlatterBlockMixin {
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void sparktraits$handleConsciencePoison(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (world.isClient
                || !(world.getBlockEntity(pos) instanceof BeveragePlateBlockEntity blockEntity)
                || !(blockEntity instanceof ConsciencePoisonedPlate consciencePlate)) {
            return;
        }

        ItemStack heldStack = player.getStackInHand(Hand.MAIN_HAND);
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(world);
        boolean consciencePoisoner = ConsciencePoisonerService.isConsciencePoisoner(player, gameComponent);
        if (heldStack.isOf(WatheItems.POISON_VIAL) && consciencePoisoner && !blockEntity.getStoredItems().isEmpty()) {
            if (consciencePlate.sparktraits$getConsciencePoisoner() == null) {
                // Conscience poison stays separate so normal poison can coexist on the same platter.
                // 善良蓝毒单独保存，因此同一个餐盘仍可同时存在普通毒。
                consciencePlate.sparktraits$setConsciencePoisoner(player.getUuidAsString());
                heldStack.decrement(1);
                player.playSoundToPlayer(SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.5f, 1f);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    NbtCompound extra = new NbtCompound();
                    GameRecordManager.putBlockPos(extra, "pos", pos);
                    GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(WatheItems.POISON_VIAL), null, extra);
                }
            }
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        if (!heldStack.isEmpty() || consciencePlate.sparktraits$getConsciencePoisoner() == null) {
            return;
        }

        List<ItemStack> platter = blockEntity.getStoredItems();
        if (platter.isEmpty() || playerAlreadyCarriesPlatterItem(player, platter)) {
            return;
        }

        ItemStack randomItem = platter.get(world.random.nextInt(platter.size())).copy();
        randomItem.setCount(1);
        randomItem.set(DataComponentTypes.MAX_STACK_SIZE, 1);
        String normalPoisoner = blockEntity.getPoisoner();
        String consciencePoisonerUuid = consciencePlate.sparktraits$getConsciencePoisoner();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordPlatterTake(serverPlayer, Registries.ITEM.getId(randomItem.getItem()), pos, normalPoisoner);
        }
        if (normalPoisoner != null) {
            randomItem.set(WatheDataComponentTypes.POISONER, normalPoisoner);
            blockEntity.setPoisoner(null);
        }
        randomItem.set(SparkTraitsDataComponentTypes.CONSCIENCE_POISONER, consciencePoisonerUuid);
        consciencePlate.sparktraits$setConsciencePoisoner(null);
        player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
        player.setStackInHand(Hand.MAIN_HAND, randomItem);
        cir.setReturnValue(ActionResult.PASS);
    }

    private static boolean playerAlreadyCarriesPlatterItem(PlayerEntity player, List<ItemStack> platter) {
        for (ItemStack platterItem : platter) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                if (player.getInventory().getStack(i).getItem() == platterItem.getItem()) {
                    return true;
                }
            }
        }
        return false;
    }
}
