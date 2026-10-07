package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.combat.CloseQuartersService;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.CombatTimingRules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class CloseQuartersActiveUseMixin {
    @Inject(method = "setCurrentHand", at = @At("RETURN"))
    private void sparktraits$beginRaisedStance(Hand hand, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) CloseQuartersService.beginRaise(player);
    }

    @Inject(method = "clearActiveItem", at = @At("HEAD"))
    private void sparktraits$endRaisedStance(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) CloseQuartersService.endRaise(player);
    }

    @Inject(method = "tickActiveItemStack", at = @At("HEAD"), cancellable = true)
    private void sparktraits$cancelExpiredRaise(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player && player.isUsingItem()
                && CloseQuartersService.isRaisedKnife(player.getActiveItem())
                && CloseQuartersService.hasRaisedKnifeTrait(player)
                && player.getItemUseTime() >= CombatTimingRules.MAX_RAISE_TICKS - 1) {
            // Cancel before vanilla reaches its finishUsing path. No release/stab is generated.
            if (player instanceof ServerPlayerEntity serverPlayer) CloseQuartersService.clear(serverPlayer);
            player.clearActiveItem();
            ci.cancel();
        }
    }
}
