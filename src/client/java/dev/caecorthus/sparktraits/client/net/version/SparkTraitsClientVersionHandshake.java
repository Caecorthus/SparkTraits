package dev.caecorthus.sparktraits.client.net.version;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConfirmS2CPacket;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.caecorthus.sparktraits.net.version.SparkTraitsVersionCheck;
import dev.caecorthus.sparktraits.net.version.SparkTraitsVersionHandshake;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;

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
                    confirmSparkTraitsServer(serverVersion, clientVersion, "login");
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

        ClientPlayNetworking.registerGlobalReceiver(SparkTraitsServerConfirmS2CPacket.ID, (payload, context) -> {
            String clientVersion = SparkTraitsVersionHandshake.localVersion();
            if (!SparkTraitsVersionCheck.isCompatible(payload.serverVersion(), clientVersion)) {
                SparkTraits.LOGGER.warn(
                        "Disconnecting from SparkTraits play confirmation mismatch: server={}, client={}.",
                        payload.serverVersion(),
                        clientVersion
                );
                context.responseSender().disconnect(Text.literal(
                        SparkTraitsVersionCheck.mismatchMessage(payload.serverVersion(), clientVersion)
                ));
                return;
            }
            confirmSparkTraitsServer(payload.serverVersion(), clientVersion, "play");
        });
    }

    private static void confirmSparkTraitsServer(String serverVersion, String clientVersion, String stage) {
        SparkTraitsServerConnection.confirmServer();
        SparkTraits.LOGGER.info(
                "Confirmed SparkTraits server through {} channel: server={}, client={}.",
                stage,
                serverVersion,
                clientVersion
        );
    }
}
