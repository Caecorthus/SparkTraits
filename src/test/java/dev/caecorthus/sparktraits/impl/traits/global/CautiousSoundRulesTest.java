package dev.caecorthus.sparktraits.impl.traits.global;

import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CautiousSoundRulesTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "step",
            "entity.player.walk",
            "entity/player/run2",
            "entity.player.wander_wood",
            "entity-player-sprint42",
            "entity.player.footstep",
            "entity.player.footsteps9"
    })
    void suppressesDelimiterBoundedMovementTokens(String path) {
        assertTrue(shouldSuppress(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "entity.player.hurt",
            "item.gun.fire",
            "ambient.wind",
            "entity.player.backstep",
            "entity.player.stepstone",
            "entity.player.walk2x",
            "entity.player.runner",
            "entity.player.wandering",
            "entity.player.sprinter",
            "entity.player.footstepper"
    })
    void preservesNonMovementAndEmbeddedTokenSounds(String path) {
        assertFalse(shouldSuppress(path));
    }

    @Test
    void requiresPlayerAssociationCautiousSuppressionAndPlayersCategory() {
        Identifier step = Identifier.of("test", "entity.player.step");

        assertFalse(CautiousSoundRules.shouldSuppressClientEntityStepSound(
                false, true, SoundCategory.PLAYERS, step
        ));
        assertFalse(CautiousSoundRules.shouldSuppressClientEntityStepSound(
                true, false, SoundCategory.PLAYERS, step
        ));
        assertFalse(CautiousSoundRules.shouldSuppressClientEntityStepSound(
                true, true, SoundCategory.BLOCKS, step
        ));
        assertFalse(CautiousSoundRules.shouldSuppressClientEntityStepSound(
                true, true, SoundCategory.PLAYERS, null
        ));
    }

    @Test
    void clientMixinCoversBothClientWorldEntitySoundOverloads() throws IOException {
        String source = readSource(
                "src/client/java/dev/caecorthus/sparktraits/client/mixin/CautiousClientWorldSoundMixin.java"
        );

        assertTrue(source.contains(
                "playSoundFromEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/sound/SoundEvent;"
                        + "Lnet/minecraft/sound/SoundCategory;FF)V"
        ));
        assertTrue(source.contains(
                "playSoundFromEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;"
                        + "Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/sound/SoundCategory;FFJ)V"
        ));
    }

    @Test
    void clientSoundDiagnosticsArePropertyGatedAndUsedByAllAdapters() throws IOException {
        String debugSource = readSource(
                "src/client/java/dev/caecorthus/sparktraits/client/audio/CautiousSoundDebug.java"
        );
        String clientWorldSource = readSource(
                "src/client/java/dev/caecorthus/sparktraits/client/mixin/CautiousClientWorldSoundMixin.java"
        );
        String presenceFootstepsSource = readSource(
                "src/client/java/dev/caecorthus/sparktraits/client/mixin/CautiousPresenceFootstepsVolumeMixin.java"
        );

        int propertyGate = debugSource.indexOf("Boolean.getBoolean(ENABLED_PROPERTY)");
        int loggerCall = debugSource.indexOf("SparkTraits.LOGGER.info(");
        assertTrue(debugSource.contains(
                "private static final String ENABLED_PROPERTY = \"sparktraits.debugCautiousSound\";"
        ));
        assertTrue(propertyGate >= 0);
        assertTrue(loggerCall > propertyGate);
        assertTrue(debugSource.substring(propertyGate, loggerCall).contains("return;"));

        assertTrue(debugSource.contains("sourceUuid={} sourceType={}"));
        assertTrue(debugSource.contains("confirmedServer={} cautiousRule={}"));
        assertTrue(debugSource.contains("decision={} soundId={} category={} volume={}"));
        assertEquals(2, countOccurrences(clientWorldSource, "CautiousSoundDebug.trace("));
        assertEquals(1, countOccurrences(presenceFootstepsSource, "CautiousSoundDebug.trace("));
    }

    @Test
    void playerConsumptionMixinSuppressesOnlyTheCompletionBurpSound() throws IOException {
        String source = readSource(
                "src/main/java/dev/caecorthus/sparktraits/mixin/CautiousPlayerConsumptionSoundMixin.java"
        );
        String mixinConfig = readSource("src/main/resources/sparktraits.mixins.json");

        assertTrue(source.contains("@Mixin(PlayerEntity.class)"));
        assertTrue(source.contains("method = \"eatFood\""));
        assertTrue(source.contains(
                "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;"
                        + "DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
        ));
        assertTrue(source.contains("sound == SoundEvents.ENTITY_PLAYER_BURP"));
        assertTrue(source.contains("GlobalTraitService.shouldSuppressCautiousSounds(player)"));
        assertTrue(mixinConfig.contains("\"CautiousPlayerConsumptionSoundMixin\""));
    }

    private static boolean shouldSuppress(String path) {
        return CautiousSoundRules.shouldSuppressClientEntityStepSound(
                true,
                true,
                SoundCategory.PLAYERS,
                Identifier.of("test", path)
        );
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(Path.of(System.getProperty("user.dir"), relativePath));
    }

    private static int countOccurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
