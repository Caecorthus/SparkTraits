package dev.caecorthus.sparktraits.impl.effective.gun;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.doctor4t.wathe.api.WatheRoles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectiveGunRulesTest {
    @Test
    void gunVictimInnocenceUsesEffectiveAlignment() {
        assertTrue(EffectiveGunRules.shouldTreatGunVictimAsInnocent(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertTrue(EffectiveGunRules.shouldTreatGunVictimAsInnocent(WatheRoles.CIVILIAN, Set.of()));
        assertFalse(EffectiveGunRules.shouldTreatGunVictimAsInnocent(WatheRoles.KILLER, Set.of()));
        assertFalse(EffectiveGunRules.shouldTreatGunVictimAsInnocent(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
    }

    @Test
    void innocentShotPunishmentIsCancelledOnlyForImpostorShotsAtEffectiveInnocents() {
        assertFalse(EffectiveGunRules.shouldCancelInnocentShotPunishment(
                WatheRoles.KILLER,
                Set.of(),
                WatheRoles.CIVILIAN,
                Set.of()
        ));
        assertFalse(EffectiveGunRules.shouldCancelInnocentShotPunishment(
                WatheRoles.KILLER,
                Set.of(),
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID)
        ));
        assertTrue(EffectiveGunRules.shouldCancelInnocentShotPunishment(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID),
                WatheRoles.CIVILIAN,
                Set.of()
        ));
        assertTrue(EffectiveGunRules.shouldCancelInnocentShotPunishment(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID),
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID)
        ));
        assertFalse(EffectiveGunRules.shouldCancelInnocentShotPunishment(
                WatheRoles.CIVILIAN,
                Set.of(ImpostorTrait.ID),
                WatheRoles.KILLER,
                Set.of()
        ));
    }
}
