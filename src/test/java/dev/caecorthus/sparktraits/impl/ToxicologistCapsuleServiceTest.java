package dev.caecorthus.sparktraits.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToxicologistCapsuleServiceTest {
    @Test
    void onlyFoodOrDrinkCanEnterCapsules() {
        assertTrue(ToxicologistCapsuleService.isEligibleCapsuleContent(true, false));
        assertTrue(ToxicologistCapsuleService.isEligibleCapsuleContent(false, true));
        assertFalse(ToxicologistCapsuleService.isEligibleCapsuleContent(false, false));
    }

    @Test
    void displayNamesPreservePoisonState() {
        assertEquals("胶囊", ToxicologistCapsuleService.capsuleDisplayName(null, false, false));
        assertEquals("胶囊（苹果）", ToxicologistCapsuleService.capsuleDisplayName("苹果", false, false));
        assertEquals("胶囊（有毒的苹果）", ToxicologistCapsuleService.capsuleDisplayName("苹果", true, false));
        assertEquals("胶囊（有毒的苹果）", ToxicologistCapsuleService.capsuleDisplayName("苹果", false, true));
    }

    @Test
    void poisonColorsUseExistingBluePoisonPalette() {
        assertEquals(-1, ToxicologistCapsuleService.capsulePoisonColor(false, false));
        assertEquals(ConsciencePoisonerService.NORMAL_POISONER_INSTINCT_COLOR,
                ToxicologistCapsuleService.capsulePoisonColor(true, false));
        assertEquals(ConsciencePoisonerService.BLUE_POISON_COLOR,
                ToxicologistCapsuleService.capsulePoisonColor(false, true));
        assertEquals(ConsciencePoisonerService.MIXED_POISON_COLOR,
                ToxicologistCapsuleService.capsulePoisonColor(true, true));
    }
}
