package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.event.TaskComplete;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the ten-second window opened whenever a 多巴胺 holder completes a task.
 * <p>Server-side authority only: the window map is touched from the server thread alone, so every reader must pass
 * a server player. The mood mixin goes through {@link #adjustedMood} and {@link TaskPacing}.
 * 追踪“多巴胺”持有者完成任务后开启的 10 秒窗口。仅服务端权威：窗口表只在服务端线程读写，调用方必须传入服务端玩家。
 * 理智混入通过 {@link #adjustedMood} 与 {@link TaskPacing} 读取。
 */
public final class DopamineService {

    private static final Map<UUID, Long> WINDOW_END = new HashMap<>();

    private DopamineService() {
    }

    public static void register() {
        TaskComplete.EVENT.register((player, taskType) -> {
            if (!TraitPlayerComponent.KEY.get(player).hasActiveTrait(DopamineTrait.ID)) {
                return;
            }
            WINDOW_END.put(player.getUuid(), player.getWorld().getTime() + DopamineTrait.WINDOW_TICKS);
        });
        // A singleplayer world can be reopened in the same JVM; the ends are absolute times of the stopped world.
        // 单人存档可在同一 JVM 中重新打开；这些结束时间是已停止世界的绝对时间。
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> WINDOW_END.clear());
    }

    /** Whether the post-task window is open right now. 当前是否处于任务完成后的加成窗口内。 */
    public static boolean isActive(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        Long end = WINDOW_END.get(player.getUuid());
        if (end == null) {
            return false;
        }
        if (player.getWorld().getTime() >= end) {
            WINDOW_END.remove(player.getUuid());
            return false;
        }
        return true;
    }

    /**
     * Slows a mood drop inside the window; gains and client-side prediction pass through unchanged.
     * 窗口内减缓理智下降；理智回升与客户端预测保持原样。
     */
    public static float adjustedMood(float currentMood, float proposedMood, PlayerEntity player) {
        if (proposedMood >= currentMood
                || !(player instanceof ServerPlayerEntity)
                || !TraitPlayerComponent.KEY.get(player).hasActiveTrait(DopamineTrait.ID)
                || !isActive(player)) {
            return proposedMood;
        }
        return currentMood - (currentMood - proposedMood) * DopamineTrait.MOOD_DRAIN_MULTIPLIER;
    }

    public static void clearAll() {
        WINDOW_END.clear();
    }

    public static void clear(PlayerEntity player) {
        if (player != null) {
            WINDOW_END.remove(player.getUuid());
        }
    }
}
