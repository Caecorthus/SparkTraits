package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.WatheRoles;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsciencePoisonerServiceTest {
    @Test
    void onlyConsciencePoisonersCreateBluePoison() {
        assertTrue(ConsciencePoisonerService.isConsciencePoisoner(Noellesroles.POISONER, Set.of(ConscienceTrait.ID)));
        assertFalse(ConsciencePoisonerService.isConsciencePoisoner(Noellesroles.POISONER, Set.of()));
        assertFalse(ConsciencePoisonerService.isConsciencePoisoner(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
    }

    @Test
    void bluePoisonUsesEffectiveAlignmentForImmunity() {
        assertFalse(ConsciencePoisonerService.shouldApplyBluePoison(WatheRoles.CIVILIAN, Set.of()));
        assertFalse(ConsciencePoisonerService.shouldApplyBluePoison(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertTrue(ConsciencePoisonerService.shouldApplyBluePoison(WatheRoles.KILLER, Set.of()));
        assertTrue(ConsciencePoisonerService.shouldApplyBluePoison(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertTrue(ConsciencePoisonerService.shouldApplyBluePoison(Noellesroles.JESTER, Set.of()));
    }

    @Test
    void blueTrapTriggersAreConsumedForGoodButPoisonOthers() {
        assertEquals(ConsciencePoisonerService.BlueTrapResult.NO_BLUE_POISON,
                ConsciencePoisonerService.blueTrapResult(false, WatheRoles.KILLER, Set.of()));
        assertEquals(ConsciencePoisonerService.BlueTrapResult.CONSUME_ONLY,
                ConsciencePoisonerService.blueTrapResult(true, WatheRoles.CIVILIAN, Set.of()));
        assertEquals(ConsciencePoisonerService.BlueTrapResult.CONSUME_ONLY,
                ConsciencePoisonerService.blueTrapResult(true, WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertEquals(ConsciencePoisonerService.BlueTrapResult.CONSUME_AND_POISON,
                ConsciencePoisonerService.blueTrapResult(true, WatheRoles.KILLER, Set.of()));
        assertEquals(ConsciencePoisonerService.BlueTrapResult.CONSUME_AND_POISON,
                ConsciencePoisonerService.blueTrapResult(true, WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertEquals(ConsciencePoisonerService.BlueTrapResult.CONSUME_AND_POISON,
                ConsciencePoisonerService.blueTrapResult(true, Noellesroles.JESTER, Set.of()));
    }

    @Test
    void bluePoisonHighlightColorsAreStable() {
        int poisonerColor = Noellesroles.POISONER.color();

        assertEquals(poisonerColor, ConsciencePoisonerService.poisonHighlightColor(true, false, poisonerColor));
        assertEquals(ConsciencePoisonerService.BLUE_POISON_COLOR,
                ConsciencePoisonerService.poisonHighlightColor(false, true, poisonerColor));
        assertEquals(0x0F8789, ConsciencePoisonerService.poisonHighlightColor(true, true, poisonerColor));
        assertEquals(0x0F8789, ConsciencePoisonerService.poisonInstinctColor(true, true));
    }

    @Test
    void poisonedTargetsIgnoreConscienceRange() {
        assertFalse(ConsciencePoisonerService.poisonStateIgnoresConscienceRange(false, false));
        assertTrue(ConsciencePoisonerService.poisonStateIgnoresConscienceRange(true, false));
        assertTrue(ConsciencePoisonerService.poisonStateIgnoresConscienceRange(false, true));
        assertTrue(ConsciencePoisonerService.poisonStateIgnoresConscienceRange(true, true));
    }

    @Test
    void hiddenBluePoisonParticlesAreRoleGated() {
        assertFalse(ConsciencePoisonerService.shouldShowHiddenBluePoisonParticles(false, false, false));
        assertTrue(ConsciencePoisonerService.shouldShowHiddenBluePoisonParticles(true, false, false));
        assertTrue(ConsciencePoisonerService.shouldShowHiddenBluePoisonParticles(false, true, false));
        assertTrue(ConsciencePoisonerService.shouldShowHiddenBluePoisonParticles(false, false, true));
    }

    @Test
    void blueGasAndBlueTrapTimersUseDedicatedBluePoisonRules() {
        assertEquals(400, ConsciencePoisonerService.BLUE_GAS_POISON_TICKS);
        assertEquals(350, ConsciencePoisonerService.bluePoisonTicksAfterTrap(-1, 350, 100, 600));
        assertEquals(400, ConsciencePoisonerService.bluePoisonTicksAfterTrap(500, 350, 100, 600));
        assertEquals(0, ConsciencePoisonerService.bluePoisonTicksAfterTrap(50, 350, 100, 600));
        assertEquals(600, ConsciencePoisonerService.bluePoisonTicksAfterTrap(800, 350, 100, 600));
    }

    @Test
    void fineDrinkIsUnlimitedAndCostsSeventyFive() {
        assertEquals(75, ConsciencePoisonerService.FINE_DRINK_PRICE);
        assertEquals(-1, ConsciencePoisonerService.FINE_DRINK_MAX_STOCK);
        assertFalse(ConsciencePoisonerService.FINE_DRINK_HAS_STOCK_LIMIT);
        assertEquals("noellesroles:fine_drink", ConsciencePoisonerService.FINE_DRINK_ID.toString());
    }
}
