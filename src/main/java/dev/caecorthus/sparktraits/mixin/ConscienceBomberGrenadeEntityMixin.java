package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.BombManiacGrenadeAccess;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.GrenadeEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies the launch snapshot only at Wathe's ordinary grenade kill call.
 *  仅在 Wathe 普通手雷的击杀调用处应用发射快照。 */
@Mixin(GrenadeEntity.class)
public abstract class ConscienceBomberGrenadeEntityMixin implements BombManiacGrenadeAccess {
    @Unique
    private boolean sparktraits$bombManiac;

    @Override
    public void sparktraits$setBombManiac(boolean bombManiac) {
        sparktraits$bombManiac = bombManiac;
    }

    @Override
    public boolean sparktraits$isBombManiac() {
        return sparktraits$bombManiac;
    }

    @Redirect(
            method = "onCollision",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;)V"
            )
    )
    private void sparktraits$protectBombManiacCivilian(
            ServerPlayerEntity target,
            boolean spawnBody,
            ServerPlayerEntity killer,
            Identifier deathReason
    ) {
        GameWorldComponent game = GameWorldComponent.KEY.get(target.getWorld());
        Role role = game.getRole(target);
        boolean effectiveCivilian = EffectiveTraitService.isEffectiveCivilian(
                role,
                TraitPlayerComponent.KEY.get(target).getActiveTraitIds()
        );
        boolean shouldKill = !sparktraits$isBombManiac()
                || ConscienceBomberFrenzyRules.shouldKillTarget(
                        role == null ? null : role.identifier(),
                        effectiveCivilian
                );
        if (shouldKill) {
            GameFunctions.killPlayer(target, spawnBody, killer, deathReason);
        }
    }
}
