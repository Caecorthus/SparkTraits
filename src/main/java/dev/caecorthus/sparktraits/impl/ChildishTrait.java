package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Global trait that scales the whole player body down to three quarters.
 * 全局天赋：将玩家整体体型缩小到四分之三。
 */
public final class ChildishTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("childish");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return GlobalTraitService.CHILDISH_COLOR;
    }

    @Override
    public void onAssigned(ServerPlayerEntity player, TraitAssignmentReason reason) {
        GlobalTraitService.applyChildishScale(player);
    }

    @Override
    public void onRemoved(ServerPlayerEntity player, TraitRemovalReason reason) {
        GlobalTraitService.removeChildishScale(player);
    }
}
