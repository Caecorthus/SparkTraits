package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BombManiacCooldownStateTest {
    @Test
    void restoreKeepsExactEntryIdentityOrRemovesTransientEntry() {
        Object original = new Object();
        Object transientReplacement = new Object();
        Map<String, Object> entries = new HashMap<>();
        entries.put("grenade", transientReplacement);

        ConscienceBomberFrenzyService.restoreCooldownEntry(
                entries,
                "grenade",
                new ConscienceBomberFrenzyService.CooldownSnapshot(original, 120)
        );

        assertSame(original, entries.get("grenade"));

        entries.put("grenade", transientReplacement);
        ConscienceBomberFrenzyService.restoreCooldownEntry(
                entries,
                "grenade",
                new ConscienceBomberFrenzyService.CooldownSnapshot(null, 0)
        );

        assertFalse(entries.containsKey("grenade"));
    }

    @Test
    void expiredSnapshotRemovesTransientEntry() {
        Map<String, Object> entries = new HashMap<>();
        entries.put("grenade", new Object());

        ConscienceBomberFrenzyService.restoreCooldownEntry(
                entries,
                "grenade",
                new ConscienceBomberFrenzyService.CooldownSnapshot(new Object(), 0)
        );

        assertFalse(entries.containsKey("grenade"));
    }

    @Test
    void markedUseRestoresCountWhileOrdinaryUseKeepsWatheResult() {
        assertEquals(1, ConscienceBomberFrenzyService.restoredGrenadeStackCount(true, 1, 0));
        assertEquals(0, ConscienceBomberFrenzyService.restoredGrenadeStackCount(false, 1, 0));
    }

    @Test
    void cooldownSyncSuppressionIsOwnerBoundAndExceptionSafe() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000123");
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000456");

        assertFalse(ConscienceBomberFrenzyService.isGrenadeCooldownSyncSuppressed(owner));
        assertThrows(IllegalStateException.class, () -> {
            try (ConscienceBomberFrenzyService.CooldownSyncSuppression ignored =
                         ConscienceBomberFrenzyService.suppressGrenadeCooldownSync(owner, true)) {
                assertTrue(ConscienceBomberFrenzyService.isGrenadeCooldownSyncSuppressed(owner));
                assertFalse(ConscienceBomberFrenzyService.isGrenadeCooldownSyncSuppressed(other));
                throw new IllegalStateException("exercise finally cleanup");
            }
        });
        assertFalse(ConscienceBomberFrenzyService.isGrenadeCooldownSyncSuppressed(owner));

        try (ConscienceBomberFrenzyService.CooldownSyncSuppression ignored =
                     ConscienceBomberFrenzyService.suppressGrenadeCooldownSync(owner, false)) {
            assertFalse(ConscienceBomberFrenzyService.isGrenadeCooldownSyncSuppressed(owner));
        }
    }
}
