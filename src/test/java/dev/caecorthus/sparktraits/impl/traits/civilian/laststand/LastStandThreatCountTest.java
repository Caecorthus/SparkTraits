package dev.caecorthus.sparktraits.impl.traits.civilian.laststand;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastStandThreatCountTest {
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void grandWitchCountsTowardTheExistingTwoThreatThreshold() {
        int threats = LastStandTrait.countLastStandThreatPlayers(List.of(FIRST), Map.of(
                FIRST, WatheRoles.KILLER,
                SECOND, sparkWitchRole("grand_witch")
        ));

        assertEquals(2, threats);
        assertTrue(LastStandTrait.canSelectLastStand(WatheRoles.CIVILIAN, threats));
    }

    @Test
    void accompliceCountsTowardTheExistingTwoThreatThreshold() {
        int threats = LastStandTrait.countLastStandThreatPlayers(List.of(FIRST), Map.of(
                FIRST, WatheRoles.KILLER,
                SECOND, sparkWitchRole("accomplice")
        ));

        assertEquals(2, threats);
        assertTrue(LastStandTrait.canSelectLastStand(WatheRoles.CIVILIAN, threats));
    }

    @Test
    void grandWitchAndAccompliceCanSatisfyTheThresholdTogether() {
        int threats = LastStandTrait.countLastStandThreatPlayers(List.of(), Map.of(
                FIRST, sparkWitchRole("grand_witch"),
                SECOND, sparkWitchRole("accomplice")
        ));

        assertEquals(2, threats);
        assertTrue(LastStandTrait.canSelectLastStand(WatheRoles.CIVILIAN, threats));
    }

    @Test
    void twoAccomplicePlayersCountAsTwoThreats() {
        Role accomplice = sparkWitchRole("accomplice");
        int threats = LastStandTrait.countLastStandThreatPlayers(List.of(), Map.of(
                FIRST, accomplice,
                SECOND, accomplice
        ));

        assertEquals(2, threats);
    }

    @Test
    void murderousWitchDoesNotCountTowardTheSelectionThreshold() {
        int threats = LastStandTrait.countLastStandThreatPlayers(List.of(FIRST), Map.of(
                FIRST, WatheRoles.KILLER,
                SECOND, sparkWitchRole("murderous_witch")
        ));

        assertEquals(1, threats);
        assertFalse(LastStandTrait.canSelectLastStand(WatheRoles.CIVILIAN, threats));
    }

    @Test
    void aCustomThreatInTheNativeKillerBucketIsCountedOnce() {
        int threats = LastStandTrait.countLastStandThreatPlayers(List.of(FIRST), Map.of(
                FIRST, sparkWitchRole("grand_witch")
        ));

        assertEquals(1, threats);
    }

    @Test
    void canApplyUsesTheThreatCounter() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/civilian/laststand/LastStandTrait.java"
        ));
        int canApplyStart = source.indexOf("public boolean canApply");
        int canApplyEnd = source.indexOf("static boolean canSelectLastStand", canApplyStart);

        assertTrue(canApplyStart >= 0);
        assertTrue(canApplyEnd > canApplyStart);
        String canApplyBody = source.substring(canApplyStart, canApplyEnd);

        assertTrue(canApplyBody.contains("countLastStandThreatPlayers("));
        assertTrue(canApplyBody.contains("getAllKillerTeamPlayers()"));
        assertTrue(canApplyBody.contains("getRoles()"));
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
