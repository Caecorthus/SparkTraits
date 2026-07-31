package dev.caecorthus.sparktraits.impl.effective;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConscienceWraithInstinctDelegationContractTest {
    @Test
    void activeWraithDelegatesConscienceInstinctToNativeRoleRules() {
        assertTrue(EffectiveTraitService.shouldDelegateConscienceInstinctToNative(true));
        assertFalse(EffectiveTraitService.shouldDelegateConscienceInstinctToNative(false));
    }
}
