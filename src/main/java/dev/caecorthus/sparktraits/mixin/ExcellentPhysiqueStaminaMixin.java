package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.GlobalTraitService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds Excellent Physique's extra natural stamina recovery after Wathe's stamina tick.
 * 在 Wathe 体力 tick 之后追加体质优异的自然体力恢复。
 */
@Mixin(PlayerEntity.class)
public abstract class ExcellentPhysiqueStaminaMixin {
    @Unique
    private float sparktraits$previousSprintingTicks;

    @Inject(method = "tickMovement", at = @At("HEAD"), order = 0)
    private void sparktraits$capturePreviousStamina(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            sparktraits$previousSprintingTicks = PlayerStaminaComponent.KEY.get(player).getSprintingTicks();
        }
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void sparktraits$recoverExcellentPhysiqueStamina(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) {
            return;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (game == null || !game.isRunning() || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }
        GlobalTraitService.recoverExcellentPhysiqueStamina(player, ((Entity) player).isSprinting());
        DepressionTraitService.applyFiniteStaminaPenalty(player, sparktraits$previousSprintingTicks);
    }
}
