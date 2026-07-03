package dev.caecorthus.sparktraits.client.net;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.net.SparkTraitsServerConnection;
import dev.caecorthus.sparktraits.net.SparkTraitsVersionHandshake;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;

import java.util.concurrent.CompletableFuture;

public final class SparkTraitsClientVersionHandshake {
    private static boolean clientRegistered;

    private SparkTraitsClientVersionHandshake() {
    }

    public static synchronized void registerClient() {
        if (clientRegistered) {
            SparkTraits.LOGGER.info("SparkTraits client login version receiver is already registered.");
            return;
        }
        clientRegistered = true;

        boolean registered = ClientLoginNetworking.registerGlobalReceiver(SparkTraitsVersionHandshake.VERSION_CHECK_ID,
                (client, handler, buf, callbacks) -> {
                    String serverVersion = SparkTraitsVersionHandshake.readVersion(buf);
                    String clientVersion = SparkTraitsVersionHandshake.localVersion();
                    SparkTraitsServerConnection.confirmServer();
                    SparkTraits.LOGGER.info(
                            "Answering SparkTraits login version query: server={}, client={}.",
                            serverVersion,
                            clientVersion
                    );
                    return CompletableFuture.completedFuture(
                            SparkTraitsVersionHandshake.writeVersion(clientVersion));
                });
        if (registered) {
            // The server treats an unregistered receiver as a missing client-side mod.
            // 服务端会把未注册的接收器判定为客户端缺少该模组。
            SparkTraits.LOGGER.info(
                    "Registered SparkTraits client login version receiver on channel {}.",
                    SparkTraitsVersionHandshake.VERSION_CHECK_ID
            );
        } else {
            SparkTraits.LOGGER.warn(
                    "SparkTraits client login version receiver already existed on channel {}.",
                    SparkTraitsVersionHandshake.VERSION_CHECK_ID
            );
        }
    }
}
