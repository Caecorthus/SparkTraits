package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.client.compat.SparkStrengthCoronerBridge;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.client.audio.DepressionRageLoopController;
import dev.caecorthus.sparktraits.client.hud.DepressionHud;
import dev.caecorthus.sparktraits.client.net.version.SparkTraitsClientVersionHandshake;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandFinalMomentService;
import dev.caecorthus.sparktraits.impl.resource.SparkTraitsParticles;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.client.particle.PoisonParticle;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class SparkTraitsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SparkTraitsServerConnection.reset();
        SparkTraitsClientVersionHandshake.registerClient();
        ParticleFactoryRegistry.getInstance().register(SparkTraitsParticles.BLUE_POISON, PoisonParticle.Factory::new);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SparkTraitsServerConnection.reset());
        registerFinalMomentHighlight();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            DepressionRageLoopController.tick(client);
            if (SparkTraitsServerConnection.isConfirmedServer()) {
                DepressionHud.tick();
            }
        });
    }

    private static void registerFinalMomentHighlight() {
        GetInstinctHighlight.EVENT.register(target -> {
            if (!SparkTraitsServerConnection.isConfirmedServer()) {
                return null;
            }
            PlayerEntity viewer = MinecraftClient.getInstance().player;
            if (viewer == null || !(target instanceof PlayerEntity targetPlayer)) {
                return null;
            }
            TraitWorldComponent traitWorld = TraitWorldComponent.KEY.get(viewer.getWorld());
            if (!traitWorld.isFinalMomentActive()) {
                return null;
            }
            GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
            if (!game.hasAnyRole(targetPlayer) || !GameFunctions.isPlayerPlayingAndAlive(targetPlayer)) {
                return null;
            }

            // Final Moment reveals every living player by faction color until the round ends.
            // 终局时刻会按阵营颜色高亮所有存活玩家，直到本局结束。
            Role role = game.getRole(targetPlayer);
            TraitPlayerComponent targetTraits = TraitPlayerComponent.KEY.get(targetPlayer);
            boolean targetHasConscience = targetTraits.isConscienceInstinctVisible();
            boolean targetHasImpostor = targetTraits.isImpostorInstinctVisible();
            boolean viewerIsEffectiveKiller = EffectiveTraitService.isEffectiveKiller(viewer, game);
            boolean finalMomentLooseEnd = traitWorld.isFinalMomentLooseEnd(targetPlayer.getUuid());

            /*
             * 该事件是 Wathe 默认本能事件链的后备路径。SparkTraits 自己的
             * WatheClientMixin 通常会更早返回，所以这里必须与 Mixin 使用完全
             * 相同的公开标记、内鬼蓝色和验尸官可选桥逻辑，避免不同调用入口
             * 产生不同颜色。
             *
             * This listener is the fallback path for callers that still dispatch
             * Wathe's event directly. Keep it identical to the early mixin path.
             */
            Integer coronerDisguiseColor = (!targetHasConscience && !targetHasImpostor)
                    ? SparkStrengthCoronerBridge.resolveKillerDisguiseInstinctColor(targetPlayer)
                    : null;
            int highlightColor = coronerDisguiseColor != null
                    ? coronerDisguiseColor
                    : LastStandFinalMomentService.finalMomentHighlightColorForViewer(
                            role,
                            targetHasConscience,
                            targetHasImpostor,
                            finalMomentLooseEnd,
                            viewerIsEffectiveKiller
                    );
            return GetInstinctHighlight.HighlightResult.always(
                    highlightColor,
                    GetInstinctHighlight.HighlightResult.PRIORITY_HIGH + 1
            );
        });
    }
}
