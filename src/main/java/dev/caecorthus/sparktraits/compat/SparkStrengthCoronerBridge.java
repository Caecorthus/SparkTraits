package dev.caecorthus.sparktraits.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Optional bridge to SparkStrength's Coroner body disguise, which native killers see as a cohort.
 * 到 SparkStrength 验尸官尸体伪装的可选桥接；原生杀手会把伪装成杀手/卧底的验尸官看作同伙。
 */
public final class SparkStrengthCoronerBridge {
    private static final String MOD_ID = "sparkstrength";
    private static final String CORONER_SERVICE = "annina.sparkstrength.role.coroner.CoronerService";
    private static final MethodType QUERY_TYPE = MethodType.methodType(boolean.class, PlayerEntity.class);

    private SparkStrengthCoronerBridge() {
    }

    /** Mirrors SparkStrength's own killer-cohort disguise check: killer-faction or Undercover body identity.
     *  与 SparkStrength 自身的杀手同伙伪装判定一致：杀手阵营或卧底尸体身份。 */
    public static boolean appearsAsKillerCohort(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        MethodHandle[] methods = Methods.QUERIES;
        return query(methods[0], player) || query(methods[1], player);
    }

    private static boolean query(MethodHandle method, PlayerEntity player) {
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
     * Resolved once on first query; class initialization keeps later per-frame instinct reads lock-free.
     * 首次查询时解析一次；类初始化保证之后每帧本能判定的读取无需加锁。
     */
    private static final class Methods {
        // [hasKillerFactionDisguise, hasUndercoverDisguise]; a null slot means that query is unavailable.
        // [hasKillerFactionDisguise, hasUndercoverDisguise]；为 null 表示该查询不可用。
        private static final MethodHandle[] QUERIES = resolve();
    }

    private static MethodHandle[] resolve() {
        MethodHandle[] methods = new MethodHandle[2];
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return methods;
        }
        try {
            Class<?> service = Class.forName(CORONER_SERVICE, false, SparkStrengthCoronerBridge.class.getClassLoader());
            methods[0] = booleanQuery(service, "hasKillerFactionDisguise");
            methods[1] = booleanQuery(service, "hasUndercoverDisguise");
        } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            // Older or absent providers simply leave the Coroner looking like a civilian to Impostor instinct.
            // 旧版或缺失的提供方只会让验尸官在内鬼本能中保持平民外观，不引入强制依赖。
        }
        return methods;
    }

    private static MethodHandle booleanQuery(Class<?> service, String name) throws ReflectiveOperationException {
        Method method = service.getMethod(name, PlayerEntity.class);
        if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != boolean.class) {
            throw new NoSuchMethodException(name + " must be public static and return boolean");
        }
        return MethodHandles.publicLookup().unreflect(method).asType(QUERY_TYPE);
    }
}
