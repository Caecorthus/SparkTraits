package dev.caecorthus.sparktraits.client.instinct;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Impostor instinct takes the killer's path. While Wathe dispatches {@code GetInstinctHighlight} for a living Impostor
 * viewer holding the instinct key, that viewer's raw role reads as Wathe's plain killer
 * ({@code GameWorldComponent#getRole(UUID)} / {@code #isRole(UUID, Role)}), so every listener answers exactly as for a
 * real killer: killer-gated reveals (NoellesRoles' Undercover cohort, SparkStrength's Coroner disguise) apply, every
 * hide (Survival Master, SparkWitch's Stowaway, the Jester Moment) applies, and the Impostor's own civilian-role
 * highlights stay off while the key is held. Nothing else changes: the scope covers only that dispatch on the thread
 * that opened it and only the local viewer's UUID; without the key, and on every server or other thread, roles are raw.
 * 内鬼本能走杀手的路径。Wathe 为按住本能键的存活内鬼观察者分发 {@code GetInstinctHighlight} 期间，该观察者的原始职业
 * 读作 Wathe 普通杀手（{@code GameWorldComponent#getRole(UUID)} / {@code #isRole(UUID, Role)}），因此每个监听器的结果
 * 都与真杀手完全相同：只给杀手看的结果（NoellesRoles 卧底同伙、SparkStrength 验尸官伪装）生效，所有隐藏（生存大师、
 * SparkWitch 偷渡客、小丑时刻）生效，按住本能键时内鬼自身平民职业的高亮不显示。其余一律不变：作用域只覆盖开启它的线程上
 * 的这一次分发，且只针对本地观察者的 UUID；不按键时，以及在服务端或其他线程上，职业都保持原始值。
 */
public final class ImpostorKillerView {
    private static @Nullable Thread scopeThread;
    private static @Nullable UUID scopeViewer;

    private ImpostorKillerView() {
    }

    /** Runs the event dispatch with the viewer read as a plain killer; restores any outer scope. / 以普通杀手身份运行事件分发，并恢复外层作用域。 */
    public static <T> T during(UUID viewer, Supplier<T> dispatch) {
        Thread outerThread = scopeThread;
        UUID outerViewer = scopeViewer;
        scopeThread = Thread.currentThread();
        scopeViewer = viewer;
        try {
            return dispatch.get();
        } finally {
            scopeThread = outerThread;
            scopeViewer = outerViewer;
        }
    }

    /**
     * Hot path (every {@code getRole}/{@code isRole} on the client JVM, the integrated server included): one field read
     * and compare before the UUID check.
     * 热路径（客户端 JVM 上的每次 {@code getRole}/{@code isRole}，含集成服务端）：先读一个字段并比较，再比较 UUID。
     */
    public static boolean readsAsKiller(@Nullable UUID uuid) {
        return scopeThread == Thread.currentThread() && uuid != null && uuid.equals(scopeViewer);
    }

    public static @Nullable Role role(@Nullable UUID uuid, @Nullable Role raw) {
        return readsAsKiller(uuid) ? WatheRoles.KILLER : raw;
    }

    public static boolean isRole(@Nullable UUID uuid, @Nullable Role role, boolean raw) {
        return readsAsKiller(uuid) ? role == WatheRoles.KILLER : raw;
    }
}
