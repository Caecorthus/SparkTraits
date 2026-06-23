package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;

/**
 * Resolves crowbar restrictions for Conscience killers.
 * 统一处理善良杀手的撬棍冷却限制。
 */
public final class ConscienceCrowbarService {
    public static final int CONSCIENCE_CROWBAR_COOLDOWN_TICKS = 2 * 60 * 20;

    private ConscienceCrowbarService() {
    }

    public static int crowbarCooldownTicks(Role role, Collection<Identifier> traits, int originalCooldownTicks) {
        if (EffectiveTraitService.isOriginalKiller(role) && EffectiveTraitService.hasConscience(traits)) {
            return CONSCIENCE_CROWBAR_COOLDOWN_TICKS;
        }
        return originalCooldownTicks;
    }

    public static int crowbarCooldownTicks(PlayerEntity player, int originalCooldownTicks) {
        if (player == null) {
            return originalCooldownTicks;
        }
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getWorld());
        return crowbarCooldownTicks(
                gameComponent.getRole(player),
                TraitPlayerComponent.KEY.get(player).getActiveTraitIds(),
                originalCooldownTicks
        );
    }
}
