package dev.caecorthus.sparktraits.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Loader-safe optional bridge to SparkWitch's Wraith owner API; unavailable queries preserve normal behavior.
 * 到 SparkWitch 冤魂所有者 API 的加载器安全可选桥接；查询不可用时保留正常行为。
 */
public final class SparkWitchWraithBridge {
    private static final String MOD_ID = "sparkwitch";
    private static final String API_CLASS = "dev.caecorthus.sparkwitch.api.SparkWitchApi";
    private static Method isWraithActive;
    private static boolean initialized;

    private SparkWitchWraithBridge() {
    }

    public static boolean isWraithActive(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        resolveMethods();
        return invokeBoolean(isWraithActive, player);
    }

    private static boolean invokeBoolean(Method method, PlayerEntity player) {
        if (method == null) {
            return false;
        }
        try {
            Object result = method.invoke(null, player);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException ignored) {
            return false;
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
            Class<?> api = Class.forName(API_CLASS);
            isWraithActive = booleanQuery(api, "isWraithActive");
        } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            // SparkWitch is optional; absent or incompatible APIs must leave normal mood behavior intact.
            // SparkWitch 是可选依赖；缺失或不兼容的 API 必须保留正常理智条行为。
        }
    }

    private static Method booleanQuery(Class<?> api, String name) throws NoSuchMethodException {
        Method method = api.getMethod(name, PlayerEntity.class);
        if (!Modifier.isStatic(method.getModifiers())
                || (method.getReturnType() != boolean.class && method.getReturnType() != Boolean.class)) {
            throw new NoSuchMethodException(name + " must be public static and return boolean");
        }
        return method;
    }
}
