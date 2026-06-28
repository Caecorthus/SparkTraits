package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepressionTraitServiceTest {
    @BeforeAll
    static void registerTraits() {
        if (!TraitRegistry.contains(ImpostorTrait.ID)) {
            TraitRegistry.register(new ImpostorTrait());
        }
        if (!TraitRegistry.contains(LastStandTrait.ID)) {
            TraitRegistry.register(new LastStandTrait());
        }
        if (!TraitRegistry.contains(GoodTraits.DEPRESSION)) {
            GoodTraits.register();
        }
    }

    @Test
    void randomDepressionRequiresLargeEnoughGameAndSpecificGoodRoles() {
        assertTrue(DepressionTraitService.canSelectDepression(WatheRoles.CIVILIAN, Set.of(), 24, true));
        assertTrue(DepressionTraitService.canSelectDepression(Noellesroles.DETECTIVE, Set.of(), 24, true));

        assertFalse(DepressionTraitService.canSelectDepression(WatheRoles.CIVILIAN, Set.of(), 23, true));
        assertFalse(DepressionTraitService.canSelectDepression(WatheRoles.VIGILANTE, Set.of(), 24, true));
        assertFalse(DepressionTraitService.canSelectDepression(WatheRoles.VETERAN, Set.of(), 24, true));
        assertFalse(DepressionTraitService.canSelectDepression(Noellesroles.UNDERCOVER, Set.of(), 24, true));
        assertFalse(DepressionTraitService.canSelectDepression(Noellesroles.SURVIVAL_MASTER, Set.of(), 24, true));
        assertFalse(DepressionTraitService.canSelectDepression(sparkWitchRole("apprentice_witch"), Set.of(), 24, true));
        assertFalse(DepressionTraitService.canSelectDepression(WatheRoles.KILLER, Set.of(), 24, true));
        assertFalse(DepressionTraitService.canSelectDepression(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID), 24, true));
    }

    @Test
    void adminLockedDepressionCanBypassPopulationOnly() {
        assertTrue(DepressionTraitService.canSelectDepression(WatheRoles.CIVILIAN, Set.of(), 1, false));
        assertFalse(DepressionTraitService.canSelectDepression(WatheRoles.VIGILANTE, Set.of(), 1, false));
        assertFalse(DepressionTraitService.canSelectDepression(WatheRoles.CIVILIAN, Set.of(ImpostorTrait.ID), 1, false));
    }

    @Test
    void depressionConflictsOnlyWithLastStand() {
        assertTrue(TraitRules.areIncompatible(
                TraitRegistry.get(GoodTraits.DEPRESSION),
                TraitRegistry.get(LastStandTrait.ID)
        ));
        assertFalse(TraitRules.areIncompatible(
                TraitRegistry.get(GoodTraits.DEPRESSION),
                TraitRegistry.get(GoodTraits.FOCUS)
        ));
    }

    @Test
    void depressionTraitKeepsOriginalRegistrationRules() {
        Trait depression = TraitRegistry.get(GoodTraits.DEPRESSION);

        assertNotNull(depression);
        assertEquals(DepressionTraitService.COLOR, depression.color());
        assertEquals(TraitAudience.INNOCENT_ONLY, depression.audience());
        assertTrue(depression.uniquePerGame());
        assertTrue(depression.incompatibleTraits().contains(ImpostorTrait.ID));
        assertTrue(depression.incompatibleTraits().contains(LastStandTrait.ID));
    }

    @Test
    void triggerChanceIsLinearFromMidMoodToMinusTwenty() {
        assertEquals(0.0, DepressionTraitService.triggerChance(0.55f), 0.0001);
        assertEquals(100.0, DepressionTraitService.triggerChance(-0.20f), 0.0001);
        assertEquals(50.0, DepressionTraitService.triggerChance(0.175f), 0.0001);
    }

    @Test
    void psychoCounterChanceIsLinearFromTenPercentAtMidMoodToGuaranteedAtZero() {
        assertEquals(0.0, DepressionTraitService.psychoCounterChance(0.56f), 0.0001);
        assertEquals(10.0, DepressionTraitService.psychoCounterChance(0.55f), 0.0001);
        assertEquals(55.0, DepressionTraitService.psychoCounterChance(0.275f), 0.0001);
        assertEquals(100.0, DepressionTraitService.psychoCounterChance(0.0f), 0.0001);
    }

    @Test
    void depressionOnlyMultipliesActualMoodDrain() {
        assertEquals(0.25f, DepressionTraitService.depressionAdjustedMood(
                1.0f,
                0.5f,
                Set.of(GoodTraits.DEPRESSION)
        ), 0.0001f);
        assertEquals(1.0f, DepressionTraitService.depressionAdjustedMood(
                1.0f,
                1.0f,
                Set.of(GoodTraits.DEPRESSION)
        ), 0.0001f);
        assertEquals(0.5f, DepressionTraitService.depressionAdjustedMood(
                1.0f,
                0.5f,
                Set.of()
        ), 0.0001f);
    }

    @Test
    void depressionScalesFiniteStaminaAndRecovery() {
        assertEquals(160, DepressionTraitService.depressionStaminaMax(200, true));
        assertEquals(-1, DepressionTraitService.depressionStaminaMax(-1, true));
        assertEquals(200, DepressionTraitService.depressionStaminaMax(200, false));

        assertEquals(10.2f, DepressionTraitService.depressionRecoveredStamina(10.0f, 10.25f, true), 0.0001f);
        assertEquals(9.0f, DepressionTraitService.depressionRecoveredStamina(10.0f, 9.0f, true), 0.0001f);
        assertEquals(10.25f, DepressionTraitService.depressionRecoveredStamina(10.0f, 10.25f, false), 0.0001f);
    }

    @Test
    void depressionStaminaAppliesOnlyToFiniteDepressionOutsidePsycho() {
        Role finiteRole = sparkWitchRole("finite_test");
        Role infiniteRole = new Role(
                Identifier.of("sparktraits", "infinite_test"),
                0xFFFFFF,
                true,
                false,
                Role.MoodType.REAL,
                -1,
                false
        );

        assertTrue(DepressionTraitService.shouldApplyDepressionStamina(finiteRole, true, false));
        assertFalse(DepressionTraitService.shouldApplyDepressionStamina(finiteRole, false, false));
        assertFalse(DepressionTraitService.shouldApplyDepressionStamina(finiteRole, true, true));
        assertFalse(DepressionTraitService.shouldApplyDepressionStamina(infiniteRole, true, false));
        assertFalse(DepressionTraitService.shouldApplyDepressionStamina(null, true, false));
        assertEquals(-0.2, DepressionTraitService.DEPRESSION_STAMINA_MODIFIER_VALUE, 0.0001);
    }

    @Test
    void playerBodyEquipmentMixinIsRegistered() throws Exception {
        try (InputStream stream = DepressionTraitServiceTest.class
                .getClassLoader()
                .getResourceAsStream("sparktraits.mixins.json")) {
            assertNotNull(stream);
            String config = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(config.contains("\"PlayerBodyEntityEquipmentMixin\""));
        }
    }

    @Test
    void suicideCountdownStartsWhenTriggerChanceIsPositive() {
        assertFalse(DepressionTraitService.shouldRunSuicideCountdown(0.55f));
        assertTrue(DepressionTraitService.shouldRunSuicideCountdown(0.549f));
        assertTrue(DepressionTraitService.shouldRunSuicideCountdown(0.25f));
    }

    @Test
    void depressionVisualStrengthIsLinearFromMidMoodToZero() {
        assertEquals(0.0f, DepressionTraitService.depressionScreenEffectStrength(true, false, 0.55f), 0.0001f);
        assertEquals(0.5f, DepressionTraitService.depressionScreenEffectStrength(true, false, 0.275f), 0.0001f);
        assertEquals(1.0f, DepressionTraitService.depressionScreenEffectStrength(true, false, 0.0f), 0.0001f);
        assertEquals(1.0f, DepressionTraitService.depressionScreenEffectStrength(true, true, -1, 0.5f), 0.0001f);
        assertEquals(0.0f, DepressionTraitService.depressionScreenEffectStrength(false, true, 100, -0.2f), 0.0001f);
    }

    @Test
    void psychoRestrictionsOnlyApplyWhileDepressionPsychoIsActive() {
        assertFalse(DepressionTraitService.shouldMuteVoice(false));
        assertTrue(DepressionTraitService.shouldMuteVoice(true));
        assertFalse(DepressionTraitService.shouldBlockInventoryInsert(false, false));
        assertFalse(DepressionTraitService.shouldBlockInventoryInsert(true, true));
        assertTrue(DepressionTraitService.shouldBlockInventoryInsert(true, false));
    }

    private static Role sparkWitchRole(String path) {
        return new Role(
                Identifier.of("sparkwitch", path),
                0xFFFFFF,
                true,
                false,
                Role.MoodType.REAL,
                200,
                false
        );
    }
}
