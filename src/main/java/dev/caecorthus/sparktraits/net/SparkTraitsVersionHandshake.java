package dev.caecorthus.sparktraits.net;

import dev.caecorthus.sparktraits.SparkTraits;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SparkTraitsVersionHandshake {
    public static final Identifier VERSION_CHECK_ID = SparkTraits.id("version_check");

    private static boolean serverRegistered;

    private SparkTraitsVersionHandshake() {
    }

    public static synchronized void registerServer() {
        if (serverRegistered) {
            return;
        }
        serverRegistered = true;
        SparkTraits.LOGGER.info(
                "Registering SparkTraits login version check on channel {}.",
                VERSION_CHECK_ID
        );

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            String serverVersion = localVersion();
            SparkTraits.LOGGER.info(
                    "Sending SparkTraits login version query {} to {} with server version {}.",
                    VERSION_CHECK_ID,
                    handler.getConnectionInfo(),
                    serverVersion
            );
            ServerLoginNetworking.registerReceiver(handler, VERSION_CHECK_ID,
                    (minecraftServer, networkHandler, understood, buf, loginSynchronizer, responseSender) ->
                            handleResponse(networkHandler, understood, buf, serverVersion));
            sender.sendPacket(VERSION_CHECK_ID, writeVersion(serverVersion));
        });
    }

    public static String localVersion() {
        return FabricLoader.getInstance()
                .getModContainer(SparkTraits.MOD_ID)
                .orElseThrow()
                .getMetadata()
                .getVersion()
                .getFriendlyString();
    }

    public static PacketByteBuf writeVersion(String version) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(version);
        return buf;
    }

    public static String readVersion(PacketByteBuf buf) {
        return buf.readString();
    }

    private static void handleResponse(
            ServerLoginNetworkHandler handler,
            boolean understood,
            PacketByteBuf buf,
            String serverVersion
    ) {
        // Reject answered mismatches early, but tolerate unanswered login queries behind proxies.
        // 已回应但版本不一致时尽早拒绝；代理后的未回应登录查询则允许继续。
        if (!understood) {
            SparkTraits.LOGGER.warn(
                    "SparkTraits login version query {} was not understood by {}. Expected client version {}.",
                    VERSION_CHECK_ID,
                    handler.getConnectionInfo(),
                    serverVersion
            );
            if (SparkTraitsVersionCheck.shouldRejectUnansweredLoginQuery()) {
                handler.disconnect(Text.literal(SparkTraitsVersionCheck.missingClientMessage(serverVersion)));
            } else {
                SparkTraits.LOGGER.warn(
                        "Allowing {} to continue because proxies can drop Fabric login-query responses.",
                        handler.getConnectionInfo()
                );
            }
            return;
        }

        String clientVersion = readVersion(buf);
        SparkTraits.LOGGER.info(
                "Received SparkTraits login version response from {}: client={}, server={}.",
                handler.getConnectionInfo(),
                clientVersion,
                serverVersion
        );
        if (!SparkTraitsVersionCheck.isCompatible(serverVersion, clientVersion)) {
            SparkTraits.LOGGER.warn(
                    "Rejecting SparkTraits version mismatch for {}: client={}, server={}.",
                    handler.getConnectionInfo(),
                    clientVersion,
                    serverVersion
            );
            handler.disconnect(Text.literal(SparkTraitsVersionCheck.mismatchMessage(serverVersion, clientVersion)));
        }
    }
}
