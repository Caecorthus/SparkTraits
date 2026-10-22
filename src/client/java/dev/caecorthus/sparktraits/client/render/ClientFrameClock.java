package dev.caecorthus.sparktraits.client.render;

/**
 * Frame id for per-frame client caches, valid only while {@code GameRenderer.render} draws a frame. Packets and client
 * ticks run between frames and may change synced state, so outside a frame callers compute directly instead of
 * reusing a value cached while the previous frame was drawn.
 * 逐帧客户端缓存使用的帧编号，仅在 {@code GameRenderer.render} 绘制一帧期间有效。数据包与客户端 tick 在两帧之间运行，
 * 可能改变同步状态，因此帧外调用方直接计算，不复用上一帧绘制时缓存的值。
 */
public final class ClientFrameClock {
    /** Returned outside a frame: never equal to a cached frame id. 帧外返回值：永不等于已缓存的帧编号。 */
    public static final long NO_FRAME = -1L;

    private static long frame;
    private static boolean rendering;

    private ClientFrameClock() {
    }

    /** Render thread, at the start of {@code GameRenderer.render}. 渲染线程，在 GameRenderer.render 开始时调用。 */
    public static void beginFrame() {
        frame++;
        rendering = true;
    }

    /** Render thread, on every return of {@code GameRenderer.render}. 渲染线程，在 GameRenderer.render 每次返回时调用。 */
    public static void endFrame() {
        rendering = false;
    }

    /** The current frame id while a frame draws, otherwise {@link #NO_FRAME}. 正在绘制时返回当前帧编号，否则返回 NO_FRAME。 */
    public static long currentFrame() {
        return rendering ? frame : NO_FRAME;
    }
}
