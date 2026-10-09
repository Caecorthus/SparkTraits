package dev.caecorthus.sparktraits.impl.replay;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Record-only match events that SparkAssist reads at round end to award local hidden achievements.
 * These types deliberately have no replay formatter, so Wathe's end-of-round replay skips them.
 * A failed write is logged and swallowed: recording must never break the gameplay path that called it.
 * 仅写入对局记录的事件，SparkAssist 在回合结束时读取它们以颁发本地隐藏成就。
 * 这些类型刻意不注册回放格式化器，Wathe 的回合结束回放会跳过它们。
 * 写入失败只记日志并吞掉异常：记录绝不能打断调用它的玩法流程。
 */
public final class SparkTraitsAchievementRecords {
    static final String CLOSE_QUARTERS_PARRY = SparkTraits.id("close_quarters_parry").toString();
    static final String WEAPON = "weapon";
    static final String ATTACKER_ROLE = "attacker_role";
    static final String ALIVE_OPPONENTS = "alive_opponents";

    private SparkTraitsAchievementRecords() {
    }

    /**
     * Close Quarters really blocked a melee attack: actor = the blocker, target = the attacker.
     * 近身格挡真正挡下了一次近战攻击：actor 为格挡者，target 为攻击者。
     */
    public static void recordCloseQuartersParry(ServerPlayerEntity blocker, ServerPlayerEntity attacker, ItemStack weapon) {
        guarded(CLOSE_QUARTERS_PARRY, () -> {
            GameRecordManager.EventBuilder event = GameRecordManager.event(CLOSE_QUARTERS_PARRY)
                    .actor(blocker)
                    .target(attacker)
                    .put(WEAPON, Registries.ITEM.getId(weapon.getItem()).toString());
            Role role = GameWorldComponent.KEY.get(attacker.getWorld()).getRole(attacker);
            if (role != null) {
                event.put(ATTACKER_ROLE, role.identifier().toString());
            }
            event.record();
        });
    }

    /**
     * Extra data for Last Stand's global events: living players outside the civilian side.
     * 背水一战全局事件的附加数据：平民阵营以外的存活玩家数。
     */
    static NbtCompound aliveOpponentsData(int aliveOpponents) {
        NbtCompound data = new NbtCompound();
        data.putInt(ALIVE_OPPONENTS, aliveOpponents);
        return data;
    }

    /**
     * Runs a record write only during an active match and never lets it throw into gameplay.
     * 仅在对局进行中执行记录写入，且绝不让异常传回玩法流程。
     */
    static void guarded(String event, Runnable write) {
        if (!GameRecordManager.hasActiveMatch()) {
            return;
        }
        try {
            write.run();
        } catch (RuntimeException | LinkageError exception) {
            SparkTraits.LOGGER.warn("Unable to record {}", event, exception);
        }
    }
}
