package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitRoleEligibilityTest {
    @Test
    void sparkWitchRolesCannotReceiveTraits() {
        assertFalse(TraitRoleEligibility.canReceiveTraits(role("grand_witch")));
        assertFalse(TraitRoleEligibility.canReceiveTraits(role("accomplice")));
        assertFalse(TraitRoleEligibility.canReceiveTraits(role("apprentice_witch")));
        assertFalse(TraitRoleEligibility.canReceiveTraits(role("murderous_witch")));
    }

    @Test
    void unrelatedRolesStillReceiveTraits() {
        assertTrue(TraitRoleEligibility.canReceiveTraits(WatheRoles.CIVILIAN));
        assertTrue(TraitRoleEligibility.canReceiveTraits(WatheRoles.KILLER));
        assertTrue(TraitRoleEligibility.canReceiveTraits(Noellesroles.DETECTIVE));
        assertTrue(TraitRoleEligibility.canReceiveTraits(role("other")));
        assertTrue(TraitRoleEligibility.canReceiveTraits(null));
    }

    private static Role role(String path) {
        return new Role(
                Identifier.of("sparkwitch", path),
                0xFFFFFF,
                false,
                false,
                Role.MoodType.FAKE,
                200,
                false
        );
    }
}
