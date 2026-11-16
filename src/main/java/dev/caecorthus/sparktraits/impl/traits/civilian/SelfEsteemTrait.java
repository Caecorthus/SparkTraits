package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * 自尊心 / Self-esteem: the holder never dies of a mental breakdown — their mood is held just above the collapse
 * line instead. Rolls at an eighth of the normal rate, at most one holder per round, and needs a real mood bar.
 * Passenger-only. 自尊心：持有者不会因精神崩溃而死亡——理智被卡在崩溃线之上。刷新概率为其他词条的八分之一，
 * 每局最多一人，需要真理智。乘客阵营专属。
 */
public final class SelfEsteemTrait implements Trait {
    public static final Identifier ID = SparkTraits.id("self_esteem");
    public static final int COLOR = 0xD9B3FF;

    /** Weight of every ordinary trait; this trait rolls at an eighth of it. 普通词条的权重；本词条取其八分之一。 */
    public static final int NORMAL_WEIGHT = 100;
    public static final double ROLL_WEIGHT = NORMAL_WEIGHT / 8.0D;

    /**
     * Mood is pinned here once it would cross Wathe's collapse line (-1), which is what triggers the breakdown death.
     * 理智将要越过 Wathe 的崩溃线（-1，即精神崩溃死亡的判定值）时被卡在这个值。
     */
    public static final float MOOD_FLOOR = -0.95F;

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
        return (int) ROLL_WEIGHT;
    }

    @Override
    public double rollWeight() {
        return ROLL_WEIGHT;
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

    /**
     * 抑郁's own suicide countdown kills with the same mental-breakdown reason outside Wathe's mood tick, so a holder of
     * both would still die of a "breakdown"; the two never roll together.
     * 抑郁的自毁倒计时在 Wathe 理智 tick 之外以同一个精神崩溃死因击杀，同时持有时仍会“精神崩溃”死亡，因此两者互斥。
     */
    @Override
    public Set<Identifier> incompatibleTraits() {
        return Set.of(ImpostorTrait.ID, CivilianTraits.DEPRESSION);
    }

    /**
     * Whether this player is a holder about to be killed by a mental breakdown. 该玩家是否为即将因精神崩溃死亡的持有者。
     */
    public static boolean protects(ServerPlayerEntity player, Identifier deathReason) {
        return player != null
                && GameConstants.DeathReasons.MENTAL_BREAKDOWN.equals(deathReason)
                && TraitPlayerComponent.KEY.get(player).hasActiveTrait(ID);
    }

    /**
     * Same gate as the other civilian traits, plus a real mood bar: without one there is no breakdown to survive.
     * 与其他平民词条相同的门槛，并要求真理智：没有真理智就没有精神崩溃可免。
     */
    @Override
    public boolean canApply(TraitSelectionContext context) {
        return Trait.super.canApply(context)
                && CivilianTraitService.canSelectCivilianTrait(context.role(), context.selectedTraitIds())
                && GlobalTraitService.hasEffectiveRealMood(context.role(), context.selectedTraitIds());
    }
}
