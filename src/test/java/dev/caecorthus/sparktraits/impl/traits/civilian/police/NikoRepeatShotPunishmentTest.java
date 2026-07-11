package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NikoRepeatShotPunishmentTest {
    @Test
    void eachNikoRepeatShotUsesCurrentVictimsEffectiveAlignment() {
        assertEquals(VigilanteVeteranTraitService.NikoRepeatShotPunishment.NONE, decideFor(WatheRoles.KILLER, Set.of()));
        assertEquals(VigilanteVeteranTraitService.NikoRepeatShotPunishment.KILL_SHOOTER, decideFor(WatheRoles.CIVILIAN, Set.of()));
        assertEquals(VigilanteVeteranTraitService.NikoRepeatShotPunishment.KILL_SHOOTER, decideFor(WatheRoles.KILLER, Set.of(ConscienceTrait.ID)));
        assertEquals(VigilanteVeteranTraitService.NikoRepeatShotPunishment.NONE, decideFor(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID)));
    }

    @Test
    void nikoRepeatShotHonorsCancelCustomAllowAndDefaultEventResults() {
        AtomicBoolean customExecuted = new AtomicBoolean();
        ShouldPunishGunShooter.PunishResult custom = ShouldPunishGunShooter.PunishResult.custom(
                () -> customExecuted.set(true)
        );

        assertEquals(
                VigilanteVeteranTraitService.NikoRepeatShotPunishment.NONE,
                decide(true, ShouldPunishGunShooter.PunishResult.cancel(), false)
        );
        assertEquals(
                VigilanteVeteranTraitService.NikoRepeatShotPunishment.CUSTOM,
                decide(false, custom, false)
        );
        assertEquals(
                VigilanteVeteranTraitService.NikoRepeatShotPunishment.KILL_SHOOTER,
                decide(true, ShouldPunishGunShooter.PunishResult.allow(), false)
        );
        assertEquals(
                VigilanteVeteranTraitService.NikoRepeatShotPunishment.KILL_SHOOTER,
                decide(true, null, false)
        );
        assertFalse(customExecuted.get());
    }

    @Test
    void nikoRepeatShotKeepsConfiguredPunishmentModesDistinct() {
        assertEquals(
                VigilanteVeteranTraitService.NikoRepeatShotPunishment.KILL_SHOOTER,
                decide(true, null, false, GameWorldComponent.ShootInnocentPunishment.KILL_SHOOTER)
        );
        assertEquals(
                VigilanteVeteranTraitService.NikoRepeatShotPunishment.PREVENT_GUN_PICKUP,
                decide(true, null, false, GameWorldComponent.ShootInnocentPunishment.PREVENT_GUN_PICKUP)
        );
    }

    @Test
    void creativeNikoSkipsNativePunishmentButNotCustomPunishment() {
        ShouldPunishGunShooter.PunishResult custom = ShouldPunishGunShooter.PunishResult.custom(() -> {
        });

        assertEquals(
                VigilanteVeteranTraitService.NikoRepeatShotPunishment.NONE,
                decide(true, null, true)
        );
        assertEquals(
                VigilanteVeteranTraitService.NikoRepeatShotPunishment.NONE,
                decide(true, ShouldPunishGunShooter.PunishResult.allow(), true)
        );
        assertEquals(
                VigilanteVeteranTraitService.NikoRepeatShotPunishment.CUSTOM,
                decide(true, custom, true)
        );
    }

    @Test
    void onlyNativeNikoPunishmentsAttemptBackfire() {
        assertTrue(VigilanteVeteranTraitService.NikoRepeatShotPunishment.KILL_SHOOTER.attemptsBackfire());
        assertTrue(VigilanteVeteranTraitService.NikoRepeatShotPunishment.PREVENT_GUN_PICKUP.attemptsBackfire());
        assertFalse(VigilanteVeteranTraitService.NikoRepeatShotPunishment.NONE.attemptsBackfire());
        assertFalse(VigilanteVeteranTraitService.NikoRepeatShotPunishment.CUSTOM.attemptsBackfire());
    }

    private static VigilanteVeteranTraitService.NikoRepeatShotPunishment decideFor(
            dev.doctor4t.wathe.api.Role victimRole,
            Set<net.minecraft.util.Identifier> victimTraits
    ) {
        boolean effectiveCivilian = EffectiveTraitService.shouldTreatGunVictimAsInnocent(victimRole, victimTraits);
        return VigilanteVeteranTraitService.decideNikoRepeatShotPunishment(
                effectiveCivilian,
                null,
                false,
                GameWorldComponent.ShootInnocentPunishment.KILL_SHOOTER
        );
    }

    private static VigilanteVeteranTraitService.NikoRepeatShotPunishment decide(
            boolean effectiveCivilian,
            ShouldPunishGunShooter.PunishResult eventResult,
            boolean shooterCreative
    ) {
        return decide(
                effectiveCivilian,
                eventResult,
                shooterCreative,
                GameWorldComponent.ShootInnocentPunishment.KILL_SHOOTER
        );
    }

    private static VigilanteVeteranTraitService.NikoRepeatShotPunishment decide(
            boolean effectiveCivilian,
            ShouldPunishGunShooter.PunishResult eventResult,
            boolean shooterCreative,
            GameWorldComponent.ShootInnocentPunishment configuredPunishment
    ) {
        return VigilanteVeteranTraitService.decideNikoRepeatShotPunishment(
                effectiveCivilian,
                eventResult,
                shooterCreative,
                configuredPunishment
        );
    }

}
