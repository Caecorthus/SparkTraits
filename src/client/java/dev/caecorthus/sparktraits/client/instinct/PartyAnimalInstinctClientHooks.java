package dev.caecorthus.sparktraits.client.instinct;

import dev.caecorthus.sparktraits.client.compat.SparkStrengthPartyBalloonBridge;
import dev.caecorthus.sparktraits.impl.compatibility.noellesroles.PartyAnimalInstinctRules;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.voice.HeliumBuzzPlayerComponent;
import org.jetbrains.annotations.Nullable;

/** Adapts synced client state to the Party Animal's keyless outline rules in {@link PartyAnimalInstinctRules}.
 *  将客户端同步状态适配到 PartyAnimalInstinctRules 中派对狂免按键描边的规则。 */
public final class PartyAnimalInstinctClientHooks {
    private PartyAnimalInstinctClientHooks() {
    }

    /**
     * The helium or balloon outline Wathe's event would draw on {@code target} for a Party Animal viewer, or
     * {@code null} when there is none or something outranks it. The event is only asked when the target carries one,
     * and its verdict keeps every listener filter and higher-priority skip in force.
     * 对派对狂观察者，返回 Wathe 事件会在目标上画出的氦气或气球描边；没有该描边、或被更高优先级结果压过时返回 null。
     * 仅在目标带有其中之一时才询问事件，事件裁决保留所有监听器过滤与更高优先级的 skip。
     */
    public static @Nullable Integer ownKeylessHighlight(InstinctViewerContext context, PlayerEntity target) {
        if (!context.isPartyAnimal()) {
            return null;
        }
        Integer balloonColor = SparkStrengthPartyBalloonBridge.highlightColor(target);
        HeliumBuzzPlayerComponent buzz = HeliumBuzzPlayerComponent.KEY.get(target);
        Integer heliumColor = buzz.isActive() ? PartyAnimalInstinctRules.heliumColor(buzz.getAmplifier()) : null;
        if (balloonColor == null && heliumColor == null) {
            return null;
        }
        GetInstinctHighlight.HighlightResult verdict = GetInstinctHighlight.EVENT.invoker().getHighlight(target);
        boolean own = PartyAnimalInstinctRules.isOwnKeylessVerdict(
                verdict != null,
                verdict != null && verdict.requiresKeybind(),
                verdict != null ? verdict.color() : -1,
                balloonColor,
                heliumColor
        );
        return own ? verdict.color() : null;
    }

    /**
     * Wathe's default killer-instinct colour for {@code target}, which the helium and balloon listeners would otherwise
     * repaint while the key is held.
     * 目标在 Wathe 默认杀手本能下的颜色；按住本能键时，氦气与气球监听器原本会改写这一颜色。
     */
    public static int nativeKillerInstinctColor(InstinctViewerContext context, PlayerEntity target) {
        if (GameFunctions.isPlayerSpectatingOrCreative(target)) {
            return -1;
        }
        return EffectiveTraitService.effectiveKillerInstinctColor(
                context.game().canUseKillerFeatures(target),
                false,
                false
        );
    }
}
