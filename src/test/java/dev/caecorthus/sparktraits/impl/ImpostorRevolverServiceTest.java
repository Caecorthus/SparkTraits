package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.WatheRoles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpostorRevolverServiceTest {
    @Test
    void onlyImpostorsCanBuyRevolverFromShop() {
        assertTrue(ImpostorRevolverService.shouldAddRevolverToShop(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID)
        ));
        assertFalse(ImpostorRevolverService.shouldAddRevolverToShop(
                WatheRoles.CIVILIAN,
                Set.of()
        ));
    }

    @Test
    void impostorRevolverCostsOneHundredFifty() {
        assertEquals(150, ImpostorRevolverService.REVOLVER_PRICE);
        assertEquals("sparktraits:impostor_revolver", ImpostorRevolverService.REVOLVER_SHOP_ID.toString());
    }

    @Test
    void impostorsCanBuyRevolversButCannotPickUpGroundGuns() {
        assertTrue(ImpostorRevolverService.shouldAddRevolverToShop(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID)
        ));
        assertTrue(ImpostorRevolverService.shouldBlockNonShopGunAcquisition(
                Set.of(ImpostorTrait.ID),
                true
        ));
        assertFalse(ImpostorRevolverService.shouldBlockNonShopGunAcquisition(
                Set.of(),
                true
        ));
        assertFalse(ImpostorRevolverService.shouldBlockNonShopGunAcquisition(
                Set.of(ImpostorTrait.ID),
                false
        ));
        assertTrue(ImpostorRevolverService.shouldBlockGroundGunPickup(
                Set.of(ImpostorTrait.ID),
                true
        ));
        assertFalse(ImpostorRevolverService.shouldBlockGroundGunPickup(
                Set.of(),
                true
        ));
        assertFalse(ImpostorRevolverService.shouldBlockGroundGunPickup(
                Set.of(ImpostorTrait.ID),
                false
        ));
    }
}
