package dev.caecorthus.sparktraits.client.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritSleuthVisibilityMixinContractTest {
    private static final Path MIXIN_SOURCE = Path.of(
            "src/client/java/dev/caecorthus/sparktraits/client/mixin/SpiritSleuthLivingEntityRendererMixin.java"
    );
    private static final Path BODY_MIXIN_SOURCE = Path.of(
            "src/client/java/dev/caecorthus/sparktraits/client/mixin/SpiritSleuthPlayerBodyRendererMixin.java"
    );

    @Test
    void wrapsTheLivingRendererInvisibilityDecisionAndPreservesVanillaFallback() throws IOException {
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
        assertTrue(source.contains("SpiritSleuthVisibilityRules.shouldRevealSpectatorPlayerHead("));
        assertTrue(source.contains("return resolveSpectatorPlayerHeadVisibility("));
        String normalizedSource = source.replaceAll("\\s+", " ");
        assertTrue(normalizedSource.contains(
                "return resolveSpectatorPlayerHeadVisibility( invisibleToViewer, "
                        + "GlobalTraitService.hasTrait(viewer, SpiritSleuthTrait.ID), "
                        + "viewer == null || viewer.isSpectator(), targetPlayer.isSpectator(), "
                        + "targetIsGameParticipant, targetIsDeadParticipant, "
                        + "targetTraits.isLastStandPending(), "
                        + "targetTraits.isTemporaryFakeDeathPending() );"
        ));
        assertTrue(source.contains("GameWorldComponent.KEY.get(targetPlayer.getWorld())"));
        assertTrue(source.contains("game.isRunning()"));
        assertTrue(source.contains("game.hasAnyRole(targetPlayer)"));
        assertTrue(source.contains("game.isPlayerDead(targetPlayer.getUuid())"));
        assertTrue(source.contains("TraitPlayerComponent.KEY.get(targetPlayer)"));
        assertTrue(source.contains("isLastStandPending()"));
        assertTrue(source.contains("isTemporaryFakeDeathPending()"));
        assertTrue(source.contains("SpiritSleuthVisibilityRules.resolveInvisibleToViewer("));
    }

    @Test
    void keepsStaticHelperPrivateForMixinApplication() throws NoSuchMethodException {
        Method helper = SpiritSleuthLivingEntityRendererMixin.class.getDeclaredMethod(
                "resolveSpectatorPlayerHeadVisibility",
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class,
                boolean.class
        );

        assertTrue(
                Modifier.isPrivate(helper.getModifiers()),
                "Static helper methods declared by a mixin must be private"
        );
    }

    @Test
    void wrapsWatheBodyRendererInvisibilityUsingTheBodyOwnersState() throws IOException {
        assertTrue(Files.isRegularFile(BODY_MIXIN_SOURCE),
                "Spirit Sleuth must cover Wathe's body renderer seam");
        String source = Files.readString(BODY_MIXIN_SOURCE);

        assertTrue(source.contains("@Mixin(PlayerBodyEntityRenderer.class)"));
        assertTrue(source.contains("@WrapOperation("));
        assertTrue(source.contains(
                "\"renderBody(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;FF"
                        + "Lnet/minecraft/client/util/math/MatrixStack;"
                        + "Lnet/minecraft/client/render/VertexConsumerProvider;IF)V\""
        ));
        assertTrue(source.contains(
                "\"render(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;FF"
                        + "Lnet/minecraft/client/util/math/MatrixStack;"
                        + "Lnet/minecraft/client/render/VertexConsumerProvider;I"
                        + "Lnet/minecraft/client/render/entity/model/BipedEntityModel;"
                        + "Lnet/minecraft/client/render/RenderLayer;FF)V\""
        ));
        assertTrue(source.contains(
                "target = \"Ldev/doctor4t/wathe/entity/PlayerBodyEntity;"
                        + "isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z\""
        ));
        assertEquals(1, countOccurrences(source, "original.call("));
        assertTrue(source.contains("body.getPlayerUuid()"));
        assertTrue(source.contains("game.hasAnyRole(playerUuid)"));
        assertTrue(source.contains("game.isPlayerDead(playerUuid)"));
        assertTrue(source.contains("SparkTraitsApi.isFakeDeathBody(body)"));
        assertTrue(source.contains("SpiritSleuthVisibilityRules.shouldRevealSpectatorHead("));
        assertFalse(source.contains("getRenderLayer("));
        assertFalse(source.contains("GameFunctions.isPlayerPlayingAndAlive("));
    }

    @Test
    void registersTheRendererMixinInTheClientConfiguration() throws IOException {
        String clientMixins = Files.readString(Path.of(
                "src/client/resources/sparktraits.client.mixins.json"
        ));

        assertTrue(clientMixins.contains("\"SpiritSleuthLivingEntityRendererMixin\""));
        assertTrue(clientMixins.contains("\"SpiritSleuthPlayerBodyRendererMixin\""));
    }

    private static int countOccurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
