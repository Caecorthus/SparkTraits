package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.doctor4t.wathe.api.WatheRoles;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoodTraitServiceTest {
    @BeforeAll
    static void registerGoodTraits() {
        if (!TraitRegistry.contains(ImpostorTrait.ID)) {
            TraitRegistry.register(new ImpostorTrait());
        }
        if (!TraitRegistry.contains(GoodTraits.EXTROVERTED)) {
            GoodTraits.register();
        }
    }

    @Test
    void goodTraitsRequireOriginalGoodRolesWithoutImpostor() {
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.CIVILIAN, Set.of(GoodTraits.EXTROVERTED)));
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.VIGILANTE, Set.of(GoodTraits.INTROVERTED)));
        assertTrue(TraitRules.canApplyAll(null, null, null, Noellesroles.DETECTIVE, Set.of(GoodTraits.INTROVERTED)));

        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.KILLER, Set.of(GoodTraits.EXTROVERTED)));
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.CORRUPT_COP, Set.of(GoodTraits.INTROVERTED)));
        assertFalse(TraitRules.canApplyAll(
                null,
                null,
                null,
                WatheRoles.CIVILIAN,
                Set.of(GoodTraits.EXTROVERTED, ImpostorTrait.ID)
        ));
    }

    @Test
    void focusExcludesWathePoliceRoles() {
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.CIVILIAN, Set.of(GoodTraits.FOCUS)));
        assertTrue(TraitRules.canApplyAll(null, null, null, Noellesroles.DETECTIVE, Set.of(GoodTraits.FOCUS)));

        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.VIGILANTE, Set.of(GoodTraits.FOCUS)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.VETERAN, Set.of(GoodTraits.FOCUS)));
    }

    @Test
    void undercoverConflictsWithSocialAndFocusTraits() {
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.UNDERCOVER, Set.of(GoodTraits.EXTROVERTED)));
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.UNDERCOVER, Set.of(GoodTraits.INTROVERTED)));
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.UNDERCOVER, Set.of(GoodTraits.FOCUS)));
    }

    @Test
    void moneyTreeRequiresVisibleMoneyOrNativeTaskMoney() {
        assertFalse(GoodTraitService.canSelectMoneyTree(WatheRoles.CIVILIAN, Set.of(), false));
        assertTrue(GoodTraitService.canSelectMoneyTree(WatheRoles.CIVILIAN, Set.of(), true));
        assertTrue(GoodTraitService.canSelectMoneyTree(Noellesroles.RECALLER, Set.of(), false));

        assertFalse(GoodTraitService.canSelectMoneyTree(WatheRoles.KILLER, Set.of(), true));
        assertFalse(GoodTraitService.canSelectMoneyTree(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID),
                true
        ));
    }

    @Test
    void extrovertedAndIntrovertedAreMutuallyExclusive() {
        assertTrue(TraitRules.areIncompatible(
                TraitRegistry.get(GoodTraits.EXTROVERTED),
                TraitRegistry.get(GoodTraits.INTROVERTED)
        ));
        assertFalse(TraitRules.canApplyAll(
                null,
                null,
                null,
                WatheRoles.CIVILIAN,
                Set.of(GoodTraits.EXTROVERTED, GoodTraits.INTROVERTED)
        ));
    }

    @Test
    void socialMoodDrainStopsOnlyAtMatchingCrowdSize() {
        assertTrue(GoodTraitService.shouldPreventSocialMoodDrain(Set.of(GoodTraits.EXTROVERTED), 2));
        assertFalse(GoodTraitService.shouldPreventSocialMoodDrain(Set.of(GoodTraits.EXTROVERTED), 1));

        assertTrue(GoodTraitService.shouldPreventSocialMoodDrain(Set.of(GoodTraits.INTROVERTED), 0));
        assertTrue(GoodTraitService.shouldPreventSocialMoodDrain(Set.of(GoodTraits.INTROVERTED), 1));
        assertFalse(GoodTraitService.shouldPreventSocialMoodDrain(Set.of(GoodTraits.INTROVERTED), 2));
    }

    @Test
    void socialMoodAdjustmentOnlyCancelsDrain() {
        assertEquals(0.8f, GoodTraitService.socialMoodAdjustedMood(
                0.8f,
                0.4f,
                Set.of(GoodTraits.EXTROVERTED),
                2
        ), 0.0001f);
        assertEquals(0.4f, GoodTraitService.socialMoodAdjustedMood(
                0.8f,
                0.4f,
                Set.of(GoodTraits.EXTROVERTED),
                1
        ), 0.0001f);
        assertEquals(1.0f, GoodTraitService.socialMoodAdjustedMood(
                0.8f,
                1.0f,
                Set.of(GoodTraits.INTROVERTED),
                0
        ), 0.0001f);
    }

    @Test
    void focusPreventsOnlyOrdinaryGoodGunMoodPenalty() {
        assertTrue(GoodTraitService.shouldPreventGunMoodPenalty(WatheRoles.CIVILIAN, Set.of(GoodTraits.FOCUS)));
        assertFalse(GoodTraitService.shouldPreventGunMoodPenalty(WatheRoles.CIVILIAN, Set.of()));
        assertFalse(GoodTraitService.shouldPreventGunMoodPenalty(WatheRoles.CIVILIAN, Set.of(GoodTraits.FOCUS, ImpostorTrait.ID)));
        assertFalse(GoodTraitService.shouldPreventGunMoodPenalty(WatheRoles.KILLER, Set.of(GoodTraits.FOCUS)));
        assertFalse(GoodTraitService.shouldPreventGunMoodPenalty(WatheRoles.VIGILANTE, Set.of(GoodTraits.FOCUS)));
        assertFalse(GoodTraitService.shouldPreventGunMoodPenalty(WatheRoles.VETERAN, Set.of(GoodTraits.FOCUS)));
    }

    @Test
    void moneyTreeRewardTimingIsFixed() {
        assertEquals(5, GoodTraitService.MONEY_TREE_REWARD);
        assertEquals(600, GoodTraitService.MONEY_TREE_INTERVAL_TICKS);
    }
}
