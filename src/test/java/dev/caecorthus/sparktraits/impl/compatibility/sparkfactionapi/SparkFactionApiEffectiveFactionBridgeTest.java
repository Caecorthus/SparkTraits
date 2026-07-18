package dev.caecorthus.sparktraits.impl.compatibility.sparkfactionapi;

import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SparkFactionApiEffectiveFactionBridgeTest {
    @Test
    void alignmentTraitsOverrideOnlyTheirNativeOppositeFaction() {
        assertEquals(
                Identifier.of("wathe", "killer"),
                SparkFactionApiEffectiveFactionBridge.resolveEffectiveFaction(
                        java.util.Set.of(ImpostorTrait.ID),
                        Identifier.of("wathe", "civilian")
                )
        );
        assertEquals(
                Identifier.of("wathe", "civilian"),
                SparkFactionApiEffectiveFactionBridge.resolveEffectiveFaction(
                        java.util.Set.of(ConscienceTrait.ID),
                        Identifier.of("wathe", "killer")
                )
        );
        assertNull(SparkFactionApiEffectiveFactionBridge.resolveEffectiveFaction(
                java.util.Set.of(ImpostorTrait.ID),
                Identifier.of("sparkwitch", "witch")
        ));
        assertNull(SparkFactionApiEffectiveFactionBridge.resolveEffectiveFaction(null, null));
    }
}
