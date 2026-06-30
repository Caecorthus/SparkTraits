package dev.caecorthus.sparktraits.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.Noellesroles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrogantAsfTraitTest {
    private static final Path MAIN_RESOURCES = Path.of("src/main/resources");
    private static final Path CLIENT_RESOURCES = Path.of("src/client/resources");

    @BeforeAll
    static void registerTraits() {
        if (!TraitRegistry.contains(ArrogantAsfTrait.ID)) {
            TraitRegistry.register(new ArrogantAsfTrait());
        }
    }

    @Test
    void arrogantAsfIsForcedCorruptCopOnlyTrait() {
        Trait trait = TraitRegistry.get(ArrogantAsfTrait.ID);

        assertNotNull(trait);
        assertEquals(Identifier.of("sparktraits", "arrogant_asf"), trait.id());
        assertEquals(CorruptCopTraitService.ARROGANT_ASF_COLOR, trait.color());
        assertEquals(TraitAudience.NEUTRAL_ONLY, trait.audience());
        assertEquals(0, trait.weight());
    }

    @Test
    void arrogantAsfOnlyAppliesToCorruptCop() {
        assertTrue(TraitRules.canApplyAll(null, null, null, Noellesroles.CORRUPT_COP, Set.of(ArrogantAsfTrait.ID)));

        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.JESTER, Set.of(ArrogantAsfTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.TAOTIE, Set.of(ArrogantAsfTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.KILLER, Set.of(ArrogantAsfTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.CIVILIAN, Set.of(ArrogantAsfTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, WatheRoles.VIGILANTE, Set.of(ArrogantAsfTrait.ID)));
        assertFalse(TraitRules.canApplyAll(null, null, null, Noellesroles.DETECTIVE, Set.of(ArrogantAsfTrait.ID)));
    }

    @Test
    void abilityKeyTogglesArrogantAsfOnlyForTraitHolders() {
        assertTrue(CorruptCopTraitService.nextArrogantAsfActive(true, false));
        assertFalse(CorruptCopTraitService.nextArrogantAsfActive(true, true));
        assertFalse(CorruptCopTraitService.nextArrogantAsfActive(false, false));
    }

    @Test
    void activeArrogantAsfScalesPureSidewaysVelocity() {
        Vec3d bonus = CorruptCopTraitService.arrogantAsfLateralVelocityBonus(
                new Vec3d(3.0d, 0.0d, 0.0d),
                0.07f,
                0.0f,
                true,
                true,
                true
        );

        assertVectorEquals(new Vec3d(0.035d, 0.0d, 0.0d), bonus);
    }

    @Test
    void activeArrogantAsfKeepsForwardOnlyVelocityVanilla() {
        Vec3d bonus = CorruptCopTraitService.arrogantAsfLateralVelocityBonus(
                new Vec3d(0.0d, 0.0d, 3.0d),
                0.07f,
                0.0f,
                true,
                true,
                true
        );

        assertVectorEquals(Vec3d.ZERO, bonus);
    }

    @Test
    void activeArrogantAsfOnlyBoostsLateralComponentForDiagonalInput() {
        Vec3d bonus = CorruptCopTraitService.arrogantAsfLateralVelocityBonus(
                new Vec3d(1.0d, 0.0d, 1.0d),
                0.07f,
                0.0f,
                true,
                true,
                true
        );

        assertVectorEquals(new Vec3d(0.024748737d, 0.0d, 0.0d), bonus);
    }

    @Test
    void activeArrogantAsfRotatesLateralBonusWithYaw() {
        Vec3d bonus = CorruptCopTraitService.arrogantAsfLateralVelocityBonus(
                new Vec3d(-1.0d, 0.0d, 0.0d),
                0.07f,
                90.0f,
                true,
                true,
                true
        );

        assertVectorEquals(new Vec3d(0.0d, 0.0d, -0.035d), bonus);
    }

    @Test
    void inactiveMissingOrDeadArrogantAsfAddsNoVelocityBonus() {
        Vec3d movementInput = new Vec3d(1.0d, 0.0d, 0.0d);

        assertVectorEquals(Vec3d.ZERO, CorruptCopTraitService.arrogantAsfLateralVelocityBonus(
                movementInput, 0.07f, 0.0f, true, false, true
        ));
        assertVectorEquals(Vec3d.ZERO, CorruptCopTraitService.arrogantAsfLateralVelocityBonus(
                movementInput, 0.07f, 0.0f, false, true, true
        ));
        assertVectorEquals(Vec3d.ZERO, CorruptCopTraitService.arrogantAsfLateralVelocityBonus(
                movementInput, 0.07f, 0.0f, true, true, false
        ));
    }

    @Test
    void arrogantAsfMixinIsRegistered() throws IOException {
        String mainMixins = Files.readString(MAIN_RESOURCES.resolve("sparktraits.mixins.json"));
        String lateralVelocitySource = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/ArrogantAsfLateralVelocityMixin.java"
        ));

        assertTrue(mainMixins.contains("\"ArrogantAsfLateralVelocityMixin\""));
        assertFalse(mainMixins.contains("\"ArrogantAsfMovementInputStateMixin\""));
        assertFalse(mainMixins.contains("\"ArrogantAsfMovementSpeedMixin\""));
        assertFalse(mainMixins.contains("\"ArrogantAsfSidewaysSpeedMixin\""));
        assertFalse(mainMixins.contains("\"ArrogantAsfMovementInputMixin\""));
        assertTrue(lateralVelocitySource.contains("@Mixin(Entity.class)"));
        assertTrue(lateralVelocitySource.contains("method = \"updateVelocity\""));
        assertTrue(lateralVelocitySource.contains("@At(\"TAIL\")"));
        assertTrue(lateralVelocitySource.contains("arrogantAsfLateralVelocityBonus"));
        assertFalse(lateralVelocitySource.contains("getMovementSpeed"));
        assertFalse(lateralVelocitySource.contains("target = \"Lnet/minecraft/util/math/Vec3d;<init>(DDD)V\""));
    }

    @Test
    void arrogantAsfUsesNoellesRolesAbilityKey() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/mixin/NoellesRolesPacketMixin.java"
        ));

        assertTrue(source.contains("toggleArrogantAsfAbility"));
        assertTrue(source.contains("AbilityC2SPacket"));
    }

    @Test
    void arrogantAsfHudMirrorsNoellesRolesAbilityStatus() throws IOException {
        String clientMixins = Files.readString(CLIENT_RESOURCES.resolve("sparktraits.client.mixins.json"));
        String hudSource = Files.readString(Path.of("src/client/java/dev/caecorthus/sparktraits/client/ArrogantAsfHud.java"));

        assertTrue(clientMixins.contains("\"ArrogantAsfInGameHudMixin\""));
        assertTrue(hudSource.contains("NoellesrolesClient.abilityBind"));
        assertTrue(hudSource.contains("tip.sparktraits.arrogant_asf.active"));
        assertTrue(hudSource.contains("tip.sparktraits.arrogant_asf.inactive"));
        assertTrue(hudSource.contains("isArrogantAsfActive"));
    }

    @Test
    void arrogantAsfMusicResourceIsRegisteredAndStreamed() throws IOException {
        JsonObject sounds = JsonParser.parseString(Files.readString(
                MAIN_RESOURCES.resolve("assets/sparktraits/sounds.json")
        )).getAsJsonObject();
        JsonObject event = sounds.getAsJsonObject("music.takediskrush");
        JsonObject sound = event.getAsJsonArray("sounds").get(0).getAsJsonObject();
        byte[] header = Files.readAllBytes(MAIN_RESOURCES.resolve(
                "assets/sparktraits/sounds/music/takediskrush.ogg"
        ));

        assertNotNull(event);
        assertEquals("sparktraits:music/takediskrush", sound.get("name").getAsString());
        assertTrue(sound.get("stream").getAsBoolean());
        assertEquals(0.9f, sound.get("volume").getAsFloat(), 0.0001f);
        assertEquals("subtitle.sparktraits.music.takediskrush", event.get("subtitle").getAsString());
        assertFalse(sound.has("attenuation_distance"));
        assertEquals("OggS", new String(header, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void arrogantAsfMusicUsesWatheStyleRegistrarAndBackgroundAmbience() throws IOException {
        String buildGradle = Files.readString(Path.of("build.gradle"));
        String fabricMod = Files.readString(MAIN_RESOURCES.resolve("fabric.mod.json"));
        String sparkTraitsSource = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/SparkTraits.java"));
        String soundsSource = Files.readString(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/SparkTraitsSounds.java"));
        String clientSource = Files.readString(Path.of("src/client/java/dev/caecorthus/sparktraits/client/SparkTraitsClient.java"));
        String corruptCopSource = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/CorruptCopTraitService.java"
        ));
        String traitHooksSource = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/TraitGameHooks.java"
        ));
        String clientMixins = Files.readString(CLIENT_RESOURCES.resolve("sparktraits.client.mixins.json"));

        assertTrue(buildGradle.contains("modImplementation(\"dev.doctor4t:ratatouille:${project.ratatouille_version}\")"));
        assertTrue(fabricMod.contains("\"ratatouille\": \">=${ratatouille_version}\""));
        assertTrue(sparkTraitsSource.contains("SparkTraitsSounds.initialize()"));
        assertTrue(soundsSource.contains("SoundEventRegistrar"));
        assertTrue(soundsSource.contains("registrar.create(\"music.takediskrush\")"));
        assertTrue(soundsSource.contains("registrar.registerEntries()"));
        assertTrue(soundsSource.contains("Registries.SOUND_EVENT.containsId"));
        assertTrue(soundsSource.contains("Registered SparkTraits sound event"));
        assertFalse(soundsSource.contains("Registry.register"));
        assertTrue(clientSource.contains("registerArrogantAsfMusic()"));
        assertTrue(clientSource.contains("AmbienceUtil.registerBackgroundAmbience"));
        assertTrue(clientSource.contains("new BackgroundAmbience"));
        assertTrue(clientSource.contains("SparkTraitsSounds.MUSIC_TAKEDISKRUSH"));
        assertTrue(clientSource.contains("traits.hasActiveTrait(ArrogantAsfTrait.ID)"));
        assertTrue(clientSource.contains("traits.isArrogantAsfActive()"));
        assertFalse(clientSource.contains("ArrogantAsfMusicController::tick"));
        assertFalse(clientSource.contains("playSoundToPlayer"));
        assertFalse(clientSource.contains("StopSoundS2CPacket"));
        assertFalse(corruptCopSource.contains("ArrogantAsfMusicService"));
        assertFalse(traitHooksSource.contains("ArrogantAsfMusicService"));
        assertFalse(clientMixins.contains("\"SoundManagerAccessor\""));
        assertFalse(clientMixins.contains("\"SoundSystemAccessor\""));
        assertTrue(Files.notExists(Path.of("src/client/java/dev/caecorthus/sparktraits/client/ArrogantAsfMusicController.java")));
        assertTrue(Files.notExists(Path.of("src/client/java/dev/caecorthus/sparktraits/client/ArrogantAsfMusicInstance.java")));
        assertTrue(Files.notExists(Path.of("src/client/java/dev/caecorthus/sparktraits/client/ArrogantAsfSoundAccess.java")));
        assertTrue(Files.notExists(Path.of("src/client/java/dev/caecorthus/sparktraits/client/mixin/SoundManagerAccessor.java")));
        assertTrue(Files.notExists(Path.of("src/client/java/dev/caecorthus/sparktraits/client/mixin/SoundSystemAccessor.java")));
        assertTrue(Files.notExists(Path.of("src/main/java/dev/caecorthus/sparktraits/impl/ArrogantAsfMusicRules.java")));
    }

    @Test
    void arrogantAsfLocalizationIsPresent() throws IOException {
        JsonObject english = readLanguageFile("en_us");
        JsonObject chinese = readLanguageFile("zh_cn");

        assertEquals("Arrogant ASF", english.get("trait.sparktraits.arrogant_asf.name").getAsString());
        assertEquals("Use your ability key to toggle 1.5x lateral movement speed.",
                english.get("trait.sparktraits.arrogant_asf.description").getAsString());
        assertEquals("展示豪度", chinese.get("trait.sparktraits.arrogant_asf.name").getAsString());
        assertEquals("使用技能键开关横向移动速度变为原来的 1.5 倍。",
                chinese.get("trait.sparktraits.arrogant_asf.description").getAsString());
        assertEquals("Arrogant ASF: ON (%s to turn off)",
                english.get("tip.sparktraits.arrogant_asf.active").getAsString());
        assertEquals("Arrogant ASF: OFF (%s to turn on)",
                english.get("tip.sparktraits.arrogant_asf.inactive").getAsString());
        assertEquals("展示豪度：开启（按 %s 关闭）",
                chinese.get("tip.sparktraits.arrogant_asf.active").getAsString());
        assertEquals("展示豪度：关闭（按 %s 开启）",
                chinese.get("tip.sparktraits.arrogant_asf.inactive").getAsString());
        assertEquals("TAKEDISKRUSH plays.",
                english.get("subtitle.sparktraits.music.takediskrush").getAsString());
        assertEquals("TAKEDISKRUSH 响起。",
                chinese.get("subtitle.sparktraits.music.takediskrush").getAsString());
    }

    private static JsonObject readLanguageFile(String language) throws IOException {
        return JsonParser.parseString(Files.readString(MAIN_RESOURCES.resolve(
                "assets/sparktraits/lang/" + language + ".json"
        ))).getAsJsonObject();
    }

    private static void assertVectorEquals(Vec3d expected, Vec3d actual) {
        assertEquals(expected.x, actual.x, 0.0001d);
        assertEquals(expected.y, actual.y, 0.0001d);
        assertEquals(expected.z, actual.z, 0.0001d);
    }
}
