package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BluePoisonPresentationRulesTest {
    @Test
    void nativePoisonRemainsTheSinglePresentationOwner() {
        assertEquals(BluePoisonPresentationRules.Owner.NONE,
                BluePoisonPresentationRules.owner(false, false));
        assertEquals(BluePoisonPresentationRules.Owner.WATHE_NATIVE,
                BluePoisonPresentationRules.owner(true, false));
        assertEquals(BluePoisonPresentationRules.Owner.BLUE_POISON,
                BluePoisonPresentationRules.owner(false, true));
        assertEquals(BluePoisonPresentationRules.Owner.WATHE_NATIVE,
                BluePoisonPresentationRules.owner(true, true));
    }

    @Test
    void symptomsStartAfterWatheDelayAndUseLocalCountdown() {
        assertFalse(BluePoisonPresentationRules.canPulse(1400, 1201));
        assertTrue(BluePoisonPresentationRules.canPulse(1400, 1200));
        assertEquals(1180, BluePoisonPresentationRules.remainingTicks(1200, 20));
        assertEquals(0, BluePoisonPresentationRules.remainingTicks(10, 20));
        assertEquals(52, BluePoisonPresentationRules.pulseInterval(1200, 1400));
        assertEquals(51, BluePoisonPresentationRules.cooldownAfterPulse(1200, 1400));
    }

    @Test
    void componentSyncIncludesOwnerAndKeepsInitialDuration() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/component/TraitPlayerComponent.java"
        ));

        assertTrue(source.contains("consciencePoisonInitialTicks"));
        assertTrue(source.contains("recipient == player"));
        assertTrue(source.contains("ConsciencePoisonInitialTicks"));
        assertTrue(source.contains("buf.readableBytes()"));
    }

    @Test
    void clientMixinDrivesWathePulseOnlyForBlueOnlyPoison() throws IOException {
        String source = Files.readString(Path.of(
                "src/client/java/dev/caecorthus/sparktraits/client/mixin/PlayerPoisonComponentBluePoisonMixin.java"
        ));
        String mixins = Files.readString(Path.of(
                "src/client/resources/sparktraits.client.mixins.json"
        ));

        assertTrue(source.contains("method = \"clientTick\""));
        assertTrue(source.contains("nativePoisonTicks > 0"));
        assertTrue(source.contains("BluePoisonPresentationRules.canPulse"));
        assertTrue(source.contains("SoundEvents.ENTITY_WARDEN_HEARTBEAT"));
        assertTrue(mixins.contains("PlayerPoisonComponentBluePoisonMixin"));
    }
}
