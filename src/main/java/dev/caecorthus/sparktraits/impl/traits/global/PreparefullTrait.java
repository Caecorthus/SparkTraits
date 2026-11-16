package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import net.minecraft.util.Identifier;

/**
 * 养精蓄锐 / Preparefully: standing still for ten seconds charges the trait; the next time the holder moves they get
 * Speed III for three seconds. The charge percentage is shown in the bottom-left corner. Universal trait.
 * 养精蓄锐：静止 10 秒完成蓄力；下次移动时获得 3 秒速度 III。左下角显示蓄力百分比。通用词条。
 */
public final class PreparefullTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("preparefull");
    public static final int COLOR = 0xE0C060;

    /** Ticks of standing still needed to fill the charge. 蓄满所需静止 tick 数（10 秒）。 */
    public static final int CHARGE_TICKS = 10 * 20;
    /** Speed III duration granted on the next movement. 下次移动时给予的速度 III 持续 tick 数（3 秒）。 */
    public static final int SPEED_TICKS = 3 * 20;
    /** Speed III amplifier index. 速度 III 的等级索引。 */
    public static final int SPEED_AMPLIFIER = 2;
    /**
     * Horizontal movement under this distance counts as standing still: vehicles and pistons nudge a player by a
     * fraction of a block without them moving.
     * 水平位移小于该距离视为静止：矿车与活塞会让玩家被推动极小距离。
     */
    public static final double STILL_THRESHOLD = 0.02D;

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
