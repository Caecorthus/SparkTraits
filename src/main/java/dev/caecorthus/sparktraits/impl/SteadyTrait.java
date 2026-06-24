package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Global trait that raises the owner's real mood ceiling.
 * 全局天赋：提高拥有者真理智系统的理智上限。
 */
public final class SteadyTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("steady");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return GlobalTraitService.STEADY_COLOR;
    }

    @Override
    public Set<Identifier> incompatibleTraits() {
        return Set.of(ImpostorTrait.ID);
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return GlobalTraitService.canSelectSteady(context.role(), context.selectedTraitIds());
    }

    @Override
    public void onRemoved(ServerPlayerEntity player, TraitRemovalReason reason) {
        GlobalTraitService.clampMoodAfterSteadyRemoved(player);
    }
}
