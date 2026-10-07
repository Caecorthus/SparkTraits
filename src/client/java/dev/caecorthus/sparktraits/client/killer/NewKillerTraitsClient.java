package dev.caecorthus.sparktraits.client.killer;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.client.render.DepressionScreenEffects;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.ForcedMeleeCooldownService;
import dev.caecorthus.sparktraits.net.combat.ForcedMeleeCooldownPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public final class NewKillerTraitsClient {
    private static boolean initialized;

    private NewKillerTraitsClient() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        dev.caecorthus.sparktraits.api.SparkTraitsApi.installLastEscapeVisionProvider(
                DepressionScreenEffects::getLastEscapeComposition);
        ClientPlayNetworking.registerGlobalReceiver(ForcedMeleeCooldownPayload.ID,
                (payload, context) -> ForcedMeleeCooldownService.acceptClientSnapshot(context.player(), payload));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ForcedMeleeCooldownService.clearClient());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ForcedMeleeCooldownService.clearClient());
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null) {
                DepressionScreenEffects.close();
            } else {
                LastEscapeInput.suppress(client);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DepressionScreenEffects.close());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DepressionScreenEffects.close());
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return SparkTraits.id("killer_world_effects");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        MinecraftClient.getInstance().execute(DepressionScreenEffects::close);
                    }
                });
    }

}
