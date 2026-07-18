package dev.caecorthus.sparktraits.impl.traits.killer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrustWeaponTagContractTest {
    private static final Path TAG_PATH = Path.of(
            "src/main/resources/data/sparktraits/tags/item/thrust_weapons.json"
    );

    @Test
    void baseTagContainsKnifeAndPoisonNeedleWithoutBlockingContributors() throws IOException {
        assertTrue(Files.isRegularFile(TAG_PATH), "missing sparktraits:thrust_weapons item tag");

        JsonObject tag = JsonParser.parseString(Files.readString(TAG_PATH)).getAsJsonObject();
        JsonArray values = tag.getAsJsonArray("values");
        Set<String> itemIds = new HashSet<>();
        values.forEach(value -> itemIds.add(value.getAsString()));

        assertFalse(tag.get("replace").getAsBoolean());
        assertEquals(Set.of("wathe:knife", "noellesroles:poison_needle"), itemIds);
    }

    @Test
    void selectionAndRuntimeUseTheSameTaggedWeaponRule() throws IOException {
        String tags = source("impl/traits/killer/KillerWeaponTags.java");
        String service = source("impl/traits/killer/KillerTraitService.java");

        assertTrue(tags.contains("SparkTraits.id(\"thrust_weapons\")"));
        assertTrue(tags.contains("stack.isIn(THRUST_WEAPONS)"));
        assertTrue(service.contains("KillerWeaponTags.isThrustWeapon(entry.stack())"));
        assertTrue(service.contains("player.getInventory().contains(KillerWeaponTags::isThrustWeapon)"));
        assertTrue(service.contains("KillerWeaponTags.isThrustWeapon(player.getMainHandStack())"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/" + relativePath
        )).replaceAll("\\s+", " ");
    }
}
