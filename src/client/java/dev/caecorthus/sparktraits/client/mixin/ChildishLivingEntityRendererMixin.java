package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparktraits.client.render.ChildishPlayerRendering;
import net.minecraft.client.render.entity.LivingEntityRenderer;
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
}
