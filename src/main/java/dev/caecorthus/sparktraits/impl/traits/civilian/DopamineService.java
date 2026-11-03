package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.event.TaskComplete;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the ten-second window opened whenever a 多巴胺 holder completes a task.
 * <p>Server-side authority only; the mood mixin reads {@link #isActive(PlayerEntity)}.
 * 追踪“多巴胺”持有者完成任务后开启的 10 秒窗口。仅服务端权威，理智混入读取 {@link #isActive(PlayerEntity)}。
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

    public static void clear(PlayerEntity player) {
        if (player != null) {
            WINDOW_END.remove(player.getUuid());
        }
    }
}
