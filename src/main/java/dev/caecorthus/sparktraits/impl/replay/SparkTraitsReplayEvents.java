package dev.caecorthus.sparktraits.impl.replay;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.ReplayRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

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

    public static void recordLastStandTriggered(ServerPlayerEntity player) {
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), LAST_STAND_TRIGGERED, player, null);
    }

    public static void recordFinalMomentStarted(ServerWorld world) {
        GameRecordManager.recordGlobalEvent(world, FINAL_MOMENT_START, null, null);
    }

    public static void recordLooseEndConversion(ServerPlayerEntity player) {
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), LOOSE_END_CONVERSION, player, null);
    }
}
