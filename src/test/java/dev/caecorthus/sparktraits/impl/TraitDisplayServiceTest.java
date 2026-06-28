package dev.caecorthus.sparktraits.impl;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraitDisplayServiceTest {
    private static final Identifier ACTIVE = Identifier.of("sparktraits", "active");
    private static final Identifier DEATH = Identifier.of("sparktraits", "death");

    @Test
    void spectatorPlayerTraitsPreferActiveTraits() {
        assertEquals(
                List.of(ACTIVE),
                TraitDisplayService.spectatorPlayerTraits(true, true, List.of(ACTIVE), List.of(DEATH))
        );
    }

    @Test
    void spectatorPlayerTraitsFallbackToDeathSnapshotOnlyForDeadTargets() {
        assertEquals(
                List.of(DEATH),
                TraitDisplayService.spectatorPlayerTraits(true, true, List.of(), List.of(DEATH))
        );
        assertEquals(
                List.of(),
                TraitDisplayService.spectatorPlayerTraits(true, false, List.of(), List.of(DEATH))
        );
    }

    @Test
    void nonSpectatorPlayerTraitsStayHidden() {
        assertEquals(
                List.of(),
                TraitDisplayService.spectatorPlayerTraits(false, true, List.of(ACTIVE), List.of(DEATH))
        );
    }
}
