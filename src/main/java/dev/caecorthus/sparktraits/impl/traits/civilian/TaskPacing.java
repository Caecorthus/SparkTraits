package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Task pacing for the civilian task traits: 焦虑症 speeds up Wathe's next-task countdown, 极简主义 caps how many
 * tasks can be open at once. 多巴胺 no longer touches the countdown — it only slows mood drain and grants Speed.
 * 平民任务类词条的任务节奏：焦虑症加快 Wathe 的下一个任务倒计时，极简主义限制同时存在的任务数量。
 * 多巴胺不再影响倒计时——它只减缓理智流失并给予速度。
 */
public final class TaskPacing {

    private TaskPacing() {
    }

    /**
     * Extra ticks to take off the next-task countdown this tick, on top of the one Wathe already takes.
     * 本 tick 在 Wathe 自身减去的 1 tick 之外，额外从“下一个任务倒计时”里扣除的 tick 数。
     */
    public static int extraTimerTicks(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity)) {
            return 0;
        }
        if (!TraitPlayerComponent.KEY.get(player).hasActiveTrait(AnxietyTrait.ID)) {
            return 0;
        }
        return extraTimerTicks(AnxietyTrait.REFRESH_MULTIPLIER, player.getWorld().getTime());
    }

    /**
     * Spreads the fractional part of the speed-up evenly over world time, so no per-player remainder is stored:
     * x1.8 gives 8 extra ticks in every 10.
     * 把加速倍率的小数部分按世界时间均匀摊开，不需要为每个玩家保存余数：1.8 倍时每 10 tick 额外扣 8 tick。
     */
    static int extraTimerTicks(float multiplier, long worldTime) {
        double extraPerTick = multiplier - 1.0;
        if (extraPerTick <= 0.0) {
            return 0;
        }
        return (int) (Math.floor((worldTime + 1) * extraPerTick) - Math.floor(worldTime * extraPerTick));
    }

    /**
     * Whether a 极简主义 holder already has as many open tasks as the trait allows.
     * 极简主义持有者当前的任务数是否已达到词条允许的上限。
     */
    public static boolean atTaskCap(PlayerEntity player, int activeTasks) {
        return activeTasks >= MinimalismTrait.TASK_CAP
                && player != null
                && TraitPlayerComponent.KEY.get(player).hasActiveTrait(MinimalismTrait.ID);
    }
}
