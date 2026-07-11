package dev.caecorthus.sparktraits.impl.selection;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitSelectorTest {
    @Test
    void randomSelectionWeightChangesOnlyNonUniversalCandidates() {
        assertEquals(100.0D, TraitSelector.randomSelectionWeight(trait("u", 100.0D, TraitAudience.UNIVERSAL)));
        assertEquals(150.0D, TraitSelector.randomSelectionWeight(trait("k", 100.0D, TraitAudience.KILLER_ONLY)));
        assertEquals(18.75D, TraitSelector.randomSelectionWeight(trait("i", 12.5D, TraitAudience.INNOCENT_ONLY)));
        assertEquals(150.0D, TraitSelector.randomSelectionWeight(trait("n", 100.0D, TraitAudience.NEUTRAL_ONLY)));
        assertEquals(12.5D, trait("base", 12.5D, TraitAudience.KILLER_ONLY).rollWeight());
    }

    @Test
    void weightedPickUsesBoostWithoutChangingSlotChance() {
        Trait faction = trait("faction", 100.0D, TraitAudience.KILLER_ONLY);
        Trait universal = trait("universal", 100.0D, TraitAudience.UNIVERSAL);
        Random fixed = new Random(0L) {
            @Override
            public double nextDouble() {
                return 0.55D;
            }

            @Override
            public float nextFloat() {
                return 0.749F;
            }
        };

        assertSame(faction, TraitSelector.pickWeighted(List.of(faction, universal), fixed));
        assertTrue(TraitSelector.shouldRollSlot(0.75F, fixed));

        Random boundary = new Random(0L) {
            @Override
            public float nextFloat() {
                return 0.75F;
            }
        };
        assertFalse(TraitSelector.shouldRollSlot(0.75F, boundary));
    }

    private static Trait trait(String path, double weight, TraitAudience audience) {
        return new Trait() {
            @Override
            public Identifier id() {
                return Identifier.of("test", path);
            }

            @Override
            public int color() {
                return 0;
            }

            @Override
            public double rollWeight() {
                return weight;
            }

            @Override
            public TraitAudience audience() {
                return audience;
            }
        };
    }
}
