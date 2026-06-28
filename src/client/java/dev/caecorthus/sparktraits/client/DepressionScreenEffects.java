package dev.caecorthus.sparktraits.client;

import com.google.gson.JsonSyntaxException;
import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.GoodTraits;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.network.ClientPlayerEntity;
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
    private static PostEffectProcessor processor;
    private static int processorWidth = -1;
    private static int processorHeight = -1;

    private DepressionScreenEffects() {
    }

    public static void render(ClientPlayerEntity player, float delta) {
        float strength = grayscaleStrength(player);
        if (strength <= 0.001f) {
            closeProcessor();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PostEffectProcessor activeProcessor = ensureProcessor(client);
        if (activeProcessor == null) {
            return;
        }

        activeProcessor.setUniforms("DesaturateFactor", strength * DESATURATE_FACTOR);
        activeProcessor.setUniforms("SpreadFactor", strength * SPREAD_FACTOR);
        activeProcessor.render(delta);
        client.getFramebuffer().beginWrite(false);
    }

    private static float grayscaleStrength(ClientPlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) {
            return 0.0f;
        }
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        float mood = PlayerMoodComponent.KEY.get(player).getMood();
        return DepressionTraitService.depressionScreenEffectStrength(
                traits.hasActiveTrait(GoodTraits.DEPRESSION),
                traits.isDepressionPsychoActive(),
                mood
        );
    }

    private static PostEffectProcessor ensureProcessor(MinecraftClient client) {
        Framebuffer framebuffer = client.getFramebuffer();
        if (processor != null) {
            if (processorWidth != framebuffer.textureWidth || processorHeight != framebuffer.textureHeight) {
                processor.setupDimensions(framebuffer.textureWidth, framebuffer.textureHeight);
                processorWidth = framebuffer.textureWidth;
                processorHeight = framebuffer.textureHeight;
            }
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
            return processor;
        } catch (IOException | JsonSyntaxException exception) {
            SparkTraits.LOGGER.warn("Unable to load Depression grayscale shader", exception);
            closeProcessor();
            return null;
        }
    }

    private static void closeProcessor() {
        if (processor != null) {
            processor.close();
            processor = null;
        }
        processorWidth = -1;
        processorHeight = -1;
    }
}
