package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.net.conscience.BluePoisonMarkersPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Delivers the hidden blue-poison markers (blue platter poison, blue bed scorpion) to the players that may see them
 * and to nobody else. The markers used to ride in the block entities' client NBT, which every chunk-tracking client
 * receives; now that NBT says nothing about the blue layer and this class sends {@link BluePoisonMarkersPayload} to
 * "blue poison viewers" only (TraitSyncVisibility.Recipient#bluePoisonViewer: Conscience Poisoner, Toxicologist,
 * spectator or creative). A player outside that set never gets a packet from here, not even an empty one.
 * <p>A viewer is entered and left by {@link #onRecipientViewChanged}, driven by TraitPlayerComponent's recipient
 * poll; every send additionally re-checks the live predicate, so the poll's one-tick lag can only delay a marker,
 * never leak one. Markers reach an entered viewer on a marker change, when a chunk is sent to them, and by a scan of
 * their tracked chunks when they become a viewer. Server thread only.
 * 把隐藏蓝毒标记（餐盘蓝毒、床上蓝蝎子）只发送给有权查看的玩家。这些标记原本随方块实体的客户端 NBT 下发，所有追踪该区块的
 * 客户端都能收到；现在该 NBT 不再包含任何蓝毒层信息，由本类仅向“蓝毒观察者”（TraitSyncVisibility.Recipient#bluePoisonViewer：
 * 善良毒师、毒理学家、旁观或创造模式）发送 {@link BluePoisonMarkersPayload}。不在该集合中的玩家不会从这里收到任何数据包，空包也没有。
 * <p>观察者的进入与退出由 {@link #onRecipientViewChanged} 处理，由 TraitPlayerComponent 的接收者轮询驱动；每次发送还会再次检查
 * 实时判定，因此轮询的一 tick 延迟只会推迟标记，不会泄露标记。已进入的观察者会在标记变化时、区块发送给他们时收到标记，
 * 并在成为观察者时通过扫描其追踪的区块补齐。仅在服务端线程使用。
 */
public final class BluePoisonMarkerSync {
    /** Stale markers (a block entity replaced by the train reset, a broken block) are withdrawn within this many ticks. */
    private static final int VALIDATE_INTERVAL_TICKS = 10;
    private static final Map<UUID, ViewerState> VIEWERS = new HashMap<>();
    private static boolean registered;

    private BluePoisonMarkerSync() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PayloadTypeRegistry.playS2C().register(BluePoisonMarkersPayload.ID, BluePoisonMarkersPayload.CODEC);
        // A new connection starts with an empty client store; the recipient poll enters the viewer again.
        // 新连接的客户端存储为空；接收者轮询会重新让其进入观察者集合。
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> VIEWERS.remove(handler.player.getUuid()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> VIEWERS.remove(handler.player.getUuid()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> VIEWERS.clear());
        ServerTickEvents.END_WORLD_TICK.register(BluePoisonMarkerSync::withdrawStaleMarkers);
    }

    /** Whether the block entity carries a blue marker right now. 该方块实体当前是否带有蓝毒标记。 */
    public static boolean isMarked(@Nullable BlockEntity blockEntity) {
        return (blockEntity instanceof ConsciencePoisonedPlate plate && plate.sparktraits$getConsciencePoisoner() != null)
                || (blockEntity instanceof ConscienceScorpionBed bed && bed.sparktraits$hasConscienceScorpion());
    }

    /**
     * The viewer set follows the recipient view: entering scans the chunks the player tracks, leaving clears their
     * client store. Nothing is sent when there is nothing to show or to clear.
     * 观察者集合跟随接收者视角：进入时扫描该玩家追踪的区块，退出时清空其客户端存储。没有可显示或可清除的内容时不发送任何数据包。
     */
    public static void onRecipientViewChanged(ServerPlayerEntity player, boolean bluePoisonViewer) {
        ViewerState state = VIEWERS.get(player.getUuid());
        if (bluePoisonViewer == (state != null)) {
            return;
        }
        if (!bluePoisonViewer) {
            VIEWERS.remove(player.getUuid());
            // They held these markers as a viewer a tick ago; the reset only takes them back.
            // 这些标记是其一 tick 前作为观察者持有的；重置只是把它们收回。
            if (!state.sent.isEmpty()) {
                send(player, true, List.of(), List.of());
            }
            return;
        }
        ServerWorld world = player.getServerWorld();
        state = new ViewerState(world.getRegistryKey());
        VIEWERS.put(player.getUuid(), state);
        List<BlockPos> marked = new ArrayList<>();
        player.getChunkFilter().forEach(chunkPos -> {
            WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z);
            if (chunk != null) {
                collectMarked(chunk, marked);
            }
        });
        if (!marked.isEmpty()) {
            state.sent.addAll(marked);
            send(player, true, marked, List.of());
        }
    }

    /**
     * Called right after vanilla sent {@code chunk} to {@code player} (first tracking, respawn, dimension change).
     * 在原版把 {@code chunk} 发送给 {@code player} 之后立即调用（首次追踪、重生、切换维度）。
     */
    public static void onChunkSent(ServerPlayerEntity player, ServerWorld world, WorldChunk chunk) {
        ViewerState state = VIEWERS.get(player.getUuid());
        if (state == null || chunk.getBlockEntities().isEmpty()) {
            return;
        }
        List<BlockPos> marked = new ArrayList<>();
        collectMarked(chunk, marked);
        if (marked.isEmpty() || !TraitPlayerComponent.isBluePoisonViewer(player)) {
            return;
        }
        state.enter(world.getRegistryKey());
        state.sent.addAll(marked);
        send(player, false, marked, List.of());
    }

    /**
     * A blue marker was set or cleared on the block entity at {@code pos}. Replaces the public block-entity update:
     * only current viewers that track the chunk learn of a new marker, and only viewers that were told about a marker
     * learn of its removal.
     * {@code pos} 处方块实体的蓝毒标记被设置或清除。它取代了公开的方块实体更新：只有正在追踪该区块的当前观察者会得知新标记，
     * 也只有被告知过该标记的观察者会得知其移除。
     */
    public static void onMarkerChanged(ServerWorld world, BlockPos pos, boolean present) {
        if (VIEWERS.isEmpty()) {
            return;
        }
        BlockPos key = pos.toImmutable();
        int chunkX = key.getX() >> 4;
        int chunkZ = key.getZ() >> 4;
        for (Map.Entry<UUID, ViewerState> entry : VIEWERS.entrySet()) {
            ServerPlayerEntity viewer = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (viewer == null || viewer.getServerWorld() != world || !TraitPlayerComponent.isBluePoisonViewer(viewer)) {
                continue;
            }
            ViewerState state = entry.getValue();
            state.enter(world.getRegistryKey());
            if (present) {
                if (viewer.getChunkFilter().isWithinDistance(chunkX, chunkZ)) {
                    state.sent.add(key);
                    send(viewer, false, List.of(key), List.of());
                }
            } else if (state.sent.remove(key)) {
                send(viewer, false, List.of(), List.of(key));
            }
        }
    }

    /**
     * Safety net for markers that vanish without a setter call: the train reset replaces block entities, a block can
     * be broken, a chunk can unload. Also drops offline viewers and forgets what was sent for a world the viewer left
     * (their client store is per world and already empty).
     * 兜底处理未经设置方法而消失的标记：列车重置会替换方块实体，方块可能被破坏，区块可能卸载。同时移除已离线的观察者，
     * 并在观察者离开某个世界后忘掉为该世界发送过的内容（其客户端存储按世界保存，此时已为空）。
     */
    private static void withdrawStaleMarkers(ServerWorld world) {
        if (VIEWERS.isEmpty() || world.getTime() % VALIDATE_INTERVAL_TICKS != 0) {
            return;
        }
        Iterator<Map.Entry<UUID, ViewerState>> viewers = VIEWERS.entrySet().iterator();
        while (viewers.hasNext()) {
            Map.Entry<UUID, ViewerState> entry = viewers.next();
            ServerPlayerEntity viewer = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (viewer == null) {
                viewers.remove();
                continue;
            }
            if (viewer.getServerWorld() != world) {
                continue;
            }
            ViewerState state = entry.getValue();
            state.enter(world.getRegistryKey());
            if (state.sent.isEmpty()) {
                continue;
            }
            List<BlockPos> stale = new ArrayList<>();
            for (BlockPos pos : state.sent) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
                if (chunk == null || !isMarked(chunk.getBlockEntities().get(pos))) {
                    stale.add(pos);
                }
            }
            if (stale.isEmpty()) {
                continue;
            }
            stale.forEach(state.sent::remove);
            if (TraitPlayerComponent.isBluePoisonViewer(viewer)) {
                send(viewer, false, List.of(), stale);
            }
        }
    }

    private static void collectMarked(WorldChunk chunk, List<BlockPos> marked) {
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            if (isMarked(entry.getValue())) {
                marked.add(entry.getKey().toImmutable());
            }
        }
    }

    private static void send(ServerPlayerEntity player, boolean reset, List<BlockPos> added, List<BlockPos> removed) {
        if (!ServerPlayNetworking.canSend(player, BluePoisonMarkersPayload.ID)) {
            return;
        }
        for (BluePoisonMarkersPayload packet : BluePoisonMarkersPayload.split(reset, added, removed)) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    /** What one viewer's client currently holds, for the world it was sent in. 某个观察者的客户端当前持有的标记及其所属世界。 */
    private static final class ViewerState {
        private RegistryKey<World> world;
        private final Set<BlockPos> sent = new HashSet<>();

        private ViewerState(RegistryKey<World> world) {
            this.world = world;
        }

        /** The client drops its store when its world changes, so the server forgets with it. 客户端切换世界时会清空存储，服务端随之忘记。 */
        private void enter(RegistryKey<World> current) {
            if (world != current) {
                world = current;
                sent.clear();
            }
        }
    }
}
