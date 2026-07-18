package dev.caecorthus.sparktraits.client.instinct;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.GoingDarkRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;

/** Adapts synced client state to Going Dark's pure suppression rules.
 *  将客户端同步状态适配到隐蔽行动的纯屏蔽规则。 */
public final class GoingDarkInstinctClientHooks {
    private GoingDarkInstinctClientHooks() {
    }

    public static boolean shouldSuppress(PlayerEntity viewer, PlayerEntity target, GameWorldComponent game) {
        return GoingDarkRules.shouldSuppressInstinct(
                TraitPlayerComponent.KEY.get(target).isGoingDarkInstinctHidden(),
                GameFunctions.isPlayerPlayingAndAlive(viewer),
                WatheClient.canSeeSpectatorInformation(),
                TraitWorldComponent.KEY.get(viewer.getWorld()).isFinalMomentActive(),
                game.getRole(viewer),
                TraitPlayerComponent.KEY.get(viewer).getActiveTraitIds()
        );
    }
}
