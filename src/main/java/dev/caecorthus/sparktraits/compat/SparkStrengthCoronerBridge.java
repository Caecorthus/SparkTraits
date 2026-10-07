package dev.caecorthus.sparktraits.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Optional bridge to SparkStrength's Coroner body disguise, which native killers see as a cohort.
 * 到 SparkStrength 验尸官尸体伪装的可选桥接；原生杀手会把伪装成杀手/卧底的验尸官看作同伙。
 */
public final class SparkStrengthCoronerBridge {
    private static final String MOD_ID = "sparkstrength";
    private static final String CORONER_SERVICE = "annina.sparkstrength.role.coroner.CoronerService";
    private static boolean resolved;
    private static Method hasKillerFactionDisguise;
    private static Method hasUndercoverDisguise;

    private SparkStrengthCoronerBridge() {
    }

    /** Mirrors SparkStrength's own killer-cohort disguise check: killer-faction or Undercover body identity.
     *  与 SparkStrength 自身的杀手同伙伪装判定一致：杀手阵营或卧底尸体身份。 */
    public static boolean appearsAsKillerCohort(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        resolve();
        return query(hasKillerFactionDisguise, player) || query(hasUndercoverDisguise, player);
    }

    private static boolean query(Method method, PlayerEntity player) {
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(null, player));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> service = Class.forName(CORONER_SERVICE, false, SparkStrengthCoronerBridge.class.getClassLoader());
            hasKillerFactionDisguise = booleanQuery(service, "hasKillerFactionDisguise");
            hasUndercoverDisguise = booleanQuery(service, "hasUndercoverDisguise");
        } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            // Older or absent providers simply leave the Coroner looking like a civilian to Impostor instinct.
            // 旧版或缺失的提供方只会让验尸官在内鬼本能中保持平民外观，不引入强制依赖。
        }
    }

    private static Method booleanQuery(Class<?> service, String name) throws NoSuchMethodException {
        Method method = service.getMethod(name, PlayerEntity.class);
        if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != boolean.class) {
            throw new NoSuchMethodException(name + " must be public static and return boolean");
        }
        return method;
    }
}
