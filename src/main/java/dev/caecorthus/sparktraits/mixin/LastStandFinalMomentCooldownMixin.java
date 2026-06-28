package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.LastStandFinalMomentService;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerItemCooldownManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Clears knife cooldown only for the Last Stand Final Moment Loose End.
 * 只为背水一战终局时刻的亡命徒清除匕首冷却。
 */
@Mixin(value = ItemCooldownManager.class, priority = 1000)
public abstract class LastStandFinalMomentCooldownMixin {
    @ModifyVariable(method = "set", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int sparktraits$clearLastStandFinalMomentKnifeCooldown(int duration, Item item) {
        if ((Object) this instanceof ServerItemCooldownManager serverCooldownManager) {
            ServerPlayerEntity player = ((ServerItemCooldownManagerAccessor) (Object) serverCooldownManager).sparktraits$getPlayer();
            return LastStandFinalMomentService.finalMomentKnifeCooldown(player, item, duration);
        }
        return duration;
    }
}
