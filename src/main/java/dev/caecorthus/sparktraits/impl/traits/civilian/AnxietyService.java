package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.TaskComplete;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;

/**
 * 焦虑症: completing any task restores a quarter of the holder's mood ceiling.
 * 焦虑症：完成任意任务恢复持有者理智上限的四分之一。
 */
public final class AnxietyService {

    private AnxietyService() {
    }

    public static void register() {
        TaskComplete.EVENT.register((player, taskType) -> {
            TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
            if (!traits.hasActiveTrait(AnxietyTrait.ID)) {
                return;
            }
            // Restore relative to the holder's own ceiling: 沉稳 raises it above 1.0, so a literal 0.25F would be a
            // smaller share of a fuller bar. The component clamps and syncs internally.
            // 按持有者自己的上限恢复：沉稳会把上限抬到 1.0 以上，写死 0.25F 在满条时占比就偏小。
            // 组件内部会自动裁剪并同步。
            Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
            float fullMood = GlobalTraitService.moodMax(
                    EffectiveTraitService.effectiveMoodType(player, role),
                    traits.getActiveTraitIds()
            );
            PlayerMoodComponent.KEY.get(player).setMood(PlayerMoodComponent.KEY.get(player).getMood()
                    + fullMood * AnxietyTrait.TASK_MOOD_GAIN);
        });
    }
}
