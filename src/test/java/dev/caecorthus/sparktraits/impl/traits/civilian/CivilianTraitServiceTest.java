package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.doctor4t.wathe.api.WatheRoles;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.caecorthus.sparktraits.impl.selection.TraitRules;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;

class CivilianTraitServiceTest {
    @BeforeAll
    static void registerCivilianTraits() {
        if (!TraitRegistry.contains(ImpostorTrait.ID)) {
            TraitRegistry.register(new ImpostorTrait());
        }
        if (!TraitRegistry.contains(CivilianTraits.EXTROVERTED)) {
            CivilianTraits.register();
        }
    }

    @Test
    void civilianTraitsRequireOriginalCivilianRolesWithoutImpostor() {
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.CIVILIAN, Set.of(CivilianTraits.EXTROVERTED)));
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.VIGILANTE, Set.of(CivilianTraits.INTROVERTED)));
        assertTrue(TraitRules.canApplyAll(null, null, null, Noellesroles.DETECTIVE, Set.of(CivilianTraits.INTROVERTED)));

        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.KILLER, Set.of(CivilianTraits.EXTROVERTED)));
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.CORRUPT_COP, Set.of(CivilianTraits.INTROVERTED)));
        assertFalse(TraitRules.canApplyAll(
                null,
                null,
                null,
                WatheRoles.CIVILIAN,
                Set.of(CivilianTraits.EXTROVERTED, ImpostorTrait.ID)
        ));
    }

    @Test
    void socialAndFocusCivilianTraitsExcludeUndercover() {
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.UNDERCOVER, Set.of(CivilianTraits.EXTROVERTED)));
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.UNDERCOVER, Set.of(CivilianTraits.INTROVERTED)));
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.UNDERCOVER, Set.of(CivilianTraits.FOCUS)));
    }

    @Test
    void focusExcludesWathePoliceRoles() {
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.CIVILIAN, Set.of(CivilianTraits.FOCUS)));
        assertTrue(TraitRules.canApplyAll(null, null, null, Noellesroles.DETECTIVE, Set.of(CivilianTraits.FOCUS)));

        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.VIGILANTE, Set.of(CivilianTraits.FOCUS)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.VETERAN, Set.of(CivilianTraits.FOCUS)));
    }

    @Test
    void moneyTreeRequiresVisibleMoneyOrNativeTaskMoney() {
        assertFalse(CivilianTraitService.canSelectMoneyTree(WatheRoles.CIVILIAN, Set.of(), false));
        assertTrue(CivilianTraitService.canSelectMoneyTree(WatheRoles.CIVILIAN, Set.of(), true));
        assertTrue(CivilianTraitService.canSelectMoneyTree(Noellesroles.RECALLER, Set.of(), false));

        assertFalse(CivilianTraitService.canSelectMoneyTree(WatheRoles.KILLER, Set.of(), true));
        assertFalse(CivilianTraitService.canSelectMoneyTree(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID),
                true
        ));
        assertTrue(CivilianTraitService.canSelectMoneyTree(Noellesroles.UNDERCOVER, Set.of(), true));
    }

    @Test
    void extrovertedAndIntrovertedAreMutuallyExclusive() {
        assertTrue(TraitRules.areIncompatible(
                TraitRegistry.get(CivilianTraits.EXTROVERTED),
                TraitRegistry.get(CivilianTraits.INTROVERTED)
        ));
        assertFalse(TraitRules.canApplyAll(
                null,
                null,
                null,
                WatheRoles.CIVILIAN,
                Set.of(CivilianTraits.EXTROVERTED, CivilianTraits.INTROVERTED)
        ));
    }

    @Test
    void socialMoodDrainStopsOnlyAtMatchingCrowdSize() {
        assertTrue(CivilianTraitService.shouldPreventSocialMoodDrain(Set.of(CivilianTraits.EXTROVERTED), 2));
        assertFalse(CivilianTraitService.shouldPreventSocialMoodDrain(Set.of(CivilianTraits.EXTROVERTED), 1));

        assertTrue(CivilianTraitService.shouldPreventSocialMoodDrain(Set.of(CivilianTraits.INTROVERTED), 0));
        assertTrue(CivilianTraitService.shouldPreventSocialMoodDrain(Set.of(CivilianTraits.INTROVERTED), 1));
        assertFalse(CivilianTraitService.shouldPreventSocialMoodDrain(Set.of(CivilianTraits.INTROVERTED), 2));
    }

    @Test
    void socialMoodAdjustmentOnlyCancelsDrain() {
        assertEquals(0.8f, CivilianTraitService.socialMoodAdjustedMood(
                0.8f,
                0.4f,
                Set.of(CivilianTraits.EXTROVERTED),
                2
        ), 0.0001f);
        assertEquals(0.4f, CivilianTraitService.socialMoodAdjustedMood(
                0.8f,
                0.4f,
                Set.of(CivilianTraits.EXTROVERTED),
                1
        ), 0.0001f);
        assertEquals(1.0f, CivilianTraitService.socialMoodAdjustedMood(
                0.8f,
                1.0f,
                Set.of(CivilianTraits.INTROVERTED),
                0
        ), 0.0001f);
    }

    @Test
    void focusPreventsOnlyOrdinaryCivilianGunMoodPenalty() {
        assertTrue(CivilianTraitService.shouldPreventGunMoodPenalty(WatheRoles.CIVILIAN, Set.of(CivilianTraits.FOCUS)));
        assertFalse(CivilianTraitService.shouldPreventGunMoodPenalty(WatheRoles.CIVILIAN, Set.of()));
        assertFalse(CivilianTraitService.shouldPreventGunMoodPenalty(WatheRoles.CIVILIAN, Set.of(CivilianTraits.FOCUS, ImpostorTrait.ID)));
        assertFalse(CivilianTraitService.shouldPreventGunMoodPenalty(WatheRoles.KILLER, Set.of(CivilianTraits.FOCUS)));
        assertFalse(CivilianTraitService.shouldPreventGunMoodPenalty(WatheRoles.VIGILANTE, Set.of(CivilianTraits.FOCUS)));
        assertFalse(CivilianTraitService.shouldPreventGunMoodPenalty(WatheRoles.VETERAN, Set.of(CivilianTraits.FOCUS)));
    }

    @Test
    void moneyTreeRewardTimingIsFixed() {
        assertEquals(5, CivilianTraitService.MONEY_TREE_REWARD);
        assertEquals(600, CivilianTraitService.MONEY_TREE_INTERVAL_TICKS);
    }
}
