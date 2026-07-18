package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NikoBurstSchedulerSafetyTest {
    private static final String SERVICE_PATH =
            "src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/police/VigilanteVeteranTraitService.java";

    @Test
    void repeatCallbacksNeverScheduleFromInsideWatheSchedulerTick() throws IOException {
        String source = readService();
        String callbacks = section(
                source,
                "private static void repeatNikoBurstShot(",
                "private static boolean canContinueNikoBurst("
        );

        assertFalse(
                callbacks.contains("Scheduler.schedule("),
                "Niko repeat callbacks must fill a pre-scheduled continuation instead of mutating Wathe's task list"
        );
    }

    @Test
    void burstSetupPreRegistersEachRepeatAndItsFourTickContinuation() throws IOException {
        String source = readService();
        String setup = section(
                source,
                "public static void scheduleNikoRevolverBurstRepeats(",
                "private static void repeatNikoBurstShot("
        );

        assertEquals(2, occurrences(setup, "Scheduler.schedule("));
        assertEquals(2, VigilanteVeteranTraitService.NIKO_BURST_INTERVAL_TICKS);
        assertEquals(3, VigilanteVeteranTraitService.NIKO_BURST_SHOTS);
        assertTrue(source.contains("private static final int NIKO_REPEAT_SHOT_PUNISHMENT_DELAY_TICKS = 4;"));
        assertTrue(setup.contains("for (int shot = 1; shot < NIKO_BURST_SHOTS; shot++)"));
        assertTrue(setup.contains("NIKO_BURST_INTERVAL_TICKS * shot"));
        assertTrue(setup.contains(
                "repeatNikoBurstShot(shooter, scheduledWorld, scheduledGame, pendingPunishment)"
        ));
        assertTrue(setup.contains("repeatDelay + NIKO_REPEAT_SHOT_PUNISHMENT_DELAY_TICKS"));
    }

    @Test
    void repeatShotRequiresTheOriginalLiveRoundContext() throws IOException {
        String repeat = section(
                readService(),
                "private static void repeatNikoBurstShot(",
                "/** Mirrors only Wathe's per-hit punishment"
        ).replaceAll("\\s+", " ");

        assertTrue(repeat.contains(
                "if (!isCurrentNikoBurstContext(shooter, scheduledWorld, scheduledGame)"
        ));
        assertTrue(repeat.contains("|| !canContinueNikoBurst(shooter))"));
    }

    @Test
    void deferredPunishmentRequiresTheOriginalLiveRoundContext() throws IOException {
        String source = readService();
        String continuation = section(
                source,
                "private static void runNikoBurstContinuation(",
                "private static boolean isCurrentNikoBurstContext("
        ).replaceAll("\\s+", " ");
        String guard = section(
                source,
                "private static boolean isCurrentNikoBurstContext(",
                "private static void executeNikoRepeatShotPunishment("
        ).replaceAll("\\s+", " ");

        assertTrue(continuation.contains(
                "if (punishment != null && isCurrentNikoBurstContext(shooter, scheduledWorld, scheduledGame))"
        ));
        assertTrue(continuation.contains("punishment.run();"));
        assertTrue(guard.contains("getPlayer(shooter.getUuid()) == shooter"));
        assertTrue(guard.contains("shooter.getWorld() == scheduledWorld"));
        assertTrue(guard.contains("GameWorldComponent.KEY.get(scheduledWorld) == scheduledGame"));
        assertTrue(guard.contains("scheduledGame.isRunning()"));
        assertTrue(guard.contains("!shooter.isSpectator()"));
        assertTrue(guard.contains("GameFunctions.isPlayerPlayingAndAlive(shooter)"));
    }

    private static String readService() throws IOException {
        return Files.readString(Path.of(System.getProperty("user.dir"), SERVICE_PATH));
    }

    private static String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue(start >= 0, "Missing source marker: " + startMarker);
        assertTrue(end > start, "Missing source marker: " + endMarker);
        return source.substring(start, end);
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
