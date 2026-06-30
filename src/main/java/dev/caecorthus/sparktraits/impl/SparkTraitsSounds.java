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
        // English: Keep this explicit so /playsound visibility failures leave a clear startup clue.
        // 中文：显式校验注册结果，让 /playsound 不可见时启动日志能给出清晰线索。
        if (!Registries.SOUND_EVENT.containsId(MUSIC_TAKEDISKRUSH_ID)) {
            SparkTraits.LOGGER.warn("SparkTraits sound event {} was not registered.", MUSIC_TAKEDISKRUSH_ID);
            return;
        }
        SparkTraits.LOGGER.info("Registered SparkTraits sound event {} for /playsound.", MUSIC_TAKEDISKRUSH_ID);
    }
}
