package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** Owns Conscience rewards that apply independently of direct-kill rewards.
 *  统一处理不依赖直接击杀奖励的善良经济规则。 */
public final class ConscienceEconomyService {
    public static final int DEATH_DIVIDEND = 10;

    private ConscienceEconomyService() {
    }

    static int deathDividend(
            boolean confirmedRealDeath,
            boolean ownerHasConscience,
            boolean ownerPlayingAndAlive,
            boolean ownerIsVictim
    ) {
        return confirmedRealDeath && ownerHasConscience && ownerPlayingAndAlive && !ownerIsVictim
                ? DEATH_DIVIDEND
                : 0;
    }

    public static void rewardAfterConfirmedRealDeath(ServerPlayerEntity victim) {
        if (!(victim.getWorld() instanceof ServerWorld world)) {
            return;
        }
        for (ServerPlayerEntity owner : world.getPlayers()) {
            int reward = deathDividend(
                    true,
                    EffectiveTraitService.hasConscience(owner),
                    GameFunctions.isPlayerPlayingAndAlive(owner),
                    owner.getUuid().equals(victim.getUuid())
            );
            if (reward > 0) {
                PlayerShopComponent.KEY.get(owner).addToBalance(reward);
            }
        }
    }
}
