package dev.caecorthus.sparktraits.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodthirstyCooldownMixinContractTest {
    private static final Path TAG_PATH = Path.of(
            "src/main/resources/data/sparktraits/tags/item/bloodthirsty_weapons.json"
    );

    @Test
    void baseTagContainsKnifeAndPoisonNeedleWithoutBlockingContributors() throws IOException {
        assertTrue(Files.isRegularFile(TAG_PATH), "missing sparktraits:bloodthirsty_weapons item tag");

        JsonObject tag = JsonParser.parseString(Files.readString(TAG_PATH)).getAsJsonObject();
        JsonArray values = tag.getAsJsonArray("values");
        Set<String> itemIds = new HashSet<>();
        values.forEach(value -> itemIds.add(value.getAsString()));

        assertFalse(tag.get("replace").getAsBoolean());
        assertEquals(Set.of("wathe:knife", "noellesroles:poison_needle"), itemIds);
    }

    @Test
    void cooldownMixinUsesOnlyTheExtensibleWeaponTagAsItsEligibilityGate() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/BloodthirstyCooldownMixin.java"
        )).replaceAll("\\s+", " ");

        assertTrue(source.contains("Registries.ITEM.getEntry(item).isIn(BLOODTHIRSTY_WEAPONS)"));
        assertFalse(source.contains("WatheItems.KNIFE"));
        assertTrue(source.contains("KillerTraitService.bloodthirstyCooldown(player, duration)"));
    }
}
