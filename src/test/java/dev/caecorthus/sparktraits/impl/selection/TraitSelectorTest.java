package dev.caecorthus.sparktraits.impl.selection;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitDefinition;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitSelectorTest {
    private static final Identifier KILLER = Identifier.of("test", "compensation_killer");
    private static final Identifier CIVILIAN = Identifier.of("test", "compensation_civilian");
    private static final Identifier UNIQUE = Identifier.of("test", "compensation_unique");
    private static final Identifier INCOMPATIBLE = Identifier.of("test", "compensation_incompatible");
    private static final Identifier DISABLED = Identifier.of("test", "compensation_disabled");

    @BeforeAll
    static void registerCompensationTraits() {
        registerIfAbsent(TraitDefinition.builder(KILLER, 0)
                .audience(TraitAudience.KILLER_ONLY)
                .build());
        registerIfAbsent(TraitDefinition.builder(CIVILIAN, 0)
                .audience(TraitAudience.INNOCENT_ONLY)
                .build());
        registerIfAbsent(TraitDefinition.builder(UNIQUE, 0)
                .audience(TraitAudience.KILLER_ONLY)
                .uniquePerGame()
                .build());
        registerIfAbsent(TraitDefinition.builder(INCOMPATIBLE, 0)
                .audience(TraitAudience.KILLER_ONLY)
                .incompatibleWith(KILLER)
                .build());
        registerIfAbsent(TraitDefinition.builder(DISABLED, 0)
                .audience(TraitAudience.KILLER_ONLY)
                .build());
        registerIfAbsent(new ConscienceTrait());
    }

    @Test
    void compensationCandidatesUseFinalKillerRoleAndExcludeConscience() {
        TraitSelectionContext context = new TraitSelectionContext(
                null,
                null,
                null,
                WatheRoles.KILLER,
                Set.of(),
                24,
                true
        );

        List<Trait> candidates = TraitSelector.collectEligibleCandidates(
                List.of(
                        TraitRegistry.get(KILLER),
                        TraitRegistry.get(CIVILIAN),
                        TraitRegistry.get(ConscienceTrait.ID)
                ),
                context,
                Set.of(),
                Set.of(),
                Set.of(ConscienceTrait.ID),
                traitId -> true,
                traitId -> false
        );

        assertEquals(List.of(KILLER), candidates.stream().map(Trait::id).toList());
    }

    @Test
    void compensationCandidatesRespectDisabledUniqueAndIncompatibilityRules() {
        TraitSelectionContext context = new TraitSelectionContext(
                null,
                null,
                null,
                WatheRoles.KILLER,
                Set.of(KILLER),
                24,
                true
        );

        List<Trait> candidates = TraitSelector.collectEligibleCandidates(
                List.of(
                        TraitRegistry.get(UNIQUE),
                        TraitRegistry.get(INCOMPATIBLE),
                        TraitRegistry.get(CIVILIAN),
                        TraitRegistry.get(DISABLED)
                ),
                context,
                Set.of(KILLER),
                Set.of(UNIQUE),
                Set.of(),
                traitId -> !DISABLED.equals(traitId),
                traitId -> false
        );

        assertTrue(candidates.isEmpty());
    }

    @Test
    void selectorStopsAtTheConfiguredMaximumTraitCount() {
        assertTrue(TraitSelector.canSelectAnotherTrait(0));
        assertTrue(TraitSelector.canSelectAnotherTrait(2));
        assertFalse(TraitSelector.canSelectAnotherTrait(3));
        assertFalse(TraitSelector.canSelectAnotherTrait(4));
    }

    @Test
    void randomSelectionWeightChangesOnlyNonUniversalCandidates() {
        assertEquals(100.0D, TraitSelector.randomSelectionWeight(trait("u", 100.0D, TraitAudience.UNIVERSAL)));
        assertEquals(150.0D, TraitSelector.randomSelectionWeight(trait("k", 100.0D, TraitAudience.KILLER_ONLY)));
        assertEquals(18.75D, TraitSelector.randomSelectionWeight(trait("i", 12.5D, TraitAudience.INNOCENT_ONLY)));
        assertEquals(150.0D, TraitSelector.randomSelectionWeight(trait("n", 100.0D, TraitAudience.NEUTRAL_ONLY)));
        assertEquals(12.5D, trait("base", 12.5D, TraitAudience.KILLER_ONLY).rollWeight());
    }

    @Test
    void weightedPickUsesBoostWithoutChangingSlotChance() {
        Trait faction = trait("faction", 100.0D, TraitAudience.KILLER_ONLY);
        Trait universal = trait("universal", 100.0D, TraitAudience.UNIVERSAL);
        Random fixed = new Random(0L) {
            @Override
            public double nextDouble() {
                return 0.55D;
            }

            @Override
            public float nextFloat() {
                return 0.749F;
            }
        };

        assertSame(faction, TraitSelector.pickWeighted(List.of(faction, universal), fixed));
        assertTrue(TraitSelector.shouldRollSlot(0.75F, fixed));

        Random boundary = new Random(0L) {
            @Override
            public float nextFloat() {
                return 0.75F;
            }
        };
        assertFalse(TraitSelector.shouldRollSlot(0.75F, boundary));
    }

    private static void registerIfAbsent(Trait trait) {
        if (!TraitRegistry.contains(trait.id())) {
            TraitRegistry.register(trait);
        }
    }

    private static Trait trait(String path, double weight, TraitAudience audience) {
        return new Trait() {
            @Override
            public Identifier id() {
                return Identifier.of("test", path);
            }

            @Override
            public int color() {
                return 0;
            }

            @Override
            public double rollWeight() {
                return weight;
            }

            @Override
            public TraitAudience audience() {
                return audience;
            }
        };
    }
}
