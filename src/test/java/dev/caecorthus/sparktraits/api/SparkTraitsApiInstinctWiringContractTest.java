package dev.caecorthus.sparktraits.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparkTraitsApiInstinctWiringContractTest {
    @Test
    void commonFacadeCombinesEverySparkTraitsInstinctProtection() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/api/SparkTraitsApi.java"
        )).replaceAll("\\s+", " ");

        assertTrue(source.contains("public static boolean isInstinctHidden(PlayerEntity viewer, PlayerEntity target)"));
        assertTrue(source.contains("EffectiveTraitService.shouldHideFromInstinct("));
        assertTrue(source.contains("EffectiveTraitService.isSpiritProjecting(target)"));
        assertTrue(source.contains("targetTraits.isGoingDarkInstinctHidden()"));
        assertTrue(source.contains("GoingDarkRules.shouldSuppressInstinct("));
        assertTrue(source.contains("TraitWorldComponent.KEY.maybeGet(viewer.getWorld())"));
        assertTrue(source.contains("GameFunctions.isPlayerPlayingAndAlive(viewer)"));
        assertTrue(source.contains("GameFunctions.isPlayerSpectatingOrCreative(viewer)"));
        assertFalse(source.contains("net.minecraft.client"));
    }

    @Test
    void remoteSpiritProjectionIsRelayedThroughPublicTraitState() throws IOException {
        String componentSource = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/component/TraitPlayerComponent.java"
        )).replaceAll("\\s+", " ");
        String effectiveSource = Files.readString(Path.of(
                "src/main/java/dev/caecorthus/sparktraits/impl/effective/EffectiveTraitService.java"
        )).replaceAll("\\s+", " ");

        assertTrue(componentSource.contains("private boolean spiritProjectionInstinctHidden;"));
        assertTrue(componentSource.contains("SpiritPlayerComponent.KEY.maybeGet(player)"));
        assertTrue(componentSource.contains("buf.writeBoolean(spiritProjectionInstinctHidden)"));
        assertTrue(componentSource.contains("spiritProjectionInstinctHidden = buf.readableBytes() > 0 && buf.readBoolean()"));
        assertFalse(componentSource.contains("putBoolean(\"SpiritProjectionInstinctHidden\""));
        assertTrue(effectiveSource.contains("player.getWorld().isClient"));
        assertTrue(effectiveSource.contains("TraitPlayerComponent::isSpiritProjectionInstinctHidden"));
    }
}
