package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.ImpostorRevolverService;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops illegal cursor guns before Wathe can stash them back into an Impostor inventory.
 * 在 Wathe 将光标枪械塞回内鬼背包前，先把非法枪械丢回地上。
 */
@Mixin(value = ScreenHandler.class, priority = 2000)
public abstract class ScreenHandlerMixin {
    @Shadow
    public abstract ItemStack getCursorStack();

    @Shadow
    public abstract void setCursorStack(ItemStack stack);

    @Inject(method = "onClosed", at = @At("HEAD"))
    private void sparktraits$dropImpostorCursorGun(PlayerEntity player, CallbackInfo ci) {
        if (player.isCreative() || player.getWorld().isClient) {
            return;
        }
        ItemStack cursorStack = this.getCursorStack();
        if (!ImpostorRevolverService.shouldBlockNonShopGunAcquisition(
                TraitPlayerComponent.KEY.get(player).getActiveTraitIds(),
                cursorStack.isIn(WatheItemTags.GUNS)
        )) {
            return;
        }
        // Preserve the item entity instead of letting Wathe silently insert or delete the cursor gun.
        // 保留地面物品实体，避免 Wathe 静默把光标枪械塞入背包或因拦截失败而吞掉。
        player.dropItem(cursorStack.copy(), false);
        this.setCursorStack(ItemStack.EMPTY);
    }
}
