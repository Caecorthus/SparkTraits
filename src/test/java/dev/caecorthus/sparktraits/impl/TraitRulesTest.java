package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.doctor4t.wathe.api.Role;
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

    @Test
    void fullSetValidationRechecksPreviouslyAcceptedTraits() {
        Identifier needsNoBlocker = Identifier.of("sparktraits_test", "needs_no_blocker");
        Identifier blocker = Identifier.of("sparktraits_test", "blocker");
        registerIfAbsent(trait(needsNoBlocker, Set.of(), context -> !context.hasSelectedTrait(blocker)));
        registerIfAbsent(trait(blocker));

        assertTrue(TraitRules.canApplyAll(null, null, null, null, Set.of(needsNoBlocker)));
        assertFalse(TraitRules.canApplyAll(null, null, null, null, Set.of(needsNoBlocker, blocker)));
    }

    @Test
    void blockedSparkWitchRolesCannotApplyAnyTraits() {
        Identifier quiet = Identifier.of("sparktraits_test", "blocked_role_quiet");
        registerIfAbsent(trait(quiet));

        assertFalse(TraitRules.canApplyAll(null, null, null, sparkWitchRole("grand_witch"), Set.of(quiet)));
        assertTrue(TraitRules.canApplyAll(null, null, null, sparkWitchRole("grand_witch"), Set.of()));
        assertTrue(TraitRules.canApplyAll(null, null, null, sparkWitchRole("other"), Set.of(quiet)));
    }

    private static Trait trait(Identifier id, Identifier... incompatibleTraits) {
        return trait(id, Set.of(incompatibleTraits), context -> true);
    }

    private static Trait trait(Identifier id, Set<Identifier> incompatibleTraits, Rule rule) {
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
                return incompatibleTraits;
            }

            @Override
            public boolean canApply(TraitSelectionContext context) {
                return rule.canApply(context);
            }
        };
    }

    private static void registerIfAbsent(Trait trait) {
        if (!TraitRegistry.contains(trait.id())) {
            TraitRegistry.register(trait);
        }
    }

    private static Role sparkWitchRole(String path) {
        return new Role(
                Identifier.of("sparkwitch", path),
                0xFFFFFF,
                false,
                false,
                Role.MoodType.FAKE,
                200,
                false
        );
    }

    private interface Rule {
        boolean canApply(TraitSelectionContext context);
    }
}
