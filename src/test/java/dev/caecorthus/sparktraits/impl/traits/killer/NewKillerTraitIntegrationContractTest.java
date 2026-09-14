package dev.caecorthus.sparktraits.impl.traits.killer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewKillerTraitIntegrationContractTest {
    private static final Path MAIN = Path.of("src/main/java/dev/caecorthus/sparktraits");

    @Test
    void allNewTraitsHaveBilingualNamesAndDescriptions() throws IOException {
        for (String language : List.of("en_us", "zh_cn")) {
            JsonObject translations = JsonParser.parseString(Files.readString(
                    Path.of("src/main/resources/assets/sparktraits/lang", language + ".json"))).getAsJsonObject();
            for (String id : List.of("team_first", "exhilarated", "close_quarters", "last_escape")) {
                assertFalse(translations.get("trait.sparktraits." + id + ".name").getAsString().isBlank());
                assertFalse(translations.get("trait.sparktraits." + id + ".description").getAsString().isBlank());
            }
            assertEquals("%ss", translations.get("gui.sparktraits.forced_melee_cooldown").getAsString());
        }
    }

    @Test
    void lastEscapeStartsHiddenAndOptionalEconomyIsCapabilityGated() throws IOException {
        String registration = Files.readString(MAIN.resolve("impl/traits/killer/KillerTraits.java"));
        assertTrue(registration.contains("base(LAST_ESCAPE, 0xAAAAB8).hiddenFromOwnerAtStart()"));
        assertTrue(registration.contains("SparkStrengthTeamEconomyBridge.isAvailable()"));
        assertTrue(registration.contains("CloseQuartersService.canSelect(context.player())"));
        assertTrue(registration.contains(".incompatibleWith(ConscienceTrait.ID)"));
        assertTrue(registration.contains(".incompatibleWith(ImpostorTrait.ID)"));
    }

    @Test
    void exhilaratedRunsOnlyAfterTheExistingRealDeathFilter() throws IOException {
        String hooks = Files.readString(MAIN.resolve("impl/lifecycle/TraitGameHooks.java"));
        int lastStand = hooks.indexOf("if (lastStandStarted)");
        int returnAfterLastStand = hooks.indexOf("return;", lastStand);
        int realKill = hooks.indexOf("KillerTraitService.handleAfterRealKill");
        int exhilarated = hooks.indexOf("ExhilaratedService.afterRealKill");
        assertTrue(lastStand > 0 && returnAfterLastStand > lastStand);
        assertTrue(realKill > returnAfterLastStand && exhilarated > realKill);
        assertEquals(100, ExhilaratedService.DURATION_TICKS);
        assertEquals(2, ExhilaratedService.AMPLIFIER);
        String service = Files.readString(MAIN.resolve("impl/traits/killer/ExhilaratedService.java"));
        assertTrue(service.contains("killer == victim"));
        assertTrue(service.contains("GameFunctions.isPlayerPlayingAndAlive(killer)"));
        assertTrue(service.contains("killer.addStatusEffect"));
        assertFalse(service.contains("removeStatusEffect"));
    }
}
