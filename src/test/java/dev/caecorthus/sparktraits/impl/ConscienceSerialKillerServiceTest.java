package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConscienceSerialKillerServiceTest {
    @Test
    void regularTargetPoolUsesEffectiveCivilianAlignment() {
        assertTrue(ConscienceSerialKillerService.canBeSerialKillerTarget(
                WatheRoles.CIVILIAN,
                Set.of(),
                false,
                true,
                false
        ));
        assertTrue(ConscienceSerialKillerService.canBeSerialKillerTarget(
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID),
                false,
                true,
                false
        ));
        assertFalse(ConscienceSerialKillerService.canBeSerialKillerTarget(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID),
                false,
                true,
                false
        ));
        assertFalse(ConscienceSerialKillerService.canBeSerialKillerTarget(
                WatheRoles.KILLER,
                Set.of(),
                false,
                true,
                false
        ));
    }

    @Test
    void protectedTargetPoolUsesEffectiveCivilianAlignment() {
        assertTrue(ConscienceSerialKillerService.canBeProtectedTarget(
                WatheRoles.CIVILIAN,
                Set.of(),
                false,
                true,
                false
        ));
        assertTrue(ConscienceSerialKillerService.canBeProtectedTarget(
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID),
                false,
                true,
                false
        ));
        assertFalse(ConscienceSerialKillerService.canBeProtectedTarget(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID),
                false,
                true,
                false
        ));
        assertFalse(ConscienceSerialKillerService.canBeProtectedTarget(
                Noellesroles.JESTER,
                Set.of(),
                false,
                true,
                false
        ));
    }

    @Test
    void protectedTargetPoolKeepsNoellesRolesHardExclusions() {
        assertFalse(ConscienceSerialKillerService.canBeProtectedTarget(
                Noellesroles.UNDERCOVER,
                Set.of(),
                false,
                true,
                false
        ));
        assertFalse(ConscienceSerialKillerService.canBeProtectedTarget(
                Noellesroles.BODYGUARD,
                Set.of(),
                false,
                true,
                false
        ));
        assertFalse(ConscienceSerialKillerService.canBeProtectedTarget(
                Noellesroles.SURVIVAL_MASTER,
                Set.of(),
                false,
                true,
                false
        ));
        assertFalse(ConscienceSerialKillerService.canBeProtectedTarget(
                WatheRoles.CIVILIAN,
                Set.of(),
                true,
                true,
                false
        ));
        assertFalse(ConscienceSerialKillerService.canBeProtectedTarget(
                WatheRoles.CIVILIAN,
                Set.of(),
                false,
                false,
                false
        ));
        assertFalse(ConscienceSerialKillerService.canBeProtectedTarget(
                WatheRoles.CIVILIAN,
                Set.of(),
                false,
                true,
                true
        ));
    }

    @Test
    void conscienceSerialKillerKeepsOneProtectedTarget() {
        assertTrue(ConscienceSerialKillerService.shouldBlockSerialKillerRetarget(true));
        assertFalse(ConscienceSerialKillerService.shouldBlockSerialKillerRetarget(false));
    }

    @Test
    void passiveMoneyRequiresLivingProtectedTarget() {
        assertTrue(ConscienceSerialKillerService.shouldReceivePassiveMoney(true, true));
        assertFalse(ConscienceSerialKillerService.shouldReceivePassiveMoney(true, false));
        assertFalse(ConscienceSerialKillerService.shouldReceivePassiveMoney(false, true));
    }

    @Test
    void protectedTargetKeepsSerialKillerInstinctHighlight() {
        assertTrue(ConscienceSerialKillerService.shouldUseSerialKillerTargetHighlight(true, true));
        assertFalse(ConscienceSerialKillerService.shouldUseSerialKillerTargetHighlight(true, false));
        assertFalse(ConscienceSerialKillerService.shouldUseSerialKillerTargetHighlight(false, true));
    }

    @Test
    void conscienceSerialKillerRewardsOverrideRegularConscienceReward() {
        assertEquals(GameConstants.MONEY_PER_KILL, ConscienceSerialKillerService.conscienceKillReward(false, true, false));
        assertEquals(ConscienceSerialKillerService.CONSCIENCE_SERIAL_KILLER_REWARD,
                ConscienceSerialKillerService.conscienceKillReward(true, true, false));
        assertEquals(ConscienceSerialKillerService.TARGET_MURDERER_REWARD,
                ConscienceSerialKillerService.conscienceKillReward(true, true, true));
        assertEquals(0, ConscienceSerialKillerService.conscienceKillReward(true, false, true));
    }

    @Test
    void targetMurdererRewardStillRequiresNonCivilianVictim() {
        assertFalse(ConscienceSerialKillerService.shouldRewardTargetMurderer(true, false));
        assertTrue(ConscienceSerialKillerService.shouldRewardTargetMurderer(true, true));
        assertFalse(ConscienceSerialKillerService.shouldRewardTargetMurderer(false, true));
    }

    @Test
    void psychoModePriceOnlyChangesForConscienceSerialKiller() {
        assertEquals(ConscienceSerialKillerService.CONSCIENCE_SERIAL_KILLER_PSYCHO_PRICE,
                ConscienceSerialKillerService.psychoModePrice(true, 350));
        assertEquals(350, ConscienceSerialKillerService.psychoModePrice(false, 350));
    }

    @Test
    void murdererRoleClueIsOwnerFacingRoleOnly() {
        Identifier roleId = Noellesroles.SILENCER_ID;
        assertEquals(roleId, ConscienceSerialKillerService.murdererRoleClue(roleId));
    }
}
