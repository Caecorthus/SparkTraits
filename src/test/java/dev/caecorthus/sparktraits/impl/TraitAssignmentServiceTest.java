package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.rotation.GameEntry;
import dev.doctor4t.wathe.game.rotation.RoleCategory;
import dev.doctor4t.wathe.game.rotation.RotationStrength;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitAssignmentServiceTest {
    private record CompensationCandidate(String name, int bucket, double killerDebt) {
    }

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
    void requiredTraitFillsOpenSlotWithoutDroppingExistingTraits() {
        Identifier locked = Identifier.of("sparktraits_test", "locked");
        Identifier random = Identifier.of("sparktraits_test", "random");
        Identifier required = Identifier.of("sparktraits_test", "required");

        TraitAssignmentService.ForcedTraitPlan plan = TraitAssignmentService.forceRequiredTraitPlan(
                List.of(locked),
                List.of(random),
                required
        );

        assertEquals(List.of(locked), plan.lockedTraits());
        assertEquals(List.of(random, required), plan.randomTraits());
    }

    @Test
    void requiredTraitReplacesLastRandomTraitBeforeLockedTraits() {
        Identifier locked = Identifier.of("sparktraits_test", "locked");
        Identifier firstRandom = Identifier.of("sparktraits_test", "first_random");
        Identifier lastRandom = Identifier.of("sparktraits_test", "last_random");
        Identifier required = Identifier.of("sparktraits_test", "required");

        TraitAssignmentService.ForcedTraitPlan plan = TraitAssignmentService.forceRequiredTraitPlan(
                List.of(locked),
                List.of(firstRandom, lastRandom),
                required
        );

        assertEquals(List.of(locked), plan.lockedTraits());
        assertEquals(List.of(firstRandom, required), plan.randomTraits());
    }

    @Test
    void requiredTraitReplacesLastLockedTraitWhenAllSlotsAreLocked() {
        Identifier firstLocked = Identifier.of("sparktraits_test", "first_locked");
        Identifier middleLocked = Identifier.of("sparktraits_test", "middle_locked");
        Identifier lastLocked = Identifier.of("sparktraits_test", "last_locked");
        Identifier required = Identifier.of("sparktraits_test", "required");

        TraitAssignmentService.ForcedTraitPlan plan = TraitAssignmentService.forceRequiredTraitPlan(
                List.of(firstLocked, middleLocked, lastLocked),
                List.of(),
                required
        );

        assertEquals(List.of(firstLocked, middleLocked), plan.lockedTraits());
        assertEquals(List.of(required), plan.randomTraits());
    }

    @Test
    void randomDepressionCapRemovesOnlyRandomDepressions() {
        TraitAssignmentService.PlayerPlan lockedDepression = new TraitAssignmentService.PlayerPlan(
                null,
                List.of(GoodTraits.DEPRESSION),
                List.of()
        );
        TraitAssignmentService.PlayerPlan firstRandomDepression = new TraitAssignmentService.PlayerPlan(
                null,
                List.of(),
                List.of(GoodTraits.DEPRESSION)
        );
        TraitAssignmentService.PlayerPlan secondRandomDepression = new TraitAssignmentService.PlayerPlan(
                null,
                List.of(),
                List.of(GoodTraits.DEPRESSION)
        );

        TraitAssignmentService.enforceRandomDepressionCap(
                List.of(lockedDepression, firstRandomDepression, secondRandomDepression),
                32
        );

        assertEquals(List.of(GoodTraits.DEPRESSION), lockedDepression.traits());
        assertEquals(List.of(GoodTraits.DEPRESSION), firstRandomDepression.traits());
        assertEquals(List.of(), secondRandomDepression.traits());
    }

    @Test
    void randomDepressionCapAtMinimumRefreshPlayerCountRemovesEveryRandomDepression() {
        TraitAssignmentService.PlayerPlan randomDepression = new TraitAssignmentService.PlayerPlan(
                null,
                List.of(),
                List.of(GoodTraits.DEPRESSION)
        );

        TraitAssignmentService.enforceRandomDepressionCap(List.of(randomDepression), 24);

        assertEquals(List.of(), randomDepression.traits());
    }

    @Test
    void pigGodRequiresPigTrait() {
        assertTrue(TraitAssignmentService.shouldForcePigOntoPigGod(sparkWitchRole("pig_god")));
        assertFalse(TraitAssignmentService.shouldForcePigOntoPigGod(sparkWitchRole("grand_witch")));
        assertFalse(TraitAssignmentService.shouldForcePigOntoPigGod(role("otheraddon", "pig_god")));
        assertFalse(TraitAssignmentService.shouldForcePigOntoPigGod(WatheRoles.CIVILIAN));
        assertFalse(TraitAssignmentService.shouldForcePigOntoPigGod(null));
    }

    @Test
    void conscienceCompensationUsesWatheKillerDebtWithinPriorityBucket() {
        CompensationCandidate lowDebt = new CompensationCandidate("low", 0, -1.0);
        CompensationCandidate highDebt = new CompensationCandidate("high", 0, 1.0);
        Random random = constantDoubleRandom(0.5);

        CompensationCandidate selected = TraitAssignmentService.pickWeightedConscienceCompensationCandidate(
                List.of(lowDebt, highDebt),
                List.of(candidate -> candidate.bucket() == 0),
                CompensationCandidate::killerDebt,
                RotationStrength.STRONG,
                random
        );

        assertEquals(highDebt, selected);
    }

    @Test
    void conscienceCompensationUsesEqualRandomWhenRotationIsOff() {
        CompensationCandidate first = new CompensationCandidate("first", 0, 10.0);
        CompensationCandidate second = new CompensationCandidate("second", 0, -10.0);
        Random random = sequenceDoubleRandom(0.1, 0.9);

        CompensationCandidate selected = TraitAssignmentService.pickWeightedConscienceCompensationCandidate(
                List.of(first, second),
                List.of(candidate -> candidate.bucket() == 0),
                CompensationCandidate::killerDebt,
                RotationStrength.OFF,
                random
        );

        assertEquals(second, selected);
    }

    @Test
    void conscienceCompensationPriorityBucketsBeatKillerDebt() {
        CompensationCandidate plainCivilian = new CompensationCandidate("plain", 0, -1.0);
        CompensationCandidate specialCivilian = new CompensationCandidate("special", 1, 1.0);
        Random random = constantDoubleRandom(0.5);

        CompensationCandidate selected = TraitAssignmentService.pickWeightedConscienceCompensationCandidate(
                List.of(specialCivilian, plainCivilian),
                List.of(
                        candidate -> candidate.bucket() == 0,
                        candidate -> candidate.bucket() == 1
                ),
                CompensationCandidate::killerDebt,
                RotationStrength.STRONG,
                random
        );

        assertEquals(plainCivilian, selected);
    }

    @Test
    void conscienceCompensationRoleHistoryRewritePreservesWindowAndShares() {
        Deque<GameEntry> history = new ArrayDeque<>();
        history.addLast(new GameEntry(
                0.25,
                0.10,
                0.05,
                RoleCategory.CIVILIAN,
                WatheRoles.CIVILIAN.identifier().toString()
        ));
        history.addLast(new GameEntry(
                0.50,
                0.20,
                0.15,
                RoleCategory.CIVILIAN,
                Noellesroles.CONDUCTOR.identifier().toString()
        ));

        assertTrue(TraitAssignmentService.replaceLatestConscienceCompensationRoleHistoryEntry(
                history,
                Noellesroles.SERIAL_KILLER
        ));

        assertEquals(2, history.size());
        assertEquals(RoleCategory.CIVILIAN, history.getFirst().category());
        GameEntry rewritten = history.getLast();
        assertEquals(0.50, rewritten.killerShare());
        assertEquals(0.20, rewritten.vigilanteShare());
        assertEquals(0.15, rewritten.neutralShare());
        assertEquals(RoleCategory.KILLER, rewritten.category());
        assertEquals(Noellesroles.SERIAL_KILLER.identifier().toString(), rewritten.roleId());
    }

    @Test
    void conscienceCompensationRoleHistoryRewriteSkipsMissingHistory() {
        assertFalse(TraitAssignmentService.replaceLatestConscienceCompensationRoleHistoryEntry(
                new ArrayDeque<>(),
                Noellesroles.SERIAL_KILLER
        ));
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

    @Test
    void conscienceCompensationClearsOriginalCivilianStartingItems() {
        assertEquals(List.of(Identifier.of("noellesroles", "iron_man_vial")),
                TraitAssignmentService.initialRoleItemIdsToClearForConscienceCompensation(Noellesroles.PROFESSOR));
        assertEquals(List.of(Identifier.of("noellesroles", "antidote")),
                TraitAssignmentService.initialRoleItemIdsToClearForConscienceCompensation(Noellesroles.TOXICOLOGIST));
        assertEquals(List.of(Identifier.of("noellesroles", "repair_tool")),
                TraitAssignmentService.initialRoleItemIdsToClearForConscienceCompensation(Noellesroles.ENGINEER));
        assertEquals(List.of(Identifier.ofVanilla("written_book")),
                TraitAssignmentService.initialRoleItemIdsToClearForConscienceCompensation(Noellesroles.ATTENDANT));
        assertEquals(List.of(Identifier.of("wathe", "walkie_talkie")),
                TraitAssignmentService.initialRoleItemIdsToClearForConscienceCompensation(Noellesroles.UNDERCOVER));
        assertEquals(List.of(Identifier.of("noellesroles", "master_key")),
                TraitAssignmentService.initialRoleItemIdsToClearForConscienceCompensation(Noellesroles.CONDUCTOR));
        assertEquals(List.of(Identifier.of("wathe", "note")),
                TraitAssignmentService.initialRoleItemIdsToClearForConscienceCompensation(Noellesroles.AWESOME_BINGLUS));
        assertEquals(List.of(),
                TraitAssignmentService.initialRoleItemIdsToClearForConscienceCompensation(WatheRoles.CIVILIAN));
    }

    private static Random constantDoubleRandom(double value) {
        return sequenceDoubleRandom(value);
    }

    private static Random sequenceDoubleRandom(double... values) {
        return new Random() {
            private int index;

            @Override
            public double nextDouble() {
                if (index >= values.length) {
                    return values[values.length - 1];
                }
                return values[index++];
            }
        };
    }

    private static Role sparkWitchRole(String path) {
        return role("sparkwitch", path);
    }

    private static Role role(String namespace, String path) {
        return new Role(
                Identifier.of(namespace, path),
                0xFFFFFF,
                true,
                false,
                Role.MoodType.REAL,
                200,
                false
        );
    }
}
