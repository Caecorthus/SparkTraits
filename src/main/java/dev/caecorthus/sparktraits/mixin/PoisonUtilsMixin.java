package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.SparkTraitsDataComponentTypes;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.ConsciencePoisonerService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.util.PoisonUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
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
        World world = target.getWorld();
        if (world.isClient()) {
            return;
        }
        String poisoner = stack.getOrDefault(SparkTraitsDataComponentTypes.CONSCIENCE_POISONER, null);
        if (poisoner == null) {
            return;
        }
        // A blue food trap is single-use even when the effective civilian target is immune.
        // 蓝毒食物是一次性陷阱，即使命中有效好人并免疫也会消耗。
        stack.remove(SparkTraitsDataComponentTypes.CONSCIENCE_POISONER);

        TraitPlayerComponent targetTraits = TraitPlayerComponent.KEY.get(target);
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(world);
        ConsciencePoisonerService.BlueTrapResult result = ConsciencePoisonerService.blueTrapResult(
                true,
                gameComponent.getRole(target),
                targetTraits.getActiveTraitIds()
        );
        if (result != ConsciencePoisonerService.BlueTrapResult.CONSUME_AND_POISON
                || !(target instanceof ServerPlayerEntity serverTarget)) {
            return;
        }

        int ticks = ConsciencePoisonerService.bluePoisonTicksAfterTrap(
                targetTraits.getConsciencePoisonTicks(),
                world.getRandom().nextBetween(PlayerPoisonComponent.clampTime.getLeft(), PlayerPoisonComponent.clampTime.getRight()),
                world.getRandom().nextBetween(100, 300)
        );
        ConsciencePoisonerService.applyBluePoison(serverTarget, UUID.fromString(poisoner), ticks);
    }
}
