package dev.caecorthus.sparktraits.mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnifeCooldownKnockbackMixinContractTest {
    @Test
    void cooldownBypassUsesTheExtensibleThrustWeaponContract() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/KnifeCooldownKnockbackMixin.java"
        )).replaceAll("\\s+", " ");

        assertTrue(source.contains("KillerWeaponTags.isThrustWeapon(attacker.getMainHandStack()),"));
        assertFalse(source.contains("WatheItems.KNIFE"));
        assertFalse(source.contains("ModItems.POISON_NEEDLE"));
    }
}
