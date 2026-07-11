package dev.caecorthus.sparktraits.impl.traits.civilian.laststand;

import dev.caecorthus.sparktraits.impl.traits.civilian.police.PoliceTraits;
import dev.caecorthus.sparktraits.impl.traits.global.CautiousTrait;
import dev.caecorthus.sparktraits.impl.traits.global.FastHandsTrait;
import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraits;
import net.minecraft.util.Identifier;

import java.util.List;

/** Exact active-trait loadout granted to a Last Stand Final Moment Loose End. */
final class FinalMomentOutlawLoadout {
    private static final List<Identifier> TARGET_TRAITS = List.of(
            LastStandTrait.ID,
            PoliceTraits.NIKO,
            PoliceTraits.HEAVY_ARTILLERY,
            FastHandsTrait.ID,
            PoliceTraits.FAST_RELOAD,
            CautiousTrait.ID,
            KillerTraits.THRUST
    );

    private FinalMomentOutlawLoadout() {
    }

    static List<Identifier> targetTraits() {
        return TARGET_TRAITS;
    }
}
