package dev.caecorthus.sparktraits.api;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime trait registry shared by server and client.
 * 服务端和客户端都应注册同一批天赋定义，同步时只传输天赋 ID。
 */
public final class TraitRegistry {
    private static final Map<Identifier, Trait> TRAITS = new LinkedHashMap<>();

    private TraitRegistry() {
    }

    public static Trait register(Trait trait) {
        Identifier id = trait.id();
        if (TRAITS.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate SparkTraits trait id: " + id);
        }
        TRAITS.put(id, trait);
        return trait;
    }

    public static @Nullable Trait get(Identifier id) {
        return TRAITS.get(id);
    }

    public static boolean contains(Identifier id) {
        return TRAITS.containsKey(id);
    }

    public static Collection<Trait> values() {
        return Collections.unmodifiableCollection(TRAITS.values());
    }
}
