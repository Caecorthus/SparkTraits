package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.doctor4t.ratatouille.util.registrar.SoundEventRegistrar;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Registers SparkTraits sound events through the same registrar path used by Wathe ambience.
 * 通过 Wathe 环境音同款 registrar 链路注册 SparkTraits 自定义声音事件。
 */
public final class SparkTraitsSounds {
    private static final SoundEventRegistrar registrar = new SoundEventRegistrar(SparkTraits.MOD_ID);

    public static final Identifier MUSIC_TAKEDISKRUSH_ID = SparkTraits.id("music.takediskrush");
    public static final SoundEvent MUSIC_TAKEDISKRUSH = registrar.create("music.takediskrush");

    private SparkTraitsSounds() {
    }

    public static void initialize() {
        registrar.registerEntries();
        // English: Keep this explicit so /playsound visibility failures leave a clear startup clue.
        // 中文：显式校验注册结果，让 /playsound 不可见时启动日志能给出清晰线索。
        if (!Registries.SOUND_EVENT.containsId(MUSIC_TAKEDISKRUSH_ID)) {
            SparkTraits.LOGGER.warn("SparkTraits sound event {} was not registered.", MUSIC_TAKEDISKRUSH_ID);
            return;
        }
        SparkTraits.LOGGER.info("Registered SparkTraits sound event {} for /playsound.", MUSIC_TAKEDISKRUSH_ID);
    }
}
