package dev.caecorthus.sparktraits.impl.effective;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectiveTraitServiceInstinctSuppressionTest {
    @Test
    void finalMomentBypassesEverySparkTraitsInstinctSuppression() {
        assertFalse(shouldHide(true, true, false, false, false));
        assertFalse(shouldHide(true, false, true, false, false));
        assertFalse(shouldHide(true, false, false, true, false));
        assertFalse(shouldHide(true, false, false, false, true));
        assertFalse(shouldHide(true, true, true, true, true));
    }

    @Test
    void protectionsHideOutsideFinalMoment() {
        assertTrue(shouldHide(false, true, false, false, false));
        assertTrue(shouldHide(false, false, true, false, false));
        assertTrue(shouldHide(false, false, false, true, false));
        assertTrue(shouldHide(false, false, false, false, true));
    }

    @Test
    void targetRemainsVisibleWithoutAnyProtection() {
        assertFalse(shouldHide(false, false, false, false, false));
    }

    private static boolean shouldHide(
            boolean finalMomentActive,
            boolean lastStandPending,
            boolean killerInstinctHidden,
            boolean spiritProjecting,
            boolean goingDarkSuppressed
    ) {
        return EffectiveTraitService.shouldHideFromInstinct(
                finalMomentActive,
                lastStandPending,
                killerInstinctHidden,
                spiritProjecting,
                goingDarkSuppressed
        );
    }
}
