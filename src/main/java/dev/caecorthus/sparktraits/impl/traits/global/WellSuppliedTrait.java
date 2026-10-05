package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import net.minecraft.util.Identifier;

/**
 * Global trait that multiplies the owner's round-start money. It is a free bonus that does not take a trait slot.
 * 全局天赋：提高拥有者的开局金钱。属于白送的额外天赋，不占用天赋槽位。
 */
public final class WellSuppliedTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("well_supplied");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return GlobalTraitService.WELL_SUPPLIED_COLOR;
    }

    @Override
    public boolean occupiesTraitSlot() {
        return false;
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return GlobalTraitService.usesMoneySystem(context.player(), context.gameComponent(), context.role());
    }
}
