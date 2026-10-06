package dev.caecorthus.sparktraits.impl.traits;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraitDisplayServiceTest {
    @Test
    void deadSnapshotOverridesPartialEntityCache() {
        List<Identifier> activeCache = List.of(Identifier.of("sparktraits", "owner_visible"));
        List<Identifier> deathSnapshot = List.of(
                Identifier.of("sparktraits", "owner_visible"),
                Identifier.of("sparktraits", "hidden_trait")
        );

        assertEquals(
                deathSnapshot,
                TraitDisplayService.spectatorPlayerTraits(true, true, activeCache, deathSnapshot)
        );
    }

    @Test
    void livingSpectatorStillUsesActiveEntityTraits() {
        List<Identifier> activeCache = List.of(Identifier.of("sparktraits", "active_trait"));
        List<Identifier> deathSnapshot = List.of(Identifier.of("sparktraits", "old_trait"));

        assertEquals(
                activeCache,
                TraitDisplayService.spectatorPlayerTraits(true, false, activeCache, deathSnapshot)
        );
    }
}
