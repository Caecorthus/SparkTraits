package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

/**
 * Rules and runtime helpers for Corrupt Cop-only SparkTraits.
 * SparkTraits 黑警专属天赋的规则与运行时辅助入口。
 */
public final class CorruptCopTraitService {
    public static final int ARROGANT_ASF_COLOR = 0x193264;
    public static final float ARROGANT_ASF_LATERAL_SPEED_MULTIPLIER = 1.75f;
    private static final double NORMALIZED_MOVEMENT_INPUT_EPSILON = 1.0E-7d;

    private CorruptCopTraitService() {
    }

    public static boolean canSelectArrogantAsf(Role role) {
        return role != null && role.identifier().equals(Noellesroles.CORRUPT_COP_ID);
    }

    public static boolean nextArrogantAsfActive(boolean hasArrogantAsf, boolean currentlyActive) {
        return hasArrogantAsf && !currentlyActive;
    }

    public static boolean toggleArrogantAsfAbility(ServerPlayerEntity player) {
        if (player == null || !GameFunctions.isPlayerAliveAndSurvival(player)
                || SwallowedPlayerComponent.isPlayerSwallowed(player)) {
            return false;
        }
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        if (!traits.hasActiveTrait(ArrogantAsfTrait.ID)) {
            return false;
        }
        traits.setArrogantAsfActive(nextArrogantAsfActive(true, traits.isArrogantAsfActive()));
        return true;
    }

    public static Vec3d arrogantAsfLateralVelocityBonus(PlayerEntity player, Vec3d movementInput, float speed) {
        TraitPlayerComponent traits = player == null ? null : TraitPlayerComponent.KEY.get(player);
        return arrogantAsfLateralVelocityBonus(
                movementInput,
                speed,
                player == null ? 0.0f : player.getYaw(),
                traits != null && traits.hasActiveTrait(ArrogantAsfTrait.ID),
                traits != null && traits.isArrogantAsfActive(),
                player != null && GameFunctions.isPlayerAliveAndSurvival(player)
        );
    }

    public static Vec3d arrogantAsfLateralVelocityBonus(
            Vec3d movementInput,
            float speed,
            float yaw,
            boolean hasArrogantAsf,
            boolean arrogantAsfActive,
            boolean aliveSurvival
    ) {
        if (!hasArrogantAsf || !arrogantAsfActive || !aliveSurvival || movementInput == null) {
            return Vec3d.ZERO;
        }

        double lengthSquared = movementInput.lengthSquared();
        if (lengthSquared < NORMALIZED_MOVEMENT_INPUT_EPSILON) {
            return Vec3d.ZERO;
        }

        Vec3d normalizedInput = lengthSquared > 1.0d ? movementInput.normalize() : movementInput;
        double lateralInput = normalizedInput.x;
        if (Math.abs(lateralInput) < NORMALIZED_MOVEMENT_INPUT_EPSILON) {
            return Vec3d.ZERO;
        }

        // English: Vanilla already applied one lateral share; add the remaining multiplier after normalization.
        // 中文：原版已经应用了一份横移速度；归一化之后只补上剩余倍率。
        double bonusSpeed = lateralInput * speed * (ARROGANT_ASF_LATERAL_SPEED_MULTIPLIER - 1.0d);
        float radians = yaw * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(
                bonusSpeed * MathHelper.cos(radians),
                0.0d,
                bonusSpeed * MathHelper.sin(radians)
        );
    }
}
