package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Good-side Depression trait with runtime stamina hooks.
 * 好人侧抑郁天赋，并在天赋生命周期中维护体力修正。
 */
public final class DepressionTrait implements Trait {
    @Override
    public Identifier id() {
        return GoodTraits.DEPRESSION;
    }

    @Override
    public int color() {
        return DepressionTraitService.COLOR;
    }

    @Override
    public TraitAudience audience() {
        return TraitAudience.INNOCENT_ONLY;
    }

    @Override
    public Set<Identifier> incompatibleTraits() {
        return Set.of(ImpostorTrait.ID, LastStandTrait.ID, GoodTraits.INTROVERTED, GoodTraits.EXTROVERTED);
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return Trait.super.canApply(context)
                && GoodTraitService.canSelectGoodTrait(context.role(), context.selectedTraitIds())
                && DepressionTraitService.canSelectDepression(
                        context.role(),
                        context.selectedTraitIds(),
                        context.startingPlayerCount(),
                        context.enforceStartingPlayerCount()
                );
    }

    @Override
    public void onAssigned(ServerPlayerEntity player, TraitAssignmentReason reason) {
        DepressionTraitService.applyDepressionStamina(player);
    }

    @Override
    public void onRemoved(ServerPlayerEntity player, TraitRemovalReason reason) {
        DepressionTraitService.removeDepressionStamina(player);
    }
}
