package dev.caecorthus.sparktraits.network;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record OpenCriminologistScreenS2CPacket(
        UUID bodyUuid,
        List<Candidate> candidates
) implements CustomPayload {
    public record Candidate(UUID uuid, String name) {
    }

    public static final CustomPayload.Id<OpenCriminologistScreenS2CPacket> ID =
            new CustomPayload.Id<>(SparkTraits.id("open_criminologist_screen"));
    public static final PacketCodec<RegistryByteBuf, OpenCriminologistScreenS2CPacket> CODEC =
            PacketCodec.of(OpenCriminologistScreenS2CPacket::write, OpenCriminologistScreenS2CPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(PacketByteBuf buf) {
        buf.writeUuid(bodyUuid);
        buf.writeVarInt(candidates.size());
        for (Candidate candidate : candidates) {
            buf.writeUuid(candidate.uuid());
            buf.writeString(candidate.name());
        }
    }

    private static OpenCriminologistScreenS2CPacket read(PacketByteBuf buf) {
        UUID bodyUuid = buf.readUuid();
        int size = buf.readVarInt();
        List<Candidate> candidates = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            candidates.add(new Candidate(buf.readUuid(), buf.readString()));
        }
        return new OpenCriminologistScreenS2CPacket(bodyUuid, candidates);
    }
}
