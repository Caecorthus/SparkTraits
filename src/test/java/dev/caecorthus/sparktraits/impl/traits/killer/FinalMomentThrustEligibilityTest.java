package dev.caecorthus.sparktraits.impl.traits.killer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalMomentThrustEligibilityTest {
    @Test
    void activeThrustWorksForEligibleKillersOrFinalMomentLooseEndsOnly() {
        assertTrue(KillerTraitService.canUseThrust(true, true, false));
        assertTrue(KillerTraitService.canUseThrust(true, false, true));
        assertFalse(KillerTraitService.canUseThrust(true, false, false));
        assertFalse(KillerTraitService.canUseThrust(false, true, true));
    }
}
