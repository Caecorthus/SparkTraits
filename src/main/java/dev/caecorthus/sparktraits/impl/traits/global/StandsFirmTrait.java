package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import net.minecraft.util.Identifier;

/**
 * 立场坚定 / Stands firm: immune to damage dealt by your own faction. Falls out of the train and voodoo are not
 * player-to-player hits, so they still apply.
 * 立场坚定：免疫同阵营造成的伤害。坠落列车与巫毒魔法不是玩家直接伤害，因此仍然生效。
 */
public final class StandsFirmTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("stands_firm");
    public static final int COLOR = 0xE0D8B0;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return COLOR;
    }

    /** Unique per round. 每局唯一。 */
    @Override
    public boolean uniquePerGame() {
        return true;
    }

    /**
     * Hidden trait: it never appears in the owner's trait list, so the holder cannot see it in the inventory bar.
     * 隐藏词条：不会出现在持有者的词条栏里。
     */
    @Override
    public boolean hiddenFromOwnerAtStart() {
        return true;
    }

    /** Universal: any faction can roll it. 通用：任何阵营都能抽到。 */
    @Override
    public TraitAudience audience() {
        return TraitAudience.UNIVERSAL;
    }
}
