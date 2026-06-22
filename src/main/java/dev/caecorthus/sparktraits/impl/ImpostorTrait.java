package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import net.minecraft.util.Identifier;

import java.util.Set;

/** Civilian trait that flips the player's effective alignment to killers.
 *  好人天赋：将玩家的有效阵营翻转为杀手阵营。 */
public final class ImpostorTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("impostor");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return EffectiveTraitService.IMPOSTOR_COLOR;
    }

    @Override
    public boolean uniquePerGame() {
        return true;
    }

    @Override
    public TraitAudience audience() {
        return TraitAudience.INNOCENT_ONLY;
    }

    @Override
    public Set<Identifier> incompatibleTraits() {
        return Set.of(LastStandTrait.ID);
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return EffectiveTraitService.canSelectImpostor(context.role(), context.gameComponent(), context.selectedTraitIds());
    }
}
