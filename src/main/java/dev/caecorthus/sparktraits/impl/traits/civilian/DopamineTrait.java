package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * 多巴胺 / dopamine: for ten seconds after completing a task, mood drain is 30% slower and tasks refresh 30% faster.
 * At most one holder per round, and incompatible with 极简主义 (Minimalism).
 * 多巴胺：完成任务后 10 秒内，理智流失速度降低 30%，任务刷新速度提升 30%。每局最多一人，与极简主义冲突。
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
    /** Mood drain multiplier inside the window (-30%). 窗口内理智流失倍率（降低 30%）。 */
    public static final float MOOD_DRAIN_MULTIPLIER = 0.7F;
    /** Task refresh multiplier inside the window (+30% faster). 窗口内任务刷新倍率（提速 30%）。 */
    public static final float TASK_REFRESH_MULTIPLIER = 1.3F;

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
        return Set.of(MINIMALISM_ID);
    }
}
