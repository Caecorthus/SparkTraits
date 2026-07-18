package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import dev.doctor4t.wathe.api.WatheRoles;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalMomentVigilanteRoleTest {
    @Test
    void onlyMarkedFinalMomentLooseEndUsesVigilanteRuntimeRules() {
        assertSame(
                WatheRoles.VIGILANTE,
                VigilanteVeteranTraitService.runtimeVigilanteRole(WatheRoles.LOOSE_END, true)
        );
        assertSame(
                WatheRoles.LOOSE_END,
                VigilanteVeteranTraitService.runtimeVigilanteRole(WatheRoles.LOOSE_END, false)
        );
        assertSame(
                WatheRoles.CIVILIAN,
                VigilanteVeteranTraitService.runtimeVigilanteRole(WatheRoles.CIVILIAN, true)
        );
        assertSame(
                WatheRoles.VIGILANTE,
                VigilanteVeteranTraitService.runtimeVigilanteRole(WatheRoles.VIGILANTE, false)
        );
    }

    @Test
    void nikoNightVisionUsesTheRuntimeVigilanteRole() throws IOException {
        Path sourcePath = Path.of(
                System.getProperty("user.dir"),
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/police/VigilanteVeteranTraitService.java"
        );
        String source = Files.readString(sourcePath).replaceAll("\\s+", " ");

        assertTrue(source.contains(
                "shouldRefreshNikoNightVision( GameFunctions.isPlayerPlayingAndAlive(player), roleOf(player),"
        ));
    }
}
