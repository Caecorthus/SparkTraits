package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparktraits.client.render.ClientFrameClock;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Brackets each drawn frame for {@link ClientFrameClock}'s per-frame caches. The frame ends in a finally block, so a
 * cancelling inject from another mod or a recovered exception can never leave a frame open for ticks and packets.
 * 为 {@link ClientFrameClock} 的逐帧缓存标出每一帧的绘制区间。帧在 finally 中结束，
 * 因此其他模组的取消注入或被恢复的异常都不会让一帧对 tick 与数据包保持打开。
 */
@Mixin(GameRenderer.class)
public abstract class ClientFrameClockMixin {
    @WrapMethod(method = "render")
    private void sparktraits$bracketFrame(RenderTickCounter tickCounter, boolean tick, Operation<Void> original) {
        ClientFrameClock.beginFrame();
        try {
            original.call(tickCounter, tick);
        } finally {
            ClientFrameClock.endFrame();
        }
    }
}
