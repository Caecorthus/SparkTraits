package dev.caecorthus.sparktraits.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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
public final class SparkStrengthCoronerClientBridge {
    private static final String MOD_ID = "sparkstrength";
    private static final String HOOKS_CLASS =
            "annina.sparkstrength.client.role.coroner.CoronerClientHooks";

    private SparkStrengthCoronerClientBridge() {
    }

    // Resolved once on first use by thread-safe class initialization; calls take no lock and box no arguments.
    // 首次使用时由线程安全的类初始化解析一次；调用时不加锁，也不装箱参数。
    private static final class Holder {
        private static final @Nullable MethodHandle RESOLVE_KILLER_DISGUISE_COLOR = resolveMethod();
    }

    /**
     * Returns SparkStrength's killer-disguise color, or {@code null} when the
     * optional mod is absent, incompatible, or the target is not such a disguise.
     */
    public static @Nullable Integer resolveKillerDisguiseInstinctColor(Entity target) {
        if (target == null) {
            return null;
        }
        MethodHandle method = Holder.RESOLVE_KILLER_DISGUISE_COLOR;
        if (method == null) {
            return null;
        }
        try {
            return (Integer) method.invokeExact(target);
        } catch (Throwable ignored) {
            // Like the reflective call it replaces, any failure inside SparkStrength falls back to SparkTraits' logic.
            // 与被替换的反射调用一致，SparkStrength 内部的任何失败都回退到 SparkTraits 的逻辑。
            return null;
        }
    }

    private static @Nullable MethodHandle resolveMethod() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> hooks = Class.forName(
                    HOOKS_CLASS,
                    false,
                    SparkStrengthCoronerClientBridge.class.getClassLoader()
            );
            return MethodHandles.publicLookup().unreflect(integerQuery(
                    hooks,
                    "resolveKillerDisguiseInstinctColor"
            ));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            /*
             * SparkStrength 是可选扩展；缺失或版本没有此公开接缝时，
             * SparkTraits 必须继续使用自己的本能判定，不能让客户端崩溃。
             *
             * SparkStrength is optional. A missing or older seam must leave
             * SparkTraits' own instinct logic intact instead of crashing the client.
             */
            return null;
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
