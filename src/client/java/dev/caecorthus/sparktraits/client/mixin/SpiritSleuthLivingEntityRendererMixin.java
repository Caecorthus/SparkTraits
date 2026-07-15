package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparktraits.api.SparkTraitsApi;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.global.SpiritSleuthTrait;
import dev.caecorthus.sparktraits.impl.traits.global.SpiritSleuthVisibilityRules;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Reuses vanilla's translucent spectator rendering for confirmed dead players seen by Spirit Sleuth.
 * 为灵探复用原版半透明旁观者渲染，但仅作用于已确认死亡的玩家。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class SpiritSleuthLivingEntityRendererMixin {
    @WrapOperation(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private boolean sparktraits$revealSpectatorHead(
            LivingEntity target,
            PlayerEntity viewer,
            Operation<Boolean> original
    ) {
        boolean invisibleToViewer = original.call(target, viewer);
        if (!invisibleToViewer
                || !SparkTraitsServerConnection.isConfirmedServer()
                || !(target instanceof PlayerEntity targetPlayer)) {
            return invisibleToViewer;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(targetPlayer.getWorld());
        boolean targetIsDeadParticipant = game.isRunning()
                && game.hasAnyRole(targetPlayer)
                && game.isPlayerDead(targetPlayer.getUuid());

        // A spectator viewer can be an alive temporary spectator; preserve Wathe and NoellesRoles isolation.
        // 观察者自身也可能只是临时进入旁观模式；此时继续遵守 Wathe 与 NoellesRoles 的隔离规则。
        boolean reveal = SpiritSleuthVisibilityRules.shouldRevealSpectatorHead(
                SparkTraitsApi.hasActiveTrait(viewer, SpiritSleuthTrait.ID),
                viewer == null || viewer.isSpectator(),
                targetPlayer.isSpectator(),
                targetIsDeadParticipant,
                TraitPlayerComponent.KEY.get(targetPlayer).isLastStandPending()
        );
        return reveal ? false : invisibleToViewer;
    }
}
