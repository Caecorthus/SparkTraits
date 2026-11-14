package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import net.minecraft.util.Identifier;

/**
 * 敏感 / Sensitive: the holder can always see the round timer, and their kills against innocents and neutrals
 * extend it. Universal trait, rolled at a quarter of the normal rate and never offered to a role that already sees
 * the timer. 敏感：持有者始终能看到本场剩余时间；其击杀好人 / 中立会延长时间。通用词条，
 * 刷新概率为其他词条的四分之一，且不会出现在本来就能看到时间的身份上。
 */
public final class SensitiveTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("sensitive");
    public static final int COLOR = 0x7FD1E8;

    /** Weight of every ordinary trait; this trait rolls at a quarter of it. 普通词条的权重；本词条取其四分之一。 */
    public static final int NORMAL_WEIGHT = 100;
    public static final int WEIGHT = NORMAL_WEIGHT / 4;

    /** Time gained per innocent kill. 击杀好人延长的时间（1 分钟）。 */
    public static final int INNOCENT_KILL_SECONDS = 60;
    /** Time gained per neutral kill. 击杀中立延长的时间（30 秒）。 */
    public static final int NEUTRAL_KILL_SECONDS = 30;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return COLOR;
    }

    @Override
    public int weight() {
        return WEIGHT;
    }

    @Override
    public TraitAudience audience() {
        return TraitAudience.UNIVERSAL;
    }

    /**
     * Conflicts with every role that already sees the round timer: the trait would be dead weight on them.
     * 与所有本来就能看到时间的身份冲突：那类身份拿到本词条等于白拿。
     */
    @Override
    public boolean canApply(TraitSelectionContext context) {
        return Trait.super.canApply(context)
                && (context.role() == null || !context.role().canSeeTime());
    }
}
