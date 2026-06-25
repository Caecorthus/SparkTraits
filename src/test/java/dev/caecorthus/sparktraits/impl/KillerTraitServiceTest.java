package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.WatheRoles;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillerTraitServiceTest {
    @Test
    void killerTraitsRejectConscienceAndImpostorOwners() {
        assertTrue(KillerTraitService.canSelectKillerTrait(WatheRoles.KILLER, Set.of()));
        assertFalse(KillerTraitService.canSelectKillerTrait(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertFalse(KillerTraitService.canSelectKillerTrait(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertFalse(KillerTraitService.canSelectKillerTrait(WatheRoles.CIVILIAN, Set.of()));
    }

    @Test
    void bloodthirstyCooldownUsesFloorPlayerCap() {
        assertEquals(1000, KillerTraitService.bloodthirstyCooldown(1000, 3, 2));
        assertEquals(940, KillerTraitService.bloodthirstyCooldown(1000, 2, 17));
        assertEquals(850, KillerTraitService.bloodthirstyCooldown(1000, 10, 17));
    }

    @Test
    void killerMoneyEffectsUseFloorsAndCaps() {
        assertEquals(45, KillerTraitService.showmanReward(9));
        assertEquals(50, KillerTraitService.showmanReward(11));
        assertEquals(24, KillerTraitService.plunderedAmount(99));
        assertEquals(90, KillerTraitService.charismaPrice(100));
        assertEquals(22, KillerTraitService.charismaPrice(25));
    }

    @Test
    void paranoidAndOppressiveApplyExactMultipliers() {
        assertEquals(1000, KillerTraitService.paranoidPsychoTicks(600));
        assertEquals(0.2f, KillerTraitService.oppressiveAdjustedMood(0.5f, 0.25f, true), 0.0001f);
        assertEquals(0.25f, KillerTraitService.oppressiveAdjustedMood(0.5f, 0.25f, false), 0.0001f);
    }

    @Test
    void corneredTeamIncludesImpostorsButNotConscienceKillers() {
        assertTrue(KillerTraitService.isCorneredTeamMember(WatheRoles.KILLER, Set.of()));
        assertFalse(KillerTraitService.isCorneredTeamMember(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertTrue(KillerTraitService.isCorneredTeamMember(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertFalse(KillerTraitService.isCorneredTeamMember(Noellesroles.JESTER, Set.of()));
    }

    @Test
    void secondStrikeRetriesOnlyWhenAnOrdinaryShieldWasConsumed() {
        assertTrue(KillerTraitService.shouldRetrySecondStrike(true, false, false, false, true, true));
        assertFalse(KillerTraitService.shouldRetrySecondStrike(true, false, false, false, false, true));
        assertFalse(KillerTraitService.shouldRetrySecondStrike(true, false, false, true, true, true));
        assertFalse(KillerTraitService.shouldRetrySecondStrike(true, false, false, false, true, false));
        assertFalse(KillerTraitService.shouldRetrySecondStrike(false, false, false, false, true, true));
    }

    @Test
    void paranoidAndThrustCanBeAssignedOnlyWhenFinalShopSupportsThem() {
        assertTrue(KillerTraitService.canSelectParanoid(WatheRoles.KILLER, Set.of(), true));
        assertFalse(KillerTraitService.canSelectParanoid(WatheRoles.KILLER, Set.of(), false));
        assertTrue(KillerTraitService.canSelectThrust(WatheRoles.KILLER, Set.of(), true));
        assertFalse(KillerTraitService.canSelectThrust(WatheRoles.KILLER, Set.of(), false));
    }

}
