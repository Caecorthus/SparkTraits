package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ImpostorItemEntityMixin {
    @Shadow
    public abstract ItemStack getStack();

    @Shadow
    public abstract @Nullable Entity getOwner();

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void sparktraits$allowImpostorRevolverPickup(PlayerEntity player, CallbackInfo ci) {
        if (player.isCreative() || player.getWorld().isClient || !EffectiveTraitService.hasImpostor(player)) {
            return;
        }
        if (!getStack().isOf(WatheItems.REVOLVER) || player.equals(getOwner())) {
            return;
        }
        ItemStack cursorStack = player.currentScreenHandler.getCursorStack();
        boolean hasRevolver = player.getInventory().contains(stack -> stack.isOf(WatheItems.REVOLVER))
                || cursorStack.isOf(WatheItems.REVOLVER);
        if (!hasRevolver) {
            ((GameWorldComponentAccessor) GameWorldComponent.KEY.get(player.getWorld()))
                    .sparktraits$getPreventGunPickup()
                    .remove(player.getUuid());
        }
    }
}
