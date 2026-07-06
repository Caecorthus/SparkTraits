package dev.caecorthus.sparktraits.impl.effective.alignment;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.doctor4t.wathe.api.WatheRoles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectiveAlignmentTest {
    @Test
    void detectsAlignmentFlippingTraits() {
        assertTrue(EffectiveAlignment.hasConscience(Set.of(ConscienceTrait.ID)));
        assertTrue(EffectiveAlignment.hasImpostor(Set.of(ImpostorTrait.ID)));
        assertFalse(EffectiveAlignment.hasConscience(Set.of(ImpostorTrait.ID)));
        assertFalse(EffectiveAlignment.hasImpostor(Set.of(ConscienceTrait.ID)));
    }

    @Test
    void originalAlignmentComesFromBaseRole() {
        assertTrue(EffectiveAlignment.isOriginalKiller(WatheRoles.KILLER));
        assertFalse(EffectiveAlignment.isOriginalKiller(WatheRoles.CIVILIAN));
        assertTrue(EffectiveAlignment.isOriginalCivilian(WatheRoles.CIVILIAN));
        assertFalse(EffectiveAlignment.isOriginalCivilian(WatheRoles.KILLER));
        assertFalse(EffectiveAlignment.isOriginalKiller(null));
        assertFalse(EffectiveAlignment.isOriginalCivilian(null));
    }

    @Test
    void effectiveAlignmentAppliesConscienceAndImpostorFlips() {
        assertFalse(EffectiveAlignment.isEffectiveKiller(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertTrue(EffectiveAlignment.isEffectiveCivilian(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));

        assertTrue(EffectiveAlignment.isEffectiveKiller(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
        assertFalse(EffectiveAlignment.isEffectiveCivilian(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));

        assertTrue(EffectiveAlignment.isEffectiveKiller(WatheRoles.KILLER, Set.of()));
        assertFalse(EffectiveAlignment.isEffectiveCivilian(WatheRoles.KILLER, Set.of()));
        assertFalse(EffectiveAlignment.isEffectiveKiller(WatheRoles.CIVILIAN, Set.of()));
        assertTrue(EffectiveAlignment.isEffectiveCivilian(WatheRoles.CIVILIAN, Set.of()));
    }
}
