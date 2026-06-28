package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.DepressionScreenEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs Depression's grayscale post effect after world rendering and before HUD rendering.
 * 在世界渲染后、HUD 渲染前运行抑郁灰阶后处理，避免 HUD 阶段重复触发重型效果。
 */
@Mixin(GameRenderer.class)
public abstract class DepressionGameRendererMixin {
    @Shadow
    @Final
    MinecraftClient client;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;beginWrite(Z)V"))
    private void sparktraits$renderDepressionScreenEffects(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        ClientPlayerEntity player = this.client.player;
        if (!tick || this.client.world == null || player == null) {
            return;
        }
        DepressionScreenEffects.render(player, tickCounter.getTickDelta(true));
    }
}
