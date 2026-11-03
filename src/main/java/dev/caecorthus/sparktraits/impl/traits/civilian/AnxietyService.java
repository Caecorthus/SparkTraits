package dev.caecorthus.sparktraits.impl.traits.civilian;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.event.TaskComplete;
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
            if (!TraitPlayerComponent.KEY.get(player).hasActiveTrait(AnxietyTrait.ID)) {
                return;
            }
            PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
            // 1.0f is the full bar; the component clamps and syncs internally.
            // 1.0f 即满理智；组件内部会自动裁剪并同步。
            mood.setMood(1.0F);
        });
    }
}
