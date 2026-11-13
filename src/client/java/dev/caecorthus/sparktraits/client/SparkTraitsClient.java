package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.client.compat.SparkStrengthCoronerClientBridge;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.client.audio.DepressionRageLoopController;
import dev.caecorthus.sparktraits.client.audio.FinalMomentMusicController;
import dev.caecorthus.sparktraits.client.hud.DepressionHud;
import dev.caecorthus.sparktraits.client.instinct.InstinctViewerContext;
import dev.caecorthus.sparktraits.client.jester.JesterMomentClientHooks;
import dev.caecorthus.sparktraits.client.killer.NewKillerTraitsClient;
import dev.caecorthus.sparktraits.client.net.version.SparkTraitsClientVersionHandshake;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandFinalMomentService;
import dev.caecorthus.sparktraits.impl.resource.SparkTraitsParticles;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.client.particle.PoisonParticle;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;

public class SparkTraitsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        dev.caecorthus.sparktraits.client.gui.OwnerInventoryClientAdapter.install();
        SparkTraitsServerConnection.reset();
        SparkTraitsClientVersionHandshake.registerClient();
        NewKillerTraitsClient.initialize();
        ParticleFactoryRegistry.getInstance().register(SparkTraitsParticles.BLUE_POISON, PoisonParticle.Factory::new);
        // A new connection starts unconfirmed, even if the previous one never reached its play session.
        // 新连接从未确认状态开始，即使上一个连接没有进入 play 会话。
        ClientLoginConnectionEvents.INIT.register((handler, client) -> SparkTraitsServerConnection.reset());
        /*
         * JOIN also starts a play session after a proxy backend switch, which fires no DISCONNECT: session-scoped
         * state must not survive it. JOIN runs on the client thread. The forced-melee cooldown snapshot is cleared by
         * NewKillerTraitsClient's own JOIN listener.
         * 代理切换后端同样会触发 JOIN 开始新的 play 会话，但不会触发 DISCONNECT：会话级状态不能跨过它保留。
         * JOIN 在客户端线程执行。强制近战冷却快照由 NewKillerTraitsClient 自己的 JOIN 监听清理。
         */
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            FinalMomentMusicController.reset(client);
            SparkTraitsServerConnection.beginPlaySession();
            dev.caecorthus.sparktraits.client.gui.OwnerInventoryClientAdapter.resetNativeCard();
            InstinctViewerContext.clear();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // The volatile flag is safe to drop from the network thread; features gated on it go dark at once.
            // volatile 标记可以在网络线程上直接清除；依赖它的功能随即停用。
            SparkTraitsServerConnection.reset();
            runSessionCleanup(client, handler, () -> {
                FinalMomentMusicController.reset(client);
                dev.caecorthus.sparktraits.client.gui.OwnerInventoryClientAdapter.resetNativeCard();
                InstinctViewerContext.clear();
            });
        });
        registerFinalMomentHighlight();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            DepressionRageLoopController.tick(client);
            FinalMomentMusicController.tick(client);
            JesterMomentClientHooks.tick(client);
            if (SparkTraitsServerConnection.isConfirmedServer()) {
                DepressionHud.tick();
            }
        });
    }

    /**
     * Runs a DISCONNECT cleanup on the client thread. Fabric can fire DISCONNECT from the Netty thread
     * ({@code channelInactive}), where closing render resources throws and plain client caches are raced; listeners
     * keep their registration order because the client task queue is FIFO (and runs inline when already on the
     * client thread). A cleanup that only runs after a newer play session has joined is dropped, since that session's
     * JOIN listeners already reset the same state.
     * 在客户端线程上执行 DISCONNECT 清理。Fabric 可能在 Netty 线程（{@code channelInactive}）触发 DISCONNECT，
     * 在那里关闭渲染资源会抛异常，普通客户端缓存也会产生竞态；客户端任务队列先进先出（已在客户端线程时直接执行），
     * 因此各监听保持注册顺序。若清理执行时更新的 play 会话已经加入，则跳过，因为该会话的 JOIN 监听已重置同样的状态。
     */
    public static void runSessionCleanup(MinecraftClient client, ClientPlayNetworkHandler disconnected, Runnable cleanup) {
        client.execute(() -> {
            if (shouldRunSessionCleanup(client.getNetworkHandler(), disconnected)) {
                cleanup.run();
            }
        });
    }

    /** Pure rule: clean up unless a different, newer play handler is already current. 纯规则：除非已有更新的 play 处理器，否则执行清理。 */
    public static boolean shouldRunSessionCleanup(Object currentHandler, Object disconnectedHandler) {
        return currentHandler == null || currentHandler == disconnectedHandler;
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
            return GetInstinctHighlight.HighlightResult.always(
                    LastStandFinalMomentService.finalMomentHighlightColorForViewer(
                            viewer,
                            targetPlayer,
                            game,
                            traitWorld.isFinalMomentLooseEnd(targetPlayer.getUuid()),
                            SparkStrengthCoronerClientBridge.resolveKillerDisguiseInstinctColor(targetPlayer)
                    ),
                    GetInstinctHighlight.HighlightResult.PRIORITY_HIGH + 1
            );
        });
    }
}
