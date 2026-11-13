package dev.caecorthus.sparktraits.client.killer;

import dev.caecorthus.sparktraits.net.conscience.BluePoisonMarkersPayload;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Client copy of the hidden blue-poison marker positions this viewer was sent (see BluePoisonMarkersPayload). The
 * blue particle mixins read it instead of the block entity, whose client NBT no longer carries the blue layer. It
 * belongs to one client world: a marker never matches in another world, and the first update for a new world starts
 * from empty. Client thread only; cleared on JOIN and DISCONNECT.
 * 本观察者收到的隐藏蓝毒标记位置的客户端副本（见 BluePoisonMarkersPayload）。蓝色粒子 mixin 读取它，而不再读取方块实体
 * （其客户端 NBT 已不携带蓝毒层）。它只属于一个客户端世界：标记不会在其他世界命中，新世界的第一次更新从空开始。
 * 仅在客户端线程使用；JOIN 与 DISCONNECT 时清空。
 */
public final class BluePoisonMarkerClientStore {
    /** Hard cap so a server cannot grow the store without bound. 硬上限，防止服务端无限撑大存储。 */
    static final int MAX_MARKERS = 16_384;
    private static final LongOpenHashSet MARKERS = new LongOpenHashSet();
    // Identity of the client world the markers belong to; weak, so the title screen does not keep the world alive.
    // 标记所属客户端世界的身份；使用弱引用，避免在标题界面保留该世界。
    private static WeakReference<Object> markerWorld = new WeakReference<>(null);

    private BluePoisonMarkerClientStore() {
    }

    public static void accept(@Nullable World clientWorld, BluePoisonMarkersPayload payload) {
        acceptFor(clientWorld, payload);
    }

    public static boolean contains(@Nullable World clientWorld, BlockPos pos) {
        return containsFor(clientWorld, pos);
    }

    /** World-identity form of {@link #accept}; the world is only compared, never read. 按世界身份处理；只比较世界引用，不读取其内容。 */
    static void acceptFor(@Nullable Object clientWorld, BluePoisonMarkersPayload payload) {
        if (clientWorld == null) {
            clear();
            return;
        }
        if (markerWorld.get() != clientWorld) {
            MARKERS.clear();
            markerWorld = new WeakReference<>(clientWorld);
        }
        if (payload.reset()) {
            MARKERS.clear();
        }
        for (BlockPos pos : payload.removed()) {
            MARKERS.remove(pos.asLong());
        }
        for (BlockPos pos : payload.added()) {
            if (MARKERS.size() >= MAX_MARKERS) {
                break;
            }
            MARKERS.add(pos.asLong());
        }
    }

    static boolean containsFor(@Nullable Object clientWorld, BlockPos pos) {
        return !MARKERS.isEmpty() && clientWorld != null && markerWorld.get() == clientWorld
                && MARKERS.contains(pos.asLong());
    }

    public static void clear() {
        MARKERS.clear();
        markerWorld = new WeakReference<>(null);
    }
}
