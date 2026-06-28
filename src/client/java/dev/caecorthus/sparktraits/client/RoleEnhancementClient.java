package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.entity.SparkTraitsEntities;
import dev.caecorthus.sparktraits.network.OpenCriminologistScreenS2CPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public final class RoleEnhancementClient {
    private RoleEnhancementClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenCriminologistScreenS2CPacket.ID,
                (payload, context) -> context.client().execute(() ->
                        MinecraftClient.getInstance().setScreen(new CriminologistScreen(payload.bodyUuid(), payload.candidates()))
                ));
        EntityRendererRegistry.register(SparkTraitsEntities.CAPSULE, FlyingItemEntityRenderer::new);
        RoleEnhancementHud.register();
        AttendantFlashlightRenderer.register();
    }
}
