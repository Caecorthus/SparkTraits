package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import net.minecraft.util.Identifier;

/**
 * Global trait that adds a small task completion bonus when a player has a visible reward system.
 * 全局天赋：当玩家拥有可见奖励系统时，为任务完成追加少量收益。
 */
public final class TaskMasterTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("task_master");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return GlobalTraitService.TASK_MASTER_COLOR;
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return GlobalTraitService.canSelectTaskMaster(
                context.role(),
                context.selectedTraitIds(),
                GlobalTraitService.canSeeMoneyForTrait(context.player(), context.gameComponent(), context.role())
        );
    }
}
