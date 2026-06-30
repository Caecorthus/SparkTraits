package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Registers SparkTraits sound events.
 * 注册 SparkTraits 自定义声音事件。
 */
public final class SparkTraitsSounds {
    public static final Identifier MUSIC_TAKEDISKRUSH_ID = SparkTraits.id("music.takediskrush");
    public static final SoundEvent MUSIC_TAKEDISKRUSH = Registry.register(
            Registries.SOUND_EVENT,
            MUSIC_TAKEDISKRUSH_ID,
            SoundEvent.of(MUSIC_TAKEDISKRUSH_ID)
    );

    private SparkTraitsSounds() {
    }

    public static void register() {
        // Static initializer performs the registration.
        // 静态初始化器会完成注册。
    }
}
