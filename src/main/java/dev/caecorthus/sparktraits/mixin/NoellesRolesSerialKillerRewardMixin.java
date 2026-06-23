package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.ConscienceSerialKillerService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Removes the original hunter bonus when the target is a protected Conscience target.
 *  当目标是善良连环杀手的保护对象时，移除 NoellesRoles 原本的猎杀目标奖励。 */
@Mixin(value = Noellesroles.class, remap = false)
public abstract class NoellesRolesSerialKillerRewardMixin {
    @Redirect(
            method = "lambda$registerEvents$20",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/PlayerShopComponent;addToBalance(I)V",
                    ordinal = 0
            )
    )
    private static void sparktraits$skipConscienceProtectedTargetBonus(
            PlayerShopComponent shop,
            int amount,
            ServerPlayerEntity victim,
            ServerPlayerEntity killer,
            Identifier deathReason
    ) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(killer.getWorld());
        if (!ConscienceSerialKillerService.isConscienceSerialKiller(killer, gameComponent)) {
            shop.addToBalance(amount);
        }
    }
}
