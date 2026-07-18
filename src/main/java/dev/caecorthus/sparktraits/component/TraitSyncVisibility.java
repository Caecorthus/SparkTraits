package dev.caecorthus.sparktraits.component;

import java.util.Collection;
import java.util.List;

/**
 * Selects trait identifiers that a sync recipient may inspect.
 * 选择同步接收者可以查看的天赋标识。
 */
final class TraitSyncVisibility {
    private TraitSyncVisibility() {
    }

    static <T> Collection<T> revealedTraitsFor(
            boolean owner,
            boolean spectator,
            Collection<T> activeTraits,
            Collection<T> revealedTraits
    ) {
        if (spectator) {
            return activeTraits;
        }
        if (owner) {
            return revealedTraits;
        }
        return List.of();
    }
}
