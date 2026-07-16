package dev.caecorthus.sparktraits.impl.traits.killer;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

/**
 * Owns the additive weapon gate shared by Thrust and its cooldown knockback shim.
 * 统一持有突刺与冷却击退兼容共用的可扩展武器门槛。
 */
public final class KillerWeaponTags {
    private static final TagKey<Item> THRUST_WEAPONS = TagKey.of(
            RegistryKeys.ITEM,
            SparkTraits.id("thrust_weapons")
    );

    private KillerWeaponTags() {
    }

    public static boolean isThrustWeapon(ItemStack stack) {
        return stack != null && stack.isIn(THRUST_WEAPONS);
    }
}
