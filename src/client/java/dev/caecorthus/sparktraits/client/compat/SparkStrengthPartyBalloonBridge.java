package dev.caecorthus.sparktraits.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Loader-safe optional bridge to SparkStrength's Party Balloon mark. The mark only syncs to Party Animals, and the
 * yellow outline it draws must survive SparkTraits' Conscience branch, which answers before Wathe's event.
 * 到 SparkStrength 派对气球标记的加载器安全可选桥接。该标记只同步给派对狂，其黄色描边需要在 SparkTraits 先于 Wathe 事件作答的
 * 善良分支中保留。
 */
public final class SparkStrengthPartyBalloonBridge {
    private static final String MOD_ID = "sparkstrength";
    private static final String MARK_CLASS = "annina.sparkstrength.component.partyanimal.PartyBalloonMarkComponent";
    private static final String RULES_CLASS = "annina.sparkstrength.role.partyanimal.PartyBalloonRules";

    private SparkStrengthPartyBalloonBridge() {
    }

    // Resolved once on first use by thread-safe class initialization; calls take no lock and box no arguments.
    // 首次使用时由线程安全的类初始化解析一次；调用时不加锁，也不装箱参数。
    private static final class Holder {
        private static final @Nullable MethodHandle IS_MARKED = resolveIsMarked();
        private static final @Nullable Integer HIGHLIGHT_COLOR = resolveHighlightColor();
    }

    /**
     * SparkStrength's balloon outline colour when {@code target} carries a balloon, or {@code null} when it does not
     * or the optional mod is absent or incompatible.
     * 目标带着气球时返回 SparkStrength 的气球描边颜色；未带气球、或可选模组缺失/不兼容时返回 null。
     */
    public static @Nullable Integer highlightColor(PlayerEntity target) {
        MethodHandle isMarked = Holder.IS_MARKED;
        Integer color = Holder.HIGHLIGHT_COLOR;
        if (isMarked == null || color == null) {
            return null;
        }
        try {
            return (boolean) isMarked.invokeExact(target) ? color : null;
        } catch (Throwable ignored) {
            // Any failure inside SparkStrength reads as no balloon. / SparkStrength 内部的任何失败都按未带气球处理。
            return null;
        }
    }

    private static @Nullable MethodHandle resolveIsMarked() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> mark = Class.forName(MARK_CLASS, false, SparkStrengthPartyBalloonBridge.class.getClassLoader());
            Method method = mark.getMethod("isMarked", PlayerEntity.class);
            if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != boolean.class) {
                return null;
            }
            return MethodHandles.publicLookup().unreflect(method);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // SparkStrength is optional; a missing or older seam just means no balloon outline.
            // SparkStrength 是可选依赖；缺失或版本没有此接口时仅表示没有气球描边。
            return null;
        }
    }

    private static @Nullable Integer resolveHighlightColor() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> rules = Class.forName(RULES_CLASS, false, SparkStrengthPartyBalloonBridge.class.getClassLoader());
            Field field = rules.getField("HIGHLIGHT_COLOR");
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != int.class) {
                return null;
            }
            return field.getInt(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }
}
