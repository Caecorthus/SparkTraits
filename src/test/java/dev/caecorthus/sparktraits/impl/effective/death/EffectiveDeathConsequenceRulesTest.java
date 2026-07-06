package dev.caecorthus.sparktraits.impl.effective.death;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectiveDeathConsequenceRulesTest {
    @AfterEach
    void clearPoisonSources() {
        EffectiveDeathConsequenceRules.clearPoisonSources();
    }

    @Test
    void jesterMomentTriggersOnlyForUnflippedOriginalInnocents() {
        assertTrue(EffectiveDeathConsequenceRules.shouldTriggerJesterMoment(WatheRoles.CIVILIAN, Set.of()));
        assertFalse(EffectiveDeathConsequenceRules.shouldTriggerJesterMoment(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertFalse(EffectiveDeathConsequenceRules.shouldTriggerJesterMoment(WatheRoles.KILLER, Set.of()));
        assertFalse(EffectiveDeathConsequenceRules.shouldTriggerJesterMoment(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
    }

    @Test
    void conscienceKillPunishmentIgnoresOnlyGrenadeAndGasBombPoisonAreaDamage() {
        assertTrue(EffectiveDeathConsequenceRules.shouldPunishConscienceKill(true, GameConstants.DeathReasons.GUN));
        assertFalse(EffectiveDeathConsequenceRules.shouldPunishConscienceKill(true, GameConstants.DeathReasons.GRENADE));
        assertFalse(EffectiveDeathConsequenceRules.shouldPunishConscienceKill(
                true,
                GameConstants.DeathReasons.POISON,
                Noellesroles.POISON_SOURCE_GAS_BOMB
        ));
        assertTrue(EffectiveDeathConsequenceRules.shouldPunishConscienceKill(
                true,
                GameConstants.DeathReasons.POISON,
                Noellesroles.POISON_SOURCE_NEEDLE
        ));
        assertFalse(EffectiveDeathConsequenceRules.shouldPunishConscienceKill(false, GameConstants.DeathReasons.GUN));
    }

    @Test
    void poisonSourcesAreConsumedOncePerVictim() {
        UUID victim = UUID.randomUUID();
        EffectiveDeathConsequenceRules.rememberPoisonSource(victim, Noellesroles.POISON_SOURCE_GAS_BOMB);

        Identifier firstConsume = EffectiveDeathConsequenceRules.consumePoisonSource(victim);

        assertEquals(Noellesroles.POISON_SOURCE_GAS_BOMB, firstConsume);
        assertNull(EffectiveDeathConsequenceRules.consumePoisonSource(victim));
    }

    @Test
    void killRewardsUseEffectiveVictimAlignment() {
        assertFalse(EffectiveDeathConsequenceRules.shouldRewardConscienceKill(WatheRoles.CIVILIAN, Set.of()));
        assertTrue(EffectiveDeathConsequenceRules.shouldRewardConscienceKill(WatheRoles.KILLER, Set.of()));
        assertTrue(EffectiveDeathConsequenceRules.shouldRewardConscienceKill(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));

        assertEquals(GameConstants.MONEY_PER_KILL,
                EffectiveDeathConsequenceRules.impostorKillReward(WatheRoles.CIVILIAN, Set.of(), true));
        assertEquals(GameConstants.MONEY_PER_KILL,
                EffectiveDeathConsequenceRules.impostorKillReward(Noellesroles.JESTER, Set.of(), true));
        assertEquals(GameConstants.MONEY_PER_KILL,
                EffectiveDeathConsequenceRules.impostorKillReward(WatheRoles.KILLER, Set.of(ConscienceTrait.ID), true));
        assertEquals(0,
                EffectiveDeathConsequenceRules.impostorKillReward(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID), true));
        assertEquals(0,
                EffectiveDeathConsequenceRules.impostorKillReward(WatheRoles.NO_ROLE, Set.of(), true));
        assertEquals(0,
                EffectiveDeathConsequenceRules.impostorKillReward(WatheRoles.CIVILIAN, Set.of(), false));
    }
}
