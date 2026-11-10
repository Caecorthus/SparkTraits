package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * 焦虑症 / Anxiety disorder: tasks refresh 150% faster, but finishing one restores the entire mood bar.
 * Passenger-only.
 * 焦虑症：任务刷新频率提升 150%，但完成一个任务会直接回满理智。乘客阵营专属。
 */
public final class AnxietyTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("anxiety");
    public static final int COLOR = 0xB07BE8;
    /** Refresh interval is divided by this factor (+150% frequency). 刷新间隔除以该系数（提升 150%）。 */
    public static final float REFRESH_MULTIPLIER = 2.5F;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return COLOR;
    }

    @Override
    public TraitAudience audience() {
        return TraitAudience.INNOCENT_ONLY;
    }

    @Override
    public Set<Identifier> incompatibleTraits() {
        return Set.of(ImpostorTrait.ID);
    }

    /**
     * Same gate as the other civilian traits, plus a real mood bar: without one there are no real tasks to pace.
     * 与其他平民词条相同的门槛，并要求真理智：没有真理智就没有真正的任务可言。
     */
    @Override
    public boolean canApply(TraitSelectionContext context) {
        return Trait.super.canApply(context)
                && CivilianTraitService.canSelectCivilianTrait(context.role(), context.selectedTraitIds())
                && GlobalTraitService.hasEffectiveRealMood(context.role(), context.selectedTraitIds());
    }
}
