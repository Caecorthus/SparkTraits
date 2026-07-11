package dev.caecorthus.sparktraits.component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/** Plans an ordered active-trait replacement without changing live component state. */
final class TraitActiveReplacement {
    private TraitActiveReplacement() {
    }

    static <T> Plan<T> plan(
            Collection<T> currentActive,
            Collection<T> currentRevealed,
            Collection<T> requestedActive
    ) {
        LinkedHashSet<T> current = new LinkedHashSet<>(currentActive);
        LinkedHashSet<T> target = new LinkedHashSet<>(requestedActive);
        LinkedHashSet<T> revealed = new LinkedHashSet<>(currentRevealed);

        List<T> retained = target.stream().filter(current::contains).toList();
        List<T> removed = current.stream().filter(trait -> !target.contains(trait)).toList();
        List<T> missing = target.stream().filter(trait -> !current.contains(trait)).toList();
        List<T> retainedRevealed = retained.stream().filter(revealed::contains).toList();
        return new Plan<>(List.copyOf(target), retained, removed, missing, retainedRevealed);
    }

    record Plan<T>(
            List<T> target,
            List<T> retained,
            List<T> removed,
            List<T> missing,
            List<T> retainedRevealed
    ) {
        Plan {
            target = List.copyOf(target);
            retained = List.copyOf(retained);
            removed = List.copyOf(removed);
            missing = List.copyOf(missing);
            retainedRevealed = List.copyOf(retainedRevealed);
        }
    }
}
