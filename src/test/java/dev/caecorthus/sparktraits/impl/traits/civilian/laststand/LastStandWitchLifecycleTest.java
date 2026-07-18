package dev.caecorthus.sparktraits.impl.traits.civilian.laststand;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastStandWitchLifecycleTest {
    private static final UUID HOLDER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ATTACKER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void grandWitchAndAccompliceKillsQualifyForLastStand() {
        assertTrue(canTriggerFromSparkWitchRole("grand_witch"));
        assertTrue(canTriggerFromSparkWitchRole("accomplice"));
    }

    @Test
    void pendingLastStandBlocksCustomNeutralResolution() {
        assertTrue(LastStandService.shouldBlockRoundEnd(true, GameFunctions.WinStatus.NEUTRAL));
    }

    @Test
    void revivedTriggeredHolderStartsFinalMomentAgainstGrandWitchAndAccomplice() {
        assertFinalMomentStartsAfterRevive("grand_witch");
        assertFinalMomentStartsAfterRevive("accomplice");
    }

    @Test
    void finalMomentStillDoesNotOverrideTimeOrResolvedNeutralWins() {
        List<LastStandFinalMomentService.PlayerState> players = revivedPlayers("grand_witch");

        assertFalse(LastStandFinalMomentService.canTriggerInactiveFinalMoment(
                true,
                false,
                GameFunctions.WinStatus.TIME,
                players
        ));
        assertFalse(LastStandFinalMomentService.canTriggerInactiveFinalMoment(
                true,
                false,
                GameFunctions.WinStatus.NEUTRAL,
                players
        ));
    }

    @Test
    void finalMomentPreHookRunsBeforeWinConditionListeners() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/MurderGameModeMixin.java"
        ));
        int hook = source.indexOf("sparktraits$startFinalMomentBeforeWinConditionHooks");
        int trigger = source.indexOf("LastStandFinalMomentService.triggerFinalMomentIfEligible", hook);
        int cancel = source.indexOf("ci.cancel()", trigger);

        assertTrue(hook >= 0);
        assertTrue(trigger > hook);
        assertTrue(cancel > trigger);
        assertTrue(source.contains("target = \"Ldev/doctor4t/wathe/api/event/CheckWinCondition;checkWin"));
    }

    private static boolean canTriggerFromSparkWitchRole(String path) {
        return LastStandService.canTriggerFromKill(
                WatheRoles.CIVILIAN,
                Set.of(LastStandTrait.ID),
                sparkWitchRole(path),
                Set.of()
        );
    }

    private static void assertFinalMomentStartsAfterRevive(String path) {
        List<LastStandFinalMomentService.PlayerState> pendingPlayers = List.of(
                state(HOLDER, WatheRoles.CIVILIAN, false, true),
                state(ATTACKER, sparkWitchRole(path), true, false)
        );
        assertFalse(LastStandFinalMomentService.canTriggerInactiveFinalMoment(
                true,
                false,
                GameFunctions.WinStatus.PASSENGERS,
                pendingPlayers
        ));
        for (GameFunctions.WinStatus baseStatus : List.of(
                GameFunctions.WinStatus.NONE,
                GameFunctions.WinStatus.PASSENGERS,
                GameFunctions.WinStatus.KILLERS
        )) {
            assertTrue(LastStandFinalMomentService.canTriggerInactiveFinalMoment(
                    true,
                    false,
                    baseStatus,
                    revivedPlayers(path)
            ));
        }
    }

    private static List<LastStandFinalMomentService.PlayerState> revivedPlayers(String path) {
        return List.of(
                state(HOLDER, WatheRoles.CIVILIAN, true, true),
                state(ATTACKER, sparkWitchRole(path), true, false)
        );
    }

    private static LastStandFinalMomentService.PlayerState state(
            UUID uuid,
            Role role,
            boolean alive,
            boolean lastStandTriggered
    ) {
        return new LastStandFinalMomentService.PlayerState(
                uuid,
                role,
                Set.of(),
                alive,
                lastStandTriggered
        );
    }

    private static Role sparkWitchRole(String path) {
        return new Role(
                Identifier.of("sparkwitch", path),
                0xFFFFFF,
                false,
                false,
                Role.MoodType.FAKE,
                -1,
                false
        );
    }
}
