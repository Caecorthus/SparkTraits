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
 * 多巴胺 / dopamine: for ten seconds after completing a task, mood drain slows by 30%-60% and the holder gets
 * Speed I for 2-5 seconds. Both scale with how many tasks the holder has finished this round: the fewer tasks, the
 * weaker the effect. At most one holder per round, incompatible with 极简主义 and 焦虑症.
 * 多巴胺：完成任务后 10 秒内，理智流失速度降低 30%~60%，并获得 2~5 秒速度 I。
 * 两者都随本局已完成任务数提升：任务越少，数值越低。每局最多一人，与极简主义、焦虑症冲突。
 */
public final class DopamineTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("dopamine");
    public static final int COLOR = 0xFF7ABF;
    /**
     * 极简主义 ships in its own PR, so the conflict is declared by id rather than through its class.
     * 极简主义在另一个 PR 里，这里按 id 声明冲突，避免依赖它的类。
     */
    public static final Identifier MINIMALISM_ID = SparkTraits.id("minimalism");

    /** Duration of the post-task window. 完成后效果持续时间。 */
    public static final int WINDOW_TICKS = 200;
    /** Drain reduction right after the first task. 首个任务后的理智减速比例。 */
    public static final float MIN_DRAIN_REDUCTION = 0.30F;
    /** Drain reduction once the bonus is fully grown. 加成成长满后的理智减速比例。 */
    public static final float MAX_DRAIN_REDUCTION = 0.60F;
    /** Speed duration right after the first task. 首个任务后的速度持续 tick 数。 */
    public static final int MIN_SPEED_TICKS = 40;
    /** Speed duration once the bonus is fully grown. 加成成长满后的速度持续 tick 数。 */
    public static final int MAX_SPEED_TICKS = 100;
    /** Completed tasks needed to reach the maximum. 达到上限所需的已完成任务数。 */
    public static final int FULL_BONUS_TASKS = 6;

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

    /** At most one holder. 最多一人持有。 */
    @Override
    public boolean uniquePerGame() {
        return true;
    }

    @Override
    public Set<Identifier> incompatibleTraits() {
        return Set.of(MINIMALISM_ID, ImpostorTrait.ID, AnxietyTrait.ID);
    }

    /**
     * How far the effect has grown, from 0 (after the first task) to 1 (after {@link #FULL_BONUS_TASKS}).
     * 加成成长度：第一个任务后为 0，达到 {@link #FULL_BONUS_TASKS} 个任务后为 1。
     */
    static float growth(int completedTasks) {
        int grown = Math.max(0, completedTasks - 1);
        return Math.min(1.0F, grown / (float) (FULL_BONUS_TASKS - 1));
    }

    /** Mood-drain reduction inside the window for a holder who has completed this many tasks. 窗口内理智减速比例。 */
    public static float drainReduction(int completedTasks) {
        return MIN_DRAIN_REDUCTION + (MAX_DRAIN_REDUCTION - MIN_DRAIN_REDUCTION) * growth(completedTasks);
    }

    /** Multiplier applied to a mood drop. 理智下降所乘的倍率。 */
    public static float drainMultiplier(int completedTasks) {
        return 1.0F - drainReduction(completedTasks);
    }

    /** Speed I duration granted on task completion. 完成任务时给予的速度 I 持续 tick 数。 */
    public static int speedTicks(int completedTasks) {
        return Math.round(MIN_SPEED_TICKS + (MAX_SPEED_TICKS - MIN_SPEED_TICKS) * growth(completedTasks));
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
