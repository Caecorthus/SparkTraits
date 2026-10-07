package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.render.ChildishPlayerRendering;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scales a baby-drawn player model back up so it stands as tall as the player's hitbox.
 * 将按幼体绘制的玩家模型整体放大，使其与玩家碰撞箱等高。
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class ChildishPlayerEntityRendererMixin {
    @Inject(
            method = "scale(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;F)V",
            at = @At("TAIL")
    )
    private void sparktraits$fillHitboxWithBabyModel(
            AbstractClientPlayerEntity player,
            MatrixStack matrices,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (((PlayerEntityRenderer) (Object) this).getModel().child) {
            float scale = ChildishPlayerRendering.BABY_MODEL_FILL_SCALE;
            matrices.scale(scale, scale, scale);
        }
    }
}
