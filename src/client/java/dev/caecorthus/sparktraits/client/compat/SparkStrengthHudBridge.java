package dev.caecorthus.sparktraits.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Loader-safe optional bridge to the rows SparkStrength stacks under Wathe's money (the killer team wallet).
 * 到 SparkStrength 在 Wathe 金币下方叠加的 HUD 行（杀手团队余额）的加载器安全可选桥接。
 */
public final class SparkStrengthHudBridge {
    private static final String MOD_ID = "sparkstrength";
    private static final String HOOKS_CLASS = "annina.sparkstrength.client.role.economy.KillerTeamEconomyClientHooks";

    private SparkStrengthHudBridge() {
    }

    // Resolved once on first use by thread-safe class initialization; per-frame calls take no lock.
    // 首次使用时由线程安全的类初始化解析一次；逐帧调用不加锁。
    private static final class Holder {
        private static final @Nullable MethodHandle TOP_RIGHT_HUD_BOTTOM = resolveMethod();
    }

    /**
     * Exclusive bottom y of SparkStrength's extra top-right rows, or 0; absent, older or failing providers report 0.
     * SparkStrength 右上角额外行的底边 y（不含），否则为 0；提供方缺失、过旧或调用失败时均返回 0。
     */
    public static int topRightHudBottom() {
        MethodHandle method = Holder.TOP_RIGHT_HUD_BOTTOM;
        if (method == null) {
            return 0;
        }
        try {
            return Math.max(0, (int) method.invokeExact());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static @Nullable MethodHandle resolveMethod() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return null;
        }
        try {
            Method method = Class.forName(HOOKS_CLASS, false, SparkStrengthHudBridge.class.getClassLoader())
                    .getMethod("topRightHudBottom");
            if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == int.class) {
                return MethodHandles.publicLookup().unreflect(method);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // Older SparkStrength builds keep the card at Wathe's own HUD band. 旧版 SparkStrength 时卡片沿用 Wathe 原有顶带。
        }
        return null;
    }
}
