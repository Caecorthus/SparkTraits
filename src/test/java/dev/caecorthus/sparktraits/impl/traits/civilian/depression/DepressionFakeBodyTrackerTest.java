package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepressionFakeBodyTrackerTest {
    private static final Identifier OVERWORLD = Identifier.of("minecraft", "overworld");
    private static final Identifier NETHER = Identifier.of("minecraft", "the_nether");

    @Test
    void exactBodyRemainsTrackedUntilRoundCleanup() {
        DepressionFakeBodyTracker tracker = new DepressionFakeBodyTracker();
        UUID playerUuid = UUID.randomUUID();
        UUID bodyUuid = UUID.randomUUID();

        tracker.track(playerUuid, OVERWORLD, bodyUuid);

        assertTrue(tracker.isTracked(playerUuid, OVERWORLD, bodyUuid));
        assertFalse(tracker.isTracked(UUID.randomUUID(), OVERWORLD, bodyUuid));
        assertFalse(tracker.isTracked(playerUuid, NETHER, bodyUuid));
        assertFalse(tracker.isTracked(playerUuid, OVERWORLD, UUID.randomUUID()));
    }

    @Test
    void roundCleanupRemovesAllTrackedBodies() {
        DepressionFakeBodyTracker tracker = new DepressionFakeBodyTracker();
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();
        UUID firstBody = UUID.randomUUID();
        UUID secondBody = UUID.randomUUID();
        UUID netherBody = UUID.randomUUID();

        tracker.track(firstPlayer, OVERWORLD, firstBody);
        tracker.track(secondPlayer, OVERWORLD, secondBody);
        tracker.track(secondPlayer, NETHER, netherBody);

        tracker.clear();

        assertFalse(tracker.isTracked(firstPlayer, OVERWORLD, firstBody));
        assertFalse(tracker.isTracked(secondPlayer, OVERWORLD, secondBody));
        assertFalse(tracker.isTracked(secondPlayer, NETHER, netherBody));
    }

    @Test
    void playerCleanupDoesNotForgetAnExtantArtificialCorpse() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/depression/DepressionTraitService.java"
        ));
        int clearPlayerStart = source.indexOf("public static void clearPlayer(ServerPlayerEntity player)");
        int clearRoundStart = source.indexOf("public static void clearRoundState(ServerWorld world)", clearPlayerStart);

        assertTrue(clearPlayerStart >= 0);
        assertTrue(clearRoundStart > clearPlayerStart);
        assertFalse(source.substring(clearPlayerStart, clearRoundStart).contains("fakeBodies."));
        assertFalse(source.contains("fakeBodies.prune"));
    }
}
