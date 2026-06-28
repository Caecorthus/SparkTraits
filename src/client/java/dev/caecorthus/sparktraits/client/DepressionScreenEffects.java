package dev.caecorthus.sparktraits.client;

import com.google.gson.JsonSyntaxException;
import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.GoodTraits;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;

/**
 * Client-only grayscale post effect for the Depression trait.
 * 抑郁天赋的客户端灰阶后处理；理智越低，饱和度越低。
 */
public final class DepressionScreenEffects {
    private static final Identifier GRAYSCALE_SHADER = SparkTraits.id("shaders/post/depression_grayscale.json");
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

        activeProcessor.setUniforms("Saturation", 1.0f - strength);
        activeProcessor.render(delta);
        client.getFramebuffer().beginWrite(false);
    }

    private static float grayscaleStrength(ClientPlayerEntity player) {
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        if (!traits.hasActiveTrait(GoodTraits.DEPRESSION)) {
            return 0.0f;
        }
        float mood = PlayerMoodComponent.KEY.get(player).getMood();
        return MathHelper.clamp(1.0f - mood, 0.0f, 1.0f);
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
