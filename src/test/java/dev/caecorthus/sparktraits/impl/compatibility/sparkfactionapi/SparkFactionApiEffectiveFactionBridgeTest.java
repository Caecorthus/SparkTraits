package dev.caecorthus.sparktraits.impl.compatibility.sparkfactionapi;

import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkFactionApiEffectiveFactionBridgeTest {
    private static final Identifier CIVILIAN = Identifier.of("wathe", "civilian");
    private static final Identifier KILLER = Identifier.of("wathe", "killer");
    private static final Identifier MURDEROUS_WITCH = Identifier.of("sparkwitch", "murderous_witch");

    @Test
    void ordinaryPlayersKeepSparkFactionApiCurrentFaction() {
        assertNull(SparkFactionApiEffectiveFactionBridge.resolveEffectiveFaction(Set.of(), CIVILIAN));
        assertNull(SparkFactionApiEffectiveFactionBridge.resolveEffectiveFaction(Set.of(), KILLER));
    }

    @Test
    void impostorTurnsRawCivilianIntoEffectiveKillerFaction() {
        Set<Identifier> traits = Set.of(ImpostorTrait.ID);

        assertEquals(KILLER, SparkFactionApiEffectiveFactionBridge.resolveEffectiveFaction(traits, CIVILIAN));
        assertTrue(EffectiveTraitService.isEffectiveKiller(WatheRoles.CIVILIAN, traits));
        assertFalse(EffectiveTraitService.isEffectiveCivilian(WatheRoles.CIVILIAN, traits));
    }

    @Test
    void conscienceTurnsRawKillerIntoEffectiveCivilianFaction() {
        Set<Identifier> traits = Set.of(ConscienceTrait.ID);

        assertEquals(CIVILIAN, SparkFactionApiEffectiveFactionBridge.resolveEffectiveFaction(traits, KILLER));
        assertTrue(EffectiveTraitService.isEffectiveCivilian(WatheRoles.KILLER, traits));
        assertFalse(EffectiveTraitService.isEffectiveKiller(WatheRoles.KILLER, traits));
    }

    @Test
    void customEffectiveFactionsAreNotOverwritten() {
        assertNull(SparkFactionApiEffectiveFactionBridge.resolveEffectiveFaction(
                Set.of(ImpostorTrait.ID),
                MURDEROUS_WITCH
        ));
        assertNull(SparkFactionApiEffectiveFactionBridge.resolveEffectiveFaction(
                Set.of(ConscienceTrait.ID),
                MURDEROUS_WITCH
        ));
    }
}
