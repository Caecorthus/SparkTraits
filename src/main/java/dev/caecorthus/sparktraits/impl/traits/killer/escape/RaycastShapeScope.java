package dev.caecorthus.sparktraits.impl.traits.killer.escape;

import java.util.function.Supplier;

/** Marks block-shape queries made for a RaycastContext, so the door exemption stays movement-only.
 *  A COLLIDER ray reuses its entity's shape context; without this, the escaping player's sight and aim
 *  (canSee, ProjectileUtil.getCollision) would also pass through closed doors.
 *  标记为 RaycastContext 发起的方块形状查询，使穿门豁免仅作用于移动。COLLIDER 射线沿用实体的形状上下文，
 *  否则逃脱者的视线与瞄准（canSee、ProjectileUtil.getCollision）也会穿过关闭的门。 */
public final class RaycastShapeScope {
    // Per-thread nesting depth. Every ray block lookup on both sides passes through here, so the counter is
    // created once per thread instead of set and removed (a fresh ThreadLocal entry) on every query.
    // 每线程的嵌套深度。双端每次射线方块查询都会经过这里，因此每个线程只创建一次计数器，
    // 而不是每次查询都设置并移除（每次都会新建 ThreadLocal 条目）。
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private RaycastShapeScope() {}

    public static boolean isRaycast() {
        return DEPTH.get()[0] > 0;
    }

    /** Nested queries and exceptions restore the caller's scope on both logical sides.
     *  双端的嵌套查询与异常都会恢复调用者的作用域。 */
    public static <T> T query(Supplier<T> query) {
        int[] depth = DEPTH.get();
        depth[0]++;
        try {
            return query.get();
        } finally {
            depth[0]--;
        }
    }
}
