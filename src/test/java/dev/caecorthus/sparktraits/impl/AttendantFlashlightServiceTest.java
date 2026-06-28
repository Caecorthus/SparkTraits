package dev.caecorthus.sparktraits.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendantFlashlightServiceTest {
    @Test
    void rangeIsThirtyBlocks() {
        assertEquals(30.0, AttendantFlashlightService.MAX_RANGE_BLOCKS);
    }

    @Test
    void blackoutBlindnessIsCancelledOnlyForAliveAttendantWithFlashlightOn() {
        assertTrue(AttendantFlashlightService.shouldCancelBlackoutBlindness(true, true, true));
        assertFalse(AttendantFlashlightService.shouldCancelBlackoutBlindness(false, true, true));
        assertFalse(AttendantFlashlightService.shouldCancelBlackoutBlindness(true, false, true));
        assertFalse(AttendantFlashlightService.shouldCancelBlackoutBlindness(true, true, false));
    }
}
