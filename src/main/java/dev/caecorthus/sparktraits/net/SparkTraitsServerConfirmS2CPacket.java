package dev.caecorthus.sparktraits.net;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SparkTraitsServerConfirmS2CPacket(String serverVersion) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkTraits.id("server_confirm");
    public static final Id<SparkTraitsServerConfirmS2CPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, SparkTraitsServerConfirmS2CPacket> CODEC =
            PacketCodec.of(SparkTraitsServerConfirmS2CPacket::write, SparkTraitsServerConfirmS2CPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(serverVersion);
    }

    public static SparkTraitsServerConfirmS2CPacket read(PacketByteBuf buf) {
        return new SparkTraitsServerConfirmS2CPacket(buf.readString());
    }
}
