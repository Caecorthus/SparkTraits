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
    private static boolean protocolConfirmed;

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
            // Check initialization, not just method presence; old/incompatible clients retain fallback. SparkWitch installs
            // its compositor once and never removes it, so version 1 is rechecked only until first seen, not every frame.
            // 确认兼容协议已初始化，不能仅凭方法存在就放弃本地后处理。SparkWitch 只安装一次合成器且从不移除，
            // 因此只在首次确认版本 1 之前逐次复核，而非每帧调用。
            if (!protocolConfirmed) {
                if (!Integer.valueOf(1).equals(protocol.invoke(null))) {
                    return false;
                }
                protocolConfirmed = true;
            }
            return Boolean.TRUE.equals(render.invoke(null, player, delta));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            render = null;
            return false;
        }
    }
}
