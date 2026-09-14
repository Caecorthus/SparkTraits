package dev.caecorthus.sparktraits.impl.traits.killer.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatTimingRulesTest {
    @Test
    void releaseStartsAtThreeAndEndsBeforeTimeout() {
        assertFalse(CombatTimingRules.canRelease(-1));
        assertFalse(CombatTimingRules.canRelease(0));
        assertFalse(CombatTimingRules.canRelease(2));
        assertTrue(CombatTimingRules.canRelease(3));
        assertTrue(CombatTimingRules.canRelease(199));
        assertFalse(CombatTimingRules.canRelease(200));
    }

    @Test
    void stanceIncludesWindupButNotTimeoutOrSecondParry() {
        assertTrue(CombatTimingRules.canParry(true, true, true, false, 0));
        assertTrue(CombatTimingRules.canParry(true, true, true, false, 199));
        assertFalse(CombatTimingRules.canParry(true, true, true, false, 200));
        assertFalse(CombatTimingRules.canParry(true, true, true, true, 3));
        assertFalse(CombatTimingRules.canParry(false, true, true, false, 3));
        assertFalse(CombatTimingRules.canParry(true, false, true, false, 3));
        assertFalse(CombatTimingRules.canParry(true, true, false, false, 3));
    }

    @Test
    void halvingRoundsUpAndNeverReducesForcedPenalties() {
        assertEquals(0, CombatTimingRules.halfRemaining(0, 0));
        assertEquals(1, CombatTimingRules.halfRemaining(1, 0));
        assertEquals(2, CombatTimingRules.halfRemaining(3, 0));
        assertEquals(150, CombatTimingRules.halfRemaining(300, 0));
        assertEquals(300, CombatTimingRules.halfRemaining(300, 300));
        assertEquals(300, CombatTimingRules.halfRemaining(500, 300));
        assertEquals(500, CombatTimingRules.halfRemaining(1000, 300));
        assertEquals(1_073_741_824, CombatTimingRules.halfRemaining(Integer.MAX_VALUE, 0));
    }

    @Test
    void postModifierWritesCannotEraseFloorOrLongerNormalCooldown() {
        assertEquals(1000, CombatTimingRules.resolveWriteDuration(20, 300, 1000, false));
        assertEquals(300, CombatTimingRules.resolveWriteDuration(0, 300, 200, false));
        assertEquals(1500, CombatTimingRules.resolveWriteDuration(1500, 300, 1000, false));
        assertEquals(500, CombatTimingRules.resolveWriteDuration(500, 300, 1000, true));
        assertEquals(300, CombatTimingRules.resolveWriteDuration(10, 300, 1000, true));
        // Without a forced penalty normal vanilla rewrites keep their existing behavior.
        assertEquals(20, CombatTimingRules.resolveWriteDuration(20, 0, 1000, false));
    }

    @Test
    void deadlinesUseElapsedServerTicksAndExpireAtBoundary() {
        assertEquals(300, CombatTimingRules.remaining(1300, 1000));
        assertEquals(1, CombatTimingRules.remaining(1300, 1299));
        assertEquals(0, CombatTimingRules.remaining(1300, 1300));
        assertEquals(0, CombatTimingRules.remaining(1300, 9999));
        assertEquals(Integer.MAX_VALUE, CombatTimingRules.remaining(1L << 40, 0));
        // Rejoin reports the remaining original deadline, not a fresh fifteen seconds.
        assertEquals(80, CombatTimingRules.remaining(1300, 1220));
    }
}
