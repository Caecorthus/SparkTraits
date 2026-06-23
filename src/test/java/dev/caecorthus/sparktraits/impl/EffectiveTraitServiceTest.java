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
    void impostorConflictsWithLastStandDuringSelection() {
        assertFalse(EffectiveTraitService.canSelectImpostor(WatheRoles.CIVILIAN, 2, Set.of(LastStandTrait.ID)));
    }

    @Test
    void undercoverCanNeverSelectImpostor() {
        assertFalse(EffectiveTraitService.canSelectImpostor(Noellesroles.UNDERCOVER, 2, Set.of()));
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
    void conscienceGetsKillRewardOnlyForNonCivilianVictims() {
        assertFalse(EffectiveTraitService.shouldRewardConscienceKill(WatheRoles.CIVILIAN, Set.of()));
        assertTrue(EffectiveTraitService.shouldRewardConscienceKill(WatheRoles.KILLER, Set.of()));
        assertTrue(EffectiveTraitService.shouldRewardConscienceKill(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
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
    void conscienceInstinctSkipsProjectingSpiritTargets() {
        assertFalse(EffectiveTraitService.shouldConscienceInstinctHighlightTarget(true, true, false, 1.0, false, false, true));
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
}
