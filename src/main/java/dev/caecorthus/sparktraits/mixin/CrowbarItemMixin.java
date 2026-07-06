package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceCrowbarService;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.item.CrowbarItem;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Extends Wathe's successful crowbar use cooldown for Conscience killers.
 * 将善良杀手成功使用撬棍后的冷却延长为两分钟。
 */
@Mixin(value = CrowbarItem.class, remap = false)
public abstract class CrowbarItemMixin {
    @Redirect(
            method = "useOnBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ItemCooldownManager;set(Lnet/minecraft/item/Item;I)V",
                    ordinal = 1
            )
    )
    private void sparktraits$setConscienceCrowbarDoorCooldown(
            ItemCooldownManager cooldownManager,
            Item item,
            int cooldownTicks,
            ItemUsageContext context
    ) {
        int effectiveCooldownTicks = SparkTraitsServerConnection.isUnconfirmedClientEntity(context.getPlayer())
                ? cooldownTicks
                : ConscienceCrowbarService.crowbarCooldownTicks(context.getPlayer(), cooldownTicks);
        cooldownManager.set(item, effectiveCooldownTicks);
    }
}
