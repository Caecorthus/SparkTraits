package dev.caecorthus.sparktraits.client.killer;

import dev.caecorthus.sparktraits.api.SparkTraitsApi;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

/** Client prediction only; the server independently rejects every blocked action. */
public final class LastEscapeInput {
    private LastEscapeInput() {
    }

    public static boolean blocked() {
        MinecraftClient client = MinecraftClient.getInstance();
        return SparkTraitsServerConnection.isConfirmedServer()
                && client.player != null
                && SparkTraitsApi.isKillerInteractionBlocked(client.player);
    }

    public static void suppress(MinecraftClient client) {
        if (!blocked()) {
            return;
        }
        // Do not stopUsingItem: a release can send KnifeStabPayload.
        client.player.clearActiveItem();
        if (client.interactionManager != null) {
            client.interactionManager.cancelBlockBreaking();
        }
        drain(client.options.attackKey);
        drain(client.options.useKey);
    }

    private static void drain(KeyBinding key) {
        while (key.wasPressed()) {
            // Consume buffered clicks rather than replaying them at phase expiry.
        }
        key.setPressed(false);
    }
}
