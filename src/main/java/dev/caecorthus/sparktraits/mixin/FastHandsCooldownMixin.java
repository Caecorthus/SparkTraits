package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.FastHandsTrait;
import dev.caecorthus.sparktraits.impl.GlobalTraitService;
import dev.caecorthus.sparktraits.impl.VigilanteVeteranTraitService;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerItemCooldownManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Reduces only player-owned server item cooldowns, leaving shop and ability timers alone.
 * 只缩短服务端玩家物品冷却，不影响商店冷却和角色技能计时器。
 */
@Mixin(value = ItemCooldownManager.class, priority = 900)
public abstract class FastHandsCooldownMixin {
    @ModifyVariable(method = "set", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int sparktraits$reduceFastHandsCooldown(int duration, Item item) {
        if ((Object) this instanceof ServerItemCooldownManager serverCooldownManager) {
            ServerPlayerEntity player = ((ServerItemCooldownManagerAccessor) (Object) serverCooldownManager).sparktraits$getPlayer();
            if (VigilanteVeteranTraitService.shouldPreserveNikoRevolverCooldown(item, duration, player)) {
                return duration;
            }
            if (GlobalTraitService.hasTrait(player, FastHandsTrait.ID)) {
                return GlobalTraitService.fastHandsCooldown(duration);
            }
        }
        return duration;
    }
}
