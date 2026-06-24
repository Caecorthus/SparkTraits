package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import net.minecraft.util.Identifier;

/**
 * Global trait that shortens normal item cooldowns for its owner.
 * 全局天赋：缩短拥有者的普通物品冷却时间。
 */
public final class FastHandsTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("fast_hands");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return GlobalTraitService.FAST_HANDS_COLOR;
    }
}
