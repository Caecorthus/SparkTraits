package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.WatheRoles;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleEnhancementServiceTest {
    @Test
    void detectiveAndToxicologistGetTaskMoneyAndMoneyHud() {
        assertTrue(RoleEnhancementService.shouldRewardTaskMoney(Noellesroles.DETECTIVE));
        assertTrue(RoleEnhancementService.shouldRewardTaskMoney(Noellesroles.TOXICOLOGIST));
        assertTrue(RoleEnhancementService.shouldSeeMoney(Noellesroles.DETECTIVE, true));
        assertTrue(RoleEnhancementService.shouldSeeMoney(Noellesroles.TOXICOLOGIST, true));
    }

    @Test
    void otherRolesDoNotGetTheNewEconomy() {
        assertFalse(RoleEnhancementService.shouldRewardTaskMoney(Noellesroles.ATTENDANT));
        assertFalse(RoleEnhancementService.shouldRewardTaskMoney(Noellesroles.CONDUCTOR));
        assertFalse(RoleEnhancementService.shouldRewardTaskMoney(WatheRoles.CIVILIAN));
        assertFalse(RoleEnhancementService.shouldSeeMoney(Noellesroles.ATTENDANT, true));
        assertFalse(RoleEnhancementService.shouldSeeMoney(Noellesroles.DETECTIVE, false));
    }

    @Test
    void onlyToxicologistGetsCapsuleShopEntry() {
        assertTrue(RoleEnhancementService.shouldAddCapsuleShopEntry(Noellesroles.TOXICOLOGIST));
        assertFalse(RoleEnhancementService.shouldAddCapsuleShopEntry(Noellesroles.DETECTIVE));
        assertFalse(RoleEnhancementService.shouldAddCapsuleShopEntry(Noellesroles.ATTENDANT));
    }
}
