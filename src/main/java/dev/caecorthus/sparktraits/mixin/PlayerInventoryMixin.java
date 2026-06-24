package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.ImpostorRevolverService;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks free inventory insertion of guns for Impostors while leaving shop slot placement intact.
 * 阻止内鬼通过免费入包获得枪械，同时不影响商店购买的直接放入快捷栏。
 */
@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {
    @Shadow
    @Final
    public PlayerEntity player;

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockImpostorGunInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (sparktraits$shouldBlockNonShopGunAcquisition(stack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockImpostorGunInsertIntoSlot(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (sparktraits$shouldBlockNonShopGunAcquisition(stack)) {
            cir.setReturnValue(false);
        }
    }

    private boolean sparktraits$shouldBlockNonShopGunAcquisition(ItemStack stack) {
        if (player == null || player.isCreative() || player.getWorld().isClient) {
            return false;
        }
        return ImpostorRevolverService.shouldBlockNonShopGunAcquisition(
                TraitPlayerComponent.KEY.get(player).getActiveTraitIds(),
                stack.isIn(WatheItemTags.GUNS)
        );
    }
}
