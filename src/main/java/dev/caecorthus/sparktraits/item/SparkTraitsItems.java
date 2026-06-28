package dev.caecorthus.sparktraits.item;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class SparkTraitsItems {
    public static final Item CAPSULE = register("capsule", new CapsuleItem(new Item.Settings().maxCount(16)));
    public static final Item FLASHLIGHT = register("flashlight", new FlashlightItem(new Item.Settings().maxCount(1)));

    private SparkTraitsItems() {
    }

    public static void init() {
        // Forces static registration. / 触发静态注册。
    }

    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, SparkTraits.id(path), item);
    }
}
