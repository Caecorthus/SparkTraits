package dev.caecorthus.sparktraits.impl.effective;

import dev.doctor4t.wathe.game.GameFunctions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectiveTraitWinPrecedenceTest {
    @Test
    void preservesAlreadyResolvedTimeAndNeutralWinsOnly() {
        assertTrue(EffectiveTraitService.preservesResolvedWinStatus(GameFunctions.WinStatus.TIME));
        assertTrue(EffectiveTraitService.preservesResolvedWinStatus(GameFunctions.WinStatus.NEUTRAL));

        assertFalse(EffectiveTraitService.preservesResolvedWinStatus(GameFunctions.WinStatus.NONE));
        assertFalse(EffectiveTraitService.preservesResolvedWinStatus(GameFunctions.WinStatus.PASSENGERS));
        assertFalse(EffectiveTraitService.preservesResolvedWinStatus(GameFunctions.WinStatus.KILLERS));
    }
}
