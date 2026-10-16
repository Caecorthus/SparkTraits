package dev.caecorthus.sparktraits.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * SparkStrength's tablet sabotage replaced Wathe's shop blackout. When that provider is present, Master Saboteur is
 * offered to players who may sabotage (SparkStrength applies the trait's effects itself); otherwise the shop rule
 * still decides. No hard dependency: an absent or older provider simply answers false.
 * SparkStrength 的平板破坏取代了 Wathe 的商店关灯。存在该提供方时，破坏大师会提供给可以破坏的玩家（词条效果由
 * SparkStrength 自己实现）；否则仍按商店规则判断。无强制依赖：缺失或旧版提供方直接返回 false。
 */
public final class SparkStrengthSabotageBridge {
    private static boolean resolved;
    private static Method hasSabotageIdentity;

    private SparkStrengthSabotageBridge() {
    }

    public static boolean canSabotage(PlayerEntity player) {
        resolve();
        if (player == null || hasSabotageIdentity == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(hasSabotageIdentity.invoke(null, player));
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
            Method method = Class.forName("annina.sparkstrength.sabotage.SabotageAccess")
                    .getMethod("hasSabotageIdentity", PlayerEntity.class);
            if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == boolean.class) {
                hasSabotageIdentity = method;
            }
        } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            // Older providers still sell Wathe's blackout, which the shop rule covers.
            // 旧版提供方仍出售 Wathe 关灯，由商店规则覆盖。
        }
    }
}
