package dev.caecorthus.sparktraits.impl.replay;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkTraitsReplayContractTest {
    private static final String REPLAY_SOURCE =
            "src/main/java/dev/caecorthus/sparktraits/impl/replay/SparkTraitsReplayEvents.java";

    @Test
    void replayAdapterOwnsOnlyTheApprovedMatchDefiningEventIds() throws IOException {
        String replaySource = source(REPLAY_SOURCE);

        assertEquals(
                Identifier.of("sparktraits", "last_stand_triggered"),
                SparkTraitsReplayEvents.LAST_STAND_TRIGGERED
        );
        assertEquals(
                Identifier.of("sparktraits", "final_moment_start"),
                SparkTraitsReplayEvents.FINAL_MOMENT_START
        );
        assertEquals(
                Identifier.of("sparktraits", "loose_end_conversion"),
                SparkTraitsReplayEvents.LOOSE_END_CONVERSION
        );
        assertTrue(replaySource.contains("GameRecordManager.recordGlobalEvent"));
        assertFalse(replaySource.contains("ConscienceTrait"));
        assertFalse(replaySource.contains("ImpostorTrait"));
    }

    @Test
    void lastStandRecordsOnlyAfterTheSuccessfulPendingTransition() throws IOException {
        String service = source(
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/laststand/LastStandService.java"
        );
        int pendingTransition = service.indexOf("startPending(world, victim, deathReason);");
        int replayRecord = service.indexOf("SparkTraitsReplayEvents.recordLastStandTriggered(victim);");

        assertTrue(pendingTransition >= 0);
        assertTrue(replayRecord > pendingTransition);
    }

    @Test
    void finalMomentRecordsItsStartBeforeOnlyActualLooseEndConversions() throws IOException {
        String service = source(
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/laststand/LastStandFinalMomentService.java"
        );
        int activeTransition = service.indexOf("traitWorld.setFinalMomentActive(true);");
        int finalMomentRecord = service.indexOf("SparkTraitsReplayEvents.recordFinalMomentStarted(world);");
        int conversionCall = service.indexOf("convertToLooseEnd(world, gameComponent, player);");
        int roleAssigned = service.indexOf(
                "RoleAssigned.EVENT.invoker().assignRole(player, WatheRoles.LOOSE_END);"
        );
        int conversionRecord = service.indexOf("SparkTraitsReplayEvents.recordLooseEndConversion(player);");

        assertTrue(activeTransition >= 0);
        assertTrue(finalMomentRecord > activeTransition);
        assertTrue(conversionCall > finalMomentRecord);
        assertTrue(roleAssigned >= 0);
        assertTrue(conversionRecord > roleAssigned);
    }

    @Test
    void startupAlignmentAndCompensationAssignmentDoNotWriteReplayEvents() throws IOException {
        String assignment = source(
                "src/main/java/dev/caecorthus/sparktraits/impl/assignment/TraitAssignmentService.java"
        );
        String effective = source(
                "src/main/java/dev/caecorthus/sparktraits/impl/effective/EffectiveTraitService.java"
        );

        assertFalse(assignment.contains("SparkTraitsReplayEvents"));
        assertFalse(effective.contains("SparkTraitsReplayEvents"));
    }

    @Test
    void englishAndChineseProvideMatchingReplayTranslations() throws IOException {
        JsonObject english = language("en_us");
        JsonObject chinese = language("zh_cn");
        String[] keys = {
                "replay.death.sparktraits.self_realization.died",
                "replay.global.sparktraits.last_stand_triggered",
                "replay.global.sparktraits.final_moment_start",
                "replay.global.sparktraits.loose_end_conversion"
        };

        for (String key : keys) {
            assertTrue(english.has(key), "Missing English replay translation: " + key);
            assertTrue(chinese.has(key), "Missing Chinese replay translation: " + key);
            assertEquals(
                    placeholderCount(english.get(key).getAsString()),
                    placeholderCount(chinese.get(key).getAsString()),
                    "Placeholder mismatch for " + key
            );
        }

        assertEquals(1, placeholderCount(english.get(keys[0]).getAsString()));
        assertEquals(1, placeholderCount(english.get(keys[1]).getAsString()));
        assertEquals(0, placeholderCount(english.get(keys[2]).getAsString()));
        assertEquals(1, placeholderCount(english.get(keys[3]).getAsString()));
    }

    private static JsonObject language(String language) throws IOException {
        return JsonParser.parseString(source(
                "src/main/resources/assets/sparktraits/lang/" + language + ".json"
        )).getAsJsonObject();
    }

    private static int placeholderCount(String value) {
        return value.split("%s", -1).length - 1;
    }

    private static String source(String relativePath) throws IOException {
        Path path = Path.of(System.getProperty("user.dir"), relativePath);
        assertTrue(Files.isRegularFile(path), "Missing required file: " + relativePath);
        return Files.readString(path);
    }
}
