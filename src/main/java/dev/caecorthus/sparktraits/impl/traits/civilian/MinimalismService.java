package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.event.ResetPlayer;
import dev.doctor4t.wathe.api.event.TaskComplete;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 极简主义: when the holder goes 90 seconds without completing a task, one task is refreshed for them.
 * The cap itself is enforced by {@code TaskPacing.atTaskCap}; this class only tracks the idle clock.
 * 极简主义：持有者连续 90 秒没有完成任务时，为其补刷 1 个任务。
 * 任务上限仍由 {@code TaskPacing.atTaskCap} 负责，这里只负责计时。
 */
public final class MinimalismService {

    /** Last world time at which each holder completed a task (or received a replacement one). 最近一次完成任务的世界时间。 */
    private static final Map<UUID, Long> LAST_COMPLETION = new HashMap<>();

    private MinimalismService() {
    }

    public static void register() {
        TaskComplete.EVENT.register((player, taskType) -> {
            if (TraitPlayerComponent.KEY.get(player).hasActiveTrait(MinimalismTrait.ID)) {
                LAST_COMPLETION.put(player.getUuid(), player.getWorld().getTime());
            }
        });
        ResetPlayer.EVENT.register(MinimalismService::clear);
        // A singleplayer world can be reopened in the same JVM; the stored times belong to the stopped world.
        // 单人存档可在同一 JVM 中重新打开；保存的时间属于已停止的世界。
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearAll());
    }

    /** Whether the holder has gone {@link MinimalismTrait#IDLE_REFRESH_TICKS} without a task. 是否已空转满 90 秒。 */
    public static boolean idleLongEnough(long now, long lastCompletion) {
        return now - lastCompletion >= MinimalismTrait.IDLE_REFRESH_TICKS;
    }

    /**
     * World time of the last completed task. {@code now} seeds the clock for a fresh holder so the first replacement
     * lands 90 seconds after they receive the trait, not immediately.
     * 最近一次完成任务的世界时间。新持有者用 {@code now} 作为起点，使第一次补发落在拿到词条 90 秒之后。
     */
    public static long lastCompletion(PlayerEntity player, long now) {
        return LAST_COMPLETION.computeIfAbsent(player.getUuid(), uuid -> now);
    }

    public static void mark(PlayerEntity player, long now) {
        LAST_COMPLETION.put(player.getUuid(), now);
    }

    public static void clear(PlayerEntity player) {
        if (player != null) {
            LAST_COMPLETION.remove(player.getUuid());
        }
    }

    public static void clearAll() {
        LAST_COMPLETION.clear();
    }
}
