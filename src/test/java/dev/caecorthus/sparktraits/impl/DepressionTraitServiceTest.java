package dev.caecorthus.sparktraits.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    void depressionPsychoFreezesMoodDrainButAllowsRecovery() {
        assertEquals(1.0f, DepressionTraitService.depressionAdjustedMood(
                1.0f,
                0.5f,
                Set.of(GoodTraits.DEPRESSION),
                true
        ), 0.0001f);
        assertEquals(1.1f, DepressionTraitService.depressionAdjustedMood(
                1.0f,
                1.1f,
                Set.of(GoodTraits.DEPRESSION),
                true
        ), 0.0001f);
        assertEquals(0.5f, DepressionTraitService.depressionAdjustedMood(
                1.0f,
                0.5f,
                Set.of(),
                true
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
    void lowMoodSprintIsAllowedBySyncedPsychoStateOrRuntimeState() {
        assertFalse(DepressionTraitService.shouldAllowLowMoodSprint(false, false));
        assertTrue(DepressionTraitService.shouldAllowLowMoodSprint(true, false));
        assertTrue(DepressionTraitService.shouldAllowLowMoodSprint(false, true));
        assertTrue(DepressionTraitService.shouldAllowLowMoodSprint(true, true));
    }

    @Test
    void activePsychoEndsWhenPlayerOrAttackerStopsPlaying() {
        assertFalse(DepressionTraitService.shouldEndActivePsycho(true, true));
        assertTrue(DepressionTraitService.shouldEndActivePsycho(false, true));
        assertTrue(DepressionTraitService.shouldEndActivePsycho(true, false));
        assertTrue(DepressionTraitService.shouldEndActivePsycho(false, false));
    }

    @Test
    void postPsychoStaminaRestoresFullFiniteStaminaAndKeepsInfiniteStamina() {
        DepressionTraitService.PostPsychoStaminaState finite =
                DepressionTraitService.postPsychoStaminaState(200, 160);

        assertEquals(160, finite.maxSprintTime());
        assertEquals(160.0f, finite.sprintingTicks(), 0.0001f);
        assertFalse(finite.exhausted());

        DepressionTraitService.PostPsychoStaminaState infinite =
                DepressionTraitService.postPsychoStaminaState(-1, 160);

        assertEquals(-1, infinite.maxSprintTime());
        assertEquals(Integer.MAX_VALUE, infinite.sprintingTicks(), 0.0001f);
        assertFalse(infinite.exhausted());
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
    void suicideCountdownPausesDuringAnyPsychoStateForDepressionOnly() {
        assertTrue(DepressionTraitService.shouldPauseSuicideCountdown(true, true, false, true, false));
        assertTrue(DepressionTraitService.shouldPauseSuicideCountdown(true, true, false, false, true));
        assertTrue(DepressionTraitService.shouldPauseSuicideCountdown(true, true, true, false, false));
        assertTrue(DepressionTraitService.shouldPauseSuicideCountdown(true, false, false, false, false));
        assertTrue(DepressionTraitService.shouldPauseSuicideCountdown(false, true, false, true, true));
        assertFalse(DepressionTraitService.shouldPauseSuicideCountdown(true, true, false, false, false));
    }

    @Test
    void mentalBreakdownSuppressionIsScopedToDepressionPsychoOnly() {
        assertTrue(DepressionTraitService.shouldSuppressMentalBreakdown(
                true,
                true,
                false,
                GameConstants.DeathReasons.MENTAL_BREAKDOWN
        ));
        assertTrue(DepressionTraitService.shouldSuppressMentalBreakdown(
                true,
                false,
                true,
                GameConstants.DeathReasons.MENTAL_BREAKDOWN
        ));
        assertFalse(DepressionTraitService.shouldSuppressMentalBreakdown(
                false,
                true,
                true,
                GameConstants.DeathReasons.MENTAL_BREAKDOWN
        ));
        assertFalse(DepressionTraitService.shouldSuppressMentalBreakdown(
                true,
                true,
                true,
                GameConstants.DeathReasons.GUN
        ));
    }

    @Test
    void depressionPsychoMoodFloorAndRestoreValueAreStable() {
        assertEquals(0.0f, DepressionTraitService.depressionPsychoMoodFloor(-1.0f), 0.0001f);
        assertEquals(0.25f, DepressionTraitService.depressionPsychoMoodFloor(0.25f), 0.0001f);
        assertEquals(0.7f, DepressionTraitService.depressionPsychoRestoredMood(), 0.0001f);
    }

    @Test
    void depressionVisualStrengthIsLinearFromMidMoodToZero() {
        assertEquals(0.0f, DepressionTraitService.depressionScreenEffectStrength(true, false, 0.55f), 0.0001f);
        assertEquals(0.375f, DepressionTraitService.depressionScreenEffectStrength(true, false, 0.275f), 0.0001f);
        assertEquals(0.75f, DepressionTraitService.depressionScreenEffectStrength(true, false, 0.0f), 0.0001f);
        assertEquals(0.75f, DepressionTraitService.depressionScreenEffectStrength(true, true, -1, 0.5f), 0.0001f);
        assertEquals(0.0f, DepressionTraitService.depressionScreenEffectStrength(false, true, 100, -0.2f), 0.0001f);
    }

    @Test
    void depressionPsychoSkinMixinOverridesPlayerTextureAndArmSkin() throws IOException {
        String source = Files.readString(Path.of(
                "src/client/java/dev/caecorthus/sparktraits/client/mixin/DepressionPsychoSkinMixin.java"
        ));

        assertTrue(source.contains("ModifyReturnValue"));
        assertTrue(source.contains("@At(\"RETURN\")"));
        assertTrue(source.contains("Identifier originalTexture"));
        assertTrue(source.contains("return texture == null ? originalTexture : texture;"));
        assertTrue(source.contains("@WrapOperation("));
        assertTrue(source.contains("method = \"renderArm\""));
        assertTrue(source.contains("getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"));
        assertTrue(source.contains("return new SkinTextures("));
        assertTrue(source.contains("textures/entity/depression_psycho"));
        assertFalse(source.contains("cancellable = true"));
        assertFalse(source.contains("@At(\"HEAD\")"));
    }

    @Test
    void activeJesterMomentKillAddsOnlyInitialDepressionPsychoArmour() {
        assertEquals(2, DepressionTraitService.initialPsychoArmour(Noellesroles.JESTER, true));
        assertEquals(0, DepressionTraitService.initialPsychoArmour(Noellesroles.JESTER, false));
        assertEquals(0, DepressionTraitService.initialPsychoArmour(WatheRoles.KILLER, true));
        assertEquals(0, DepressionTraitService.initialPsychoArmour(Noellesroles.SHADOW_JESTER, true));
    }

    @Test
    void depressionPsychoArmourClampsWithoutRefillingConsumedArmour() {
        assertEquals(0, DepressionTraitService.maintainedPsychoArmour(3, 0));
        assertEquals(2, DepressionTraitService.maintainedPsychoArmour(2, 2));
        assertEquals(1, DepressionTraitService.maintainedPsychoArmour(1, 2));
        assertEquals(0, DepressionTraitService.maintainedPsychoArmour(0, 2));
        assertEquals(2, DepressionTraitService.maintainedPsychoArmour(5, 2));
    }

    @Test
    void depressionBlindRageChaseLoopUsesAudioLengthInterval() {
        assertEquals(1121, DepressionTraitService.RAGE_LOOP_INTERVAL_TICKS);
        assertTrue(DepressionTraitService.shouldPlayRageLoop(0));
        assertTrue(DepressionTraitService.shouldPlayRageLoop(-1));
        assertFalse(DepressionTraitService.shouldPlayRageLoop(1));
        assertEquals(1121, DepressionTraitService.nextRageLoopTicks(0));
        assertEquals(1120, DepressionTraitService.nextRageLoopTicks(1121));
    }

    @Test
    void depressionPsychoMaintainsPermanentSpeedTwoAndClearsItOnExit() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/DepressionTraitService.java"));

        assertEquals(Integer.MAX_VALUE, DepressionTraitService.DEPRESSION_PSYCHO_SPEED_DURATION_TICKS);
        assertEquals(1, DepressionTraitService.DEPRESSION_PSYCHO_SPEED_AMPLIFIER);
        assertTrue(source.contains("applyPsychoSpeed(player);"));
        assertTrue(source.contains("restorePrePsychoSpeed(player, state.prePsychoSpeed());"));
        assertTrue(source.contains("player.removeStatusEffect(StatusEffects.SPEED);"));
        assertTrue(source.contains("StatusEffects.SPEED"));
    }

    @Test
    void depressionMeleeKillSoundIsSelectedFromTwoSeparateEvents() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/DepressionTraitService.java"));

        assertTrue(source.contains("meleeKillSound(boolean secondVariant)"));
        assertTrue(source.contains("SparkTraitsSounds.DEPRESSION_MELEE_KILL_1"));
        assertTrue(source.contains("SparkTraitsSounds.DEPRESSION_MELEE_KILL_2"));
        assertFalse(source.contains("DEPRESSION_MELEE_KILL ="));
    }

    @Test
    void depressionSoundRegistryAndResourcesContainAllCustomEvents() throws IOException {
        String sounds = Files.readString(Path.of("src/main/resources/assets/sparktraits/sounds.json"));
        String source = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/SparkTraitsSounds.java"));

        for (String event : new String[]{
                "depression.docile_to_rage",
                "depression.rage_loop",
                "depression.blind_rage_enrage",
                "depression.blind_rage_chase",
                "depression.rage_to_docile",
                "depression.player_was_seen",
                "depression.melee_kill_1",
                "depression.melee_kill_2",
                "depression.shyguy_killed"
        }) {
            assertTrue(sounds.contains("\"" + event + "\""));
            assertTrue(source.contains("registrar.create(\"" + event + "\")"));
        }
        for (String file : new String[]{
                "docile_to_rage.ogg",
                "rage_loop.ogg",
                "blind_rage_enrage.ogg",
                "blind_rage_chase.ogg",
                "rage_to_docile.ogg",
                "player_was_seen.ogg",
                "melee_kill_1.ogg",
                "melee_kill_2.ogg",
                "shyguy_killed.ogg"
        }) {
            assertTrue(Files.exists(Path.of("src/main/resources/assets/sparktraits/sounds/depression", file)));
        }
    }

    @Test
    void depressionBlindRageEnrageAndChaseArePairOnlySounds() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/DepressionTraitService.java"));

        assertTrue(source.contains("playPairMusicSound(player, attacker, SparkTraitsSounds.DEPRESSION_BLIND_RAGE_ENRAGE)"));
        assertTrue(source.contains("playPairMusicSound(player, attacker, SparkTraitsSounds.DEPRESSION_BLIND_RAGE_CHASE)"));
        assertTrue(source.contains("tickRageLoop(ServerPlayerEntity player, @Nullable ServerPlayerEntity attacker, ActiveState state)"));
        assertFalse(source.contains("playRangeSound(player, SparkTraitsSounds.DEPRESSION_DOCILE_TO_RAGE)"));
        assertFalse(source.contains("playRangeSound(player, SparkTraitsSounds.DEPRESSION_RAGE_LOOP)"));
    }

    @Test
    void depressionPsychoSoundsUseAmbientCategoryAndLouderVolumes() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/DepressionTraitService.java"));

        assertEquals(5.0f, DepressionTraitService.DEPRESSION_RANGE_SOUND_VOLUME, 0.0001f);
        assertEquals(2.0f, DepressionTraitService.DEPRESSION_DIRECT_SOUND_VOLUME, 0.0001f);
        assertEquals(2.0f, DepressionTraitService.DEPRESSION_MUSIC_SOUND_VOLUME, 0.0001f);
        assertEquals(SoundCategory.AMBIENT, DepressionTraitService.DEPRESSION_AUDIO_CATEGORY);
        assertTrue(source.contains("source.getWorld().playSound("));
        assertTrue(source.contains("DEPRESSION_AUDIO_CATEGORY,\n                DEPRESSION_RANGE_SOUND_VOLUME"));
        assertTrue(source.contains("player.playSoundToPlayer(sound, DEPRESSION_AUDIO_CATEGORY, DEPRESSION_DIRECT_SOUND_VOLUME"));
        assertTrue(source.contains("player.playSoundToPlayer(sound, DEPRESSION_AUDIO_CATEGORY, DEPRESSION_MUSIC_SOUND_VOLUME"));
        assertFalse(source.contains("SoundCategory.MUSIC"));
    }

    @Test
    void depressionShortEffectsDoNotUseStreamingSoundSources() throws IOException {
        JsonObject sounds = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/sparktraits/sounds.json"
        ))).getAsJsonObject();

        assertFalse(firstSoundEntryStreams(sounds, "depression.docile_to_rage"));
        assertFalse(firstSoundEntryStreams(sounds, "depression.rage_loop"));
        assertFalse(firstSoundEntryStreams(sounds, "depression.blind_rage_enrage"));
        assertTrue(firstSoundEntryStreams(sounds, "depression.blind_rage_chase"));
        assertFalse(firstSoundEntryStreams(sounds, "depression.rage_to_docile"));
        assertFalse(firstSoundEntryStreams(sounds, "depression.player_was_seen"));
        assertFalse(firstSoundEntryStreams(sounds, "depression.melee_kill_1"));
        assertFalse(firstSoundEntryStreams(sounds, "depression.melee_kill_2"));
        assertFalse(firstSoundEntryStreams(sounds, "depression.shyguy_killed"));
    }

    @Test
    void psychoRestrictionsOnlyApplyWhileDepressionPsychoIsActive() {
        assertFalse(DepressionTraitService.shouldMuteVoice(false));
        assertTrue(DepressionTraitService.shouldMuteVoice(true));
        assertFalse(DepressionTraitService.shouldBlockInventoryInsert(false, false));
        assertFalse(DepressionTraitService.shouldBlockInventoryInsert(true, true));
        assertTrue(DepressionTraitService.shouldBlockInventoryInsert(true, false));
    }

    @Test
    void depressionAfterKillRunsBeforeLastStandFakeDeathReturn() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/TraitGameHooks.java"));

        int lastStandStarted = source.indexOf("boolean lastStandStarted = LastStandService.tryStartAfterKill");
        int depressionAfterKill = source.indexOf("DepressionTraitService.handleAfterKill(victim, killer)");
        int lastStandReturn = source.indexOf("if (lastStandStarted)");
        int clearTraits = source.indexOf("playerTraits.clearActiveTraits(TraitRemovalReason.DEATH)");

        assertTrue(lastStandStarted >= 0);
        assertTrue(depressionAfterKill > lastStandStarted);
        assertTrue(lastStandReturn > depressionAfterKill);
        assertTrue(clearTraits > lastStandReturn);
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

    private static boolean firstSoundEntryStreams(JsonObject sounds, String event) {
        JsonElement firstSound = sounds.getAsJsonObject(event).getAsJsonArray("sounds").get(0);
        if (!firstSound.isJsonObject()) {
            return false;
        }
        JsonElement stream = firstSound.getAsJsonObject().get("stream");
        return stream != null && stream.getAsBoolean();
    }
}
