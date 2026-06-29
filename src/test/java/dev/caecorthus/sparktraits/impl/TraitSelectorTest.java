package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitSelectorTest {
    @Test
    void traitSlotRollUsesConfiguredChanceAsStrictUpperBound() {
        assertTrue(TraitSelector.shouldRollSlot(0.75f, fixedFloatRandom(0.749f)));
        assertFalse(TraitSelector.shouldRollSlot(0.75f, fixedFloatRandom(0.75f)));
    }

    @Test
    void defaultTraitSlotRollChanceIsSeventyFivePercent() {
        assertTrue(TraitSelector.shouldRollSlot(TraitSelector.DEFAULT_SLOT_CHANCE, fixedFloatRandom(0.74f)));
        assertFalse(TraitSelector.shouldRollSlot(TraitSelector.DEFAULT_SLOT_CHANCE, fixedFloatRandom(0.75f)));
    }

    @Test
    void weightedPickSupportsFractionalRollWeights() {
        Trait rare = weightedTrait("rare", 12.5D);
        Trait normal = weightedTrait("normal", 100.0D);

        assertEquals(rare.id(), TraitSelector.pickWeighted(
                List.of(rare, normal),
                fixedDoubleRandom(12.4D / 112.5D)
        ).id());
        assertEquals(normal.id(), TraitSelector.pickWeighted(
                List.of(rare, normal),
                fixedDoubleRandom(12.5D / 112.5D)
        ).id());
    }

    @Test
    void defaultRollWeightMatchesNormalTraitWeight() {
        Trait normal = new Trait() {
            @Override
            public Identifier id() {
                return SparkTraits.id("normal");
            }

            @Override
            public int color() {
                return 0xFFFFFF;
            }
        };

        assertEquals(100.0D, normal.rollWeight(), 0.0001D);
    }

    private static Trait weightedTrait(String path, double rollWeight) {
        return new Trait() {
            @Override
            public Identifier id() {
                return SparkTraits.id(path);
            }

            @Override
            public int color() {
                return 0xFFFFFF;
            }

            @Override
            public double rollWeight() {
                return rollWeight;
            }
        };
    }

    private static RandomGenerator fixedFloatRandom(float value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public float nextFloat() {
                return value;
            }
        };
    }

    private static RandomGenerator fixedDoubleRandom(double value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public double nextDouble() {
                return value;
            }
        };
    }
}
