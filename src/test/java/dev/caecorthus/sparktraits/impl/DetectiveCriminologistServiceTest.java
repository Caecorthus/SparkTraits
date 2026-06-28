package dev.caecorthus.sparktraits.impl;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectiveCriminologistServiceTest {
    private static final UUID KILLER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void constantsMatchDesign() {
        assertEquals(150, DetectiveCriminologistService.COST);
        assertEquals(20 * 60, DetectiveCriminologistService.INITIAL_COOLDOWN_TICKS);
        assertEquals(20 * 120, DetectiveCriminologistService.FAILURE_COOLDOWN_TICKS);
        assertEquals(20 * 30, DetectiveCriminologistService.REVEAL_PERIOD_TICKS);
        assertEquals(20 * 5, DetectiveCriminologistService.REVEAL_DURATION_TICKS);
    }

    @Test
    void openRequiresDetectiveStateAndNoLiveLockedTarget() {
        assertEquals(DetectiveCriminologistService.OpenResult.ALLOW,
                DetectiveCriminologistService.validateOpen(true, 0, 150, true, false));
        assertEquals(DetectiveCriminologistService.OpenResult.NOT_DETECTIVE,
                DetectiveCriminologistService.validateOpen(false, 0, 150, true, false));
        assertEquals(DetectiveCriminologistService.OpenResult.ON_COOLDOWN,
                DetectiveCriminologistService.validateOpen(true, 1, 150, true, false));
        assertEquals(DetectiveCriminologistService.OpenResult.NOT_ENOUGH_MONEY,
                DetectiveCriminologistService.validateOpen(true, 0, 149, true, false));
        assertEquals(DetectiveCriminologistService.OpenResult.BODY_NOT_TARGETABLE,
                DetectiveCriminologistService.validateOpen(true, 0, 150, false, false));
        assertEquals(DetectiveCriminologistService.OpenResult.TARGET_LOCKED,
                DetectiveCriminologistService.validateOpen(true, 0, 150, true, true));
    }

    @Test
    void selectionMatchesActualRecordedKiller() {
        assertEquals(DetectiveCriminologistService.SelectionResult.TRACK,
                DetectiveCriminologistService.evaluateSelection(KILLER, KILLER, true));
        assertEquals(DetectiveCriminologistService.SelectionResult.WRONG,
                DetectiveCriminologistService.evaluateSelection(KILLER, OTHER, true));
        assertEquals(DetectiveCriminologistService.SelectionResult.NO_KILLER_RECORD,
                DetectiveCriminologistService.evaluateSelection(null, OTHER, true));
        assertEquals(DetectiveCriminologistService.SelectionResult.KILLER_NOT_ALIVE,
                DetectiveCriminologistService.evaluateSelection(KILLER, KILLER, false));
    }

    @Test
    void revealPulsesForFiveSecondsEveryThirtySeconds() {
        assertTrue(DetectiveCriminologistService.isRevealPulseActive(0));
        assertTrue(DetectiveCriminologistService.isRevealPulseActive(99));
        assertFalse(DetectiveCriminologistService.isRevealPulseActive(100));
        assertFalse(DetectiveCriminologistService.isRevealPulseActive(599));
        assertTrue(DetectiveCriminologistService.isRevealPulseActive(600));
        assertTrue(DetectiveCriminologistService.isRevealPulseActive(699));
        assertFalse(DetectiveCriminologistService.isRevealPulseActive(700));
    }
}
