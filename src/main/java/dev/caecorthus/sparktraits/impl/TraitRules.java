package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import net.minecraft.util.Identifier;

import java.util.Collection;

public final class TraitRules {
    private TraitRules() {
    }

    public static boolean isCompatibleWithAll(Trait candidate, Collection<Identifier> selectedTraitIds) {
        for (Identifier selectedTraitId : selectedTraitIds) {
            Trait selected = TraitRegistry.get(selectedTraitId);
            if (selected != null && areIncompatible(candidate, selected)) {
                return false;
            }
        }
        return true;
    }

    public static boolean areIncompatible(Trait first, Trait second) {
        return first.incompatibleTraits().contains(second.id()) || second.incompatibleTraits().contains(first.id());
    }
}

