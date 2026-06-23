package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.api.WatheRoles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilencedKillerRestrictionServiceTest {
    @Test
    void silencedEffectiveKillersAreRestricted() {
        assertTrue(SilencedKillerRestrictionService.shouldRestrict(
                true,
                WatheRoles.KILLER,
                Set.of()
        ));
        assertTrue(SilencedKillerRestrictionService.shouldRestrict(
                true,
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID)
        ));
    }

    @Test
    void silencedConscienceKillersAreNotRestricted() {
        assertFalse(SilencedKillerRestrictionService.shouldRestrict(
                true,
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID)
        ));
    }

    @Test
    void unsilencedEffectiveKillersAreNotRestricted() {
        assertFalse(SilencedKillerRestrictionService.shouldRestrict(
                false,
                WatheRoles.KILLER,
                Set.of()
        ));
        assertFalse(SilencedKillerRestrictionService.shouldRestrict(
                false,
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID)
        ));
    }

    @Test
    void restrictedShopPurchasesAreDeniedWithSilenceReason() {
        ShopPurchase.PurchaseResult result = SilencedKillerRestrictionService.shopPurchaseResult(true);

        assertFalse(result.allowed());
        assertEquals(SilencedKillerRestrictionService.SHOP_DENY_KEY, result.denyReason());
    }

    @Test
    void unrestrictedShopPurchasesDeferToOtherRules() {
        assertNull(SilencedKillerRestrictionService.shopPurchaseResult(false));
    }
}
