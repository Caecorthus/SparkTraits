package dev.caecorthus.sparktraits.network;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record SelectCriminologistTargetC2SPacket(UUID bodyUuid, UUID targetUuid) implements CustomPayload {
    public static final CustomPayload.Id<SelectCriminologistTargetC2SPacket> ID =
            new CustomPayload.Id<>(SparkTraits.id("select_criminologist_target"));
    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, SelectCriminologistTargetC2SPacket> CODEC =
            PacketCodec.tuple(
                    Uuids.PACKET_CODEC, SelectCriminologistTargetC2SPacket::bodyUuid,
                    Uuids.PACKET_CODEC, SelectCriminologistTargetC2SPacket::targetUuid,
                    SelectCriminologistTargetC2SPacket::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
