package dev.caecorthus.sparktraits.net.conscience;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Private S2C update of the hidden blue-poison markers (Conscience Poisoner's blue platter poison and blue bed
 * scorpion). It is sent only to recipients that may see hidden blue poison; every other client receives no packet at
 * all, so neither the positions nor the moment a trap is armed or sprung reach them. The block entities' client NBT
 * carries nothing about the blue layer any more.
 * {@code reset} empties the client store before {@code removed} and {@code added} are applied, in that order.
 * 隐藏蓝毒标记（善良毒师的餐盘蓝毒与床上蓝蝎子）的私有 S2C 更新。只发送给可以看到隐藏蓝毒的接收者；其他客户端完全收不到
 * 任何数据包，因此既得不到位置，也得不到陷阱被布置或触发的时机。方块实体发给客户端的 NBT 不再包含任何蓝毒层信息。
 * {@code reset} 会先清空客户端存储，然后依次应用 {@code removed} 与 {@code added}。
 */
public record BluePoisonMarkersPayload(boolean reset, List<BlockPos> added, List<BlockPos> removed) implements CustomPayload {
    /** Upper bound of each position list in one packet; larger updates are split by {@link #split}. 单个数据包中每个位置列表的上限。 */
    public static final int MAX_POSITIONS = 1024;
    public static final Id<BluePoisonMarkersPayload> ID = new Id<>(SparkTraits.id("blue_poison_markers"));
    public static final PacketCodec<RegistryByteBuf, BluePoisonMarkersPayload> CODEC =
            PacketCodec.of(BluePoisonMarkersPayload::write, BluePoisonMarkersPayload::read);

    public BluePoisonMarkersPayload {
        added = List.copyOf(added);
        removed = List.copyOf(removed);
        if (added.size() > MAX_POSITIONS || removed.size() > MAX_POSITIONS) {
            throw new IllegalArgumentException("Blue poison marker update exceeds " + MAX_POSITIONS + " positions");
        }
    }

    /**
     * Packets carrying one update without exceeding the per-packet bound. Only the first one resets, removals travel
     * before additions, and an update with nothing to say (no reset, no positions) yields no packet.
     * 在不超过单包上限的前提下承载一次更新的数据包列表。只有第一个包携带重置，移除先于新增发送；
     * 没有任何内容的更新（不重置且没有位置）不产生数据包。
     */
    public static List<BluePoisonMarkersPayload> split(boolean reset, List<BlockPos> added, List<BlockPos> removed) {
        List<BluePoisonMarkersPayload> packets = new ArrayList<>();
        boolean pendingReset = reset;
        for (int from = 0; from < removed.size(); from += MAX_POSITIONS) {
            packets.add(new BluePoisonMarkersPayload(
                    pendingReset, List.of(), removed.subList(from, Math.min(removed.size(), from + MAX_POSITIONS))));
            pendingReset = false;
        }
        for (int from = 0; from < added.size(); from += MAX_POSITIONS) {
            packets.add(new BluePoisonMarkersPayload(
                    pendingReset, added.subList(from, Math.min(added.size(), from + MAX_POSITIONS)), List.of()));
            pendingReset = false;
        }
        if (pendingReset) {
            packets.add(new BluePoisonMarkersPayload(true, List.of(), List.of()));
        }
        return packets;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    void write(PacketByteBuf buf) {
        buf.writeBoolean(reset);
        writePositions(buf, added);
        writePositions(buf, removed);
    }

    static BluePoisonMarkersPayload read(PacketByteBuf buf) {
        boolean reset = buf.readBoolean();
        List<BlockPos> added = readPositions(buf);
        List<BlockPos> removed = readPositions(buf);
        return new BluePoisonMarkersPayload(reset, added, removed);
    }

    private static void writePositions(PacketByteBuf buf, List<BlockPos> positions) {
        buf.writeVarInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeBlockPos(pos);
        }
    }

    private static List<BlockPos> readPositions(PacketByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_POSITIONS) {
            throw new IllegalArgumentException("Invalid blue poison marker count");
        }
        List<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return positions;
    }
}
