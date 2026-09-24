package dev.caecorthus.sparktraits.client.killer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Optional public-facade delegation; absence, old versions and failed rendering retain fallback. */
public final class LastEscapeWitchBridge {
    private static final String COMPOSITOR = "dev.caecorthus.sparkwitch.api.SparkWitchApi";
    private static boolean resolved;
    private static Method render;
    private static Method protocol;

    private LastEscapeWitchBridge() {
    }

    public static boolean tryRender(PlayerEntity player, float delta) {
        if (!resolved) {
            resolved = true;
            if (FabricLoader.getInstance().isModLoaded("sparkwitch")) {
                try {
                    Class<?> facade = Class.forName(COMPOSITOR, false, LastEscapeWitchBridge.class.getClassLoader());
                    Method method = facade.getMethod("tryRenderLastEscapeVision", PlayerEntity.class, float.class);
                    Method version = facade.getMethod("getLastEscapeVisionProtocolVersion");
                    if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == boolean.class
                            && Modifier.isStatic(version.getModifiers()) && version.getReturnType() == int.class) {
                        render = method;
                        protocol = version;
                    }
                } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
                    // The optional dependency may predate the composition contract.
                }
            }
        }
        if (render == null) {
            return false;
        }
        try {
            // Recheck initialization, not just method presence; old/incompatible clients retain fallback.
            // 每次确认已初始化的兼容协议，不能仅凭方法存在就放弃本地后处理。
            return Integer.valueOf(1).equals(protocol.invoke(null))
                    && Boolean.TRUE.equals(render.invoke(null, player, delta));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            render = null;
            return false;
        }
    }
}
