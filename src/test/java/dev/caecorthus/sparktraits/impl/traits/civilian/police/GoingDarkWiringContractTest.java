package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoingDarkWiringContractTest {
    @Test
    void hardStopRunsAfterSpectatorAndBeforeNormalInstinctOverlays() throws IOException {
        String source = read("src/client/java/dev/caecorthus/sparktraits/client/mixin/WatheClientMixin.java");
        int spectator = source.indexOf("if (WatheClient.canSeeSpectatorInformation())");
        int hardStop = source.indexOf("GoingDarkInstinctClientHooks.shouldSuppress");
        int normalOverlays = source.indexOf("MorphlingPlayerComponent morphling");

        assertTrue(spectator >= 0 && spectator < hardStop);
        assertTrue(hardStop < normalOverlays);
        assertTrue(source.replaceAll("\\s+", " ").contains(
                "GoingDarkInstinctClientHooks.shouldSuppress(viewer, playerTarget, game)) { cir.setReturnValue(-1); return; }"
        ));
    }

    @Test
    void legacyLowPriorityEventIsRemoved() throws IOException {
        String source = read("src/client/java/dev/caecorthus/sparktraits/client/SparkTraitsClient.java");
        assertFalse(source.contains("registerGoingDarkInstinctSkip"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(System.getProperty("user.dir"), relativePath));
    }
}
