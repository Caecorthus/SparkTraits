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
 * 极简主义 / Minimalism: caps simultaneous tasks at two. Innocent-only, and only for roles that actually earn money
 * from tasks.
 * 极简主义：同时持有的任务上限降为 2 个。乘客阵营专属，且只有能从任务获得金币的身份才能拥有。
 */
public final class MinimalismTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("minimalism");
    public static final int COLOR = 0xC8C8C8;

    /** Maximum simultaneous tasks while this trait is held. 持有期间的同时任务上限。 */
    public static final int TASK_CAP = 2;

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
     * Roles that cannot earn coins from tasks cannot hold this trait: the cap would be a pure downside with no
     * economy to trade against. Reuses the same money-visibility gate as Task Master.
     * 无法通过任务获得金币的身份不能持有：该限制没有对应的经济收益。复用与“任务大师”相同的金币可见性判定。
     */
    @Override
    public boolean canApply(TraitSelectionContext context) {
        // Only civilians who genuinely have no money system may take this. usesMoneySystem() also covers roles with a
        // native task reward (bartender, recaller, timekeeper, reporter, waiter) and anything that opts into
        // CanSeeMoney, so money-earning passengers cannot end up here.
        // 只有真正没有金币系统的平民才能拿。usesMoneySystem() 额外覆盖了自带任务金币的身份
        // （酒保、召回者、计时员、记者、服务员）以及任何接入 CanSeeMoney 的身份，
        // 因此能赚金币的乘客不会被分配到这个词条。
        // Passenger-only: the cap only means something for a role that actually runs on the mood/shop loop, and a
        // neutral (or killer) must never roll it. 乘客专属：这条限制只对走理智/商店循环的角色有意义，中立的身份不该拿到。
        // Like the other civilian traits it also needs an original civilian with a real mood bar: without one
        // there are no real tasks to cap.
        // 与其他平民词条一样，还要求原始好人身份且拥有真理智：没有真理智就没有真正的任务可限制。
        return Trait.super.canApply(context)
                && CivilianTraitService.canSelectCivilianTrait(context.role(), context.selectedTraitIds())
                && GlobalTraitService.hasEffectiveRealMood(context.role(), context.selectedTraitIds())
                && !GlobalTraitService.usesMoneySystem(
                        context.player(),
                        context.gameComponent(),
                        context.role()
                );
    }
}
