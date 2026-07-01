package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.entity.Entity;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalTraitServiceTest {
    @Test
    void taskMasterRequiresEffectiveRealMoodOrVisibleMoney() {
        assertTrue(GlobalTraitService.canSelectTaskMaster(WatheRoles.CIVILIAN, Set.of(), false));
        assertTrue(GlobalTraitService.canSelectTaskMaster(WatheRoles.KILLER, Set.of(ConscienceTrait.ID), false));
        assertTrue(GlobalTraitService.canSelectTaskMaster(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID), true));

        assertFalse(GlobalTraitService.canSelectTaskMaster(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID), false));
        assertFalse(GlobalTraitService.canSelectTaskMaster(WatheRoles.KILLER, Set.of(), false));
    }

    @Test
    void taskMasterRewardIsIndependentFromNativeNoellesTaskMoney() {
        assertTrue(EffectiveTraitService.hasNativeTaskMoneyReward(Noellesroles.WAITER));
        assertTrue(GlobalTraitService.canSelectTaskMaster(Noellesroles.WAITER, Set.of(), false));
        assertEquals(25, GlobalTraitService.TASK_MASTER_MONEY_REWARD);
        assertEquals(50, EffectiveTraitService.TASK_MONEY_REWARD);
    }

    @Test
    void taskMasterExtraMoodGainIsTwentyPercentOfBaseTaskGain() {
        assertEquals(0.1f, GlobalTraitService.taskMasterExtraMoodGain(), 0.0001f);
    }

    @Test
    void moneyVisibilityMirrorsServerAndNoellesRecallerRules() {
        assertFalse(GlobalTraitService.canSeeMoneyForTrait(false, false));
        assertTrue(GlobalTraitService.canSeeMoneyForTrait(true, false));
        assertTrue(GlobalTraitService.canSeeMoneyForTrait(false, true));
    }

    @Test
    void steadyRequiresEffectiveRealMood() {
        assertTrue(GlobalTraitService.canSelectSteady(WatheRoles.CIVILIAN, Set.of()));
        assertTrue(GlobalTraitService.canSelectSteady(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));

        assertFalse(GlobalTraitService.canSelectSteady(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertFalse(GlobalTraitService.canSelectSteady(WatheRoles.KILLER, Set.of()));
    }

    @Test
    void steadyRaisesOnlyRealMoodClamp() {
        assertEquals(1.25f, GlobalTraitService.clampMood(1.5f, Role.MoodType.REAL, Set.of(SteadyTrait.ID)));
        assertEquals(1.0f, GlobalTraitService.clampMood(1.5f, Role.MoodType.REAL, Set.of()));
        assertEquals(1.0f, GlobalTraitService.clampMood(1.5f, Role.MoodType.FAKE, Set.of(SteadyTrait.ID)));
        assertEquals(-1.0f, GlobalTraitService.clampMood(-1.5f, Role.MoodType.REAL, Set.of(SteadyTrait.ID)));
    }

    @Test
    void positiveMoodBarNormalizesSteadyMax() {
        assertEquals(1.0f, GlobalTraitService.positiveMoodBarProgress(1.25f, 1.25f));
        assertEquals(0.8f, GlobalTraitService.positiveMoodBarProgress(1.0f, 1.25f));
        assertEquals(0.5f, GlobalTraitService.positiveMoodBarProgress(-0.5f, 1.25f));
    }

    @Test
    void fastHandsOnlyShortensPositiveCooldowns() {
        assertEquals(90, GlobalTraitService.fastHandsCooldown(100));
        assertEquals(1, GlobalTraitService.fastHandsCooldown(1));
        assertEquals(0, GlobalTraitService.fastHandsCooldown(0));
        assertEquals(-5, GlobalTraitService.fastHandsCooldown(-5));
    }

    @Test
    void cautiousMovementSuppressionKeepsEventsButDropsSounds() {
        assertEquals(Entity.MoveEffect.EVENTS,
                GlobalTraitService.suppressMovementSounds(Entity.MoveEffect.ALL, true));
        assertEquals(Entity.MoveEffect.NONE,
                GlobalTraitService.suppressMovementSounds(Entity.MoveEffect.SOUNDS, true));
        assertEquals(Entity.MoveEffect.EVENTS,
                GlobalTraitService.suppressMovementSounds(Entity.MoveEffect.EVENTS, true));
        assertEquals(Entity.MoveEffect.NONE,
                GlobalTraitService.suppressMovementSounds(Entity.MoveEffect.NONE, true));
        assertEquals(Entity.MoveEffect.ALL,
                GlobalTraitService.suppressMovementSounds(Entity.MoveEffect.ALL, false));
        assertEquals(Entity.MoveEffect.SOUNDS,
                GlobalTraitService.suppressMovementSounds(Entity.MoveEffect.SOUNDS, false));
    }

    @Test
    void cautiousStepSuppressionOnlyCancelsSuppressedPlayers() {
        assertTrue(GlobalTraitService.shouldSuppressCautiousStepSounds(true, true));

        assertFalse(GlobalTraitService.shouldSuppressCautiousStepSounds(true, false));
        assertFalse(GlobalTraitService.shouldSuppressCautiousStepSounds(false, true));
        assertFalse(GlobalTraitService.shouldSuppressCautiousStepSounds(false, false));
    }

    @Test
    void childishScaleShrinksPlayersToNinetyPercent() {
        assertEquals(-0.10D, GlobalTraitService.CHILDISH_SCALE_MODIFIER_VALUE, 0.0001D);
    }

    @Test
    void excellentPhysiqueRequiresOriginalFiniteStamina() {
        assertTrue(GlobalTraitService.canSelectExcellentPhysique(WatheRoles.CIVILIAN));
        assertTrue(GlobalTraitService.canSelectExcellentPhysique(WatheRoles.VIGILANTE));
        assertTrue(GlobalTraitService.canSelectExcellentPhysique(Noellesroles.WAITER));

        assertFalse(GlobalTraitService.canSelectExcellentPhysique(WatheRoles.KILLER));
        assertFalse(GlobalTraitService.canSelectExcellentPhysique(WatheRoles.LOOSE_END));
        assertFalse(GlobalTraitService.canSelectExcellentPhysique(WatheRoles.NO_ROLE));
        assertFalse(GlobalTraitService.canSelectExcellentPhysique(WatheRoles.DISCOVERY_CIVILIAN));
    }

    @Test
    void excellentPhysiqueAddsOnlyExtraNaturalRecovery() {
        assertTrue(GlobalTraitService.shouldRecoverExcellentPhysiqueStamina(WatheRoles.CIVILIAN, true, false, false));

        assertFalse(GlobalTraitService.shouldRecoverExcellentPhysiqueStamina(WatheRoles.CIVILIAN, false, false, false));
        assertFalse(GlobalTraitService.shouldRecoverExcellentPhysiqueStamina(WatheRoles.CIVILIAN, true, true, false));
        assertFalse(GlobalTraitService.shouldRecoverExcellentPhysiqueStamina(WatheRoles.CIVILIAN, true, false, true));
        assertFalse(GlobalTraitService.shouldRecoverExcellentPhysiqueStamina(WatheRoles.KILLER, true, false, false));

        assertEquals(10.25f, GlobalTraitService.excellentPhysiqueRecoveredStamina(10.0f, 200), 0.0001f);
        assertEquals(200.0f, GlobalTraitService.excellentPhysiqueRecoveredStamina(199.9f, 200), 0.0001f);
        assertEquals(10.0f, GlobalTraitService.excellentPhysiqueRecoveredStamina(10.0f, -1), 0.0001f);
    }
}
