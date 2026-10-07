package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.render.ChildishPlayerRendering;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hangs the cape on the baby body; vanilla never draws player capes on a child model.
 * 让披风挂在幼体身体上；原版从不在幼体模型上绘制玩家披风。
 */
@Mixin(CapeFeatureRenderer.class)
public abstract class ChildishCapeFeatureRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = At.Shift.AFTER
            )
    )
    private void sparktraits$attachCapeToBabyBody(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            AbstractClientPlayerEntity player,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (((CapeFeatureRenderer) (Object) this).getContextModel().child) {
            ChildishPlayerRendering.applyBabyBodyTransform(matrices);
        }
    }
}
