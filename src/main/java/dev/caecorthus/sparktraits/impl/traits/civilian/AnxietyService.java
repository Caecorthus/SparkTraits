package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.TaskComplete;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;

/**
 * 焦虑症: completing any task refills the whole mood bar.
 * 焦虑症：完成任意任务直接把理智补满。
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
            // Fill to the holder's own ceiling: 沉稳 raises it above 1.0, so a literal 1.0F would lower a fuller bar.
            // The component clamps and syncs internally.
            // 补到持有者自己的上限：沉稳会把上限抬到 1.0 以上，写死 1.0F 反而会扣掉多出来的理智。
            // 组件内部会自动裁剪并同步。
            Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
            float fullMood = GlobalTraitService.moodMax(
                    EffectiveTraitService.effectiveMoodType(player, role),
                    traits.getActiveTraitIds()
            );
            PlayerMoodComponent.KEY.get(player).setMood(fullMood);
        });
    }
}
