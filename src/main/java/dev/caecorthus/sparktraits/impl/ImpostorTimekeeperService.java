package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;

/** Applies Impostor Timekeeper's inverted reduce-time purchase.
 *  处理内鬼计时员购买减少时间时的反向加时规则。 */
public final class ImpostorTimekeeperService {
    private ImpostorTimekeeperService() {
    }

    public static int timekeeperPurchaseDelta(PlayerEntity player, int originalDeltaTicks) {
        if (player == null) {
            return originalDeltaTicks;
        }
        return timekeeperPurchaseDelta(TraitPlayerComponent.KEY.get(player).getActiveTraitIds(), originalDeltaTicks);
    }

    public static int timekeeperPurchaseDelta(Collection<Identifier> buyerTraits, int originalDeltaTicks) {
        return EffectiveTraitService.hasImpostor(buyerTraits) && originalDeltaTicks < 0
                ? -originalDeltaTicks
                : originalDeltaTicks;
    }
}
