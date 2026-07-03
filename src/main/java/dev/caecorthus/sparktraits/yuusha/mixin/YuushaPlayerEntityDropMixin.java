package dev.caecorthus.sparktraits.mixin.yuusha.mixin;

import dev.caecorthus.sparktraits.yuusha.YuushaTrait;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class YuushaPlayerEntityDropMixin {
    @Inject(
        method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sparktraits$discardTemporaryYuushaWeaponDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        if (YuushaTrait.isTemporaryYuushaWeapon(stack)) {
            stack.setCount(0);
            cir.setReturnValue(null);
        }
    }
}
