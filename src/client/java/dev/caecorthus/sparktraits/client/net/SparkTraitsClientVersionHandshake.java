package dev.caecorthus.sparktraits.client.net;

import dev.caecorthus.sparktraits.net.SparkTraitsVersionHandshake;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;

import java.util.concurrent.CompletableFuture;

public final class SparkTraitsClientVersionHandshake {
    private static boolean clientRegistered;

    private SparkTraitsClientVersionHandshake() {
    }

    public static synchronized void registerClient() {
        if (clientRegistered) {
            return;
        }
        clientRegistered = true;

        ClientLoginNetworking.registerGlobalReceiver(SparkTraitsVersionHandshake.VERSION_CHECK_ID,
                (client, handler, buf, callbacks) -> {
                    SparkTraitsVersionHandshake.readVersion(buf);
                    return CompletableFuture.completedFuture(
                            SparkTraitsVersionHandshake.writeVersion(SparkTraitsVersionHandshake.localVersion()));
                });
    }
}
