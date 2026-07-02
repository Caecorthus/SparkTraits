package dev.caecorthus.sparktraits.impl;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Compatibility rules for Wathe knife left-click knockback.
 * Wathe 刀左键击退的兼容规则。
 */
public final class KnifeKnockbackService {
    public static final double WATHE_KNIFE_ATTACK_KNOCKBACK = 0.5d;

    private KnifeKnockbackService() {
    }

    public static boolean shouldApplyCooldownBypassKnockback(
            boolean damageSucceeded,
            boolean attackerHoldingKnife,
            boolean attackerPlayingAndAlive,
            boolean targetPlayingAndAlive,
            boolean targetAliveAndSurvival,
            boolean targetInvulnerableToSource,
            int targetTimeUntilRegen,
            float attackDamage,
            float targetLastDamageTaken
    ) {
        return !damageSucceeded
                && attackerHoldingKnife
                && attackerPlayingAndAlive
                && targetPlayingAndAlive
                && targetAliveAndSurvival
                && !targetInvulnerableToSource
                && wasBlockedByVanillaHurtCooldown(targetTimeUntilRegen, attackDamage, targetLastDamageTaken);
    }

    static boolean wasBlockedByVanillaHurtCooldown(
            int targetTimeUntilRegen,
            float attackDamage,
            float targetLastDamageTaken
    ) {
        return targetTimeUntilRegen > 10 && attackDamage <= targetLastDamageTaken;
    }

    public static double cooldownBypassKnockbackStrength(double attackKnockbackAttribute) {
        return Math.max(WATHE_KNIFE_ATTACK_KNOCKBACK, attackKnockbackAttribute) * 0.5d;
    }

    public static Vec3d knockbackDirection(float attackerYaw) {
        float radians = attackerYaw * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(MathHelper.sin(radians), 0.0d, -MathHelper.cos(radians));
    }
}
