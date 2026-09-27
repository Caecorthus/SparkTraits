package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionFakeKillCooldowns;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import org.spongepowered.asm.mixin.Mixin;

/** Every received packet runs as one server task; a Depression fake kill it causes is settled when the task ends.
 *  每个收到的数据包作为一个服务器任务执行；其造成的抑郁假死在任务结束时结算。 */
@Mixin(MinecraftServer.class)
public abstract class DepressionServerTaskMixin {
    @WrapMethod(method = "executeTask(Lnet/minecraft/server/ServerTask;)V")
    private void sparktraits$scopeFakeKillCooldownsToTask(ServerTask task, Operation<Void> original) {
        DepressionFakeKillCooldowns.beginServerTask();
        try {
            original.call(task);
        } finally {
            DepressionFakeKillCooldowns.endServerTask((MinecraftServer) (Object) this);
        }
    }
}
