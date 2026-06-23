package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.ConscienceCrowbarService;
import dev.doctor4t.wathe.block.VentHatchBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Applies the same Conscience crowbar cooldown when prying vent hatches.
 * 善良杀手撬开通风口时同样使用两分钟撬棍冷却。
 */
@Mixin(value = VentHatchBlock.class, remap = false)
public abstract class VentHatchBlockMixin {
    @Redirect(
            method = "onUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ItemCooldownManager;set(Lnet/minecraft/item/Item;I)V",
                    ordinal = 1
            )
    )
    private void sparktraits$setConscienceCrowbarVentCooldown(
            ItemCooldownManager cooldownManager,
            Item item,
            int cooldownTicks,
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit
    ) {
        cooldownManager.set(item, ConscienceCrowbarService.crowbarCooldownTicks(player, cooldownTicks));
    }
}
