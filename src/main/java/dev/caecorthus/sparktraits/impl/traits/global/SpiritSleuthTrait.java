package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import net.minecraft.util.Identifier;

/**
 * Global trait that lets the owner see players in spectator mode.
 * 全局天赋：让拥有者看见处于旁观模式的玩家。
 */
public final class SpiritSleuthTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("spirit_sleuth");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public double rollWeight() {
        return 0.0D;
    }

    @Override
    public int color() {
        return GlobalTraitService.SPIRIT_SLEUTH_COLOR;
    }
}
