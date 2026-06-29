package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

/**
 * Rules and runtime helpers for Corrupt Cop-only SparkTraits.
 * SparkTraits 黑警专属天赋的规则与运行时辅助入口。
 */
public final class CorruptCopTraitService {
    public static final int ARROGANT_ASF_COLOR = 0x193264;
    public static final float ARROGANT_ASF_HORIZONTAL_SPEED_MULTIPLIER = 3.0f;

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

    public static float horizontalMovementSpeed(
            float original,
            boolean hasArrogantAsf,
            boolean arrogantAsfActive,
            boolean aliveSurvival
    ) {
        if (!hasArrogantAsf || !arrogantAsfActive || !aliveSurvival) {
            return original;
        }
        return original * ARROGANT_ASF_HORIZONTAL_SPEED_MULTIPLIER;
    }

    public static float horizontalMovementSpeed(PlayerEntity player, float original) {
        TraitPlayerComponent traits = player == null ? null : TraitPlayerComponent.KEY.get(player);
        return horizontalMovementSpeed(
                original,
                traits != null && traits.hasActiveTrait(ArrogantAsfTrait.ID),
                traits != null && traits.isArrogantAsfActive(),
                player != null && GameFunctions.isPlayerAliveAndSurvival(player)
        );
    }
}
