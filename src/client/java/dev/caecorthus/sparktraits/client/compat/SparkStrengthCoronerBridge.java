package dev.caecorthus.sparktraits.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Loader-safe optional bridge to SparkStrength's Coroner instinct resolver.
 *
 * <p>SparkTraits owns the early WatheClient instinct mixin, so the normal Wathe
 * {@code GetInstinctHighlight} event can be bypassed before SparkStrength gets a
 * chance to report a killer disguise. This bridge lets SparkTraits ask SparkStrength
 * for only that one presentation result without putting SparkStrength in SparkTraits'
 * compile-time or Fabric metadata dependencies.</p>
 *
 * <p>加载器安全的 SparkStrength 验尸官本能桥接。SparkTraits 掌握 WatheClient
 * 的早期本能 Mixin，可能在 SparkStrength 收到普通本能事件之前就直接返回结果。
 * 因此这里只通过反射查询一个可选的客户端显示结果，不产生编译期或 fabric.mod.json
 * 硬依赖；SparkStrength 缺失或接口不兼容时直接回退到原有逻辑。</p>
 */
public final class SparkStrengthCoronerBridge {
    private static final String MOD_ID = "sparkstrength";
    private static final String HOOKS_CLASS =
            "annina.sparkstrength.client.role.coroner.CoronerClientHooks";

    private static Method resolveKillerDisguiseColor;
    private static boolean initialized;

    private SparkStrengthCoronerBridge() {
    }

    /**
     * Returns SparkStrength's killer-disguise color, or {@code null} when the
     * optional mod is absent, incompatible, or the target is not such a disguise.
     */
    public static @Nullable Integer resolveKillerDisguiseInstinctColor(Entity target) {
        if (target == null) {
            return null;
        }
        resolveMethods();
        Method method = resolveKillerDisguiseColor;
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(null, target);
            return result instanceof Integer color ? color : null;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException ignored) {
            return null;
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
            Class<?> hooks = Class.forName(
                    HOOKS_CLASS,
                    false,
                    SparkStrengthCoronerBridge.class.getClassLoader()
            );
            resolveKillerDisguiseColor = integerQuery(
                    hooks,
                    "resolveKillerDisguiseInstinctColor"
            );
        } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            /*
             * SparkStrength 是可选扩展；缺失或版本没有此公开接缝时，
             * SparkTraits 必须继续使用自己的本能判定，不能让客户端崩溃。
             *
             * SparkStrength is optional. A missing or older seam must leave
             * SparkTraits' own instinct logic intact instead of crashing the client.
             */
        }
    }

    private static Method integerQuery(Class<?> hooks, String name) throws NoSuchMethodException {
        Method method = hooks.getMethod(name, Entity.class);
        if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != Integer.class) {
            throw new NoSuchMethodException(name + " must be public static and return Integer");
        }
        return method;
    }
}
