package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import net.minecraft.util.Identifier;

/**
 * Global trait that reveals the floating heads of confirmed dead spectators.
 * 全局天赋：显示已确认死亡旁观者的悬浮头部。
 */
public final class SpiritSleuthTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("spirit_sleuth");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return GlobalTraitService.SPIRIT_SLEUTH_COLOR;
    }
}
