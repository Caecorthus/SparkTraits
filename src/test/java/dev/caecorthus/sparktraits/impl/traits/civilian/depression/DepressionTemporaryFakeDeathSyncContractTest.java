package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DepressionTemporaryFakeDeathSyncContractTest {
    private static final Path SERVICE_SOURCE = Path.of(
            "src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/depression/DepressionTraitService.java"
    );

    @Test
    void synchronizesTemporaryFakeDeathAcrossEveryPendingExit() throws IOException {
        String source = Files.readString(SERVICE_SOURCE);

        String startPending = methodBody(source, "private static void startPending(");
        assertTrue(startPending.contains("setTemporaryFakeDeathPending(true)"));

        String startPsycho = methodBody(source, "private static void startPsycho(");
        assertTrue(startPsycho.contains("setTemporaryFakeDeathPending(false)"));

        String clearPending = methodBody(source, "private static void clearPending(");
        assertTrue(clearPending.contains("setTemporaryFakeDeathPending(false)"));
        assertTrue(clearPending.indexOf("setTemporaryFakeDeathPending(false)")
                < clearPending.indexOf("if (state == null)"));

        String tickPending = methodBody(source, "private static void tickPending(");
        assertTrue(tickPending.contains("continue;"));
        assertTrue(!tickPending.contains("pendingPlayers.remove("));

        String clearRoundState = methodBody(source, "public static void clearRoundState(");
        assertTrue(clearRoundState.contains("getPlayerManager().getPlayer(uuid)"));
        assertTrue(clearRoundState.contains("clearPending(player)"));
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "Missing method: " + signature);
        int nextMethod = source.indexOf("\n    private static ", start + signature.length());
        return nextMethod < 0 ? source.substring(start) : source.substring(start, nextMethod);
    }
}
