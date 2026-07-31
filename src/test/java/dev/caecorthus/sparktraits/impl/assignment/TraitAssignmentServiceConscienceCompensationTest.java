package dev.caecorthus.sparktraits.impl.assignment;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraitAssignmentServiceConscienceCompensationTest {
    private static final Identifier LOCKED = Identifier.of("test", "compensation_locked");
    private static final Identifier RANDOM = Identifier.of("test", "compensation_random");
    private static final Identifier REPLACEMENT = Identifier.of("test", "compensation_replacement");
    private static final Identifier RELEASED_UNIQUE = Identifier.of("test", "compensation_released_unique");
    private static final Identifier RETAINED_UNIQUE = Identifier.of("test", "compensation_retained_unique");
    private static final Identifier LOCKED_UNIQUE = Identifier.of("test", "compensation_locked_unique");

    @BeforeAll
    static void registerUniqueTraits() {
        registerUnique(RELEASED_UNIQUE);
        registerUnique(RETAINED_UNIQUE);
        registerUnique(LOCKED_UNIQUE);
    }

    @Test
    void replacingRandomTraitsPreservesLockedTraits() {
        TraitAssignmentService.PlayerPlan plan = new TraitAssignmentService.PlayerPlan(
                null,
                List.of(LOCKED),
                List.of(RANDOM)
        );

        plan.clearRandomTraits();
        assertEquals(List.of(LOCKED), plan.lockedTraits());
        assertEquals(List.of(), plan.randomTraits());

        plan.replaceRandomTraits(List.of(REPLACEMENT));
        assertEquals(List.of(LOCKED), plan.lockedTraits());
        assertEquals(List.of(REPLACEMENT), plan.randomTraits());
        assertEquals(List.of(LOCKED, REPLACEMENT), plan.traits());
    }

    @Test
    void rebuildingReservationsReleasesOnlyTraitsNoLongerInPlans() {
        TraitAssignmentService.PlayerPlan converted = new TraitAssignmentService.PlayerPlan(
                null,
                List.of(LOCKED_UNIQUE),
                List.of(RELEASED_UNIQUE)
        );
        TraitAssignmentService.PlayerPlan other = new TraitAssignmentService.PlayerPlan(
                null,
                List.of(),
                List.of(RETAINED_UNIQUE)
        );
        LinkedHashSet<Identifier> reservations = new LinkedHashSet<>(List.of(
                RELEASED_UNIQUE,
                RETAINED_UNIQUE,
                LOCKED_UNIQUE
        ));

        converted.clearRandomTraits();
        TraitAssignmentService.rebuildUniqueTraitReservations(reservations, List.of(converted, other));

        assertEquals(new LinkedHashSet<>(List.of(LOCKED_UNIQUE, RETAINED_UNIQUE)), reservations);
    }

    private static void registerUnique(Identifier id) {
        if (TraitRegistry.contains(id)) {
            return;
        }
        TraitRegistry.register(new Trait() {
            @Override
            public Identifier id() {
                return id;
            }

            @Override
            public int color() {
                return 0;
            }

            @Override
            public boolean uniquePerGame() {
                return true;
            }
        });
    }
}
