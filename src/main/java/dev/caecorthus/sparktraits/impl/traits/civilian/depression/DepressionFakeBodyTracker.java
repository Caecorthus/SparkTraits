package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Runtime-only identities retained through chunk unloads until round cleanup.
 * 仅在运行时记录抑郁假尸，并跨区块卸载保留到回合清理。
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

    void clear() {
        bodies.clear();
    }

    private record TrackedBody(UUID playerUuid, Identifier worldId) {
    }
}
