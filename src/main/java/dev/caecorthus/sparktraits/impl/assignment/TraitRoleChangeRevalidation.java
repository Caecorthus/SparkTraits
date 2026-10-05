package dev.caecorthus.sparktraits.impl.assignment;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.selection.TraitSelector;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandTrait;
import dev.caecorthus.sparktraits.impl.traits.global.ChildishTrait;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.caecorthus.sparktraits.impl.traits.global.WellSuppliedTrait;
import dev.caecorthus.sparktraits.impl.traits.global.pig.PigTrait;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceSerialKillerService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Replaces the traits a living player could not have rolled for the role they now hold with as many fresh draws from
 * that role's pool. Only SparkWitch's Grand Witch recruitment calls this (through the public facade); no role-change
 * listener runs it.
 * 将存活玩家在当前身份下无法于开局获得的天赋，替换为从该身份候选池重新抽取的同等数量天赋。仅由 SparkWitch 大魔女招募
 * 经公共门面调用，不挂接任何换身份监听。
 */
public final class TraitRoleChangeRevalidation {
    /**
     * Pig and Childish resize the body, and a mid-round resize would show everyone the role change (owner decision
     * 2026-10-05); owners who already had them keep them.
     * 猪与幼稚会改变体型，局中改变体型会让所有人看出换身份（所有者 2026-10-05 决定）；原本就拥有者保留。
     */
    static final Set<Identifier> REPLACEMENT_EXCLUSIONS = Set.of(PigTrait.ID, ChildishTrait.ID);

    private TraitRoleChangeRevalidation() {
    }

    public static void replaceTraitsIneligibleForCurrentRole(
            ServerPlayerEntity player,
            BiConsumer<List<Text>, List<Text>> visitor
    ) {
        if (player == null || !(player.getWorld() instanceof ServerWorld world)) {
            return;
        }
        GameWorldComponent game = GameWorldComponent.KEY.maybeGet(world).orElse(null);
        if (game == null || !game.isRunning() || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        Role role = game.getRole(player);
        if (role == null) {
            return;
        }
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.maybeGet(player).orElse(null);
        if (traits == null || ownsLiveTraitTransition(player, traits)) {
            return;
        }

        List<Identifier> active = traits.getActiveTraitIds();
        List<Identifier> kept = TraitAssignmentService.retainTraitsEligibleForRole(
                world, game, player, role, active, TraitAssignmentService.UNCAPPED);
        Outcome outcome = plan(active, traits.getRevealedTraitIds(), kept, traits::isVisibleToOwner);
        if (outcome.dropped().isEmpty()) {
            return;
        }

        // One draw per dropped trait, hidden ones included (owner decision 2026-10-05); a dropped trait never returns.
        // 每移除一个天赋补抽一次，隐藏天赋同样计入（所有者 2026-10-05 决定）；被移除的天赋不会被重新抽回。
        TraitWorldComponent traitWorld = TraitWorldComponent.KEY.get(world);
        List<Identifier> rolled = TraitSelector.selectReplacementTraits(
                world, game, traitWorld, player, new Random(world.getRandom().nextLong()), game.getAllPlayers().size(),
                outcome.kept(), outcome.dropped().size(), replacementExclusions(outcome.dropped()));
        List<Identifier> rolledVisible = visibleAtStart(rolled);

        // Kept traits stay untouched; dropped ones get onRemoved, rolled ones onAssigned.
        // 保留的天赋不受影响；被移除的天赋收到 onRemoved，补抽的天赋收到 onAssigned。
        traits.restoreActiveTraitsForRuntime(
                concat(outcome.kept(), rolled), concat(outcome.keptRevealed(), rolledVisible), TraitAssignmentReason.RANDOM);
        clearDroppedTraitState(player, outcome.dropped());
        TraitAssignmentService.markUniqueTraits(traitWorld, rolled);
        if (rolled.contains(WellSuppliedTrait.ID)) {
            // The caller has written the new role's balance first; it stands in for starting money (owner decision
            // 2026-10-05). / 调用方已先写入新身份余额，视作起始金币（所有者 2026-10-05 决定）。
            GlobalTraitService.applyWellSuppliedStartingMoney(player);
        }
        // Round-end winners and replay tooltips read this snapshot after the player leaves; death snapshots stay.
        // 玩家离线后回合结算与回放提示读取此快照；死亡快照保持不变。
        traitWorld.snapshotRoundTraits(player.getUuid(), traits.getActiveTraitIds());
        visitor.accept(names(outcome.namedDropped()), names(rolledVisible));
    }

    static Set<Identifier> replacementExclusions(List<Identifier> dropped) {
        LinkedHashSet<Identifier> exclusions = new LinkedHashSet<>(REPLACEMENT_EXCLUSIONS);
        exclusions.addAll(dropped);
        return Set.copyOf(exclusions);
    }

    /**
     * Fresh traits are revealed unless hidden at start, as at round start.
     * 与开局一致，新天赋除开局隐藏者外均对本人揭示。
     */
    static List<Identifier> visibleAtStart(List<Identifier> traitIds) {
        return traitIds.stream().filter(traitId -> {
            Trait trait = TraitRegistry.get(traitId);
            return trait == null || !trait.hiddenFromOwnerAtStart();
        }).toList();
    }

    private static List<Identifier> concat(List<Identifier> first, List<Identifier> second) {
        List<Identifier> joined = new ArrayList<>(first);
        joined.addAll(second);
        return List.copyOf(joined);
    }

    /**
     * Pure diff: dropped traits keep their original order, the revealed set narrows to the kept subset, and only
     * dropped traits the owner could see are named.
     * 纯差异计算：被移除天赋保持原顺序，已揭示集合收窄为保留子集，仅列出本人可见的被移除天赋。
     */
    static Outcome plan(
            List<Identifier> active,
            Collection<Identifier> revealed,
            List<Identifier> kept,
            Predicate<Identifier> visibleToOwner
    ) {
        Set<Identifier> keptSet = Set.copyOf(kept);
        List<Identifier> keptRevealed = kept.stream().filter(revealed::contains).toList();
        List<Identifier> dropped = active.stream().filter(traitId -> !keptSet.contains(traitId)).toList();
        List<Identifier> named = dropped.stream().filter(visibleToOwner).toList();
        return new Outcome(List.copyOf(kept), keptRevealed, dropped, named);
    }

    /**
     * Uses the trait's own coloured name, as the owner inventory card does; unregistered ids are never named.
     * 与本人背包卡片一致，使用天赋自身的带色名称；未注册的 id 不会被列出。
     */
    static List<Text> names(List<Identifier> traitIds) {
        List<Text> names = new ArrayList<>(traitIds.size());
        for (Identifier traitId : traitIds) {
            Trait trait = TraitRegistry.get(traitId);
            if (trait != null) {
                names.add(trait.name().copy().styled(style -> style.withColor(trait.color())));
            }
        }
        return List.copyOf(names);
    }

    /**
     * Last Stand and Depression own a pending death transition, or an active Depression psycho whose stash holds the
     * real inventory; their cleanup assumes death or reset (a fake death would stay stuck in spectator, a psycho would
     * end without its stash), so the whole call waits. Recruitment never targets spectators or Depression psychos.
     * 背水一战与抑郁持有待决死亡转换，或抑郁疯魔（其暂存保存真实物品）；这些清理假定玩家已死亡或重置（假死者会卡在
     * 旁观模式，疯魔会在未归还暂存时结束），因此整次调用跳过。招募本就不会选中旁观者或抑郁疯魔玩家。
     */
    private static boolean ownsLiveTraitTransition(ServerPlayerEntity player, TraitPlayerComponent traits) {
        return LastStandService.isDeathIntercepted(player) || traits.isLastStandPending()
                || DepressionTraitService.isPending(player) || traits.isTemporaryFakeDeathPending()
                || DepressionTraitService.isPsychoActive(player);
    }

    /**
     * Side-state cleanups, run only for the dropped trait that owns that state. Each is safe on a living, non-pending
     * player: no items, kills, teleports or game-mode changes. Timed bombs, blue poison and the Depression counter
     * target sit on this player but belong to someone else's trait, so they stay.
     * 仅对拥有该状态的被移除天赋执行附属状态清理；对存活且非待决的玩家均安全：不发放物品、不击杀、不传送、不改游戏
     * 模式。定时炸弹、蓝毒与抑郁反制目标虽在本玩家身上，但归属他人的天赋，因此保留。
     */
    private static void clearDroppedTraitState(ServerPlayerEntity player, List<Identifier> dropped) {
        if (dropped.contains(ConscienceTrait.ID)) {
            // Murderer clue of a Conscience Serial Killer. / 善良连环杀手的凶手线索。
            ConscienceSerialKillerService.clearPlayer(player);
        }
        if (dropped.contains(LastStandTrait.ID)) {
            // Post-revive instinct hiding, Noelles immunity and return point. / 复活后的本能隐藏、Noelles 豁免与回归点。
            LastStandService.clearPlayer(player);
        }
        if (dropped.contains(CivilianTraits.DEPRESSION)) {
            // Owner-side suicide countdown only; the hunted-by counter target belongs to another player's psycho.
            // 只重置本人的自杀倒计时；被追杀提示（反制目标）属于他人的疯魔，予以保留。
            DepressionTraitService.clearOwnerStateForTraitRemoval(player);
        }
    }

    record Outcome(List<Identifier> kept, List<Identifier> keptRevealed, List<Identifier> dropped,
                   List<Identifier> namedDropped) {
    }
}
