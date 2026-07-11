package dev.caecorthus.sparktraits.component;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraitSyncVisibilityTest {
    private static final List<String> ACTIVE_TRAITS = List.of("active", "hidden");
    private static final List<String> REVEALED_TRAITS = List.of("active");

    @Test
    void regularRecipientReceivesNoRevealedTraitIds() {
        assertEquals(
                List.of(),
                TraitSyncVisibility.revealedTraitsFor(false, false, ACTIVE_TRAITS, REVEALED_TRAITS)
        );
    }

    @Test
    void ownerReceivesRevealedTraitIds() {
        assertEquals(
                REVEALED_TRAITS,
                TraitSyncVisibility.revealedTraitsFor(true, false, ACTIVE_TRAITS, REVEALED_TRAITS)
        );
    }

    @Test
    void spectatorReceivesAllActiveTraitIds() {
        assertEquals(
                ACTIVE_TRAITS,
                TraitSyncVisibility.revealedTraitsFor(false, true, ACTIVE_TRAITS, REVEALED_TRAITS)
        );
    }
}
