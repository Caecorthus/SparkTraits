package dev.caecorthus.sparktraits.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Save confirmed cleanup performed after disconnect. / 保存断线后确认的清理结果。 */
@Mixin(PlayerManager.class)
public interface LastEscapePlayerManagerAccessor {
    @Invoker("savePlayerData")
    void sparktraits$saveLastEscapePlayerData(ServerPlayerEntity player);
}
