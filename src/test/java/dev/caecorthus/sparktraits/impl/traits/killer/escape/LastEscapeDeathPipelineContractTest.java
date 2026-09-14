package dev.caecorthus.sparktraits.impl.traits.killer.escape;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Source contracts complement, but do not replace, transformed multi-mod runtime validation. */
class LastEscapeDeathPipelineContractTest {
    private static String source(String path) throws Exception {
        return Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits", path));
    }

    @Test void earlyPhaseGuardEnclosesAttemptAndExceptionCleanup() throws Exception {
        String source = source("mixin/GameFunctionsMixin.java");
        assertTrue(source.contains("@WrapMethod"));
        assertTrue(source.indexOf("LastEscapeService.blocksDeath") < source.indexOf("KillerTraitService.beginKillAttempt"));
        assertTrue(source.contains("finally"));
        assertTrue(source.contains("KillerTraitService.abortKillAttempt()"));
        assertFalse(source.contains("@At(\"RETURN\")"));
        assertTrue(source.contains("force || trainEscape"));
    }

    @Test void finalSavePrecedesVanillaCommitAndFollowsTofanaPriority() throws Exception {
        String source = source("mixin/LastEscapeTerminalMixin.java");
        assertTrue(source.contains("priority = 900"));
        assertTrue(source.contains("changeGameMode(Lnet/minecraft/world/GameMode;)Z"));
        assertTrue(source.contains("!LastEscapeService.isTrainDeath(reason)"));
        assertTrue(source.contains("!GameConstants.DeathReasons.ESCAPED.equals(reason)"));
        assertFalse(source.contains("markPlayerDead"));
        assertFalse(source.contains("KillPlayer.AFTER"));
    }

    @Test void savedAttemptTerminatesBeforePsychoCleanup() throws Exception {
        String source = source("impl/traits/killer/escape/LastEscapeService.java");
        assertTrue(source.indexOf("KillerTraitService.terminateKillAttempt()") < source.indexOf("psycho.stopPsycho()"));
        assertTrue(source.contains("player.clearActiveItem()"));
        assertFalse(source.contains("player.stopUsingItem()"));
        assertTrue(source("impl/traits/killer/KillerTraitService.java").contains("attempt.terminated"));
    }

    @Test void grayscaleSyncIsPrivateAndCollisionStateContainsNoTraitIds() throws Exception {
        String source = source("component/TraitWorldComponent.java");
        assertTrue(source.contains("lastEscape.activeDeadlines(world.getTime())"));
        assertTrue(source.contains("lastEscape.hasGrayscale(recipient.getUuid())"));
        assertFalse(source.contains("lastEscape.hasGrayscale(player.getUuid())"));
    }
}
