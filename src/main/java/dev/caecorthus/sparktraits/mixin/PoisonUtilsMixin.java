package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.SparkTraitsDataComponentTypes;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConsciencePoisonerService;
import dev.doctor4t.wathe.util.PoisonUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/** Triggers blue poisoned food while leaving normal poison handling to Wathe.
 *  触发蓝毒食物，同时保留 Wathe 原本的普通毒处理。 */
@Mixin(value = PoisonUtils.class, remap = false)
public abstract class PoisonUtilsMixin {
    @Inject(method = "applyFoodPoison", at = @At("HEAD"))
    private static void sparktraits$applyConscienceFoodPoison(PlayerEntity target, ItemStack stack, CallbackInfo ci) {
        if (target.getWorld().isClient()) {
            return;
        }
        String poisoner = stack.getOrDefault(SparkTraitsDataComponentTypes.CONSCIENCE_POISONER, null);
        if (poisoner == null) {
            return;
        }
        // A blue food trap is single-use; effective civilians only lose sanity instead of being poisoned.
        // 蓝毒食物是一次性陷阱；命中有效好人时只扣理智，不会中毒。
        stack.remove(SparkTraitsDataComponentTypes.CONSCIENCE_POISONER);
        if (target instanceof ServerPlayerEntity serverTarget) {
            ConsciencePoisonerService.triggerBlueTrap(serverTarget, UUID.fromString(poisoner));
        }
    }
}
