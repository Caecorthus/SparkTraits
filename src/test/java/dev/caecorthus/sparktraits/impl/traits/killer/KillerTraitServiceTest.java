package dev.caecorthus.sparktraits.impl.traits.killer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillerTraitServiceTest {
    @Test
    void bloodthirstyUsesFivePercentAndKeepsPlayerCap() {
        assertEquals(900, KillerTraitService.bloodthirstyCooldown(1000, 2, 17));
        assertEquals(750, KillerTraitService.bloodthirstyCooldown(1000, 10, 17));
        assertEquals(1000, KillerTraitService.bloodthirstyCooldown(1000, 3, 2));
    }

    @Test
    void showmanPaysEightForOtherLivingWitnessesUpToTen() {
        assertEquals(72, KillerTraitService.showmanReward(9));
        assertEquals(80, KillerTraitService.showmanReward(11));
        assertEquals(0, KillerTraitService.showmanReward(-1));
        assertTrue(KillerTraitService.shouldCountShowmanWitness(false, false, true, true));
        assertFalse(KillerTraitService.shouldCountShowmanWitness(true, false, true, true));
        assertFalse(KillerTraitService.shouldCountShowmanWitness(false, true, true, true));
        assertFalse(KillerTraitService.shouldCountShowmanWitness(false, false, false, true));
        assertFalse(KillerTraitService.shouldCountShowmanWitness(false, false, true, false));
    }

    @Test
    void plundererTakesFlooredThirdWithoutMinimum() {
        assertEquals(33, KillerTraitService.plunderedAmount(99));
        assertEquals(33, KillerTraitService.plunderedAmount(100));
        assertEquals(0, KillerTraitService.plunderedAmount(2));
        assertEquals(0, KillerTraitService.plunderedAmount(-10));
    }

    @Test
    void paranoidAddsDurationAndOneCurrentArmourLayer() {
        assertEquals(1000, KillerTraitService.paranoidPsychoTicks(600));
        assertEquals(2, KillerTraitService.paranoidPsychoArmour(1));
        assertEquals(3, KillerTraitService.paranoidPsychoArmour(2));
    }

    @Test
    void corneredUsesOneCombinedReward() {
        assertEquals(75, KillerTraitService.corneredReward(false));
        assertEquals(200, KillerTraitService.corneredReward(true));
    }
}
