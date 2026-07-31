package dev.caecorthus.sparktraits.impl.assignment;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConscienceCompensationTraitPlanTest {
    private static final Identifier OLD_CIVILIAN_UNIQUE = Identifier.of("test", "old_civilian_unique");
    private static final Identifier NEW_KILLER_UNIQUE = Identifier.of("test", "new_killer_unique");
    private static final Identifier ORDINARY_KILLER = Identifier.of("test", "ordinary_killer");

    @BeforeAll
    static void registerUniqueTraits() {
        registerIfAbsent(uniqueTrait(OLD_CIVILIAN_UNIQUE));
        registerIfAbsent(uniqueTrait(NEW_KILLER_UNIQUE));
    }

    @Test
    void compensationRedrawReplacesOldRandomTraitsAndRecalibratesUniqueReservations() {
        TraitAssignmentService.PlayerPlan plan = new TraitAssignmentService.PlayerPlan(
                null,
                List.of(),
                List.of(OLD_CIVILIAN_UNIQUE)
        );
        LinkedHashSet<Identifier> reservations = new LinkedHashSet<>(Set.of(OLD_CIVILIAN_UNIQUE));

        TraitAssignmentService.replaceRandomTraitsForConscienceCompensation(
                plan,
                reservations,
                List.of(NEW_KILLER_UNIQUE, ORDINARY_KILLER)
        );

        assertEquals(List.of(NEW_KILLER_UNIQUE, ORDINARY_KILLER), plan.traits());
        assertEquals(Set.of(NEW_KILLER_UNIQUE), reservations);
    }

    @Test
    void recalibrationRetainsUniqueReservationsFromLockedPlans() {
        TraitAssignmentService.PlayerPlan locked = new TraitAssignmentService.PlayerPlan(
                null,
                List.of(OLD_CIVILIAN_UNIQUE),
                List.of()
        );
        TraitAssignmentService.PlayerPlan forcedConsciencePlan = new TraitAssignmentService.PlayerPlan(
                null,
                List.of(),
                List.of(ORDINARY_KILLER)
        );
        LinkedHashSet<Identifier> reservations = new LinkedHashSet<>(
                Set.of(OLD_CIVILIAN_UNIQUE, NEW_KILLER_UNIQUE)
        );

        TraitAssignmentService.recalibrateUniqueTraitReservations(
                reservations,
                List.of(locked, forcedConsciencePlan)
        );

        assertEquals(Set.of(OLD_CIVILIAN_UNIQUE), reservations);
    }

    @Test
    void runtimeRedrawUsesStandardSelectorAfterFinalRoleWithSameWorldRngAndCount() throws IOException {
        String source = Files.readString(Path.of(
                System.getProperty("user.dir"),
                "src/main/java/dev/caecorthus/sparktraits/impl/assignment/TraitAssignmentService.java"
        )).replaceAll("\\s+", " ");
        int assigned = source.indexOf("RoleAssigned.EVENT.invoker().assignRole(extraKiller.player(), compensationRole);");
        int selected = source.indexOf("TraitSelector.selectRandomTraits(", assigned);
        int installed = source.indexOf("extraKiller.replaceRandomTraits(rerolledTraits);", selected);
        String redraw = source.substring(selected, installed);

        assertTrue(assigned >= 0);
        assertTrue(selected > assigned);
        assertTrue(installed > selected);
        assertTrue(redraw.contains(
                "world, gameComponent, traitWorld, extraKiller.player(), random, players.size(), extraKiller.lockedTraits(), randomUniqueTraitReservations, CONSCIENCE_COMPENSATION_REROLL_EXCLUSIONS"
        ));
    }

    @Test
    void compensationTargetMustHaveNeitherTraitNorRoleLocks() {
        assertTrue(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                WatheRoles.CIVILIAN,
                List.of(),
                false,
                false
        ));
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                WatheRoles.CIVILIAN,
                List.of(),
                false,
                true
        ));
        assertFalse(TraitAssignmentService.canUseAsConscienceCompensationTarget(
                WatheRoles.CIVILIAN,
                List.of(),
                true,
                false
        ));
    }

    private static Trait uniqueTrait(Identifier id) {
        return new Trait() {
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
        };
    }

    private static void registerIfAbsent(Trait trait) {
        if (!TraitRegistry.contains(trait.id())) {
            TraitRegistry.register(trait);
        }
    }
}