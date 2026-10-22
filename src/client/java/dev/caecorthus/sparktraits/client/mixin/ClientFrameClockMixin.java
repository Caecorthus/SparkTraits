package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.render.ClientFrameClock;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets each drawn frame for {@link ClientFrameClock}'s per-frame caches.
 * 为 {@link ClientFrameClock} 的逐帧缓存标出每一帧的绘制区间。
 */
@Mixin(GameRenderer.class)
public abstract class ClientFrameClockMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void sparktraits$beginFrame(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        ClientFrameClock.beginFrame();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void sparktraits$endFrame(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        ClientFrameClock.endFrame();
    }
}
