package dev.caecorthus.sparktraits.impl;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnifeKnockbackServiceTest {
    @Test
    void knifeCooldownBypassAppliesOnlyWhenVanillaHurtCooldownSwallowsTheHit() {
        assertTrue(KnifeKnockbackService.shouldApplyCooldownBypassKnockback(
                false,
                true,
                true,
                true,
                true,
                false,
                20,
                1.0f,
                1.0f
        ));
        assertTrue(KnifeKnockbackService.shouldApplyCooldownBypassKnockback(
                false,
                true,
                true,
                true,
                true,
                false,
                11,
                0.2f,
                1.0f
        ));
    }

    @Test
    void knifeCooldownBypassDoesNotReplaceRealDamageOrProtectedStates() {
        assertFalse(KnifeKnockbackService.shouldApplyCooldownBypassKnockback(
                true, true, true, true, true, false, 20, 1.0f, 1.0f
        ));
        assertFalse(KnifeKnockbackService.shouldApplyCooldownBypassKnockback(
                false, false, true, true, true, false, 20, 1.0f, 1.0f
        ));
        assertFalse(KnifeKnockbackService.shouldApplyCooldownBypassKnockback(
                false, true, false, true, true, false, 20, 1.0f, 1.0f
        ));
        assertFalse(KnifeKnockbackService.shouldApplyCooldownBypassKnockback(
                false, true, true, false, true, false, 20, 1.0f, 1.0f
        ));
        assertFalse(KnifeKnockbackService.shouldApplyCooldownBypassKnockback(
                false, true, true, true, false, false, 20, 1.0f, 1.0f
        ));
        assertFalse(KnifeKnockbackService.shouldApplyCooldownBypassKnockback(
                false, true, true, true, true, true, 20, 1.0f, 1.0f
        ));
    }

    @Test
    void knifeCooldownBypassLeavesNonCooldownFailuresToVanilla() {
        assertFalse(KnifeKnockbackService.shouldApplyCooldownBypassKnockback(
                false,
                true,
                true,
                true,
                true,
                false,
                10,
                1.0f,
                1.0f
        ));
        assertFalse(KnifeKnockbackService.shouldApplyCooldownBypassKnockback(
                false,
                true,
                true,
                true,
                true,
                false,
                20,
                2.0f,
                1.0f
        ));
    }

    @Test
    void cooldownBypassUsesWatheKnifeKnockbackAsTheFloor() {
        assertEquals(0.25d, KnifeKnockbackService.cooldownBypassKnockbackStrength(0.0d), 0.0001d);
        assertEquals(0.25d, KnifeKnockbackService.cooldownBypassKnockbackStrength(0.5d), 0.0001d);
        assertEquals(0.375d, KnifeKnockbackService.cooldownBypassKnockbackStrength(0.75d), 0.0001d);
    }

    @Test
    void knockbackDirectionMatchesVanillaAttackYaw() {
        Vec3d north = KnifeKnockbackService.knockbackDirection(0.0f);
        Vec3d east = KnifeKnockbackService.knockbackDirection(90.0f);

        assertEquals(0.0d, north.x, 0.0001d);
        assertEquals(-1.0d, north.z, 0.0001d);
        assertEquals(1.0d, east.x, 0.0001d);
        assertEquals(0.0d, east.z, 0.0001d);
    }
}
