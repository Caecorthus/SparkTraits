package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import net.minecraft.util.Identifier;

/**
 * 敏感 / Sensitive: the holder can always see the round timer, and their kills against innocents and neutrals
 * extend it. Universal trait.
 * 敏感：持有者始终能看到本场剩余时间；其击杀好人 / 中立会延长时间。通用词条。
 */
public final class SensitiveTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("sensitive");
    public static final int COLOR = 0x7FD1E8;

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
    public TraitAudience audience() {
        return TraitAudience.UNIVERSAL;
    }
}
