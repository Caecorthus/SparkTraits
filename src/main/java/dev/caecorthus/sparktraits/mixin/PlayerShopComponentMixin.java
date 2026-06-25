package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.KillerTraitService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Separates the shared blackout cooldown for Conscience killers and normal killers.
 * 将善良杀手与普通杀手的共享关灯冷却分组隔离。
 */
@Mixin(value = PlayerShopComponent.class, remap = false)
public abstract class PlayerShopComponentMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Redirect(
            method = "tryBuy",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/util/ShopEntry;price()I")
    )
    private int sparktraits$applyCharismaPurchasePrice(ShopEntry entry) {
        return KillerTraitService.effectiveCharismaPurchasePrice(this.player, entry);
    }

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
