package dev.caecorthus.sparktraits.client.instinct;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.GoingDarkRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;

/** Adapts synced client state to Going Dark's pure suppression rules.
 *  将客户端同步状态适配到隐蔽行动的纯屏蔽规则。 */
public final class GoingDarkInstinctClientHooks {
    private GoingDarkInstinctClientHooks() {
    }

    public static boolean shouldSuppress(InstinctViewerContext context, PlayerEntity target) {
        return context.goingDarkSuppressesHiddenTargets()
                && TraitPlayerComponent.KEY.get(target).isGoingDarkInstinctHidden();
    }

    /**
     * The viewer half of the rule, read once per frame: whether a hidden Going Dark target would be suppressed. A target
     * that is not hidden is never suppressed, so the per-target check is only its hidden flag.
     * 规则中仅与观察者有关的部分，每帧读取一次：隐蔽中的目标是否会被屏蔽。未隐蔽的目标从不被屏蔽，因此逐目标只需检查其隐蔽标记。
     */
    static boolean viewerSuppressesHiddenTargets(
            PlayerEntity viewer,
            GameWorldComponent game,
            boolean canSeeSpectatorInformation,
            boolean finalMomentActive
    ) {
        return GoingDarkRules.shouldSuppressInstinct(
                true,
                GameFunctions.isPlayerPlayingAndAlive(viewer),
                canSeeSpectatorInformation,
                finalMomentActive,
                game.getRole(viewer),
                TraitPlayerComponent.KEY.get(viewer).getActiveTraitIds()
        );
    }
}
