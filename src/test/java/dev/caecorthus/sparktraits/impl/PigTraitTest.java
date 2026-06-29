package dev.caecorthus.sparktraits.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PigTraitTest {
    private static final Path MAIN_RESOURCES = Path.of("src/main/resources");
    private static final Path CLIENT_RESOURCES = Path.of("src/client/resources");

    @BeforeAll
    static void registerTraits() {
        if (!TraitRegistry.contains(PigTrait.ID)) {
            TraitRegistry.register(new PigTrait());
        }
        if (!TraitRegistry.contains(ChildishTrait.ID)) {
            TraitRegistry.register(new ChildishTrait());
        }
    }

    @Test
    void pigIsCommonUniversalAndNotUnique() {
        Trait pig = TraitRegistry.get(PigTrait.ID);

        assertNotNull(pig);
        assertEquals(PigTraitService.COLOR, pig.color());
        assertEquals(TraitAudience.UNIVERSAL, pig.audience());
        assertEquals(25, pig.weight());
        assertFalse(pig.uniquePerGame());
    }

    @Test
    void childishIsReducedToSameRareWeight() {
        Trait childish = TraitRegistry.get(ChildishTrait.ID);

        assertNotNull(childish);
        assertEquals(25, childish.weight());
    }

    @Test
    void pigUsesVanillaPigSounds() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/PigTraitService.java"));

        assertTrue(source.contains("SoundEvents.ENTITY_PIG_AMBIENT"));
        assertTrue(source.contains("SoundEvents.ENTITY_PIG_HURT"));
        assertTrue(source.contains("SoundEvents.ENTITY_PIG_DEATH"));
    }

    @Test
    void pigAmbientSoundChanceMatchesVanillaPigCadence() {
        assertEquals(11, PigTraitService.nextAmbientSoundChance(10, 10));
        assertEquals(PigTraitService.RESET_AMBIENT_SOUND_CHANCE, PigTraitService.nextAmbientSoundChance(10, 9));
        assertFalse(PigTraitService.shouldPlayAmbientSound(false, true, false, 10, 0));
        assertFalse(PigTraitService.shouldPlayAmbientSound(true, false, false, 10, 0));
        assertFalse(PigTraitService.shouldPlayAmbientSound(true, true, true, 10, 0));
        assertTrue(PigTraitService.shouldPlayAmbientSound(true, true, false, 10, 9));
    }

    @Test
    void pigCollisionKeepsAdultPigHeightButUsesPlayerWidth() throws IOException {
        EntityDimensions dimensions = PigTraitService.pigDimensions();
        String source = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/PigTraitService.java"));

        assertEquals(0.6F, PigTraitService.PIG_COLLISION_WIDTH);
        assertEquals(0.9F, PigTraitService.PIG_COLLISION_HEIGHT);
        assertEquals(PigTraitService.PIG_COLLISION_WIDTH, dimensions.width());
        assertEquals(PigTraitService.PIG_COLLISION_HEIGHT, dimensions.height());
        assertFalse(source.contains("EntityType.PIG.getDimensions()"));
    }

    @Test
    void pigRendererForcesModelsToAdultSize() throws IOException {
        String source = Files.readString(Path.of("src/client/java/dev/caecorthus/sparktraits/client/PigPlayerRenderer.java"));

        assertTrue(source.contains("forceAdultModels(models)"));
        assertTrue(source.contains("models.pig().child = false"));
        assertTrue(source.contains("models.head().child = false"));
        assertTrue(source.contains("models.outerArmor().child = false"));
    }

    @Test
    void pigCanApplyToOrdinaryRolesOnAnyFaction() {
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.CIVILIAN, Set.of(PigTrait.ID)));
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.KILLER, Set.of(PigTrait.ID)));
        assertTrue(TraitRules.canApplyAll(null, null, null, WatheRoles.LOOSE_END, Set.of(PigTrait.ID)));
        assertTrue(TraitRules.canApplyAll(null, null, null, Noellesroles.DETECTIVE, Set.of(PigTrait.ID)));
    }

    @Test
    void pigStillFollowsGlobalSparkWitchRoleBlock() {
        assertFalse(TraitRules.canApplyAll(null, null, null, sparkWitchRole("grand_witch"), Set.of(PigTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, sparkWitchRole("accomplice"), Set.of(PigTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, sparkWitchRole("apprentice_witch"), Set.of(PigTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, sparkWitchRole("murderous_witch"), Set.of(PigTrait.ID)));
        assertTrue(TraitRules.canApplyAll(null, null, null, sparkWitchRole("pig_god"), Set.of(PigTrait.ID)));
        assertTrue(TraitRules.canApplyAll(null, null, null, sparkWitchRole("other"), Set.of(PigTrait.ID)));
    }

    @Test
    void pigMixinsAreRegistered() throws IOException {
        String mainMixins = Files.readString(MAIN_RESOURCES.resolve("sparktraits.mixins.json"));
        String clientMixins = Files.readString(CLIENT_RESOURCES.resolve("sparktraits.client.mixins.json"));

        assertTrue(mainMixins.contains("\"PigPlayerDimensionsMixin\""));
        assertTrue(mainMixins.contains("\"PigPlayerSoundMixin\""));
        assertTrue(clientMixins.contains("\"PigPlayerRendererMixin\""));
    }

    @Test
    void pigDeathSoundRunsBeforeWatheDeathClearsTraits() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/TraitGameHooks.java"));

        int deathSound = source.indexOf("PigTraitService.playDeathSound(victim)");
        int clearTraits = source.indexOf("playerTraits.clearActiveTraits(TraitRemovalReason.DEATH)");
        assertTrue(deathSound >= 0);
        assertTrue(clearTraits > deathSound);
    }

    @Test
    void pigDeathSoundRunsBeforeLastStandFakeDeathReturn() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/TraitGameHooks.java"));

        int lastStandStarted = source.indexOf("boolean lastStandStarted = LastStandService.tryStartAfterKill");
        int deathSound = source.indexOf("PigTraitService.playDeathSound(victim)");
        int lastStandReturn = source.indexOf("if (lastStandStarted)");
        assertTrue(lastStandStarted >= 0);
        assertTrue(deathSound > lastStandStarted);
        assertTrue(lastStandReturn > deathSound);
    }

    @Test
    void pigLocalizationIsPresent() throws IOException {
        JsonObject english = readLanguageFile("en_us");
        JsonObject chinese = readLanguageFile("zh_cn");

        assertEquals("Pig", english.get("trait.sparktraits.pig.name").getAsString());
        assertEquals("猪", chinese.get("trait.sparktraits.pig.name").getAsString());
        assertTrue(english.has("trait.sparktraits.pig.description"));
        assertTrue(chinese.has("trait.sparktraits.pig.description"));
    }

    private static JsonObject readLanguageFile(String language) throws IOException {
        return JsonParser.parseString(Files.readString(MAIN_RESOURCES.resolve(
                "assets/sparktraits/lang/" + language + ".json"
        ))).getAsJsonObject();
    }

    private static Role sparkWitchRole(String path) {
        return new Role(
                Identifier.of("sparkwitch", path),
                0xFFFFFF,
                false,
                false,
                Role.MoodType.FAKE,
                200,
                false
        );
    }
}
