package dev.caecorthus.sparktraits.impl.replay;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.ReplayRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.function.IntSupplier;

/**
 * Writes SparkTraits' match-defining transitions through Wathe's replay API.
 * 通过 Wathe 回放 API 记录 SparkTraits 中会改变对局走向的状态转换。
 */
public final class SparkTraitsReplayEvents {
    static final Identifier LAST_STAND_TRIGGERED = SparkTraits.id("last_stand_triggered");
    static final Identifier FINAL_MOMENT_START = SparkTraits.id("final_moment_start");
    static final Identifier LOOSE_END_CONVERSION = SparkTraits.id("loose_end_conversion");

    private SparkTraitsReplayEvents() {
    }

    /**
     * Keeps recording the Loose End event but hides its global line: SparkFactionAPI's cause-tagged
     * role-change line already shows the same conversion, and a null formatter result is skipped by Wathe.
     * 继续记录亡命徒转换事件但隐藏其全局行：SparkFactionAPI 带原因的身份转变行已展示同一转换，Wathe 会跳过返回 null 的格式化结果。
     */
    public static void registerFormatters() {
        ReplayRegistry.registerGlobalEventFormatter(LOOSE_END_CONVERSION, (event, match, world) -> null);
    }

    /**
     * Also writes {@code alive_opponents} (int) for SparkAssist's achievements; the count is only taken during a match.
     * 同时写入供 SparkAssist 成就读取的 alive_opponents（int）；仅在对局进行中才统计该数值。
     */
    public static void recordLastStandTriggered(ServerPlayerEntity player, IntSupplier aliveOpponents) {
        SparkTraitsAchievementRecords.guarded(LAST_STAND_TRIGGERED.toString(), () ->
                GameRecordManager.recordGlobalEvent(player.getServerWorld(), LAST_STAND_TRIGGERED, player,
                        SparkTraitsAchievementRecords.aliveOpponentsData(aliveOpponents.getAsInt())));
    }

    /**
     * Also writes {@code alive_opponents} (int) for SparkAssist's achievements; the count is only taken during a match.
     * 同时写入供 SparkAssist 成就读取的 alive_opponents（int）；仅在对局进行中才统计该数值。
     */
    public static void recordFinalMomentStarted(ServerWorld world, IntSupplier aliveOpponents) {
        SparkTraitsAchievementRecords.guarded(FINAL_MOMENT_START.toString(), () ->
                GameRecordManager.recordGlobalEvent(world, FINAL_MOMENT_START, null,
                        SparkTraitsAchievementRecords.aliveOpponentsData(aliveOpponents.getAsInt())));
    }

    public static void recordLooseEndConversion(ServerPlayerEntity player) {
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), LOOSE_END_CONVERSION, player, null);
    }
}
