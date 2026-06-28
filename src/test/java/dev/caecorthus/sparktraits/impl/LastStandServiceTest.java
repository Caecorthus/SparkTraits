package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastStandServiceTest {
    @Test
    void moodChanceKeepsFloorAndCeiling() {
        assertEquals(5.0, LastStandService.calculateTriggerChance(-10.0));
        assertEquals(5.0, LastStandService.calculateTriggerChance(0.0));
        assertEquals(100.0, LastStandService.calculateTriggerChance(70.0));
        assertEquals(100.0, LastStandService.calculateTriggerChance(100.0));
    }

    @Test
    void moodChanceInterpolatesLinearly() {
        assertEquals(52.5, LastStandService.calculateTriggerChance(35.0));
    }

    @Test
    void nonRealMoodAlwaysTriggers() {
        assertEquals(100.0, LastStandService.calculateTriggerChance(0.0, Role.MoodType.NONE));
    }

    @Test
    void moodAverageUsesRecordedSampleCount() {
        assertEquals(53.33, LastStandService.roundedMoodAverage(List.of(20.0, 40.0, 100.0), 80.0));
    }

    @Test
    void moodAverageFallsBackWhenNoSamplesWereRecorded() {
        assertEquals(66.67, LastStandService.roundedMoodAverage(List.of(), 66.666));
    }

    @Test
    void populationThresholdUsesCivilianCounts() {
        assertTrue(LastStandService.meetsCivilianPopulationThreshold(3, 1));
        assertFalse(LastStandService.meetsCivilianPopulationThreshold(3, 2));
        assertFalse(LastStandService.meetsCivilianPopulationThreshold(2, 1));
    }

    @Test
    void populationThresholdCanAnticipateTheVictimDeath() {
        assertTrue(LastStandService.meetsCivilianPopulationThresholdAfterDeath(3, 2));
        assertFalse(LastStandService.meetsCivilianPopulationThresholdAfterDeath(3, 3));
    }

    @Test
    void revolverDeathDropIsPreventedOnlyForApprovedLastStandDeath() {
        assertTrue(LastStandService.shouldPreventRevolverDeathDrop(true, true));
        assertFalse(LastStandService.shouldPreventRevolverDeathDrop(true, false));
        assertFalse(LastStandService.shouldPreventRevolverDeathDrop(false, true));
    }

    @Test
    void pendingStateCanComeFromServerRuntimeOrSyncedClientFlag() {
        assertTrue(LastStandService.isPendingClientOrServer(true, false));
        assertTrue(LastStandService.isPendingClientOrServer(false, true));
        assertFalse(LastStandService.isPendingClientOrServer(false, false));
    }

    @Test
    void pendingStateAlsoDisablesCollision() {
        assertTrue(LastStandService.shouldDisablePendingCollision(true, false));
        assertTrue(LastStandService.shouldDisablePendingCollision(false, true));
        assertFalse(LastStandService.shouldDisablePendingCollision(false, false));
    }

    @Test
    void pendingStateAlsoHidesFromKillerInstinct() {
        assertTrue(LastStandService.shouldHideFromKillerInstinct(true, false));
        assertTrue(LastStandService.shouldHideFromKillerInstinct(false, true));
        assertFalse(LastStandService.shouldHideFromKillerInstinct(false, false));
    }

    @Test
    void pendingStateBlocksEveryRoundEndStatusExceptNone() {
        assertTrue(LastStandService.shouldBlockRoundEnd(true, GameFunctions.WinStatus.PASSENGERS));
        assertTrue(LastStandService.shouldBlockRoundEnd(true, GameFunctions.WinStatus.KILLERS));
        assertTrue(LastStandService.shouldBlockRoundEnd(true, GameFunctions.WinStatus.TIME));
        assertTrue(LastStandService.shouldBlockRoundEnd(true, GameFunctions.WinStatus.NEUTRAL));
        assertFalse(LastStandService.shouldBlockRoundEnd(true, GameFunctions.WinStatus.NONE));
        assertFalse(LastStandService.shouldBlockRoundEnd(false, GameFunctions.WinStatus.NEUTRAL));
    }

    @Test
    void spectatorInstinctCanStillHighlightPendingLastStandPlayer() {
        assertEquals(WatheRoles.KILLER.color(), LastStandService.pendingSpectatorHighlightColor(true, true, true, WatheRoles.KILLER));
        assertEquals(WatheRoles.CIVILIAN.color(), LastStandService.pendingSpectatorHighlightColor(true, true, true, null));
        assertEquals(-1, LastStandService.pendingSpectatorHighlightColor(false, true, true, WatheRoles.KILLER));
        assertEquals(-1, LastStandService.pendingSpectatorHighlightColor(true, false, true, WatheRoles.KILLER));
        assertEquals(-1, LastStandService.pendingSpectatorHighlightColor(true, true, false, WatheRoles.KILLER));
    }

    @Test
    void pendingNoCollisionUsesNoellesRolesEffect() {
        assertEquals(Identifier.of("noellesroles", "no_collision"), LastStandService.noCollisionEffectId());
    }

    @Test
    void lastStandUsesEffectiveKillerAlignmentForTriggeringAttacker() {
        assertFalse(LastStandService.canTriggerFromKill(
                WatheRoles.CIVILIAN,
                Set.of(LastStandTrait.ID),
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID)
        ));
        assertTrue(LastStandService.canTriggerFromKill(
                WatheRoles.CIVILIAN,
                Set.of(LastStandTrait.ID),
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID)
        ));
    }

    @Test
    void lastStandTriggersWhenEffectiveCivilianIsKilledByNonCivilianFaction() {
        assertTrue(LastStandService.canTriggerFromKill(
                WatheRoles.CIVILIAN,
                Set.of(LastStandTrait.ID),
                WatheRoles.KILLER,
                Set.of()
        ));
        assertTrue(LastStandService.canTriggerFromKill(
                WatheRoles.CIVILIAN,
                Set.of(LastStandTrait.ID),
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID)
        ));
        assertTrue(LastStandService.canTriggerFromKill(
                WatheRoles.CIVILIAN,
                Set.of(LastStandTrait.ID),
                sparkWitchNeutralRole("murderous_witch"),
                Set.of()
        ));
        assertTrue(LastStandService.canTriggerFromKill(
                WatheRoles.CIVILIAN,
                Set.of(LastStandTrait.ID),
                sparkWitchNeutralRole("grand_witch"),
                Set.of()
        ));
    }

    @Test
    void lastStandDoesNotTriggerWhenKillerIsEffectivelyCivilianOrMissing() {
        assertFalse(LastStandService.canTriggerFromKill(
                WatheRoles.CIVILIAN,
                Set.of(LastStandTrait.ID),
                WatheRoles.CIVILIAN,
                Set.of()
        ));
        assertFalse(LastStandService.canTriggerFromKill(
                WatheRoles.CIVILIAN,
                Set.of(LastStandTrait.ID),
                sparkWitchApprenticeRole(),
                Set.of()
        ));
        assertFalse(LastStandService.canTriggerFromKill(
                WatheRoles.CIVILIAN,
                Set.of(LastStandTrait.ID),
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID)
        ));
        assertFalse(LastStandService.canTriggerFromKill(
                WatheRoles.CIVILIAN,
                Set.of(LastStandTrait.ID),
                null,
                Set.of()
        ));
    }

    @Test
    void mentalBreakdownBypassesLastStand() {
        assertTrue(LastStandService.shouldBypassLastStandDeathReason(GameConstants.DeathReasons.MENTAL_BREAKDOWN));
        assertFalse(LastStandService.shouldBypassLastStandDeathReason(GameConstants.DeathReasons.BAT));
    }

    @Test
    void naturalDeathReasonsBypassLastStandEvenWhenLastAttackerExists() {
        assertTrue(LastStandService.shouldBypassLastStandDeathReason(GameConstants.DeathReasons.FELL_OUT_OF_TRAIN));
        assertTrue(LastStandService.shouldBypassLastStandDeathReason(GameConstants.DeathReasons.DROWNED));
        assertTrue(LastStandService.shouldBypassLastStandDeathReason(GameConstants.DeathReasons.ESCAPED));
        assertTrue(LastStandService.shouldBypassLastStandDeathReason(GameConstants.DeathReasons.VANILLA_DEATH));
    }

    private static Role sparkWitchNeutralRole(String path) {
        return new Role(
                Identifier.of("sparkwitch", path),
                0x7A3857,
                false,
                false,
                Role.MoodType.FAKE,
                -1,
                false
        );
    }

    private static Role sparkWitchApprenticeRole() {
        return new Role(
                Identifier.of("sparkwitch", "apprentice_witch"),
                0x75EDFA,
                true,
                false,
                Role.MoodType.REAL,
                GameConstants.getInTicks(0, 10),
                false
        );
    }
}
