package dev.caecorthus.sparktraits.component;

import com.mojang.serialization.Codec;
import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class SparkTraitsDataComponentTypes {
    public static final ComponentType<String> CONSCIENCE_POISONER = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            SparkTraits.id("conscience_poisoner"),
            ComponentType.<String>builder().codec(Codec.STRING).packetCodec(PacketCodecs.STRING).build()
    );

    private SparkTraitsDataComponentTypes() {
    }

    public static void init() {
        // Forces static registration. / 触发静态注册。
    }
}
