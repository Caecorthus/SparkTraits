package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import net.minecraft.util.Identifier;

/** Killer trait that flips the player's effective alignment to passengers.
 *  杀手天赋：将玩家的有效阵营翻转为乘客阵营。 */
public final class ConscienceTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("conscience");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return EffectiveTraitService.CONSCIENCE_COLOR;
    }

    @Override
    public boolean uniquePerGame() {
        return true;
    }

    @Override
    public TraitAudience audience() {
        return TraitAudience.KILLER_ONLY;
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return EffectiveTraitService.canSelectConscience(context.role(), context.gameComponent(), context.selectedTraitIds());
    }
}
