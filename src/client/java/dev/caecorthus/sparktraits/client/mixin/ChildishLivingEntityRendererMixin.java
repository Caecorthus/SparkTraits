package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparktraits.client.render.ChildishPlayerRendering;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws Childish players with the baby model; held items, armor and the cape follow the model's child flag.
 * 让幼稚玩家使用幼体模型绘制；手持物品、盔甲与披风都跟随模型的 child 标记。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class ChildishLivingEntityRendererMixin {
    // Ordinal 0 sets model.child; ordinal 1 only speeds up baby leg swing and stays vanilla.
    // 第 0 处设置 model.child；第 1 处只加快幼体摆腿速度，保持原版。
    @WrapOperation(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isBaby()Z",
                    ordinal = 0
            )
    )
    private boolean sparktraits$drawChildishPlayerAsBaby(LivingEntity entity, Operation<Boolean> original) {
        return original.call(entity) || ChildishPlayerRendering.rendersAsBaby(entity);
    }

    // Wraps the whole cape layer, so capes drawn by cape mods that bypass vanilla's cape code also hang on the baby body.
    // 包住整个披风图层，使绕过原版披风代码的披风模组所绘制的披风也挂在幼体身体上。
    @WrapOperation(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V"
            )
    )
    private void sparktraits$hangCapeOnBabyBody(
            FeatureRenderer<?, ?> feature,
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
        if (!ChildishPlayerRendering.hangsCapeOnBabyBody(((LivingEntityRenderer<?, ?>) (Object) this).getModel(), feature)) {
            original.call(feature, matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
            return;
        }
        matrices.push();
        ChildishPlayerRendering.applyBabyBodyTransform(matrices);
        original.call(feature, matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
        matrices.pop();
    }
}
