package dev.caecorthus.sparktraits.client.render;

import com.google.gson.JsonSyntaxException;
import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.SparkTraitsApi;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.network.ClientPlayerEntity;
import dev.caecorthus.sparktraits.client.killer.LastEscapeWitchBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;

/**
 * Client-only StarRailExpress-style grayscale post effect for the Depression trait.
 * 抑郁天赋的 StarRailExpress 风格客户端灰阶后处理；理智越低，视角越灰白。
 */
public final class DepressionScreenEffects {
    private static final Identifier GRAYSCALE_SHADER = SparkTraits.id("shaders/post/depression_insanity.json");
    private static final float DESATURATE_FACTOR = 0.69f;
    private static final float SPREAD_FACTOR = 1.43f;
    private static final ScreenEffect NO_EFFECT = new ScreenEffect(0.0f, 0.0f, 1.0f);
    private static boolean loadFailed;
    private static Framebuffer processorTarget;
    private static PostEffectProcessor processor;
    private static int processorWidth = -1;
    private static int processorHeight = -1;

    private DepressionScreenEffects() {
    }

    public static void render(ClientPlayerEntity player, float delta) {
        if (!SparkTraitsServerConnection.isConfirmedServer()) {
            closeProcessor();
            return;
        }
        // Delegation performs the pass now, so mixin ordering cannot cause a second pass.
        if (SparkTraitsApi.hasLastEscapeGrayscale(player) && LastEscapeWitchBridge.tryRender(player, delta)) {
            closeProcessor();
            return;
        }
        ScreenEffect effect = screenEffect(player);
        if (!effect.isVisible()) {
            closeProcessor();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PostEffectProcessor activeProcessor = ensureProcessor(client);
        if (activeProcessor == null) {
            return;
        }

        activeProcessor.setUniforms("DesaturateFactor", effect.desaturateFactor());
        activeProcessor.setUniforms("SpreadFactor", effect.spreadFactor());
        activeProcessor.setUniforms("Brightness", effect.brightness());
        try {
            activeProcessor.render(delta);
        } finally {
            client.getFramebuffer().beginWrite(false);
        }
    }

    private static ScreenEffect screenEffect(ClientPlayerEntity player) {
        if (SparkTraitsApi.hasLastEscapeGrayscale(player)) {
            return escapeComposition(player);
        }
        // SparkWitch supplies the exact Wraith grayscale, so Depression must not stack another processor.
        // SparkWitch 提供精确的冤魂灰阶，因此抑郁效果不得再叠加后处理器。
        if (SparkTraitsApi.isWraithActive(player)) {
            return NO_EFFECT;
        }
        return depressionEffect(player);
    }

    /** Optional Witch compositor contract: [desaturation, spread, brightness], no rendering side effects. */
    public static float[] getLastEscapeComposition(PlayerEntity player) {
        ScreenEffect effect = player != null && SparkTraitsServerConnection.isConfirmedServer()
                && SparkTraitsApi.hasLastEscapeGrayscale(player) ? escapeComposition(player) : NO_EFFECT;
        return new float[] {effect.desaturateFactor(), effect.spreadFactor(), effect.brightness()};
    }

    private static ScreenEffect escapeComposition(PlayerEntity player) {
        ScreenEffect depression = depressionEffect(player);
        // A weaker Depression effect must not darken/spread the fixed 50% escape vision.
        // A stronger effect retains its original spread and brightness instead of stacking two passes.
        float[] selected = LastEscapeVisionRules.compose(
                depression.desaturateFactor(), depression.spreadFactor(), depression.brightness());
        return new ScreenEffect(selected[0], selected[1], selected[2]);
    }

    private static ScreenEffect depressionEffect(PlayerEntity player) {
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        if (player.isCreative() || player.isSpectator()) {
            return NO_EFFECT;
        }
        float mood = PlayerMoodComponent.KEY.get(player).getMood();
        float strength = DepressionTraitService.depressionScreenEffectStrength(
                traits.hasActiveTrait(CivilianTraits.DEPRESSION),
                traits.isDepressionPsychoActive(),
                mood
        );
        return new ScreenEffect(strength * DESATURATE_FACTOR, strength * SPREAD_FACTOR, 1.2f);
    }

    private static PostEffectProcessor ensureProcessor(MinecraftClient client) {
        Framebuffer framebuffer = client.getFramebuffer();
        if (loadFailed || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
            return null;
        }
        if (processor != null && (processorTarget != framebuffer
                || processorWidth != framebuffer.textureWidth || processorHeight != framebuffer.textureHeight)) {
            closeProcessor();
        }
        if (processor != null) {
            return processor;
        }

        try {
            processor = new PostEffectProcessor(
                    client.getTextureManager(),
                    client.getResourceManager(),
                    framebuffer,
                    GRAYSCALE_SHADER
            );
            processor.setupDimensions(framebuffer.textureWidth, framebuffer.textureHeight);
            processorWidth = framebuffer.textureWidth;
            processorHeight = framebuffer.textureHeight;
            processorTarget = framebuffer;
            return processor;
        } catch (IOException | JsonSyntaxException exception) {
            SparkTraits.LOGGER.warn("Unable to load Depression/Last Escape grayscale shader", exception);
            closeProcessor();
            loadFailed = true;
            return null;
        }
    }

    /** Called on the render thread on resource reload, disconnect and client shutdown. */
    public static void close() {
        closeProcessor();
        loadFailed = false;
    }

    private static void closeProcessor() {
        if (processor != null) {
            processor.close();
            processor = null;
        }
        processorWidth = -1;
        processorHeight = -1;
        processorTarget = null;
    }

    private record ScreenEffect(float desaturateFactor, float spreadFactor, float brightness) {
        private boolean isVisible() {
            return desaturateFactor > 0.001f || spreadFactor > 0.001f;
        }
    }
}
