package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.mixin.GameWorldComponentAccessor;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;

import java.util.function.Supplier;

/**
 * Lets round-start trait planning ask money questions about the round it is planning. SparkTraits plans traits inside
 * {@code MurderGameMode.assignRolesAndGetKillerCount}, while Wathe's status is still STARTING. Listeners that answer
 * for a running round only (SparkWitch's Hanna, Noa, Planner, Saint, Judge and others answer DENY until then) would
 * otherwise keep money traits from ever rolling for their roles. While such a check runs for a player who already holds
 * a role and is not dead, the status reads ACTIVE. The field is written directly, so nothing is synced, and the old
 * status is restored before returning. Every other caller, and every other moment, sees the real status.
 * 让开局词条规划按即将开始的对局回答金币问题。SparkTraits 在 {@code MurderGameMode.assignRolesAndGetKillerCount} 内规划词条，
 * 此时 Wathe 的状态仍是 STARTING。只对进行中对局作答的监听器（SparkWitch 的汉娜、诺亚、策划、圣徒、审判官等在此之前返回 DENY）
 * 会让这些身份永远抽不到金币词条。当此类判定针对已有身份且未死亡的玩家执行时，状态读作 ACTIVE：直接写字段，因此不会同步，
 * 返回前恢复原状态。其他调用方和其他时刻看到的都是真实状态。
 */
final class PlanningRoundView {
    private PlanningRoundView() {
    }

    /**
     * Runs {@code query} as the round will see it. Planning-time shop reads that should count shops only built for a
     * running round (Hanna, Noa, Planner) go through here as well.
     * 按对局开始后的情况执行 {@code query}。规划时读取商店、且需计入仅在对局进行中才构建的商店（汉娜、诺亚、策划）时也应经过这里。
     */
    static <T> T evaluate(PlayerEntity player, Supplier<T> query) {
        if (player == null || player.getWorld() == null || player.getWorld().isClient()) {
            return query.get();
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        GameWorldComponent.GameStatus status = game.getGameStatus();
        if (!appliesTo(status, game.hasAnyRole(player.getUuid()), game.isPlayerDead(player.getUuid()))) {
            return query.get();
        }
        GameWorldComponentAccessor accessor = (GameWorldComponentAccessor) game;
        accessor.sparktraits$setGameStatusField(GameWorldComponent.GameStatus.ACTIVE);
        try {
            return query.get();
        } finally {
            accessor.sparktraits$setGameStatusField(status);
        }
    }

    /**
     * Only the planning moment: roles are picked but the round is not running yet.
     * 仅限规划时刻：身份已选定，但对局尚未进行。
     */
    static boolean appliesTo(GameWorldComponent.GameStatus status, boolean hasRole, boolean dead) {
        return status == GameWorldComponent.GameStatus.STARTING && hasRole && !dead;
    }
}
