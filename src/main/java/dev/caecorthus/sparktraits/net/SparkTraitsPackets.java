package dev.caecorthus.sparktraits.net;

import dev.caecorthus.sparktraits.SparkTraits;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class SparkTraitsPackets {
    private static boolean registered;

    private SparkTraitsPackets() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        PayloadTypeRegistry.playS2C().register(
                SparkTraitsServerConfirmS2CPacket.ID,
                SparkTraitsServerConfirmS2CPacket.CODEC
        );
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (!ServerPlayNetworking.canSend(player, SparkTraitsServerConfirmS2CPacket.ID)) {
                SparkTraits.LOGGER.warn(
                        "SparkTraits play confirmation channel {} is not available for {}.",
                        SparkTraitsServerConfirmS2CPacket.PAYLOAD_ID,
                        player.getGameProfile().getName()
                );
                return;
            }

            // Reconfirm the SparkTraits server after play starts, because proxies can drop login queries.
            // 进入 play 阶段后再次确认 SparkTraits 服务端，因为代理可能吞掉登录查询。
            sender.sendPacket(new SparkTraitsServerConfirmS2CPacket(SparkTraitsVersionHandshake.localVersion()));
        });
    }
}
