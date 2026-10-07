package dev.caecorthus.sparktraits.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

/**
 * Optional public-facade bridge preserving delayed killers even when their player is offline.
 * 可选公共门面桥接：即使玩家离线，仍保留延迟击杀的责任人。
 */
public final class SparkWitchKillAttributionBridge {
    private static final String MOD_ID = "sparkwitch";
    private static final String API_CLASS = "dev.caecorthus.sparkwitch.api.SparkWitchApi";
    private static Method runWithKillAttribution;
    private static boolean initialized;

    private SparkWitchKillAttributionBridge() {
    }

    public static void runWithKillAttribution(ServerWorld world, UUID responsiblePlayer, Runnable action) {
        invoke(resolveMethod(), world, responsiblePlayer, action);
    }

    static void invoke(Method method, ServerWorld world, UUID responsiblePlayer, Runnable action) {
        if (method == null) {
            action.run();
            return;
        }
        try {
            method.invoke(null, world, responsiblePlayer, action);
        } catch (IllegalAccessException ignored) {
            // Invocation never began; old or inaccessible APIs keep the ordinary lethal path.
            // 调用尚未开始；旧版或不可访问的 API 保留普通致死路径。
            action.run();
        } catch (InvocationTargetException exception) {
            // The action may already have run: never fall back and attempt the lethal call twice.
            // 动作可能已经执行：绝不能回退并重复尝试致死调用。
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("SparkWitch kill attribution action failed", cause);
        }
    }

    private static synchronized Method resolveMethod() {
        if (!initialized) {
            initialized = true;
            if (FabricLoader.getInstance().isModLoaded(MOD_ID)) {
                try {
                    runWithKillAttribution = attributionMethod(Class.forName(API_CLASS));
                } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
                    // SparkWitch is optional; versions without the facade method have no Judge support.
                    // SparkWitch 为可选依赖；没有此门面方法的旧版本不支持判官。
                }
            }
        }
        return runWithKillAttribution;
    }

    static Method attributionMethod(Class<?> api) throws NoSuchMethodException {
        Method method = api.getMethod("runWithKillAttribution", ServerWorld.class, UUID.class, Runnable.class);
        if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class) {
            throw new NoSuchMethodException("runWithKillAttribution must be public static and return void");
        }
        return method;
    }
}
