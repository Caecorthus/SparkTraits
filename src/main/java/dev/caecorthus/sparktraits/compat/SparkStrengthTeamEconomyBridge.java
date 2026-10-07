package dev.caecorthus.sparktraits.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Only an economy provider that applies the contribution bonus makes Team First selectable.
 * 只有支持团队贡献加成的经济提供方才允许抽取团队至上。
 */
public final class SparkStrengthTeamEconomyBridge {
    private static boolean resolved;
    private static Method supportsBonus;

    private SparkStrengthTeamEconomyBridge() {
    }

    public static boolean isAvailable() {
        resolve();
        if (supportsBonus == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(supportsBonus.invoke(null));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!FabricLoader.getInstance().isModLoaded("sparkstrength")) {
            return;
        }
        try {
            Method method = Class.forName("annina.sparkstrength.role.economy.KillerTeamEconomyService")
                    .getMethod("supportsTraitContributionBonus");
            if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == boolean.class) {
                supportsBonus = method;
            }
        } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            // Older or absent providers keep the trait out of the pool without adding a hard dependency.
            // 旧版或缺失的提供方使该词条退出抽取池，不引入强制依赖。
        }
    }
}
