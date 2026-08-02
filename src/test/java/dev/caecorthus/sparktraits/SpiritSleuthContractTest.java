package dev.caecorthus.sparktraits;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.caecorthus.sparktraits.impl.traits.global.SpiritSleuthTrait;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritSleuthContractTest {
    private static final Path MIXIN_SOURCE = Path.of(
            "src/client/java/dev/caecorthus/sparktraits/client/mixin/SpiritSleuthLivingEntityRendererMixin.java"
    );
    private static final Path CLIENT_MIXINS = Path.of("src/client/resources/sparktraits.client.mixins.json");

    @Test
    void traitUsesGlobalDefaultsAndStableMetadata() {
        SpiritSleuthTrait trait = new SpiritSleuthTrait();

        assertEquals(SparkTraits.id("spirit_sleuth"), SpiritSleuthTrait.ID);
        assertEquals(SpiritSleuthTrait.ID, trait.id());
        assertEquals(GlobalTraitService.SPIRIT_SLEUTH_COLOR, trait.color());
        assertEquals(0xB8A7FF, trait.color());
        assertEquals(TraitAudience.UNIVERSAL, trait.audience());
        assertEquals(0.0D, trait.rollWeight());
        assertFalse(trait.hiddenFromOwnerAtStart());
        assertEquals("trait.sparktraits.spirit_sleuth.name", trait.nameTranslationKey());
        assertEquals("trait.sparktraits.spirit_sleuth.description", trait.descriptionTranslationKey());
    }

    @Test
    void traitIsRegisteredAndLocalized() throws IOException {
        String registrations = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/registry/SparkTraitsBuiltInTraits.java"
        ));
        assertEquals(1, count(registrations, "TraitRegistry.register(new SpiritSleuthTrait())"));

        JsonObject english = readJson(Path.of("src/main/resources/assets/sparktraits/lang/en_us.json"));
        JsonObject chinese = readJson(Path.of("src/main/resources/assets/sparktraits/lang/zh_cn.json"));
        assertEquals("Spirit Sleuth", english.get("trait.sparktraits.spirit_sleuth.name").getAsString());
        assertEquals(
                "You can see players who are in Spectator Mode.",
                english.get("trait.sparktraits.spirit_sleuth.description").getAsString()
        );
        assertEquals("灵探", chinese.get("trait.sparktraits.spirit_sleuth.name").getAsString());
        assertEquals(
                "你可以看到处于旁观模式的玩家。",
                chinese.get("trait.sparktraits.spirit_sleuth.description").getAsString()
        );
    }

    @Test
    void mixinWrapsOnlyTheVanillaRenderVisibilityDecision() throws IOException {
        String source = Files.readString(MIXIN_SOURCE);
        String mixins = Files.readString(CLIENT_MIXINS);

        assertTrue(source.contains("@Mixin(LivingEntityRenderer.class)"));
        assertTrue(source.contains("@WrapOperation("));
        assertTrue(source.contains(
                "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;"
                        + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
        ));
        assertTrue(source.contains(
                "Lnet/minecraft/entity/LivingEntity;"
                        + "isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z"
        ));
        assertEquals(1, count(source, "original.call("));
        assertTrue(source.indexOf("original.call(") < source.indexOf("TraitPlayerComponent.KEY.get(viewer)"));
        assertTrue(source.contains("viewer != null"));
        assertTrue(source.contains(
                "TraitPlayerComponent.KEY.get(viewer).hasActiveTrait(SpiritSleuthTrait.ID)"
        ));
        assertTrue(source.contains("target instanceof PlayerEntity targetPlayer"));
        assertTrue(source.contains("targetPlayer.isSpectator()"));
        assertEquals(1, count(mixins, "SpiritSleuthLivingEntityRendererMixin"));
        assertTrue(mixins.contains("\"defaultRequire\": 1"));
        assertFalse(source.contains("require = 0"));
    }

    @Test
    void implementationDoesNotGrantSpectatorInformationOrRestoreOldPolicy() throws IOException {
        String source = Files.readString(MIXIN_SOURCE);
        String forbidden = String.join("\n", source, Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/traits/global/SpiritSleuthVisibilityRules.java"
        )));

        for (String token : new String[]{
                "SparkTraitsServerConnection",
                "WatheClient",
                "canSeeSpectatorInformation",
                "GameWorldComponent",
                "confirmedDeath",
                "isPlayerDead",
                "hasAnyRole",
                "isRunning",
                "LastStand",
                "FakeDeath",
                "PlayerBodyEntity",
                "viewer.isSpectator",
                "viewer.isCreative",
                "setModelPose",
                "getRenderLayer"
        }) {
            assertFalse(forbidden.contains(token), () -> "Forbidden Spirit Sleuth coupling: " + token);
        }
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static int count(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }
}
