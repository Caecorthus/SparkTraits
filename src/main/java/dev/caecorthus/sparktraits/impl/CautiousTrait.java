package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import net.minecraft.util.Identifier;

/**
 * Global trait that keeps the owner's own movement and consumption quiet.
 * 全局天赋：让拥有者自身的移动与进食饮用保持安静。
 */
public final class CautiousTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("cautious");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return GlobalTraitService.CAUTIOUS_COLOR;
    }
}
