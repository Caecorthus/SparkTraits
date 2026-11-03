package dev.caecorthus.sparktraits.impl.traits.civilian.jester;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandTrait;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Hidden civilian trait that turns one qualifying good-player gunshot into NoellesRoles' real Jester. It is invisible
 * to its owner at start, unique per game, incompatible with Last Stand and Impostor, and uses Last Stand's exact
 * eligibility gate and roll weight. Until triggered it keeps its original civilian alignment.
 * 隐藏平民天赋：被好人用枪击中时转化为 NoellesRoles 的真正小丑。开局对本人隐藏、每局唯一、与背水一战和内鬼互斥，
 * 并沿用背水一战的资格门槛与抽取权重。触发前保持原本的好人阵营。
 */
public final class JesterTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("jester");
    /** Same color as NoellesRoles' Jester role. / 与 NoellesRoles 小丑职业相同的颜色。 */
    private static final int COLOR = 0xF8C8DC;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public int color() {
        return COLOR;
    }

    @Override
    public boolean uniquePerGame() {
        return true;
    }

    @Override
    public boolean hiddenFromOwnerAtStart() {
        return true;
    }

    @Override
    public TraitAudience audience() {
        return TraitAudience.INNOCENT_ONLY;
    }

    @Override
    public Set<Identifier> incompatibleTraits() {
        return Set.of(LastStandTrait.ID, ImpostorTrait.ID);
    }

    @Override
    public boolean canApply(TraitSelectionContext context) {
        return Trait.super.canApply(context)
                && LastStandTrait.canSelectHiddenCivilianTrait(context.role(), context.gameComponent());
    }
}
