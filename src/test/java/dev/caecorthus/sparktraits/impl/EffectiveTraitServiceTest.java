package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectiveTraitServiceTest {
    @Test
    void impostorAndLastStandAreIncompatible() {
        assertTrue(TraitRules.areIncompatible(new ImpostorTrait(), new LastStandTrait()));
    }

    @Test
    void conscienceAndImpostorRequireAtLeastTwoOriginalKillers() {
        assertFalse(EffectiveTraitService.canSelectConscience(WatheRoles.KILLER, 1, Set.of()));
        assertFalse(EffectiveTraitService.canSelectImpostor(WatheRoles.CIVILIAN, 1, Set.of()));
        assertTrue(EffectiveTraitService.canSelectConscience(WatheRoles.KILLER, 2, Set.of()));
        assertTrue(EffectiveTraitService.canSelectImpostor(WatheRoles.CIVILIAN, 2, Set.of()));
    }

    @Test
    void conscienceRequiresTheOriginalKillerRoleToBeEnabled() {
        assertFalse(EffectiveTraitService.canSelectConscience(WatheRoles.KILLER, 2, false, Set.of()));
        assertTrue(EffectiveTraitService.canSelectConscience(WatheRoles.KILLER, 2, true, Set.of()));
    }

    @Test
    void impostorConflictsWithLastStandDuringSelection() {
        assertFalse(EffectiveTraitService.canSelectImpostor(WatheRoles.CIVILIAN, 2, Set.of(LastStandTrait.ID)));
    }

    @Test
    void undercoverCanNeverSelectImpostor() {
        assertFalse(EffectiveTraitService.canSelectImpostor(Noellesroles.UNDERCOVER, 2, Set.of()));
    }

    @Test
    void protectedInnocentRolesCanNeverSelectImpostor() {
        assertFalse(EffectiveTraitService.canSelectImpostor(Noellesroles.SURVIVAL_MASTER, 2, Set.of()));
        assertFalse(EffectiveTraitService.canSelectImpostor(WatheRoles.VIGILANTE, 2, Set.of()));
        assertFalse(EffectiveTraitService.canSelectImpostor(WatheRoles.VETERAN, 2, Set.of()));
    }

    @Test
    void demonHunterCanSelectImpostor() {
        assertTrue(EffectiveTraitService.canSelectImpostor(Noellesroles.DEMON_HUNTER, 2, Set.of()));
    }

    @Test
    void effectiveTeamsFlipForConscienceAndImpostor() {
        Set<Identifier> conscience = Set.of(ConscienceTrait.ID);
        Set<Identifier> impostor = Set.of(ImpostorTrait.ID);

        assertFalse(EffectiveTraitService.isEffectiveKiller(WatheRoles.KILLER, conscience));
        assertTrue(EffectiveTraitService.isEffectiveCivilian(WatheRoles.KILLER, conscience));
        assertTrue(EffectiveTraitService.isEffectiveKiller(WatheRoles.CIVILIAN, impostor));
        assertFalse(EffectiveTraitService.isEffectiveCivilian(WatheRoles.CIVILIAN, impostor));
    }

    @Test
    void roundEndWinnersUseEffectiveTeams() {
        Set<Identifier> conscience = Set.of(ConscienceTrait.ID);
        Set<Identifier> impostor = Set.of(ImpostorTrait.ID);

        assertTrue(EffectiveTraitService.didEffectiveTeamWin(GameFunctions.WinStatus.PASSENGERS, WatheRoles.KILLER, conscience));
        assertFalse(EffectiveTraitService.didEffectiveTeamWin(GameFunctions.WinStatus.KILLERS, WatheRoles.KILLER, conscience));
        assertTrue(EffectiveTraitService.didEffectiveTeamWin(GameFunctions.WinStatus.KILLERS, WatheRoles.CIVILIAN, impostor));
        assertFalse(EffectiveTraitService.didEffectiveTeamWin(GameFunctions.WinStatus.PASSENGERS, WatheRoles.CIVILIAN, impostor));
    }

    @Test
    void conscienceViewerForcesCohortHidden() {
        assertEquals(
                Boolean.FALSE,
                EffectiveTraitService.cohortOverride(
                        WatheRoles.KILLER,
                        Set.of(ConscienceTrait.ID),
                        WatheRoles.KILLER,
                        Set.of()
                )
        );
    }

    @Test
    void realKillerSeesImpostorAsCohortAndConscienceNotAsCohort() {
        assertEquals(
                Boolean.TRUE,
                EffectiveTraitService.cohortOverride(
                        WatheRoles.KILLER,
                        Set.of(),
                        WatheRoles.CIVILIAN,
                        Set.of(ImpostorTrait.ID)
                )
        );
        assertEquals(
                Boolean.FALSE,
                EffectiveTraitService.cohortOverride(
                        WatheRoles.KILLER,
                        Set.of(),
                        WatheRoles.KILLER,
                        Set.of(ConscienceTrait.ID)
                )
        );
    }

    @Test
    void blackoutCooldownGroupsSeparateConscienceFromRealKillers() {
        assertTrue(EffectiveTraitService.sharesBlackoutCooldown(
                WatheRoles.KILLER,
                Set.of(),
                WatheRoles.KILLER,
                Set.of()
        ));
        assertTrue(EffectiveTraitService.sharesBlackoutCooldown(
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID),
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID)
        ));
        assertFalse(EffectiveTraitService.sharesBlackoutCooldown(
                WatheRoles.KILLER,
                Set.of(),
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID)
        ));
        assertFalse(EffectiveTraitService.sharesBlackoutCooldown(
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID),
                WatheRoles.KILLER,
                Set.of()
        ));
    }

    @Test
    void publicKillerCountExcludesConscience() {
        assertFalse(EffectiveTraitService.countsAsPublicKiller(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertTrue(EffectiveTraitService.countsAsPublicKiller(WatheRoles.KILLER, Set.of()));
        assertFalse(EffectiveTraitService.countsAsPublicKiller(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
    }

    @Test
    void taskMoneySupplementsFlippedRolesWithoutDoublePayingNativeTaskMoneyRoles() {
        assertTrue(EffectiveTraitService.shouldRewardTaskMoney(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertTrue(EffectiveTraitService.shouldRewardTaskMoney(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertFalse(EffectiveTraitService.shouldRewardTaskMoney(Noellesroles.WAITER, Set.of(ImpostorTrait.ID)));
        assertFalse(EffectiveTraitService.shouldRewardTaskMoney(Noellesroles.WAITER, Set.of()));
        assertFalse(EffectiveTraitService.shouldRewardTaskMoney(WatheRoles.CIVILIAN, Set.of()));
    }

    @Test
    void conscienceGetsKillRewardOnlyForNonCivilianVictims() {
        assertFalse(EffectiveTraitService.shouldRewardConscienceKill(WatheRoles.CIVILIAN, Set.of()));
        assertTrue(EffectiveTraitService.shouldRewardConscienceKill(WatheRoles.KILLER, Set.of()));
        assertTrue(EffectiveTraitService.shouldRewardConscienceKill(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
    }

    @Test
    void impostorGetsFullKillRewardForCivilianAndNeutralVictims() {
        assertEquals(GameConstants.MONEY_PER_KILL,
                EffectiveTraitService.impostorKillReward(WatheRoles.CIVILIAN, Set.of(), true));
        assertEquals(GameConstants.MONEY_PER_KILL,
                EffectiveTraitService.impostorKillReward(Noellesroles.JESTER, Set.of(), true));
        assertEquals(GameConstants.MONEY_PER_KILL,
                EffectiveTraitService.impostorKillReward(WatheRoles.KILLER, Set.of(ConscienceTrait.ID), true));
        assertEquals(0,
                EffectiveTraitService.impostorKillReward(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID), true));
        assertEquals(0,
                EffectiveTraitService.impostorKillReward(WatheRoles.CIVILIAN, Set.of(), false));
    }

    @Test
    void impostorDoesNotTriggerJesterMomentOrShotJesterPunishment() {
        assertTrue(EffectiveTraitService.shouldTriggerJesterMoment(WatheRoles.CIVILIAN, Set.of()));
        assertFalse(EffectiveTraitService.shouldTriggerJesterMoment(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertFalse(EffectiveTraitService.shouldTriggerJesterMoment(WatheRoles.KILLER, Set.of()));
    }

    @Test
    void gunPunishmentTreatsConscienceKillerVictimAsInnocent() {
        assertTrue(EffectiveTraitService.shouldTreatGunVictimAsInnocent(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertTrue(EffectiveTraitService.shouldTreatGunVictimAsInnocent(WatheRoles.CIVILIAN, Set.of()));
        assertFalse(EffectiveTraitService.shouldTreatGunVictimAsInnocent(WatheRoles.KILLER, Set.of()));
        assertFalse(EffectiveTraitService.shouldTreatGunVictimAsInnocent(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
    }

    @Test
    void innocentShotPunishmentIsCancelledOnlyForImpostors() {
        assertFalse(EffectiveTraitService.shouldCancelInnocentShotPunishment(
                WatheRoles.KILLER,
                Set.of(),
                WatheRoles.CIVILIAN,
                Set.of()
        ));
        assertFalse(EffectiveTraitService.shouldCancelInnocentShotPunishment(
                WatheRoles.KILLER,
                Set.of(),
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID)
        ));
        assertTrue(EffectiveTraitService.shouldCancelInnocentShotPunishment(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID),
                WatheRoles.CIVILIAN,
                Set.of()
        ));
        assertFalse(EffectiveTraitService.shouldCancelInnocentShotPunishment(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID),
                WatheRoles.KILLER,
                Set.of()
        ));
    }

    @Test
    void effectiveMoodTypeOverridesOriginalRoleMood() {
        assertEquals(Role.MoodType.REAL, EffectiveTraitService.effectiveMoodType(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertEquals(Role.MoodType.FAKE, EffectiveTraitService.effectiveMoodType(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertEquals(Role.MoodType.REAL, EffectiveTraitService.effectiveMoodType(WatheRoles.CIVILIAN, Set.of()));
    }

    @Test
    void conscienceInstinctCanHighlightLastStandHiddenTargets() {
        assertTrue(EffectiveTraitService.shouldConscienceInstinctHighlightTarget(true, false, false, 1.0, true, false, false));
        assertTrue(EffectiveTraitService.shouldConscienceInstinctHighlightTarget(true, false, false, 1.0, false, true, false));
        assertTrue(EffectiveTraitService.shouldConscienceInstinctHighlightTarget(true, true, false, 1.0, false, false, false));
    }

    @Test
    void conscienceBomberBombHolderIgnoresInstinctRangeLimit() {
        assertTrue(EffectiveTraitService.shouldConscienceInstinctHighlightTarget(true, true, false, 400.0, false, false, false, true));
        assertFalse(EffectiveTraitService.shouldConscienceInstinctHighlightTarget(true, true, false, 400.0, false, false, false, false));
    }

    @Test
    void conscienceInstinctSkipsProjectingSpiritTargets() {
        assertFalse(EffectiveTraitService.shouldConscienceInstinctHighlightTarget(true, true, false, 1.0, false, false, true));
        assertFalse(EffectiveTraitService.shouldConscienceInstinctHighlightTarget(true, true, false, 400.0, false, false, true, true));
    }

    @Test
    void invisibleTargetsSkipSparkTraitsEffectiveInstinctOverlays() {
        assertTrue(EffectiveTraitService.shouldSkipInvisibleTargetFromEffectiveInstinct(true, true, false, false));
        assertFalse(EffectiveTraitService.shouldSkipInvisibleTargetFromEffectiveInstinct(true, false, false, false));
        assertFalse(EffectiveTraitService.shouldSkipInvisibleTargetFromEffectiveInstinct(false, true, false, false));
        assertFalse(EffectiveTraitService.shouldSkipInvisibleTargetFromEffectiveInstinct(true, true, true, false));
        assertFalse(EffectiveTraitService.shouldSkipInvisibleTargetFromEffectiveInstinct(true, true, false, true));
    }

    @Test
    void survivalMasterSkipsImpostorInstinct() {
        assertTrue(EffectiveTraitService.shouldSkipSurvivalMasterForImpostorInstinct(Noellesroles.SURVIVAL_MASTER, true));
        assertFalse(EffectiveTraitService.shouldSkipSurvivalMasterForImpostorInstinct(Noellesroles.SURVIVAL_MASTER, false));
        assertFalse(EffectiveTraitService.shouldSkipSurvivalMasterForImpostorInstinct(WatheRoles.CIVILIAN, true));
    }

    @Test
    void conscienceMorphlingCorpseModeHidesFromInstinct() {
        assertTrue(EffectiveTraitService.shouldHideConscienceMorphlingFromInstinct(true, true, true));
        assertFalse(EffectiveTraitService.shouldHideConscienceMorphlingFromInstinct(true, true, false));
        assertFalse(EffectiveTraitService.shouldHideConscienceMorphlingFromInstinct(true, false, true));
        assertFalse(EffectiveTraitService.shouldHideConscienceMorphlingFromInstinct(false, true, true));
    }

    @Test
    void conscienceMorphlingDisguiseUsesEffectiveAlignmentInstinctColors() {
        assertEquals(
                EffectiveTraitService.KILLER_INSTINCT_COLOR,
                EffectiveTraitService.effectiveKillerInstinctColor(WatheRoles.KILLER, Set.of())
        );
        assertEquals(
                EffectiveTraitService.CIVILIAN_INSTINCT_COLOR,
                EffectiveTraitService.effectiveKillerInstinctColor(WatheRoles.KILLER, Set.of(ConscienceTrait.ID))
        );
        assertEquals(
                EffectiveTraitService.IMPOSTOR_INSTINCT_COLOR,
                EffectiveTraitService.effectiveKillerInstinctColor(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID))
        );
        assertEquals(
                EffectiveTraitService.CIVILIAN_INSTINCT_COLOR,
                EffectiveTraitService.effectiveKillerInstinctColor(WatheRoles.CIVILIAN, Set.of())
        );
    }

    @Test
    void conscienceMorphlingCohortFollowsDisguiseEffectiveAlignment() {
        assertEquals(
                Boolean.TRUE,
                EffectiveTraitService.conscienceMorphlingCohortOverride(
                        WatheRoles.KILLER,
                        Set.of(),
                        true,
                        true,
                        false,
                        true,
                        WatheRoles.KILLER,
                        Set.of()
                )
        );
        assertEquals(
                Boolean.TRUE,
                EffectiveTraitService.conscienceMorphlingCohortOverride(
                        WatheRoles.KILLER,
                        Set.of(),
                        true,
                        true,
                        false,
                        true,
                        WatheRoles.CIVILIAN,
                        Set.of(ImpostorTrait.ID)
                )
        );
        assertEquals(
                Boolean.FALSE,
                EffectiveTraitService.conscienceMorphlingCohortOverride(
                        WatheRoles.KILLER,
                        Set.of(),
                        true,
                        true,
                        false,
                        true,
                        WatheRoles.KILLER,
                        Set.of(ConscienceTrait.ID)
                )
        );
        assertEquals(
                Boolean.FALSE,
                EffectiveTraitService.conscienceMorphlingCohortOverride(
                        WatheRoles.KILLER,
                        Set.of(),
                        true,
                        true,
                        true,
                        true,
                        WatheRoles.KILLER,
                        Set.of()
                )
        );
        assertEquals(
                null,
                EffectiveTraitService.conscienceMorphlingCohortOverride(
                        WatheRoles.KILLER,
                        Set.of(),
                        true,
                        true,
                        false,
                        false,
                        WatheRoles.KILLER,
                        Set.of()
                )
        );
    }

    @Test
    void conscienceMorphlingCohortCanUsePublicDisguiseKillerState() {
        assertEquals(
                Boolean.TRUE,
                EffectiveTraitService.conscienceMorphlingCohortOverride(
                        WatheRoles.KILLER,
                        Set.of(),
                        true,
                        true,
                        false,
                        true,
                        true,
                        true,
                        false,
                        false
                )
        );
    }

    @Test
    void realKillerTargetKeepsCohortWhenCopiedByConscienceMorphling() {
        assertEquals(
                Boolean.TRUE,
                EffectiveTraitService.conscienceMorphlingDisguiseTargetCohortOverride(
                        WatheRoles.KILLER,
                        Set.of(),
                        WatheRoles.KILLER,
                        Set.of(),
                        true
                )
        );
    }

    @Test
    void conscienceDisguiseTargetDoesNotBecomeCohort() {
        assertEquals(
                null,
                EffectiveTraitService.conscienceMorphlingDisguiseTargetCohortOverride(
                        WatheRoles.KILLER,
                        Set.of(),
                        WatheRoles.KILLER,
                        Set.of(ConscienceTrait.ID),
                        true
                )
        );
    }

    @Test
    void conscienceKillPunishmentIgnoresAreaDamage() {
        assertTrue(EffectiveTraitService.shouldPunishConscienceKill(true, GameConstants.DeathReasons.GUN));
        assertFalse(EffectiveTraitService.shouldPunishConscienceKill(true, GameConstants.DeathReasons.GRENADE));
        assertFalse(EffectiveTraitService.shouldPunishConscienceKill(true, GameConstants.DeathReasons.POISON, Noellesroles.POISON_SOURCE_GAS_BOMB));
        assertTrue(EffectiveTraitService.shouldPunishConscienceKill(true, GameConstants.DeathReasons.POISON, Noellesroles.POISON_SOURCE_NEEDLE));
        assertFalse(EffectiveTraitService.shouldPunishConscienceKill(false, GameConstants.DeathReasons.GUN));
    }

    @Test
    void conscienceCompensationKeepsPublicKillerCountStable() {
        assertEquals(1, EffectiveTraitService.requiredExtraKillersForConscience(3, 3, 1));
        assertEquals(0, EffectiveTraitService.requiredExtraKillersForConscience(3, 4, 1));
    }

    @Test
    void transferredConscienceTimedBombsAreNonLethal() {
        assertTrue(ConscienceBombService.shouldTrackPlacedTimedBomb(true));
        assertFalse(ConscienceBombService.shouldTrackPlacedTimedBomb(false));
        assertTrue(ConscienceBombService.shouldTrackTransferredTimedBomb(false, true));
        assertTrue(ConscienceBombService.shouldTrackTransferredTimedBomb(true, false));
        assertFalse(ConscienceBombService.shouldTrackTransferredTimedBomb(false, false));
        assertTrue(ConscienceBombService.shouldCancelTransferredTimedBombDeath(true));
        assertFalse(ConscienceBombService.shouldCancelTransferredTimedBombDeath(false));
    }

    @Test
    void conscienceBombRevealRadiusIsEightBlocks() {
        assertTrue(ConscienceBombService.isWithinRevealRadius(64.0));
        assertFalse(ConscienceBombService.isWithinRevealRadius(64.01));
    }
}
