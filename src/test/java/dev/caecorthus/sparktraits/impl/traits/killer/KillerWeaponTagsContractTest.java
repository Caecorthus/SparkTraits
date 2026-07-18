package dev.caecorthus.sparktraits.impl.traits.killer;

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

class KillerWeaponTagsContractTest {
    private static final Path THRUST_TAG = Path.of(
            "src/main/resources/data/sparktraits/tags/item/thrust_weapons.json"
    );
    private static final Path TAG_HELPER = Path.of(
            "src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerWeaponTags.java"
    );

    @Test
    void baseThrustTagContainsOnlyKnifeAndPoisonNeedleWithoutBlockingContributors() throws IOException {
        assertTrue(Files.isRegularFile(THRUST_TAG), "missing sparktraits:thrust_weapons item tag");

        JsonObject tag = JsonParser.parseString(Files.readString(THRUST_TAG)).getAsJsonObject();
        JsonArray values = tag.getAsJsonArray("values");
        Set<String> itemIds = new HashSet<>();
        values.forEach(value -> itemIds.add(value.getAsString()));

        assertFalse(tag.get("replace").getAsBoolean());
        assertEquals(Set.of("wathe:knife", "noellesroles:poison_needle"), itemIds);
    }

    @Test
    void sharedHelperOwnsOnlyTheIndependentThrustWeaponTag() throws IOException {
        assertTrue(Files.isRegularFile(TAG_HELPER), "missing shared killer weapon tag helper");

        String source = compact(Files.readString(TAG_HELPER));
        assertTrue(source.contains("SparkTraits.id(\"thrust_weapons\")"));
        assertTrue(source.contains("stack.isIn(THRUST_WEAPONS)"));
        assertFalse(source.contains("bloodthirsty_weapons"));
        assertFalse(source.contains("sparkwitch"));
    }

    @Test
    void selectionRuntimeModifierAndCooldownBypassShareTheThrustTagGate() throws IOException {
        String service = compact(Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraitService.java"
        )));
        String traits = compact(Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/killer/KillerTraits.java"
        )));
        String mixin = compact(Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/KnifeCooldownKnockbackMixin.java"
        )));

        assertTrue(service.contains("KillerWeaponTags.isThrustWeapon(entry.stack())"));
        assertTrue(service.contains("player.getInventory().contains(KillerWeaponTags::isThrustWeapon)"));
        assertTrue(service.contains("KillerWeaponTags.isThrustWeapon(player.getMainHandStack())"));
        assertTrue(traits.contains("KillerTraitService.hasThrustWeaponAccess(context.player())"));
        assertTrue(mixin.contains("KillerWeaponTags.isThrustWeapon(attacker.getMainHandStack())"));

        assertFalse(service.contains("isOf(WatheItems.KNIFE)"));
        assertFalse(service.contains("isOf(ModItems.POISON_NEEDLE)"));
        assertFalse(mixin.contains("WatheItems.KNIFE"));
        assertFalse(mixin.contains("ModItems.POISON_NEEDLE"));
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", " ");
    }
}
