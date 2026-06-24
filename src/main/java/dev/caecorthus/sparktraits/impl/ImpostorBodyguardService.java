package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.bodyguard.BodyguardPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.Collection;
import java.util.UUID;

/** Handles Impostor Bodyguard betrayal rewards.
 *  处理内鬼保镖放弃替死后的目标死亡奖励。 */
public final class ImpostorBodyguardService {
    public static final int TARGET_DEATH_REWARD = 100;

    private ImpostorBodyguardService() {
    }

    public static boolean shouldProtectTarget(PlayerEntity bodyguard) {
        return bodyguard != null && shouldProtectTarget(TraitPlayerComponent.KEY.get(bodyguard).getActiveTraitIds());
    }

    public static boolean shouldProtectTarget(Collection<Identifier> bodyguardTraits) {
        return !EffectiveTraitService.hasImpostor(bodyguardTraits);
    }

    public static int targetDeathReward(Collection<Identifier> bodyguardTraits, boolean bodyguardAlive, boolean currentTarget) {
        return bodyguardAlive && currentTarget && EffectiveTraitService.hasImpostor(bodyguardTraits) ? TARGET_DEATH_REWARD : 0;
    }

    public static void handleAfterKill(ServerPlayerEntity victim) {
        if (victim == null || !(victim.getWorld() instanceof ServerWorld world)) {
            return;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (UUID bodyguardUuid : game.getAllWithRole(Noellesroles.BODYGUARD)) {
            if (!(world.getPlayerByUuid(bodyguardUuid) instanceof ServerPlayerEntity bodyguard)) {
                continue;
            }
            BodyguardPlayerComponent bodyguardComponent = BodyguardPlayerComponent.KEY.get(bodyguard);
            Collection<Identifier> bodyguardTraits = TraitPlayerComponent.KEY.get(bodyguard).getActiveTraitIds();
            int reward = targetDeathReward(
                    bodyguardTraits,
                    GameFunctions.isPlayerPlayingAndAlive(bodyguard) && !SwallowedPlayerComponent.isPlayerSwallowed(bodyguard),
                    bodyguardComponent.isCurrentTarget(victim.getUuid())
            );
            if (reward > 0) {
                PlayerShopComponent.KEY.get(bodyguard).addToBalance(reward);
            }
        }
    }
}
