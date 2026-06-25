package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.VigilanteVeteranTraitService;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerItemCooldownManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Applies Fast Reload only to original Vigilantes' revolver cooldown.
 * 只把快速装填作用到原始义警的左轮手枪冷却上。
 */
@Mixin(value = ItemCooldownManager.class, priority = 925)
public abstract class FastReloadCooldownMixin {
    @ModifyVariable(method = "set", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int sparktraits$reduceFastReloadRevolverCooldown(int duration, Item item) {
        if ((Object) this instanceof ServerItemCooldownManager serverCooldownManager) {
            ServerPlayerEntity player = ((ServerItemCooldownManagerAccessor) (Object) serverCooldownManager).sparktraits$getPlayer();
            return VigilanteVeteranTraitService.fastReloadCooldown(item, duration, player);
        }
        return duration;
    }
}
