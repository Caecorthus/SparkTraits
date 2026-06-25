package dev.caecorthus.sparktraits.impl;

import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

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
}
