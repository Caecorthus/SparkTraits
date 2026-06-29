package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.PigPlayerRenderer;
import dev.caecorthus.sparktraits.impl.PigTraitService;
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
        if (!player.isInvisible() && PigTraitService.isPig(player)) {
            Identifier headTexture = getTexture(player);
            PigPlayerRenderer.render(player, yaw, tickDelta, matrices, vertexConsumers, light, headTexture);
            ci.cancel();
        }
    }
}
