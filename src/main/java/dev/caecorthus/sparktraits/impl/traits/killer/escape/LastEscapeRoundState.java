package dev.caecorthus.sparktraits.impl.traits.killer.escape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Round memory, deliberately independent of player entities and trait reassignment. */
public final class LastEscapeRoundState {
    public static final int DURATION_TICKS = 200;
    private final Map<UUID, State> players = new HashMap<>();

    public boolean activate(UUID player, long now) {
        if (players.containsKey(player)) return false;
        players.put(player, new State(now + DURATION_TICKS, true));
        return true;
    }

    public boolean isActive(UUID player, long now) {
        State state = players.get(player);
        return state != null && state.grayscale && now < state.expiresAt;
    }

    public boolean hasGrayscale(UUID player) {
        State state = players.get(player);
        return state != null && state.grayscale;
    }

    public Map<UUID, Long> activeDeadlines(long now) {
        Map<UUID, Long> result = new HashMap<>();
        players.forEach((uuid, state) -> {
            if (state.grayscale && now < state.expiresAt) result.put(uuid, state.expiresAt);
        });
        return result;
    }

    public void onDeath(UUID player) {
        players.computeIfPresent(player, (uuid, state) -> new State(state.expiresAt, false));
    }

    public void clear() { players.clear(); }

    private record State(long expiresAt, boolean grayscale) {}
}
