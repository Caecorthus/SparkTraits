package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import net.minecraft.util.Identifier;

/**
 * Corrupt Cop-only trait that boosts horizontal movement speed.
 * 黑警专属天赋：提升横向移动速度。
 */
public final class ArrogantAsfTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("arrogant_asf");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return CorruptCopTraitService.ARROGANT_ASF_COLOR;
    }

    @Override
    public int weight() {
        return 0;
    }

    @Override
    public TraitAudience audience() {
        return TraitAudience.NEUTRAL_ONLY;
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return Trait.super.canApply(context) && CorruptCopTraitService.canSelectArrogantAsf(context.role());
    }
}
