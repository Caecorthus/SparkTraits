package dev.caecorthus.sparktraits.impl.lifecycle;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WraithResetOrderingTest {
    @Test
    void activeWraithDefersResetRemovalRegardlessOfListenerOrder() {
        assertFalse(TraitGameHooks.shouldClearTraitsOnReset(true));
        assertTrue(TraitGameHooks.shouldClearTraitsOnReset(false));
    }

    @Test
    void resetCapturesWraithStateBeforeAnyTraitCleanup() throws Exception {
        String hooks = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/lifecycle/TraitGameHooks.java"
        ));
        int reset = hooks.indexOf("ResetPlayer.EVENT.register");
        int activeQuery = hooks.indexOf("SparkTraitsApi.isWraithActive(player)", reset);
        int resetClear = hooks.indexOf("clearActiveTraits(TraitRemovalReason.RESET)", reset);

        assertTrue(activeQuery > reset);
        assertTrue(resetClear > activeQuery);
        assertTrue(hooks.substring(activeQuery, resetClear).contains("shouldClearTraitsOnReset(wraithActive)"));
    }
}
