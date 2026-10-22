package dev.caecorthus.sparktraits.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Loader-safe optional bridge to SparkWitch's Black Raven sensed-instinct resolver. The Black Raven is a civilian
 * police role (2026-10-21); SparkWitch keeps the reflected {@code resolvePrioritySensedHighlight(Entity)} signature
 * and answers its 8-block raven-feather x-ray through it.
 * 到 SparkWitch 黑羽鸦感知本能裁决器的加载器安全可选桥接。黑羽鸦现为好人警察身份（2026-10-21）；SparkWitch
 * 保留被反射的 {@code resolvePrioritySensedHighlight(Entity)} 签名，并通过它给出 8 格鸦羽色透视结果。
 */
public final class SparkWitchBlackRavenBridge {
    private static final String MOD_ID = "sparkwitch";
    private static final String HOOKS_CLASS =
            "dev.caecorthus.sparkwitch.client.blackraven.BlackRavenInstinctClientHooks";

    private SparkWitchBlackRavenBridge() {
    }

    /*
     * Resolved once, on first use, by the JVM's thread-safe class initialization: the instinct hook calls this for every
     * visible entity each frame, so no call takes a lock or boxes arguments into an Object[].
     * 首次使用时由 JVM 线程安全的类初始化解析一次：本能钩子每帧为每个可见实体调用此处，因此调用时不加锁，也不把参数装进 Object[]。
     */
    private static final class Holder {
        private static final @Nullable MethodHandle RESOLVE_SENSED_HIGHLIGHT = resolveMethod();
    }

    public static @Nullable Integer resolveSensedInstinctHighlight(Entity target) {
        if (target == null) {
            return null;
        }
        MethodHandle method = Holder.RESOLVE_SENSED_HIGHLIGHT;
        if (method == null) {
            return null;
        }
        try {
            return (Integer) method.invokeExact(target);
        } catch (Throwable ignored) {
            // Like the reflective call it replaces, any failure inside SparkWitch falls back to SparkTraits' own logic.
            // 与被替换的反射调用一致，SparkWitch 内部的任何失败都回退到 SparkTraits 自身逻辑。
            return null;
        }
    }

    private static @Nullable MethodHandle resolveMethod() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> hooks = Class.forName(HOOKS_CLASS, false, SparkWitchBlackRavenBridge.class.getClassLoader());
            return MethodHandles.publicLookup().unreflect(integerQuery(hooks, "resolvePrioritySensedHighlight"));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            /*
             * SparkWitch 是可选依赖；如果对方版本还没有黑羽鸦感知桥，SparkTraits
             * 必须继续使用自己的本能逻辑，不能因为兼容入口缺失导致客户端崩溃。
             */
            return null;
        }
    }

    private static Method integerQuery(Class<?> hooks, String name) throws NoSuchMethodException {
        Method method = hooks.getMethod(name, Entity.class);
        if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != Integer.class) {
            throw new NoSuchMethodException(name + " must be public static and return Integer");
        }
        return method;
    }
}
