package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.ImpostorRevolverService;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks Impostors from bypassing their paid revolver route through ground pickups.
 * 阻止内鬼通过地面拾枪绕过付费购买左轮路径。
 */
@Mixin(value = ItemEntity.class, priority = 2000)
public abstract class ItemEntityMixin {
    @Shadow
    public abstract ItemStack getStack();

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockImpostorGroundGunPickup(PlayerEntity player, CallbackInfo ci) {
        if (player.isCreative() || player.getWorld().isClient) {
            return;
        }
        // Ground collision only; shop insertion does not use ItemEntity pickup.
        // 只拦截地面碰撞拾取，商店直接入包不受影响。
        if (ImpostorRevolverService.shouldBlockGroundGunPickup(
                TraitPlayerComponent.KEY.get(player).getActiveTraitIds(),
                this.getStack().isIn(WatheItemTags.GUNS)
        )) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "onPlayerCollision",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;insertStack(Lnet/minecraft/item/ItemStack;)Z")
    )
    private boolean sparktraits$blockImpostorGroundGunInsert(PlayerInventory inventory, ItemStack stack, PlayerEntity player) {
        // Guard the exact vanilla ground-pickup insert call even if another mixin reaches it.
        // 即使其他 mixin 进入原版地面拾取入包调用，也在这个精确调用点拦住内鬼拿枪。
        if (ImpostorRevolverService.shouldBlockGroundGunPickup(
                TraitPlayerComponent.KEY.get(player).getActiveTraitIds(),
                stack.isIn(WatheItemTags.GUNS)
        )) {
            return false;
        }
        return inventory.insertStack(stack);
    }
}
