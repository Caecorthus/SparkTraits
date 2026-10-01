package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.render.ChameleonRenderAlpha;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Fades a Chameleon's ground shadow together with its body.
 * 让变色龙的地面阴影随身体一起淡化。
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class ChameleonEntityRenderDispatcherMixin {
    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/entity/Entity;FFLnet/minecraft/world/WorldView;F)V"
            ),
            index = 3
    )
    private float sparktraits$fadeChameleonShadow(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            Entity entity,
            float opacity,
            float tickDelta,
            WorldView world,
            float radius
    ) {
        return opacity * ChameleonRenderAlpha.resolve(entity);
    }
}
