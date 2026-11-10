package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Task pacing for the civilian task traits: 焦虑症 speeds up Wathe's next-task countdown.
 * 平民任务类词条的任务节奏：焦虑症加快 Wathe 的下一个任务倒计时。
 */
public final class TaskPacing {

    private TaskPacing() {
    }

    /**
     * Extra ticks to take off the next-task countdown this tick, on top of the one Wathe already takes.
     * 本 tick 在 Wathe 自身减去的 1 tick 之外，额外从“下一个任务倒计时”里扣除的 tick 数。
     */
    public static int extraTimerTicks(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity)
                || !TraitPlayerComponent.KEY.get(player).hasActiveTrait(AnxietyTrait.ID)) {
            return 0;
        }
        return extraTimerTicks(AnxietyTrait.REFRESH_MULTIPLIER, player.getWorld().getTime());
    }

    /**
     * Spreads the fractional part of the speed-up evenly over world time, so no per-player remainder is stored:
     * x2.5 alternates 1 and 2 extra ticks, x1.3 gives 3 extra ticks in every 10.
     * 把加速倍率的小数部分按世界时间均匀摊开，不需要为每个玩家保存余数：
     * 2.5 倍时额外扣除量在 1 与 2 之间交替，1.3 倍时每 10 tick 额外扣 3 tick。
     */
    static int extraTimerTicks(float multiplier, long worldTime) {
        double extraPerTick = multiplier - 1.0;
        if (extraPerTick <= 0.0) {
            return 0;
        }
        return (int) (Math.floor((worldTime + 1) * extraPerTick) - Math.floor(worldTime * extraPerTick));
    }
}
