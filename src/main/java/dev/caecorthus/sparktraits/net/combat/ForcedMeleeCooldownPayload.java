package dev.caecorthus.sparktraits.net.combat;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A complete private snapshot, not a broadcast of player traits or enemy penalties. */
public record ForcedMeleeCooldownPayload(UUID owner, Map<Identifier, Integer> remaining) implements CustomPayload {
    public static final Id<ForcedMeleeCooldownPayload> ID = new Id<>(SparkTraits.id("forced_melee_cooldowns"));
    public static final PacketCodec<RegistryByteBuf, ForcedMeleeCooldownPayload> CODEC =
            PacketCodec.of(ForcedMeleeCooldownPayload::write, ForcedMeleeCooldownPayload::read);

    public ForcedMeleeCooldownPayload {
        remaining = Map.copyOf(remaining);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(PacketByteBuf buf) {
        buf.writeUuid(owner);
        buf.writeVarInt(remaining.size());
        remaining.forEach((item, ticks) -> {
            buf.writeIdentifier(item);
            buf.writeVarInt(ticks);
        });
    }

    private static ForcedMeleeCooldownPayload read(PacketByteBuf buf) {
        UUID owner = buf.readUuid();
        int size = buf.readVarInt();
        if (size < 0 || size > 4096) throw new IllegalArgumentException("Invalid forced cooldown snapshot size");
        Map<Identifier, Integer> values = new HashMap<>();
        for (int i = 0; i < size; i++) values.put(buf.readIdentifier(), Math.max(0, buf.readVarInt()));
        return new ForcedMeleeCooldownPayload(owner, values);
    }
}
