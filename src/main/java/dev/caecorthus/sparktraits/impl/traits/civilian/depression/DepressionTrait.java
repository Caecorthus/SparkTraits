package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Set;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandTrait;

/**
 * Civilian-side Depression trait with runtime stamina hooks.
 * 平民侧抑郁天赋，并在天赋生命周期中维护体力修正。
 */
public final class DepressionTrait implements Trait {
    @Override
    public Identifier id() {
        return CivilianTraits.DEPRESSION;
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
        return Set.of(ImpostorTrait.ID, LastStandTrait.ID, CivilianTraits.INTROVERTED, CivilianTraits.EXTROVERTED);
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return Trait.super.canApply(context)
                && CivilianTraitService.canSelectCivilianTrait(context.role(), context.selectedTraitIds())
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
