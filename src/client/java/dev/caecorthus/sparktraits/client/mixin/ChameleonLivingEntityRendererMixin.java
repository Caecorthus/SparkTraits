package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.caecorthus.sparktraits.client.render.ChameleonRenderAlpha;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fades a still Chameleon's body through the vanilla translucent entity path and hides its layers once half faded.
 * 通过原版半透明实体渲染路径淡化静止的变色龙，并在半透明后隐藏其附加图层。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class ChameleonLivingEntityRendererMixin {
    private static final String RENDER =
            "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";

    @SuppressWarnings({"rawtypes", "unchecked"})
    @WrapOperation(
            method = RENDER,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/entity/LivingEntity;ZZZ)Lnet/minecraft/client/render/RenderLayer;"
            )
    )
    private RenderLayer sparktraits$chameleonTranslucentLayer(
            LivingEntityRenderer renderer,
            LivingEntity entity,
            boolean showBody,
            boolean translucent,
            boolean showOutline,
            Operation<RenderLayer> original
    ) {
        // Only fade bodies that would otherwise render normally; other invisibility keeps its own layer.
        // 只淡化原本正常显示的身体；其他隐身效果保持各自的渲染层。
        if (showBody && ChameleonRenderAlpha.resolve(entity) < ChameleonRenderAlpha.OPAQUE) {
            return original.call(renderer, entity, false, true, showOutline);
        }
        return original.call(renderer, entity, showBody, translucent, showOutline);
    }

    @SuppressWarnings("rawtypes")
    @WrapOperation(
            method = RENDER,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
            )
    )
    private void sparktraits$chameleonModelAlpha(
            EntityModel model,
            MatrixStack matrices,
            VertexConsumer vertices,
            int light,
            int overlay,
            int color,
            Operation<Void> original,
            @Local(argsOnly = true) LivingEntity entity
    ) {
        float alpha = ChameleonRenderAlpha.resolve(entity);
        int fadedColor = alpha < ChameleonRenderAlpha.OPAQUE ? ChameleonRenderAlpha.applyAlpha(color, alpha) : color;
        original.call(model, matrices, vertices, light, overlay, fadedColor);
    }

    @SuppressWarnings("rawtypes")
    @WrapOperation(
            method = RENDER,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V"
            )
    )
    private void sparktraits$hideChameleonFeatures(
            FeatureRenderer feature,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            Entity entity,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float animationProgress,
            float headYaw,
            float headPitch,
            Operation<Void> original
    ) {
        // Capes and other layers ignore vertex alpha, so they are dropped for the later half of the fade.
        // 披风等图层不支持顶点透明度，因此在淡化后半段直接隐藏。
        if (ChameleonRenderAlpha.hidesLayers(ChameleonRenderAlpha.resolve(entity))) {
            return;
        }
        original.call(feature, matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta,
                animationProgress, headYaw, headPitch);
    }
}
