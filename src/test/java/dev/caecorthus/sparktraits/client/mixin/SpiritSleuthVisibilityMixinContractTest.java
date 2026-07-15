package dev.caecorthus.sparktraits.client.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritSleuthVisibilityMixinContractTest {
    private static final Path MIXIN_SOURCE = Path.of(
            "src/client/java/dev/caecorthus/sparktraits/client/mixin/SpiritSleuthLivingEntityRendererMixin.java"
    );

    @Test
    void wrapsOnlyTheLivingRendererInvisibilityDecisionAndPreservesVanillaFallback() throws IOException {
        assertTrue(Files.isRegularFile(MIXIN_SOURCE), "Spirit Sleuth renderer mixin must exist");
        String source = Files.readString(MIXIN_SOURCE);

        assertTrue(source.contains("@Mixin(LivingEntityRenderer.class)"));
        assertTrue(source.contains("@WrapOperation("));
        assertTrue(source.contains(
                "method = \"render(Lnet/minecraft/entity/LivingEntity;FF"
                        + "Lnet/minecraft/client/util/math/MatrixStack;"
                        + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V\""
        ));
        assertTrue(source.contains(
                "target = \"Lnet/minecraft/entity/LivingEntity;"
                        + "isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z\""
        ));
        assertEquals(1, countOccurrences(source, "original.call("));
        assertTrue(source.contains("SparkTraitsServerConnection.isConfirmedServer()"));
        assertTrue(source.contains("GlobalTraitService.hasTrait("));
        assertTrue(source.contains("SpiritSleuthTrait.ID"));
        assertTrue(source.contains("GameWorldComponent.KEY.get("));
        assertTrue(source.contains("game.isRunning()"));
        assertTrue(source.contains("game.hasAnyRole("));
        assertTrue(source.contains("game.isPlayerDead("));
        assertTrue(source.contains("isLastStandPending()"));
        assertFalse(source.contains("GameFunctions.isPlayerPlayingAndAlive("));
        assertTrue(source.contains("SpiritSleuthVisibilityRules.shouldRevealSpectatorHead("));
    }

    @Test
    void registersTheRendererMixinInTheClientConfiguration() throws IOException {
        String clientMixins = Files.readString(Path.of(
                "src/client/resources/sparktraits.client.mixins.json"
        ));

        assertTrue(clientMixins.contains("\"SpiritSleuthLivingEntityRendererMixin\""));
    }

    private static int countOccurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
