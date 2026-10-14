package dev.caecorthus.sparktraits.impl.replay;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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
    static final String LAST_ESCAPE_TRIGGERED = SparkTraits.id("last_escape_triggered").toString();
    static final String SHOWMAN_REWARD = SparkTraits.id("showman_reward").toString();
    static final String PLUNDERER_STEAL = SparkTraits.id("plunderer_steal").toString();
    static final String DEATH_REASON = "death_reason";
    static final String WITNESSES = "witnesses";
    static final String REWARD = "reward";
    static final String STOLEN = "stolen";

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
     * Last Escape cancelled a death: actor = the survivor, target = the player whose attack would have killed them
     * (left out when no killer was passed or the death was self-inflicted, such as a gun backfire),
     * {@code death_reason} = the cancelled death reason id.
     * 绝处逢生取消了一次死亡：actor 为幸存者，target 为本会杀死他的玩家（未传入凶手或自己造成的死亡，例如枪械走火，
     * 则不写 target），death_reason 为被取消的死因标识。
     */
    public static void recordLastEscapeTriggered(
            ServerPlayerEntity survivor,
            @Nullable ServerPlayerEntity killer,
            @Nullable Identifier reason
    ) {
        guarded(LAST_ESCAPE_TRIGGERED, () -> {
            GameRecordManager.EventBuilder event = GameRecordManager.event(LAST_ESCAPE_TRIGGERED).actor(survivor);
            if (killer != null && isOtherPlayer(survivor.getUuid(), killer.getUuid())) {
                event.target(killer);
            }
            if (reason != null) {
                event.put(DEATH_REASON, reason.toString());
            }
            event.record();
        });
    }

    /**
     * The Showman paid a kill reward: actor = the killer, target = the victim, {@code witnesses} = the other living
     * round players counted within range of the victim (not capped), {@code reward} = the coins paid.
     * 作秀支付了击杀奖励：actor 为凶手，target 为死者，witnesses 为死者范围内被计入的其他存活对局玩家数（不封顶），
     * reward 为支付的金币。
     */
    public static void recordShowmanReward(ServerPlayerEntity killer, ServerPlayerEntity victim, int witnesses, int reward) {
        guarded(SHOWMAN_REWARD, () -> GameRecordManager.event(SHOWMAN_REWARD)
                .actor(killer)
                .target(victim)
                .putInt(WITNESSES, witnesses)
                .putInt(REWARD, reward)
                .record());
    }

    /**
     * The Plunderer took coins from a victim: actor = the killer, target = the victim, {@code stolen} = the coins moved.
     * 掠夺者从死者处夺取了金币：actor 为凶手，target 为死者，stolen 为转移的金币。
     */
    public static void recordPlundererSteal(ServerPlayerEntity killer, ServerPlayerEntity victim, int stolen) {
        guarded(PLUNDERER_STEAL, () -> GameRecordManager.event(PLUNDERER_STEAL)
                .actor(killer)
                .target(victim)
                .putInt(STOLEN, stolen)
                .record());
    }

    /**
     * Whether {@code other} is a player other than {@code self}; a missing player is not.
     * other 是否为 self 以外的玩家；缺失的玩家不算。
     */
    static boolean isOtherPlayer(UUID self, @Nullable UUID other) {
        return other != null && !other.equals(self);
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
