package dev.caecorthus.sparktraits.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraitSlotRollChanceTest {
    @Test
    void defaultTraitSlotRollChanceIsSeventyFivePercent() {
        assertEquals(0.75f, TraitSlotRollChance.DEFAULT, 0.0001f);
        assertEquals(75, TraitSlotRollChance.toPercent(TraitSlotRollChance.DEFAULT));
    }

    @Test
    void traitSlotRollChanceConvertsBetweenPercentAndUnitChance() {
        assertEquals(0.0f, TraitSlotRollChance.fromPercent(0), 0.0001f);
        assertEquals(0.75f, TraitSlotRollChance.fromPercent(75), 0.0001f);
        assertEquals(1.0f, TraitSlotRollChance.fromPercent(100), 0.0001f);
    }

    @Test
    void traitSlotRollChanceClampsInvalidValues() {
        assertEquals(1.0f, TraitSlotRollChance.normalize(1.5f), 0.0001f);
        assertEquals(0.0f, TraitSlotRollChance.normalize(-0.25f), 0.0001f);
        assertEquals(0.75f, TraitSlotRollChance.normalize(Float.NaN), 0.0001f);
    }
}
