package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runtime-only identities of Depression fake bodies, kept until the body is discarded or the round is cleaned up.
 * 仅在运行时记录抑郁假尸，直到假尸被移除或回合清理。
 */
final class DepressionFakeBodyTracker {
    private final Map<UUID, TrackedBody> bodies = new HashMap<>();

    void track(UUID playerUuid, Identifier worldId, UUID bodyUuid) {
        bodies.put(bodyUuid, new TrackedBody(playerUuid, worldId));
    }

    boolean isTracked(UUID playerUuid, Identifier worldId, UUID bodyUuid) {
        TrackedBody trackedBody = bodies.get(bodyUuid);
        return trackedBody != null
                && trackedBody.playerUuid().equals(playerUuid)
                && trackedBody.worldId().equals(worldId);
    }

    List<FakeBody> bodiesOf(UUID playerUuid) {
        return bodies.entrySet().stream()
                .filter(entry -> entry.getValue().playerUuid().equals(playerUuid))
                .map(entry -> new FakeBody(entry.getKey(), entry.getValue().playerUuid(), entry.getValue().worldId()))
                .toList();
    }

    List<FakeBody> bodiesIn(Identifier worldId) {
        return bodies.entrySet().stream()
                .filter(entry -> entry.getValue().worldId().equals(worldId))
                .map(entry -> new FakeBody(entry.getKey(), entry.getValue().playerUuid(), entry.getValue().worldId()))
                .toList();
    }

    void untrack(UUID bodyUuid) {
        bodies.remove(bodyUuid);
    }

    void clear() {
        bodies.clear();
    }

    record FakeBody(UUID bodyUuid, UUID playerUuid, Identifier worldId) {
    }

    private record TrackedBody(UUID playerUuid, Identifier worldId) {
    }
}
