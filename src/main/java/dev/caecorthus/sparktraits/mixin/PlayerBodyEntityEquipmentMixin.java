package dev.caecorthus.sparktraits.mixin;

import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Keeps Wathe corpses compatible with equipment scanners such as dynamic lights.
 * 让 wathe 尸体兼容动态光源等装备扫描逻辑，避免返回 null 装备列表。
 */
@Mixin(PlayerBodyEntity.class)
public abstract class PlayerBodyEntityEquipmentMixin {
    @Inject(method = "getArmorItems", at = @At("RETURN"), cancellable = true)
    private void sparktraits$emptyArmorItemsForBody(CallbackInfoReturnable<Iterable<ItemStack>> cir) {
        if (cir.getReturnValue() == null) {
            cir.setReturnValue(List.of());
        }
    }
}
