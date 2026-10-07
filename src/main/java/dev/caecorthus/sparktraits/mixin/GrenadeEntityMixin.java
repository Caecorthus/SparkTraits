package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionFakeKillCooldowns;
import dev.doctor4t.wathe.entity.GrenadeEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Names the grenade as the weapon at Wathe's grenade kill call.
 *  在 Wathe 手雷的击杀调用处指明武器为手雷。 */
@Mixin(GrenadeEntity.class)
public abstract class GrenadeEntityMixin {
    @Redirect(
            method = "onCollision",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;)V"
            )
    )
    private void sparktraits$killWithGrenadeWeaponHint(
            ServerPlayerEntity target,
            boolean spawnBody,
            ServerPlayerEntity killer,
            Identifier deathReason
    ) {
        DepressionFakeKillCooldowns.withWeaponHint(WatheItems.GRENADE,
                () -> GameFunctions.killPlayer(target, spawnBody, killer, deathReason));
    }
}
