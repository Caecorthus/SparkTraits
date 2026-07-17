package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.api.SparkTraitsApi;
import dev.caecorthus.sparktraits.client.render.PigPlayerRenderer;
import dev.caecorthus.sparktraits.impl.traits.global.pig.PigTraitService;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces only Pig trait player rendering with the custom pig-body renderer.
 * 仅替换猪天赋玩家的渲染为自定义猪身体渲染。
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PigPlayerRendererMixin {
    @Shadow
    public abstract Identifier getTexture(AbstractClientPlayerEntity player);

    @Inject(
            method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sparktraits$renderPigPlayer(
            AbstractClientPlayerEntity player,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        AbstractClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        // SparkWitch owns Wraith projection; this trait-owned renderer only yields to its public state query.
        // SparkWitch 负责冤魂投影；此天赋渲染器仅通过公开状态查询让出渲染权。
        boolean wraithViewer = SparkTraitsApi.isWraithActive(viewer);
        boolean spectatorReveal = viewer != null
                && viewer.isSpectator()
                && SparkTraitsApi.isWraithActive(player);
        if (SparkTraitsServerConnection.isConfirmedServer()
                && !wraithViewer
                && (!player.isInvisible() || spectatorReveal)
                && PigTraitService.isPig(player)) {
            Identifier headTexture = getTexture(player);
            PigPlayerRenderer.render(player, yaw, tickDelta, matrices, vertexConsumers, light, headTexture);
            ci.cancel();
        }
    }
}
