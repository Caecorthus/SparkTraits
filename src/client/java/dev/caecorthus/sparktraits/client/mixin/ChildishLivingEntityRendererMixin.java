package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import dev.caecorthus.sparktraits.client.render.ChildishPlayerRendering;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws Childish players with the baby model; held items, armor and the cape follow the model's child flag.
 * 让幼稚玩家使用幼体模型绘制；手持物品、盔甲与披风都跟随模型的 child 标记。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class ChildishLivingEntityRendererMixin {
    // Ordinal 0 sets model.child; ordinal 1 only speeds up baby leg swing and stays vanilla.
    // 第 0 处设置 model.child；第 1 处只加快幼体摆腿速度，保持原版。
    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isBaby()Z",
                    ordinal = 0
            )
    )
    private boolean sparktraits$drawChildishPlayerAsBaby(boolean baby, @Local(argsOnly = true) LivingEntity entity) {
        return baby || ChildishPlayerRendering.rendersAsBaby(entity);
    }

    /*
     * Brackets the whole cape layer, so capes drawn by cape mods that bypass vanilla's cape code also hang on the baby
     * body. Injects before and after the layer call box no arguments, unlike a wrapper, which built an argument array
     * for each of the ~10 layers of every entity. The pop reuses the push decision shared from before the call, so the
     * matrix stack stays balanced whatever the layer does to the model.
     * 包住整个披风图层，使绕过原版披风代码的披风模组所绘制的披风也挂在幼体身体上。图层调用前后的注入不装箱任何参数，
     * 不像包装器那样为每个实体约 10 个图层逐一构建参数数组。出栈沿用调用前共享的入栈判定，无论图层如何改动模型，矩阵栈都保持平衡。
     */
    @Inject(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V"
            )
    )
    private void sparktraits$pushBabyBodyForCape(
            CallbackInfo ci,
            @Local(argsOnly = true) MatrixStack matrices,
            @Local FeatureRenderer<?, ?> feature,
            @Share("babyCapePushed") LocalBooleanRef pushed
    ) {
        boolean hangsCape = ChildishPlayerRendering.hangsCapeOnBabyBody(((LivingEntityRenderer<?, ?>) (Object) this).getModel(), feature);
        pushed.set(hangsCape);
        if (hangsCape) {
            matrices.push();
            ChildishPlayerRendering.applyBabyBodyTransform(matrices);
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void sparktraits$popBabyBodyForCape(
            CallbackInfo ci,
            @Local(argsOnly = true) MatrixStack matrices,
            @Share("babyCapePushed") LocalBooleanRef pushed
    ) {
        if (pushed.get()) {
            pushed.set(false);
            matrices.pop();
        }
    }
}
