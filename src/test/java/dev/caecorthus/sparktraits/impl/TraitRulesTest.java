package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.api.Trait;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitRulesTest {
    @Test
    void incompatibilityIsBidirectional() {
        Identifier firstId = Identifier.of("sparktraits_test", "first");
        Identifier secondId = Identifier.of("sparktraits_test", "second");

        Trait first = trait(firstId, secondId);
        Trait second = trait(secondId);

        assertTrue(TraitRules.areIncompatible(first, second));
        assertTrue(TraitRules.areIncompatible(second, first));
    }

    @Test
    void unrelatedTraitsRemainCompatible() {
        Trait first = trait(Identifier.of("sparktraits_test", "quiet"));
        Trait second = trait(Identifier.of("sparktraits_test", "steady"));

        assertFalse(TraitRules.areIncompatible(first, second));
    }

    private static Trait trait(Identifier id, Identifier... incompatibleTraits) {
        return new Trait() {
            @Override
            public Identifier id() {
                return id;
            }

            @Override
            public int color() {
                return 0xFFFFFF;
            }

            @Override
            public Set<Identifier> incompatibleTraits() {
                return Set.of(incompatibleTraits);
            }
        };
    }
}
