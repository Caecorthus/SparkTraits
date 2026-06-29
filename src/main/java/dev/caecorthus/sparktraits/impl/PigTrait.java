package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Universal trait that reshapes the player into a pig body with their own head.
 * 通用猪天赋：将玩家重塑为猪身体，并保留玩家自己的头。
 */
public final class PigTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("pig");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return PigTraitService.COLOR;
    }

    @Override
    public int weight() {
        return PigTraitService.WEIGHT;
    }

    @Override
    public double rollWeight() {
        return PigTraitService.ROLL_WEIGHT;
    }

    @Override
    public Set<Identifier> incompatibleTraits() {
        return Set.of(ChildishTrait.ID);
    }

    @Override
    public void onAssigned(ServerPlayerEntity player, TraitAssignmentReason reason) {
        PigTraitService.applyPigDimensions(player);
    }

    @Override
    public void onRemoved(ServerPlayerEntity player, TraitRemovalReason reason) {
        PigTraitService.removePigDimensions(player);
    }
}
