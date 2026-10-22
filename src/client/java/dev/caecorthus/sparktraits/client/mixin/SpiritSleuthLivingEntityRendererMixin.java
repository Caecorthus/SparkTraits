package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.global.SpiritSleuthTrait;
import dev.caecorthus.sparktraits.impl.traits.global.SpiritSleuthVisibilityRules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Extends only the vanilla render visibility decision for spectator players.
 * 仅扩展原版对旁观玩家的渲染可见性判断。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class SpiritSleuthLivingEntityRendererMixin {
    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private boolean sparktraits$revealSpectatorHead(
            boolean originallyInvisible,
            @Local(argsOnly = true) LivingEntity target
    ) {
        if (!originallyInvisible) {
            return false;
        }

        // Vanilla asks on behalf of the local player (MinecraftClient.player), the same viewer read here.
        // 原版以本地玩家（MinecraftClient.player）的身份询问，这里读取的是同一观察者。
        PlayerEntity viewer = MinecraftClient.getInstance().player;
        boolean viewerHasActiveTrait = viewer != null
                && TraitPlayerComponent.KEY.get(viewer).hasActiveTrait(SpiritSleuthTrait.ID);
        boolean targetIsSpectatorPlayer = target instanceof PlayerEntity targetPlayer
                && targetPlayer.isSpectator();
        return SpiritSleuthVisibilityRules.resolveInvisibleToViewer(
                originallyInvisible,
                viewerHasActiveTrait,
                targetIsSpectatorPlayer
        );
    }
}
