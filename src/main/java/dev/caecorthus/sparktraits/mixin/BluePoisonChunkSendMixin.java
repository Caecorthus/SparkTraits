package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.BluePoisonMarkerSync;
import net.minecraft.server.network.ChunkDataSender;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chunk start-tracking seam for the private blue-poison markers. Vanilla sends every chunk a player starts to track
 * (join, movement, respawn, dimension change) through this one method, and Fabric API has no per-player chunk event.
 * The markers of that chunk follow its data packet, and only to a blue-poison viewer: the block entities' own client
 * NBT no longer carries the blue layer. Observes only; the chunk packet itself is untouched.
 * 私有蓝毒标记的“开始追踪区块”接入点。原版把玩家开始追踪的每个区块（加入、移动、重生、切换维度）都经由这一个方法发送，
 * 而 Fabric API 没有按玩家的区块事件。该区块的标记紧随其数据包发送，且只发给蓝毒观察者：方块实体自身的客户端 NBT
 * 已不再携带蓝毒层。仅作观察，不改动区块数据包本身。
 */
@Mixin(ChunkDataSender.class)
public abstract class BluePoisonChunkSendMixin {
    @Inject(method = "sendChunkData", at = @At("TAIL"))
    private static void sparktraits$sendBluePoisonMarkers(
            ServerPlayNetworkHandler handler,
            ServerWorld world,
            WorldChunk chunk,
            CallbackInfo ci
    ) {
        BluePoisonMarkerSync.onChunkSent(handler.player, world, chunk);
    }
}
