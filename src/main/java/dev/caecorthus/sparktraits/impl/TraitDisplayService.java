package dev.caecorthus.sparktraits.impl;

import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

/**
 * Shared trait-display rules for spectator HUD paths.
 * 旁观者 HUD 路径共用的词条显示规则。
 */
public final class TraitDisplayService {
    private TraitDisplayService() {
    }

    public static List<Identifier> spectatorPlayerTraits(
            boolean spectator,
            boolean targetDead,
            Collection<Identifier> activeTraits,
            Collection<Identifier> deathTraits
    ) {
        if (!spectator) {
            return List.of();
        }
        if (activeTraits != null && !activeTraits.isEmpty()) {
            return List.copyOf(activeTraits);
        }
        if (targetDead && deathTraits != null && !deathTraits.isEmpty()) {
            return List.copyOf(deathTraits);
        }
        return List.of();
    }
}
