package dev.caecorthus.sparktraits.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Method;

/**
 * Fail-closed optional bridge to SparkWitch's Wraith owner API.
 * 到 SparkWitch 冤魂所有者 API 的失败关闭可选桥接。
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
        Method method = resolveMethod();
        if (method == null) {
            return false;
        }
        try {
            Object result = method.invoke(null, player);
            return result instanceof Boolean active && active;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException ignored) {
            return false;
        }
    }

    private static synchronized Method resolveMethod() {
        if (initialized) {
            return isWraithActive;
        }
        initialized = true;
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> api = Class.forName(API_CLASS);
            Method method = api.getMethod("isWraithActive", PlayerEntity.class);
            if (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) {
                isWraithActive = method;
            }
        } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            // SparkWitch is optional; absent or incompatible APIs must never make Wraith appear active.
            // SparkWitch 是可选依赖；缺失或不兼容的 API 不得让冤魂被判定为活动。
        }
        return isWraithActive;
    }
}
