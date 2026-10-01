package dev.caecorthus.sparktraits.client.instinct;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.chameleon.ChameleonRules;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;

/** Adapts synced client state to Chameleon's pure instinct suppression rule.
 *  将客户端同步状态适配到变色龙的纯本能屏蔽规则。 */
public final class ChameleonInstinctClientHooks {
    private ChameleonInstinctClientHooks() {
    }

    public static boolean shouldSuppress(PlayerEntity viewer, PlayerEntity target) {
        return ChameleonRules.shouldSuppressInstinct(
                ChameleonRules.isFullyTransparent(
                        TraitPlayerComponent.KEY.get(target).getChameleonStillSinceTick(),
                        target.getWorld().getTime()
                ),
                GameFunctions.isPlayerPlayingAndAlive(viewer),
                WatheClient.canSeeSpectatorInformation(),
                TraitWorldComponent.KEY.get(viewer.getWorld()).isFinalMomentActive()
        );
    }
}
