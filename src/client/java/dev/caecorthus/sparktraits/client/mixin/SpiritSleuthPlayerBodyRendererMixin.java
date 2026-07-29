package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparktraits.api.SparkTraitsApi;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.caecorthus.sparktraits.impl.traits.global.SpiritSleuthTrait;
import dev.caecorthus.sparktraits.impl.traits.global.SpiritSleuthVisibilityRules;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(PlayerBodyEntityRenderer.class)
public abstract class SpiritSleuthPlayerBodyRendererMixin {
    @WrapOperation(
            method = {
                    "renderBody(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
                    "render(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/model/BipedEntityModel;Lnet/minecraft/client/render/RenderLayer;FF)V"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/entity/PlayerBodyEntity;isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private boolean sparktraits$revealSpectatorBody(
            PlayerBodyEntity body,
            PlayerEntity viewer,
            Operation<Boolean> original
    ) {
        boolean invisibleToViewer = original.call(body, viewer);
        if (!invisibleToViewer
                || viewer == null
                || !SparkTraitsServerConnection.isConfirmedServer()) {
            return invisibleToViewer;
        }

        UUID playerUuid = body.getPlayerUuid();
        PlayerEntity owner = playerUuid == null ? null : body.getWorld().getPlayerByUuid(playerUuid);
        if (owner == null) {
            return invisibleToViewer;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(body.getWorld());
        boolean targetIsDeadParticipant = game.isRunning()
                && game.hasAnyRole(playerUuid)
                && game.isPlayerDead(playerUuid);
        boolean reveal = SpiritSleuthVisibilityRules.shouldRevealSpectatorHead(
                GlobalTraitService.hasTrait(viewer, SpiritSleuthTrait.ID),
                viewer.isSpectator(),
                owner.isSpectator(),
                targetIsDeadParticipant,
                SparkTraitsApi.isFakeDeathBody(body)
        );
        return SpiritSleuthVisibilityRules.resolveInvisibleToViewer(
                invisibleToViewer,
                reveal
        );
    }
}
