package dev.caecorthus.sparktraits.impl.traits.killer.escape;

import java.util.function.Supplier;

/** Marks block-shape queries made for a RaycastContext, so the door exemption stays movement-only.
 *  A COLLIDER ray reuses its entity's shape context; without this, the escaping player's sight and aim
 *  (canSee, ProjectileUtil.getCollision) would also pass through closed doors.
 *  标记为 RaycastContext 发起的方块形状查询，使穿门豁免仅作用于移动。COLLIDER 射线沿用实体的形状上下文，
 *  否则逃脱者的视线与瞄准（canSee、ProjectileUtil.getCollision）也会穿过关闭的门。 */
public final class RaycastShapeScope {
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

    private RaycastShapeScope() {}

    public static boolean isRaycast() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }

    /** Nested queries and exceptions restore the caller's scope on both logical sides.
     *  双端的嵌套查询与异常都会恢复调用者的作用域。 */
    public static <T> T query(Supplier<T> query) {
        Boolean previous = ACTIVE.get();
        ACTIVE.set(true);
        try {
            return query.get();
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }
}
