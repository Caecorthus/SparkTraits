package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.WatheRoles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConscienceCrowbarServiceTest {
    @Test
    void conscienceKillersUseCrowbarOnceEveryTwoMinutes() {
        assertEquals(2400, ConscienceCrowbarService.crowbarCooldownTicks(
                WatheRoles.KILLER,
                Set.of(ConscienceTrait.ID),
                200
        ));
    }

    @Test
    void nonConscienceKillerCrowbarsKeepOriginalCooldown() {
        assertEquals(200, ConscienceCrowbarService.crowbarCooldownTicks(
                WatheRoles.KILLER,
                Set.of(),
                200
        ));
    }

    @Test
    void nonKillerConscienceCrowbarsKeepOriginalCooldown() {
        assertEquals(200, ConscienceCrowbarService.crowbarCooldownTicks(
                WatheRoles.CIVILIAN,
                Set.of(ConscienceTrait.ID),
                200
        ));
    }
}
