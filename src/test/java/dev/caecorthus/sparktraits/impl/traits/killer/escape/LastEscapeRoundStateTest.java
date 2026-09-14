package dev.caecorthus.sparktraits.impl.traits.killer.escape;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class LastEscapeRoundStateTest {
    @Test void phaseEndsExactlyAt200ButGrayscalePersists() {
        var state = new LastEscapeRoundState();
        UUID player = UUID.randomUUID();
        assertTrue(state.activate(player, 1000));
        assertTrue(state.isActive(player, 1199));
        assertFalse(state.isActive(player, 1200));
        assertTrue(state.hasGrayscale(player));
        assertTrue(state.activeDeadlines(1200).isEmpty());
    }

    @Test void offlineElapsedTimeDoesNotPauseOrRefreshPhase() {
        var state = new LastEscapeRoundState();
        UUID player = UUID.randomUUID();
        assertTrue(state.activate(player, 50));
        assertFalse(state.activate(player, 100));
        assertFalse(state.isActive(player, 400));
        assertFalse(state.activate(player, 400));
        assertTrue(state.hasGrayscale(player));
    }

    @Test void deathAndRevivalDoNotRestoreChargeButNextRoundDoes() {
        var state = new LastEscapeRoundState();
        UUID player = UUID.randomUUID();
        state.activate(player, 5);
        state.onDeath(player);
        assertFalse(state.isActive(player, 6));
        assertFalse(state.hasGrayscale(player));
        assertFalse(state.activate(player, 7));
        state.clear();
        assertTrue(state.activate(player, 8));
    }

    @Test void disconnectedExpiryThenConfirmedDeathKeepsChargeConsumedAndClearsOnlyOwnerVisuals() {
        var state = new LastEscapeRoundState();
        UUID disconnected = UUID.randomUUID(), other = UUID.randomUUID();
        state.activate(disconnected, 100);
        state.activate(other, 200);
        assertTrue(state.isActive(disconnected, 299));
        assertFalse(state.isActive(disconnected, 300));
        assertTrue(state.hasGrayscale(disconnected));
        state.onDeath(disconnected);
        assertFalse(state.hasGrayscale(disconnected));
        assertFalse(state.activate(disconnected, 301));
        assertTrue(state.isActive(other, 301));
        assertTrue(state.hasGrayscale(other));
        state.clear();
        assertFalse(state.isActive(other, 302));
        assertFalse(state.hasGrayscale(other));
        assertTrue(state.activate(disconnected, 302));
    }

    @Test void unrelatedDeathDoesNotConsumeChargeOrAffectAnotherPlayer() {
        var state = new LastEscapeRoundState();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        state.activate(first, 10);
        state.onDeath(second);
        assertTrue(state.activate(second, 11));
        assertEquals(2, state.activeDeadlines(12).size());
    }
}
