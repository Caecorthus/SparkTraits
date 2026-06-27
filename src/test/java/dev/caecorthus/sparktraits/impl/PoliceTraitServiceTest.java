package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoliceTraitServiceTest {
    @BeforeAll
    static void registerPoliceTraits() {
        if (!TraitRegistry.contains(PoliceTraits.MARKSMAN)) {
            PoliceTraits.register();
        }
    }

    @Test
    void policeTraitsRequireOriginalPoliceRoles() {
        assertNotNull(TraitRegistry.get(PoliceTraits.NIKO));
        assertEquals(0x39FF14, TraitRegistry.get(PoliceTraits.NIKO).color());

        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.VIGILANTE, Set.of(PoliceTraits.MARKSMAN)));
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.VIGILANTE, Set.of(PoliceTraits.FAST_RELOAD)));
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.VIGILANTE, Set.of(PoliceTraits.HEAVY_ARTILLERY)));
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.VIGILANTE, Set.of(PoliceTraits.NIKO)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.VETERAN, Set.of(PoliceTraits.MARKSMAN)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.VETERAN, Set.of(PoliceTraits.NIKO)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.CIVILIAN, Set.of(PoliceTraits.NIKO)));
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.DETECTIVE, Set.of(PoliceTraits.NIKO)));

        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.VETERAN, Set.of(PoliceTraits.WELL_TRAINED)));
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.VETERAN, Set.of(PoliceTraits.GOING_DARK)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.VIGILANTE, Set.of(PoliceTraits.WELL_TRAINED)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.CIVILIAN, Set.of(PoliceTraits.GOING_DARK)));
    }

    @Test
    void marksmanOnlyExtendsVigilanteGuns() {
        assertEquals(39.0, VigilanteVeteranTraitService.gunRange(
                VigilanteVeteranTraitService.REVOLVER_RANGE,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.MARKSMAN)
        ), 0.0001);
        assertEquals(9.1, VigilanteVeteranTraitService.gunRange(
                VigilanteVeteranTraitService.DERRINGER_RANGE,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.MARKSMAN)
        ), 0.0001);
        assertEquals(30.0, VigilanteVeteranTraitService.gunRange(
                VigilanteVeteranTraitService.REVOLVER_RANGE,
                WatheRoles.VETERAN,
                Set.of(PoliceTraits.MARKSMAN)
        ), 0.0001);
    }

    @Test
    void nikoCrouchRangeOnlyAppliesToOriginalVigilanteGuns() {
        assertEquals(300.0, VigilanteVeteranTraitService.gunRange(
                VigilanteVeteranTraitService.REVOLVER_RANGE,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ), 0.0001);
        assertEquals(300.0, VigilanteVeteranTraitService.gunRange(
                VigilanteVeteranTraitService.DERRINGER_RANGE,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ), 0.0001);
        assertEquals(30.0, VigilanteVeteranTraitService.gunRange(
                VigilanteVeteranTraitService.REVOLVER_RANGE,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                false
        ), 0.0001);
        assertEquals(30.0, VigilanteVeteranTraitService.gunRange(
                VigilanteVeteranTraitService.REVOLVER_RANGE,
                WatheRoles.VETERAN,
                Set.of(PoliceTraits.NIKO),
                true
        ), 0.0001);
        assertEquals(30.0, VigilanteVeteranTraitService.gunRange(
                VigilanteVeteranTraitService.REVOLVER_RANGE,
                Noellesroles.DETECTIVE,
                Set.of(PoliceTraits.NIKO),
                true
        ), 0.0001);
    }

    @Test
    void nikoCrouchRangeOverridesMarksmanButStandingMarksmanStillWorks() {
        assertEquals(300.0, VigilanteVeteranTraitService.gunRange(
                VigilanteVeteranTraitService.REVOLVER_RANGE,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO, PoliceTraits.MARKSMAN),
                true
        ), 0.0001);
        assertEquals(39.0, VigilanteVeteranTraitService.gunRange(
                VigilanteVeteranTraitService.REVOLVER_RANGE,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO, PoliceTraits.MARKSMAN),
                false
        ), 0.0001);
        assertEquals(9.1, VigilanteVeteranTraitService.gunRange(
                VigilanteVeteranTraitService.DERRINGER_RANGE,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO, PoliceTraits.MARKSMAN),
                false
        ), 0.0001);
    }

    @Test
    void fastReloadOnlyShortensVigilanteRevolverCooldown() {
        int revolverCooldown = GameConstants.getInTicks(0, 10);

        assertEquals(140, VigilanteVeteranTraitService.fastReloadCooldown(
                true,
                revolverCooldown,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.FAST_RELOAD)
        ));
        assertEquals(revolverCooldown, VigilanteVeteranTraitService.fastReloadCooldown(
                false,
                revolverCooldown,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.FAST_RELOAD)
        ));
        assertEquals(revolverCooldown, VigilanteVeteranTraitService.fastReloadCooldown(
                true,
                revolverCooldown,
                WatheRoles.CIVILIAN,
                Set.of(PoliceTraits.FAST_RELOAD)
        ));
    }

    @Test
    void nikoRevolverCooldownIsNotShortenedByReloadTraits() {
        int revolverCooldown = GameConstants.getInTicks(0, 10);

        assertEquals(1200, VigilanteVeteranTraitService.fastReloadCooldown(
                true,
                revolverCooldown,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertEquals(1200, VigilanteVeteranTraitService.fastReloadCooldown(
                true,
                revolverCooldown,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO, PoliceTraits.FAST_RELOAD),
                true
        ));
        assertEquals(140, VigilanteVeteranTraitService.fastReloadCooldown(
                true,
                revolverCooldown,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO, PoliceTraits.FAST_RELOAD),
                false
        ));
        assertTrue(VigilanteVeteranTraitService.shouldPreserveNikoRevolverCooldown(
                1200,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldPreserveNikoRevolverCooldown(
                1200,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                false
        ));
    }

    @Test
    void nikoBurstStartsOnlyForValidCrouchingOriginalVigilanteRevolverShots() {
        assertTrue(VigilanteVeteranTraitService.shouldStartNikoRevolverBurst(
                true,
                false,
                true,
                true,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldStartNikoRevolverBurst(
                false,
                false,
                true,
                true,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldStartNikoRevolverBurst(
                true,
                true,
                true,
                true,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldStartNikoRevolverBurst(
                true,
                false,
                false,
                true,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldStartNikoRevolverBurst(
                true,
                false,
                true,
                false,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldStartNikoRevolverBurst(
                true,
                false,
                true,
                true,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                false
        ));
        assertFalse(VigilanteVeteranTraitService.shouldStartNikoRevolverBurst(
                true,
                false,
                true,
                true,
                Noellesroles.DETECTIVE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
    }

    @Test
    void nikoBurstContinuationKeepsStateChecksWithoutCooldownGate() {
        assertTrue(VigilanteVeteranTraitService.shouldContinueNikoRevolverBurst(
                true,
                true,
                true,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldContinueNikoRevolverBurst(
                false,
                true,
                true,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldContinueNikoRevolverBurst(
                true,
                false,
                true,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldContinueNikoRevolverBurst(
                true,
                true,
                false,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldContinueNikoRevolverBurst(
                true,
                true,
                true,
                Noellesroles.CORRUPT_COP,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldContinueNikoRevolverBurst(
                true,
                true,
                true,
                WatheRoles.VIGILANTE,
                Set.of(),
                true
        ));
    }

    @Test
    void nikoNightVisionRequiresLivingCrouchingOriginalVigilante() {
        assertTrue(VigilanteVeteranTraitService.shouldRefreshNikoNightVision(
                true,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldRefreshNikoNightVision(
                false,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldRefreshNikoNightVision(
                true,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.NIKO),
                false
        ));
        assertFalse(VigilanteVeteranTraitService.shouldRefreshNikoNightVision(
                true,
                WatheRoles.VETERAN,
                Set.of(PoliceTraits.NIKO),
                true
        ));
        assertFalse(VigilanteVeteranTraitService.shouldRefreshNikoNightVision(
                true,
                WatheRoles.VIGILANTE,
                Set.of(),
                true
        ));
    }

    @Test
    void heavyArtilleryRetriesCloseGunDamageOnlyWhileTargetSurvives() {
        assertTrue(VigilanteVeteranTraitService.isHeavyArtilleryShot(
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.HEAVY_ARTILLERY),
                GameConstants.DeathReasons.GUN,
                25.0
        ));
        assertFalse(VigilanteVeteranTraitService.isHeavyArtilleryShot(
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.HEAVY_ARTILLERY),
                GameConstants.DeathReasons.GUN,
                25.0001
        ));
        assertFalse(VigilanteVeteranTraitService.isHeavyArtilleryShot(
                WatheRoles.CIVILIAN,
                Set.of(PoliceTraits.HEAVY_ARTILLERY),
                GameConstants.DeathReasons.GUN,
                4.0
        ));
        assertFalse(VigilanteVeteranTraitService.isHeavyArtilleryShot(
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.HEAVY_ARTILLERY),
                GameConstants.DeathReasons.KNIFE,
                4.0
        ));

        assertTrue(VigilanteVeteranTraitService.shouldRetryHeavyArtilleryDamage(true, true));
        assertFalse(VigilanteVeteranTraitService.shouldRetryHeavyArtilleryDamage(true, false));
        assertFalse(VigilanteVeteranTraitService.shouldRetryHeavyArtilleryDamage(false, true));
    }

    @Test
    void wellTrainedReducesDrainAndIgnoresLowMoodThresholds() {
        assertEquals(0.65f, VigilanteVeteranTraitService.wellTrainedAdjustedMood(
                1.0f,
                0.5f,
                WatheRoles.VETERAN,
                Set.of(PoliceTraits.WELL_TRAINED)
        ), 0.0001f);
        assertEquals(0.5f, VigilanteVeteranTraitService.wellTrainedAdjustedMood(
                1.0f,
                0.5f,
                WatheRoles.CIVILIAN,
                Set.of(PoliceTraits.WELL_TRAINED)
        ), 0.0001f);
        assertEquals(1.0f, VigilanteVeteranTraitService.wellTrainedAdjustedMood(
                0.5f,
                1.0f,
                WatheRoles.VETERAN,
                Set.of(PoliceTraits.WELL_TRAINED)
        ), 0.0001f);

        assertTrue(VigilanteVeteranTraitService.ignoresLowMood(WatheRoles.VETERAN, Set.of(PoliceTraits.WELL_TRAINED)));
        assertFalse(VigilanteVeteranTraitService.ignoresLowMood(WatheRoles.VETERAN, Set.of()));
        assertFalse(VigilanteVeteranTraitService.ignoresLowMood(WatheRoles.CIVILIAN, Set.of(PoliceTraits.WELL_TRAINED)));
    }

    @Test
    void goingDarkOnlyHidesLivingVeteransDuringBlackoutDefaultInstinct() {
        assertTrue(VigilanteVeteranTraitService.goingDarkInstinctHidden(
                true,
                true,
                WatheRoles.VETERAN,
                Set.of(PoliceTraits.GOING_DARK)
        ));
        assertFalse(VigilanteVeteranTraitService.goingDarkInstinctHidden(
                false,
                true,
                WatheRoles.VETERAN,
                Set.of(PoliceTraits.GOING_DARK)
        ));
        assertFalse(VigilanteVeteranTraitService.goingDarkInstinctHidden(
                true,
                false,
                WatheRoles.VETERAN,
                Set.of(PoliceTraits.GOING_DARK)
        ));
        assertFalse(VigilanteVeteranTraitService.goingDarkInstinctHidden(
                true,
                true,
                WatheRoles.VIGILANTE,
                Set.of(PoliceTraits.GOING_DARK)
        ));

        assertTrue(VigilanteVeteranTraitService.shouldSkipGoingDarkDefaultInstinct(true, true, false));
        assertFalse(VigilanteVeteranTraitService.shouldSkipGoingDarkDefaultInstinct(true, false, false));
        assertFalse(VigilanteVeteranTraitService.shouldSkipGoingDarkDefaultInstinct(true, true, true));
    }
}
