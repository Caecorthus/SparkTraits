package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Separates the shared blackout cooldown for Conscience killers and normal killers.
 * 将善良杀手与普通杀手的共享关灯冷却分组隔离。
 */
@Mixin(value = PlayerShopComponent.class, remap = false)
public abstract class PlayerShopComponentMixin {
    @Redirect(
            method = "applyBlackoutCooldownToAllKillers",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;canUseKillerFeatures(Lnet/minecraft/entity/player/PlayerEntity;)Z")
    )
    private static boolean sparktraits$shareBlackoutCooldownWithMatchingGroup(
            GameWorldComponent gameComponent,
            PlayerEntity cooldownTarget,
            ServerPlayerEntity purchaser
    ) {
        return gameComponent.canUseKillerFeatures(cooldownTarget)
                && EffectiveTraitService.sharesBlackoutCooldown(
                        gameComponent.getRole(purchaser),
                        TraitPlayerComponent.KEY.get(purchaser).getActiveTraitIds(),
                        gameComponent.getRole(cooldownTarget),
                        TraitPlayerComponent.KEY.get(cooldownTarget).getActiveTraitIds()
                );
    }
}
