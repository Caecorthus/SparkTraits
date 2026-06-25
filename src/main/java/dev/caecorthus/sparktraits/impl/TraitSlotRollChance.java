package dev.caecorthus.sparktraits.impl;

/**
 * Shared trait-slot roll chance rules kept outside CCA components for testable command and selector behavior.
 * 天赋槽位 roll 概率的共用规则；放在 CCA 组件外，方便命令和选择器复用并测试。
 */
public final class TraitSlotRollChance {
    public static final float DEFAULT = 0.75f;
    public static final String NBT_KEY = "TraitSlotRollChance";

    private TraitSlotRollChance() {
    }

    public static float normalize(float chance) {
        if (!Float.isFinite(chance)) {
            return DEFAULT;
        }
        return Math.clamp(chance, 0.0f, 1.0f);
    }

    public static float fromPercent(int percent) {
        return normalize(percent / 100.0f);
    }

    public static int toPercent(float chance) {
        return Math.round(normalize(chance) * 100.0f);
    }
}
