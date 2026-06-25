package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.KillerTraitService;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerItemCooldownManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Applies Bloodthirsty only to the owner's knife cooldown.
 * 只把嗜血作用到持有者自己的刀冷却上。
 */
@Mixin(value = ItemCooldownManager.class, priority = 950)
public abstract class BloodthirstyCooldownMixin {
    @ModifyVariable(method = "set", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int sparktraits$reduceBloodthirstyKnifeCooldown(int duration, Item item) {
        if (item == WatheItems.KNIFE && (Object) this instanceof ServerItemCooldownManager serverCooldownManager) {
            ServerPlayerEntity player = ((ServerItemCooldownManagerAccessor) (Object) serverCooldownManager).sparktraits$getPlayer();
            return KillerTraitService.bloodthirstyCooldown(player, duration);
        }
        return duration;
    }
}
