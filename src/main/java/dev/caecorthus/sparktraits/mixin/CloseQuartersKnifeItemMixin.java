package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.combat.CloseQuartersService;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.CombatTimingRules;
import dev.doctor4t.wathe.item.KnifeItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KnifeItem.class)
public abstract class CloseQuartersKnifeItemMixin {
    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void sparktraits$longerRaise(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        if (user instanceof PlayerEntity player && CloseQuartersService.isRaisedKnife(stack)
                && CloseQuartersService.hasRaisedKnifeTrait(player)) cir.setReturnValue(CombatTimingRules.MAX_RAISE_TICKS);
    }

    @ModifyConstant(method = "onStoppedUsing", constant = @Constant(intValue = 10))
    private int sparktraits$threeTickWindup(int original, ItemStack stack, World world, LivingEntity user, int remaining) {
        // Wathe uses elapsed > threshold, not >= threshold. 2 means the first accepted tick is 3.
        return user instanceof PlayerEntity player && CloseQuartersService.isRaisedKnife(stack)
                && CloseQuartersService.hasRaisedKnifeTrait(player) ? CombatTimingRules.KNIFE_WINDUP_TICKS - 1 : original;
    }

    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    private void sparktraits$captureRelease(ItemStack stack, World world, LivingEntity user, int remaining, CallbackInfo ci) {
        if (!(user instanceof PlayerEntity player) || !CloseQuartersService.isRaisedKnife(stack)
                || !CloseQuartersService.hasRaisedKnifeTrait(player)) return;
        if (remaining <= 0) {
            if (player instanceof ServerPlayerEntity serverPlayer) CloseQuartersService.clear(serverPlayer);
            ci.cancel();
        } else if (player instanceof ServerPlayerEntity serverPlayer) {
            CloseQuartersService.captureRelease(serverPlayer, stack);
        }
    }
}
