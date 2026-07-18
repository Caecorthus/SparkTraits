package dev.caecorthus.sparktraits.impl.traits.civilian;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTreeBalanceContractTest {
    @Test
    void moneyTreePaysFiveEveryTenSeconds() {
        assertEquals(5, CivilianTraitService.MONEY_TREE_REWARD);
        assertEquals(20 * 10, CivilianTraitService.MONEY_TREE_INTERVAL_TICKS);
    }
}
