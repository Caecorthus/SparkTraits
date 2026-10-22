package dev.caecorthus.sparktraits.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Loader-safe optional bridge to SparkWitch's Wraith owner API; unavailable queries preserve normal behavior.
 * 到 SparkWitch 冤魂所有者 API 的加载器安全可选桥接；查询不可用时保留正常行为。
 */
public final class SparkWitchWraithBridge {
    private static final String MOD_ID = "sparkwitch";
    private static final String API_CLASS = "dev.caecorthus.sparkwitch.api.SparkWitchApi";
    private static final MethodType QUERY_TYPE = MethodType.methodType(boolean.class, PlayerEntity.class);

    private SparkWitchWraithBridge() {
    }

    public static boolean isWraithActive(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        return invokeBoolean(Methods.IS_WRAITH_ACTIVE, player);
    }

    private static boolean invokeBoolean(MethodHandle method, PlayerEntity player) {
        if (method == null) {
            return false;
        }
        try {
            return (boolean) method.invokeExact(player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Resolved once on first query; class initialization makes later reads lock-free on the per-tick sync paths.
     * 首次查询时解析一次；类初始化保证之后在每 tick 同步路径上的读取无需加锁。
     */
    private static final class Methods {
        private static final MethodHandle IS_WRAITH_ACTIVE = resolveMethods();
    }

    private static MethodHandle resolveMethods() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> api = Class.forName(API_CLASS);
            return booleanQuery(api, "isWraithActive");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // SparkWitch is optional; absent or incompatible APIs must leave normal mood behavior intact. A failure here
            // must not escape: it would fail the holder class and turn every later query into an error.
            // SparkWitch 是可选依赖；缺失或不兼容的 API 必须保留正常理智条行为。此处的失败不能抛出：
            // 否则持有类初始化失败，之后的每次查询都会报错。
            return null;
        }
    }

    private static MethodHandle booleanQuery(Class<?> api, String name) throws ReflectiveOperationException {
        Method method = api.getMethod(name, PlayerEntity.class);
        if (!Modifier.isStatic(method.getModifiers())
                || (method.getReturnType() != boolean.class && method.getReturnType() != Boolean.class)) {
            throw new NoSuchMethodException(name + " must be public static and return boolean");
        }
        return MethodHandles.publicLookup().unreflect(method).asType(QUERY_TYPE);
    }
}
