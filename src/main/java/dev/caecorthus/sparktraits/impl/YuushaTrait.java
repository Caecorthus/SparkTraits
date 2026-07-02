package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Innocent-side active trait inspired by a heroic full-bloom burst.
 * 好人阵营主动天赋：以一次“满开”换取爆发，并承受本局永久后遗症。
 */
public final class YuushaTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("yuusha");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return YuushaTraitService.YUUSHA_COLOR;
    }

    @Override
    public TraitAudience audience() {
        return TraitAudience.INNOCENT_ONLY;
    }

    @Override
    public double rollWeight(TraitSelectionContext context) {
        return YuushaTraitService.rollWeight(context);
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return Trait.super.canApply(context) && YuushaTraitService.canSelectYuusha(context);
    }

    @Override
    public void onAssigned(ServerPlayerEntity player, TraitAssignmentReason reason) {
        YuushaTraitService.preparePlayer(player);
    }

    @Override
    public void onRemoved(ServerPlayerEntity player, TraitRemovalReason reason) {
        YuushaTraitService.clearPlayer(player);
    }
}
