package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitLockValidationServiceTest {
    private static final Identifier REGISTERED_KILLER_TRAIT_ID = Identifier.of("sparktraits_test", "registered_killer_trait");

    @BeforeAll
    static void registerTestTraits() {
        if (!TraitRegistry.contains(REGISTERED_KILLER_TRAIT_ID)) {
            TraitRegistry.register(trait(REGISTERED_KILLER_TRAIT_ID, TraitAudience.KILLER_ONLY));
        }
    }

    @Test
    void killerOnlyTraitConflictsWithCivilianRole() {
        assertFalse(TraitLockValidationService.isAudienceCompatibleWithRole(
                trait(SparkTraits.id("conscience"), TraitAudience.KILLER_ONLY),
                WatheRoles.CIVILIAN
        ));
    }

    @Test
    void killerOnlyTraitAllowsKillerRole() {
        assertTrue(TraitLockValidationService.isAudienceCompatibleWithRole(
                trait(SparkTraits.id("conscience"), TraitAudience.KILLER_ONLY),
                WatheRoles.KILLER
        ));
    }

    @Test
    void unknownRoleDoesNotCreateCommandTimeConflict() {
        assertTrue(TraitLockValidationService.isAudienceCompatibleWithRole(
                trait(SparkTraits.id("conscience"), TraitAudience.KILLER_ONLY),
                null
        ));
    }

    @Test
    void pendingTraitBlocksConflictingRoleForcedLater() {
        TraitLockValidationService.RoleConflict conflict = TraitLockValidationService.findPendingTraitAudienceConflict(
                List.of(REGISTERED_KILLER_TRAIT_ID),
                WatheRoles.CIVILIAN
        );

        assertNotNull(conflict);
        assertEquals(REGISTERED_KILLER_TRAIT_ID, conflict.trait().id());
        assertEquals(WatheRoles.CIVILIAN, conflict.role());
    }

    @Test
    void addTraitRoleConflictMessageNamesTraitAndRole() {
        assertEquals(
                "无法添加，因为 conscience 与 civilian 冲突。",
                TraitLockValidationService.addTraitRoleConflictMessage(
                        trait(SparkTraits.id("conscience"), TraitAudience.KILLER_ONLY),
                        WatheRoles.CIVILIAN
                )
        );
    }

    @Test
    void uniqueTraitConflictMessageNamesOwnerAndTrait() {
        assertEquals(
                "无法锁定，因为 Kricy 已经有了 last_stand。",
                TraitLockValidationService.lockUniqueTraitConflictMessage(
                        "Kricy",
                        trait(SparkTraits.id("last_stand"), TraitAudience.INNOCENT_ONLY)
                )
        );
    }

    private static Trait trait(Identifier id, TraitAudience audience) {
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
            public TraitAudience audience() {
                return audience;
            }
        };
    }
}
