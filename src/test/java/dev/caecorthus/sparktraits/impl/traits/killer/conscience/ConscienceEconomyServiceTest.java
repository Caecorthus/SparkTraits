package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import dev.doctor4t.wathe.game.GameConstants;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConscienceEconomyServiceTest {
    @Test
    void dividendRequiresAnotherPlayersConfirmedDeathAndLivingConscienceOwner() {
        assertEquals(10, ConscienceEconomyService.deathDividend(true, true, true, false));
        assertEquals(0, ConscienceEconomyService.deathDividend(false, true, true, false));
        assertEquals(0, ConscienceEconomyService.deathDividend(true, false, true, false));
        assertEquals(0, ConscienceEconomyService.deathDividend(true, true, false, false));
        assertEquals(0, ConscienceEconomyService.deathDividend(true, true, true, true));
    }

    @Test
    void existingDirectRewardsRemainIndependent() {
        assertEquals(100, ConscienceSerialKillerService.conscienceKillReward(false, true, false));
        assertEquals(150, ConscienceSerialKillerService.conscienceKillReward(true, true, false));
        assertEquals(200, ConscienceSerialKillerService.conscienceKillReward(true, true, true));
        assertEquals(110, 100 + ConscienceEconomyService.DEATH_DIVIDEND);
        assertEquals(160, 150 + ConscienceEconomyService.DEATH_DIVIDEND);
        assertEquals(210, 200 + ConscienceEconomyService.DEATH_DIVIDEND);
    }

    @Test
    void wathePassiveIncomeContractIsFiveEveryTenSecondsWithExistingCap() {
        assertEquals(5, GameConstants.PASSIVE_MONEY_TICKER.apply(200L));
        assertEquals(0, GameConstants.PASSIVE_MONEY_TICKER.apply(201L));
        assertEquals(200, GameConstants.KILLER_PASSIVE_MONEY_CAP);
    }

    @Test
    void sparkTraitsDoesNotOverrideWathePassiveKillerEligibility() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/MurderGameModeMixin.java"
        ));

        assertFalse(source.contains("sparktraits$passiveMoneyOnlyForRealKillers"));
        assertFalse(source.contains("shouldReceiveKillerPassiveMoney"));
    }
}
