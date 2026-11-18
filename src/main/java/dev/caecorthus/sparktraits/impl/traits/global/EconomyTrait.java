package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import net.minecraft.util.Identifier;

/**
 * 经济 / Economy: purchases above 100 coins refund 10% of what was actually paid. Universal trait.
 * 经济：单笔消费超过 100 金币时返还实付金额的 10%。通用词条。
 */
public final class EconomyTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("economy");
    public static final int COLOR = 0xD4AF37;
    /** Only purchases strictly above this threshold qualify. 仅超过该金额的消费才返还。 */
    public static final int THRESHOLD = 100;
    /** Refund percentage. 返还百分比。 */
    public static final int REFUND_PERCENT = 10;

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

    /**
     * Same money gate as Snowball and Well Supplied, plus a shop that actually sells something above the threshold:
     * otherwise the refund can never fire and the trait only wastes one of its slots.
     * 与滚雪球、物资充沛同一金钱门槛，另外商店里必须真有高于门槛的商品：否则返还永远不会触发，拿到只会白占一个词条位。
     */
    @Override
    public boolean canApply(TraitSelectionContext context) {
        return GlobalTraitService.usesMoneySystem(context.player(), context.gameComponent(), context.role())
                && EconomyService.hasRefundableShopEntry(context.player(), context.role(), context.selectedTraitIds());
    }
}
