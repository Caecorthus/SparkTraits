package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NikoHeavyArtilleryCompositionTest {
    private static final Set<Identifier> BOTH = Set.of(PoliceTraits.NIKO, PoliceTraits.HEAVY_ARTILLERY);

    @Test
    void exactVigilanteAndMarkedFinalMomentLooseEndSupportTheComposition() {
        assertCompositionSupported(WatheRoles.VIGILANTE, false);
        assertCompositionSupported(WatheRoles.LOOSE_END, true);

        assertCompositionUnsupported(WatheRoles.VETERAN, false);
        assertCompositionUnsupported(WatheRoles.LOOSE_END, false);
        assertCompositionUnsupported(WatheRoles.CIVILIAN, true);
    }

    @Test
    void nikoAndHeavyRetainTheirIndependentTraitCrouchAndRangeGates() {
        Role role = WatheRoles.VIGILANTE;

        assertTrue(canStartNiko(role, BOTH, true));
        assertTrue(isHeavy(role, BOTH, 4.9));
        assertFalse(canStartNiko(role, BOTH, false));
        assertTrue(isHeavy(role, BOTH, 5.0));
        assertFalse(isHeavy(role, BOTH, 5.1));

        assertTrue(canStartNiko(role, Set.of(PoliceTraits.NIKO), true));
        assertFalse(isHeavy(role, Set.of(PoliceTraits.NIKO), 4.9));
        assertFalse(canStartNiko(role, Set.of(PoliceTraits.HEAVY_ARTILLERY), true));
        assertTrue(isHeavy(role, Set.of(PoliceTraits.HEAVY_ARTILLERY), 4.9));
    }

    @Test
    void heavyRetryPreservesShieldAndLastStandHandoffsButNotDeadOrTransitioningJesterTargets() {
        assertFalse(VigilanteVeteranTraitService.shouldRetryHeavyArtilleryDamage(true, false, false));
        assertTrue(VigilanteVeteranTraitService.shouldRetryHeavyArtilleryDamage(true, true, false));
        assertFalse(VigilanteVeteranTraitService.shouldRetryHeavyArtilleryDamage(true, true, true));
        assertFalse(VigilanteVeteranTraitService.shouldRetryHeavyArtilleryDamage(false, true, false));
    }

    @Test
    void initialAndBothSyntheticBurstHitsUseTheSameHeavyResolver() throws IOException {
        String source = Files.readString(Path.of(
                System.getProperty("user.dir"),
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/police/VigilanteVeteranTraitService.java"
        ));
        String initialResolver = section(
                source,
                "public static void killPlayerWithPoliceGunTraits(",
                "public static float wellTrainedAdjustedMood("
        );
        String syntheticResolver = section(
                source,
                "private static void resolveNikoBurstShot(",
                "private static void runNikoBurstContinuation("
        );
        String scheduler = section(
                source,
                "public static void scheduleNikoRevolverBurstRepeats(",
                "private static void repeatNikoBurstShot("
        );

        assertTrue(initialResolver.contains("killPlayerWithHeavyArtillery("));
        assertTrue(syntheticResolver.contains("killPlayerWithHeavyArtillery("));
        assertTrue(scheduler.contains("for (int shot = 1; shot < NIKO_BURST_SHOTS; shot++)"));
        assertTrue(scheduler.contains("NIKO_BURST_INTERVAL_TICKS * shot"));
        assertTrue(VigilanteVeteranTraitService.NIKO_BURST_SHOTS == 3);
    }

    private static void assertCompositionSupported(Role assignedRole, boolean markedFinalMomentLooseEnd) {
        Role runtimeRole = VigilanteVeteranTraitService.runtimeVigilanteRole(
                assignedRole,
                markedFinalMomentLooseEnd
        );
        assertSame(WatheRoles.VIGILANTE, runtimeRole);
        assertTrue(canStartNiko(runtimeRole, BOTH, true));
        assertTrue(isHeavy(runtimeRole, BOTH, 4.9));
    }

    private static void assertCompositionUnsupported(Role assignedRole, boolean markedFinalMomentLooseEnd) {
        Role runtimeRole = VigilanteVeteranTraitService.runtimeVigilanteRole(
                assignedRole,
                markedFinalMomentLooseEnd
        );
        assertFalse(canStartNiko(runtimeRole, BOTH, true));
        assertFalse(isHeavy(runtimeRole, BOTH, 4.9));
    }

    private static boolean canStartNiko(Role role, Set<Identifier> traits, boolean crouching) {
        return VigilanteVeteranTraitService.shouldStartNikoRevolverBurst(
                true,
                false,
                true,
                true,
                role,
                traits,
                crouching
        );
    }

    private static boolean isHeavy(Role role, Set<Identifier> traits, double distance) {
        return VigilanteVeteranTraitService.isHeavyArtilleryShot(
                role,
                traits,
                GameConstants.DeathReasons.GUN,
                distance * distance
        );
    }

    private static String section(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue(start >= 0, "Missing source marker: " + startMarker);
        assertTrue(end > start, "Missing source marker: " + endMarker);
        return source.substring(start, end);
    }
}
