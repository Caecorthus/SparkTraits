package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTrait;
import dev.doctor4t.wathe.api.WatheRoles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepressionRandomSelectionTest {
    private final DepressionTrait depression = new DepressionTrait();

    @Test
    void naturalRandomCapStartsAtTwentyFourAndGrowsEveryEightPlayers() {
        assertEquals(0, DepressionTraitService.randomDepressionCap(23));
        assertEquals(1, DepressionTraitService.randomDepressionCap(24));
        assertEquals(1, DepressionTraitService.randomDepressionCap(31));
        assertEquals(2, DepressionTraitService.randomDepressionCap(32));
        assertEquals(2, DepressionTraitService.randomDepressionCap(39));
        assertEquals(3, DepressionTraitService.randomDepressionCap(40));
    }

    @Test
    void realTraitEnforcesNaturalThresholdButAssignmentBypassesIt() {
        assertFalse(depression.canApply(context(23, true)));
        assertTrue(depression.canApply(context(24, true)));
        assertTrue(depression.canApply(context(23, false)));
    }

    private static TraitSelectionContext context(int startingPlayerCount, boolean enforceStartingPlayerCount) {
        return new TraitSelectionContext(
                null,
                null,
                null,
                WatheRoles.CIVILIAN,
                Set.of(),
                startingPlayerCount,
                enforceStartingPlayerCount
        );
    }
}
