package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitAssignmentServiceTest {
    @Test
    void forcedConscienceCandidateUsesRandomIndex() {
        Random random = new Random() {
            @Override
            public int nextInt(int bound) {
                return 1;
            }
        };

        assertEquals(1, TraitAssignmentService.pickForcedConscienceCandidateIndex(3, random));
    }

    @Test
    void conscienceCompensationDoesNotUseDisabledPlainKiller() {
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationKiller(
                WatheRoles.KILLER,
                false,
                false,
                true
        ));
    }

    @Test
    void conscienceCompensationCanUseEnabledPlainKillerFallback() {
        assertTrue(TraitAssignmentService.canUseAsConscienceCompensationKiller(
                WatheRoles.KILLER,
                true,
                false,
                true
        ));
    }

    @Test
    void conscienceCompensationCanUseEnabledUnassignedSpecialKiller() {
        assertTrue(TraitAssignmentService.canUseAsConscienceCompensationKiller(
                Noellesroles.SERIAL_KILLER,
                true,
                false,
                true
        ));
    }

    @Test
    void conscienceCompensationSkipsAlreadyAssignedSpecialKiller() {
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationKiller(
                Noellesroles.SERIAL_KILLER,
                true,
                true,
                true
        ));
    }

    @Test
    void conscienceCompensationShufflesSpecialKillerCandidates() {
        List<Role> candidates = new ArrayList<>(List.of(Noellesroles.SERIAL_KILLER, Noellesroles.BOMBER));
        Random random = new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };

        assertEquals(
                Noellesroles.BOMBER,
                TraitAssignmentService.pickShuffledConscienceCompensationKillerRole(candidates, WatheRoles.KILLER, random)
        );
    }

    @Test
    void conscienceCompensationTargetMustBeUnlockedCivilian() {
        assertTrue(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                WatheRoles.CIVILIAN,
                Set.of(),
                false
        ));
        assertTrue(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                Noellesroles.CONDUCTOR,
                Set.of(),
                false
        ));
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                WatheRoles.CIVILIAN,
                Set.of(),
                true
        ));
    }

    @Test
    void conscienceCompensationTargetRejectsGunCivilianRoles() {
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                WatheRoles.VIGILANTE,
                Set.of(),
                false
        ));
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                WatheRoles.VETERAN,
                Set.of(),
                false
        ));
    }

    @Test
    void conscienceCompensationTargetRejectsNeutralAndKillerRoles() {
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                Noellesroles.JESTER,
                Set.of(),
                false
        ));
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                Noellesroles.VULTURE,
                Set.of(),
                false
        ));
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                WatheRoles.KILLER,
                Set.of(),
                false
        ));
    }

    @Test
    void conscienceCompensationTargetRejectsAlignmentTraits() {
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID),
                false
        ));
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                WatheRoles.CIVILIAN,
                Set.of(ConscienceTrait.ID),
                false
        ));
    }
}
