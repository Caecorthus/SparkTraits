package dev.caecorthus.sparktraits.impl.effective;

import dev.caecorthus.sparktraits.compat.SparkWitchWraithBridge;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.doctor4t.wathe.api.Role;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EffectiveTraitServiceWraithMoodTest {
    @Test
    void normalConscienceKillerKeepsRealMoodBar() {
        assertEquals(
                Role.MoodType.REAL,
                EffectiveTraitService.effectiveMoodType(null, List.of(ConscienceTrait.ID), false)
        );
    }

    @Test
    void activeConscienceWraithHidesMoodBarWithoutRoleOrStageInference() {
        List<net.minecraft.util.Identifier> traits = List.of(ConscienceTrait.ID);
        for (String roleId : List.of(
                "wraith",
                "wind_spirit",
                "guardian_angel",
                "vendetta",
                "saboteur",
                "curser"
        )) {
            Role role = new Role(
                    net.minecraft.util.Identifier.of("sparkwitch", roleId),
                    0,
                    false,
                    false,
                    Role.MoodType.REAL,
                    -1,
                    true
            );
            assertEquals(
                    Role.MoodType.NONE,
                    EffectiveTraitService.effectiveMoodType(role, traits, true),
                    roleId
            );
        }
    }

    @Test
    void cleanupRestoresMoodBarWithoutRemovingConscience() {
        List<net.minecraft.util.Identifier> traits = List.of(ConscienceTrait.ID);

        assertEquals(Role.MoodType.NONE, EffectiveTraitService.effectiveMoodType(null, traits, true));
        assertEquals(Role.MoodType.REAL, EffectiveTraitService.effectiveMoodType(null, traits, false));
        assertEquals(List.of(ConscienceTrait.ID), traits);
    }

    @Test
    void missingSparkWitchFailsOpen() {
        assertFalse(SparkWitchWraithBridge.isWraithActive(null));
        assertEquals(Role.MoodType.REAL, EffectiveTraitService.effectiveConscienceMoodType(false));
    }

    @Test
    void bridgeExposesTheApprovedActivePlayerSignature() throws Exception {
        Method method = SparkWitchWraithBridge.class.getMethod(
                "isWraithActive",
                net.minecraft.entity.player.PlayerEntity.class
        );

        assertEquals(boolean.class, method.getReturnType());
        assertEquals(true, Modifier.isPublic(method.getModifiers()));
        assertEquals(true, Modifier.isStatic(method.getModifiers()));
    }

    @Test
    void bridgeUsesOnlyLoaderSafeReflectiveSparkWitchReferences() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/compat/SparkWitchWraithBridge.java"
        ));

        assertEquals(false, source.contains("import dev.caecorthus.sparkwitch"));
        assertEquals(true, source.contains("Class.forName(API_CLASS)"));
        assertEquals(true, source.contains("booleanQuery(api, \"isWraithActive\")"));
        assertEquals(false, source.contains("isKillerAlignedWraith"));
    }
}
