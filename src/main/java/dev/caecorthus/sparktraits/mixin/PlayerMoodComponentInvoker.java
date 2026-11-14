package dev.caecorthus.sparktraits.mixin;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes Wathe's private task generator so 极简主义 can hand out a replacement task.
 * Wathe 的任务生成器是 private，这里开一个调用口，供“极简主义”补发任务用。
 */
@Mixin(PlayerMoodComponent.class)
public interface PlayerMoodComponentInvoker {

    @Invoker("generateTask")
    PlayerMoodComponent.TrainTask sparktraits$generateTask();
}
