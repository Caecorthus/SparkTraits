package dev.caecorthus.sparktraits.impl.effective.economy;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectiveEconomyRulesTest {
    @Test
    void taskMoneySupplementsFlippedRolesWithoutDoublePayingNativeTaskMoneyRoles() {
        assertEquals(50, EffectiveEconomyRules.TASK_MONEY_REWARD);
        assertTrue(EffectiveEconomyRules.shouldRewardTaskMoney(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertTrue(EffectiveEconomyRules.shouldRewardTaskMoney(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertFalse(EffectiveEconomyRules.shouldRewardTaskMoney(Noellesroles.WAITER, Set.of(ImpostorTrait.ID)));
        assertFalse(EffectiveEconomyRules.shouldRewardTaskMoney(Noellesroles.WAITER, Set.of()));
        assertFalse(EffectiveEconomyRules.shouldRewardTaskMoney(WatheRoles.CIVILIAN, Set.of()));
        assertFalse(EffectiveEconomyRules.shouldRewardTaskMoney(WatheRoles.KILLER, null));
    }

    @Test
    void nativeTaskMoneyRolesMatchNoellesRolesTaskRewardSources() {
        assertTrue(EffectiveEconomyRules.hasNativeTaskMoneyReward(Noellesroles.BARTENDER));
        assertTrue(EffectiveEconomyRules.hasNativeTaskMoneyReward(Noellesroles.RECALLER));
        assertTrue(EffectiveEconomyRules.hasNativeTaskMoneyReward(Noellesroles.TIMEKEEPER));
        assertTrue(EffectiveEconomyRules.hasNativeTaskMoneyReward(Noellesroles.REPORTER));
        assertTrue(EffectiveEconomyRules.hasNativeTaskMoneyReward(Noellesroles.WAITER));
        assertFalse(EffectiveEconomyRules.hasNativeTaskMoneyReward(WatheRoles.CIVILIAN));
        assertFalse(EffectiveEconomyRules.hasNativeTaskMoneyReward(WatheRoles.KILLER));
        assertFalse(EffectiveEconomyRules.hasNativeTaskMoneyReward(null));
    }

    @Test
    void partyAnimalTargetRewardNeverBlocksAnyTargetReward() {
        assertFalse(EffectiveEconomyRules.shouldBlockPartyAnimalTargetReward(WatheRoles.CIVILIAN, Set.of()));
        assertFalse(EffectiveEconomyRules.shouldBlockPartyAnimalTargetReward(WatheRoles.KILLER, Set.of()));
        assertFalse(EffectiveEconomyRules.shouldBlockPartyAnimalTargetReward(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertFalse(EffectiveEconomyRules.shouldBlockPartyAnimalTargetReward(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertFalse(EffectiveEconomyRules.shouldBlockPartyAnimalTargetReward(Noellesroles.UNDERCOVER, Set.of()));
        assertFalse(EffectiveEconomyRules.shouldBlockPartyAnimalTargetReward(null, null));
    }

    @Test
    void conscienceGetsKillRewardOnlyForNonCivilianVictims() {
        assertFalse(EffectiveEconomyRules.shouldRewardConscienceKill(null, Set.of()));
        assertFalse(EffectiveEconomyRules.shouldRewardConscienceKill(WatheRoles.CIVILIAN, Set.of()));
        assertFalse(EffectiveEconomyRules.shouldRewardConscienceKill(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertTrue(EffectiveEconomyRules.shouldRewardConscienceKill(WatheRoles.KILLER, Set.of()));
        assertTrue(EffectiveEconomyRules.shouldRewardConscienceKill(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
    }

    @Test
    void impostorGetsFullKillRewardForRewardableVictimsOnlyWhenShopIsAvailable() {
        assertTrue(EffectiveEconomyRules.shouldRewardImpostorKill(WatheRoles.CIVILIAN, Set.of()));
        assertTrue(EffectiveEconomyRules.shouldRewardImpostorKill(Noellesroles.JESTER, Set.of()));
        assertTrue(EffectiveEconomyRules.shouldRewardImpostorKill(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertFalse(EffectiveEconomyRules.shouldRewardImpostorKill(WatheRoles.KILLER, Set.of()));
        assertFalse(EffectiveEconomyRules.shouldRewardImpostorKill(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertFalse(EffectiveEconomyRules.shouldRewardImpostorKill(WatheRoles.NO_ROLE, Set.of()));
        assertFalse(EffectiveEconomyRules.shouldRewardImpostorKill(null, Set.of()));

        assertEquals(GameConstants.MONEY_PER_KILL,
                EffectiveEconomyRules.impostorKillReward(WatheRoles.CIVILIAN, Set.of(), true));
        assertEquals(GameConstants.MONEY_PER_KILL,
                EffectiveEconomyRules.impostorKillReward(Noellesroles.JESTER, Set.of(), true));
        assertEquals(GameConstants.MONEY_PER_KILL,
                EffectiveEconomyRules.impostorKillReward(WatheRoles.KILLER, Set.of(ConscienceTrait.ID), true));
        assertEquals(0,
                EffectiveEconomyRules.impostorKillReward(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID), true));
        assertEquals(0,
                EffectiveEconomyRules.impostorKillReward(WatheRoles.CIVILIAN, Set.of(), false));
    }

    @Test
    void originalKillerRewardExcludesConscienceKillers() {
        assertTrue(EffectiveEconomyRules.shouldReceiveOriginalKillerReward(true, false));
        assertFalse(EffectiveEconomyRules.shouldReceiveOriginalKillerReward(false, false));
        assertFalse(EffectiveEconomyRules.shouldReceiveOriginalKillerReward(true, true));
        assertFalse(EffectiveEconomyRules.shouldReceiveOriginalKillerReward(false, true));
    }

    @Test
    void killerPassiveMoneyKeepsConscienceSerialKillerException() {
        assertTrue(EffectiveEconomyRules.shouldReceiveKillerPassiveMoney(true, false, false, false));
        assertFalse(EffectiveEconomyRules.shouldReceiveKillerPassiveMoney(false, false, false, true));
        assertFalse(EffectiveEconomyRules.shouldReceiveKillerPassiveMoney(true, true, false, true));
        assertFalse(EffectiveEconomyRules.shouldReceiveKillerPassiveMoney(true, true, true, false));
        assertTrue(EffectiveEconomyRules.shouldReceiveKillerPassiveMoney(true, true, true, true));
    }
}
