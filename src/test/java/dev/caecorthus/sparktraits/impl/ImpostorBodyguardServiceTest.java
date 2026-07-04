package dev.caecorthus.sparktraits.impl;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpostorBodyguardServiceTest {
    @Test
    void impostorBodyguardsDoNotProtectTheirTarget() {
        assertFalse(ImpostorBodyguardService.shouldProtectTarget(Set.of(ImpostorTrait.ID)));
        assertTrue(ImpostorBodyguardService.shouldProtectTarget(Set.of()));
    }

    @Test
    void targetDeathRewardRequiresLivingImpostorBodyguardAndCurrentTarget() {
        Set<Identifier> impostor = Set.of(ImpostorTrait.ID);

        assertEquals(100, ImpostorBodyguardService.targetDeathReward(impostor, true, true));
        assertEquals(0, ImpostorBodyguardService.targetDeathReward(Set.of(), true, true));
        assertEquals(0, ImpostorBodyguardService.targetDeathReward(impostor, false, true));
        assertEquals(0, ImpostorBodyguardService.targetDeathReward(impostor, true, false));
    }

    @Test
    void bodyguardMixinSupportsOldAndNewKillPlayerListenerLambdaNames() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/mixin/NoellesRolesBodyguardMixin.java"));

        assertTrue(source.contains("lambda$registerEvents$9"));
        assertTrue(source.contains("lambda$registerEvents$5"));
        assertTrue(source.contains("isPlayerPlayingAndAlive"));
    }
}
