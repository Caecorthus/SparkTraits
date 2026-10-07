package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Pure decisions behind the downstream blue-poison facade (no world, stack or registry access).
 * 下游蓝毒门面背后的纯规则判定，不访问世界、物品堆或注册表。
 */
public final class BluePoisonInteropRules {
    /** What converting native Wathe poison into blue poison does to one poison surface.
     *  把 Wathe 原生毒转换为蓝毒时，对单个投毒载体的处理结果。 */
    public enum Conversion {
        /** No native poison: nothing changes, even if a blue layer is already present.
         *  没有原生毒：不做任何修改，即使已经存在蓝毒层。 */
        NONE,
        /** Native poison is removed and a blue layer owned by the converter is added.
         *  移除原生毒，并添加归属转换者的蓝毒层。 */
        REPLACE_NATIVE_WITH_BLUE,
        /** Native poison is removed; the existing blue layer keeps its original owner.
         *  移除原生毒；已有蓝毒层保留原归属者。 */
        DROP_NATIVE_KEEP_BLUE
    }

    private BluePoisonInteropRules() {
    }

    public static Conversion conversion(boolean hasNativePoison, boolean hasBlueLayer) {
        if (!hasNativePoison) {
            return Conversion.NONE;
        }
        return hasBlueLayer ? Conversion.DROP_NATIVE_KEEP_BLUE : Conversion.REPLACE_NATIVE_WITH_BLUE;
    }

    /** Parses a stored poisoner UUID string; malformed or missing values read as "no poisoner" instead of throwing.
     *  解析保存的投毒者 UUID 字符串；缺失或格式错误时视为无投毒者，不抛异常。 */
    public static @Nullable UUID parsePoisoner(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    /**
     * Thread-safe predicate list; a throwing predicate counts as "not exempt" and only the first failure is reported.
     * 线程安全的谓词列表；抛异常的谓词视为“不豁免”，且只报告第一次失败，避免每 tick 刷日志。
     */
    public static final class DrainExemptions<T> {
        private final CopyOnWriteArrayList<Predicate<? super T>> predicates = new CopyOnWriteArrayList<>();
        private final AtomicBoolean failureReported = new AtomicBoolean();
        private final Consumer<Throwable> firstFailureReporter;

        public DrainExemptions(Consumer<Throwable> firstFailureReporter) {
            this.firstFailureReporter = firstFailureReporter;
        }

        public void register(@Nullable Predicate<? super T> exemption) {
            if (exemption != null) {
                // Re-registering the same predicate instance is a no-op. 重复注册同一谓词实例不会叠加。
                predicates.addIfAbsent(exemption);
            }
        }

        public boolean isExempt(T subject) {
            for (Predicate<? super T> predicate : predicates) {
                try {
                    if (predicate.test(subject)) {
                        return true;
                    }
                } catch (RuntimeException | LinkageError failure) {
                    if (failureReported.compareAndSet(false, true)) {
                        firstFailureReporter.accept(failure);
                    }
                }
            }
            return false;
        }
    }
}
