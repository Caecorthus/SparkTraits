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
            traits.setGoingDarkInstinctHidden(GoingDarkRules.isTargetHidden(
                    blackoutActive,
                    GameFunctions.isPlayerPlayingAndAlive(player),
                    game.getRole(player),
                    traits.getActiveTraitIds()
            ));
        }
    }
}
