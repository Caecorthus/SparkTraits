package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.YuushaTraitService;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerItemCooldownManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Reduces the temporary Mankai knife cooldown to 20 seconds while Yuusha is blooming.
 * “満開”期间，将勇者临时匕首的冷却压到 20 秒。
 */
@Mixin(value = ItemCooldownManager.class, priority = 900)
public abstract class YuushaKnifeCooldownMixin {
    @ModifyVariable(method = "set", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int sparktraits$reduceYuushaTemporaryKnifeCooldown(int duration, Item item) {
        if ((Object) this instanceof ServerItemCooldownManager serverCooldownManager) {
            ServerPlayerEntity player = ((ServerItemCooldownManagerAccessor) (Object) serverCooldownManager).sparktraits$getPlayer();
            return YuushaTraitService.yuushaKnifeCooldown(player, item, duration);
        }
        return duration;
    }
}
