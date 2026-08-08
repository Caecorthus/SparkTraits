package dev.caecorthus.sparktraits.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Loader-safe optional bridge to SparkWitch's Black Raven sensed-instinct resolver.
 * 到 SparkWitch 黑羽鸦感知本能裁决器的加载器安全可选桥接。
 */
public final class SparkWitchBlackRavenBridge {
    private static final String MOD_ID = "sparkwitch";
    private static final String HOOKS_CLASS =
            "dev.caecorthus.sparkwitch.client.blackraven.BlackRavenInstinctClientHooks";
    private static Method resolveSensedHighlight;
    private static boolean initialized;

    private SparkWitchBlackRavenBridge() {
    }

    public static @Nullable Integer resolveSensedInstinctHighlight(Entity target) {
        if (target == null) {
            return null;
        }
        resolveMethods();
        Method method = resolveSensedHighlight;
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(null, target);
            return result instanceof Integer color ? color : null;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException ignored) {
            return null;
        }
    }

    private static synchronized void resolveMethods() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> hooks = Class.forName(HOOKS_CLASS, false, SparkWitchBlackRavenBridge.class.getClassLoader());
            resolveSensedHighlight = integerQuery(hooks, "resolvePrioritySensedHighlight");
        } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            /*
             * SparkWitch 是可选依赖；如果对方版本还没有黑羽鸦感知桥，SparkTraits
             * 必须继续使用自己的善良本能逻辑，不能因为兼容入口缺失导致客户端崩溃。
             */
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
