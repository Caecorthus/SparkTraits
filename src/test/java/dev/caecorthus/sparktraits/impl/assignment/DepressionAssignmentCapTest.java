package dev.caecorthus.sparktraits.impl.assignment;

import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepressionAssignmentCapTest {
    private static final Identifier OTHER_TRAIT = Identifier.of("test", "other_trait");

    @Test
    void randomCapRemovesOnlyExcessRandomDepressionAndKeepsLockedAssignments() {
        TraitAssignmentService.PlayerPlan locked = plan(
                List.of(CivilianTraits.DEPRESSION),
                List.of()
        );
        TraitAssignmentService.PlayerPlan firstRandom = plan(
                List.of(),
                List.of(CivilianTraits.DEPRESSION, OTHER_TRAIT)
        );
        TraitAssignmentService.PlayerPlan excessRandom = plan(
                List.of(),
                List.of(CivilianTraits.DEPRESSION)
        );

        TraitAssignmentService.enforceRandomDepressionCap(
                List.of(locked, firstRandom, excessRandom),
                24
        );

        assertEquals(List.of(CivilianTraits.DEPRESSION), locked.traits());
        assertEquals(List.of(CivilianTraits.DEPRESSION, OTHER_TRAIT), firstRandom.traits());
        assertEquals(List.of(), excessRandom.traits());
    }

    @Test
    void belowNaturalThresholdStillKeepsLockedDepression() {
        TraitAssignmentService.PlayerPlan locked = plan(
                List.of(CivilianTraits.DEPRESSION),
                List.of()
        );
        TraitAssignmentService.PlayerPlan random = plan(
                List.of(),
                List.of(CivilianTraits.DEPRESSION)
        );

        TraitAssignmentService.enforceRandomDepressionCap(List.of(locked, random), 23);

        assertEquals(List.of(CivilianTraits.DEPRESSION), locked.traits());
        assertEquals(List.of(), random.traits());
    }

    private static TraitAssignmentService.PlayerPlan plan(
            List<Identifier> locked,
            List<Identifier> random
    ) {
        return new TraitAssignmentService.PlayerPlan(null, locked, random);
    }
}
