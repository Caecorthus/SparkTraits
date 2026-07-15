package dev.caecorthus.sparktraits.impl.traits.civilian.laststand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastStandFakeBodyTest {
    @Test
    void pendingStateMatchesOnlyItsExactBodyUuid() {
        UUID pendingBodyUuid = UUID.randomUUID();

        assertTrue(LastStandService.matchesPendingBodyUuid(pendingBodyUuid, pendingBodyUuid));
        assertFalse(LastStandService.matchesPendingBodyUuid(UUID.randomUUID(), pendingBodyUuid));
        assertFalse(LastStandService.matchesPendingBodyUuid(pendingBodyUuid, null));
    }
}
