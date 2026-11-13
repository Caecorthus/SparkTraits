package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** Publishes Going Dark's blackout state without owning its gameplay rules.
 *  仅负责发布隐蔽行动的关灯状态，不承载玩法规则。 */
public final class GoingDarkSyncService {
    private GoingDarkSyncService() {
    }

    public static void sync(ServerWorld world, boolean blackoutActive) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
            // Runs for every player on every blackout tick. The rule needs the blackout and the Going Dark trait
            // anyway, so both are tested first and the life-state and role lookups are left to actual holders;
            // the result is the same.
            // 关灯期间每 tick 对每名玩家执行。规则本身就要求处于关灯且持有隐蔽行动，因此先判断这两项，
            // 存活状态与身份查询只留给真正的持有者；结果不变。
            traits.setGoingDarkInstinctHidden(blackoutActive
                    && traits.hasActiveTrait(PoliceTraits.GOING_DARK)
                    && GoingDarkRules.isTargetHidden(
                    true,
                    GameFunctions.isPlayerPlayingAndAlive(player),
                    game.getRole(player),
                    traits.getActiveTraitIds()
            ));
        }
    }
}
