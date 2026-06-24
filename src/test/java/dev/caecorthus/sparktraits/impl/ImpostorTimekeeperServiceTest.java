package dev.caecorthus.sparktraits.impl;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImpostorTimekeeperServiceTest {
    @Test
    void impostorTimekeeperAddsTimeWhenBuyingReduceTime() {
        assertEquals(-900, ImpostorTimekeeperService.timekeeperPurchaseDelta(Set.of(), -900));
        assertEquals(900, ImpostorTimekeeperService.timekeeperPurchaseDelta(Set.of(ImpostorTrait.ID), -900));
    }
}
