package dev.caecorthus.sparktraits.impl;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

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
}
