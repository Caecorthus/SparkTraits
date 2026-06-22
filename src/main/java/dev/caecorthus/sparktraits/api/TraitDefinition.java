package dev.caecorthus.sparktraits.api;

import net.minecraft.util.Identifier;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Data-first trait implementation for simple passive traits.
 * 面向普通被动天赋的数据式实现；复杂天赋可以直接实现 {@link Trait}。
 */
public record TraitDefinition(
        Identifier id,
        int color,
        int weight,
        boolean uniquePerGame,
        boolean hiddenFromOwnerAtStart,
        TraitAudience audience,
        Set<Identifier> incompatibleTraits,
        Predicate<TraitSelectionContext> predicate
) implements Trait {
    public TraitDefinition {
        incompatibleTraits = Set.copyOf(incompatibleTraits);
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return audience.canApply(context) && predicate.test(context);
    }

    public static Builder builder(Identifier id, int color) {
        return new Builder(id, color);
    }

    public static final class Builder {
        private final Identifier id;
        private final int color;
        private int weight = 100;
        private boolean uniquePerGame;
        private boolean hiddenFromOwnerAtStart;
        private TraitAudience audience = TraitAudience.UNIVERSAL;
        private final Set<Identifier> incompatibleTraits = new LinkedHashSet<>();
        private Predicate<TraitSelectionContext> predicate = context -> true;

        private Builder(Identifier id, int color) {
            this.id = id;
            this.color = color;
        }

        public Builder weight(int weight) {
            this.weight = Math.max(0, weight);
            return this;
        }

        public Builder uniquePerGame() {
            this.uniquePerGame = true;
            return this;
        }

        public Builder hiddenFromOwnerAtStart() {
            this.hiddenFromOwnerAtStart = true;
            return this;
        }

        public Builder audience(TraitAudience audience) {
            this.audience = audience;
            return this;
        }

        public Builder incompatibleWith(Identifier traitId) {
            this.incompatibleTraits.add(traitId);
            return this;
        }

        public Builder predicate(Predicate<TraitSelectionContext> predicate) {
            this.predicate = predicate;
            return this;
        }

        public TraitDefinition build() {
            return new TraitDefinition(
                    id,
                    color,
                    weight,
                    uniquePerGame,
                    hiddenFromOwnerAtStart,
                    audience,
                    incompatibleTraits,
                    predicate
            );
        }
    }
}
