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
    public static final Identifier DEPRESSION_DOCILE_TO_RAGE_ID = SparkTraits.id("depression.docile_to_rage");
    public static final SoundEvent DEPRESSION_DOCILE_TO_RAGE = registrar.create("depression.docile_to_rage");
    public static final Identifier DEPRESSION_RAGE_LOOP_ID = SparkTraits.id("depression.rage_loop");
    public static final SoundEvent DEPRESSION_RAGE_LOOP = registrar.create("depression.rage_loop");
    public static final Identifier DEPRESSION_BLIND_RAGE_ENRAGE_ID = SparkTraits.id("depression.blind_rage_enrage");
    public static final SoundEvent DEPRESSION_BLIND_RAGE_ENRAGE = registrar.create("depression.blind_rage_enrage");
    public static final Identifier DEPRESSION_BLIND_RAGE_CHASE_ID = SparkTraits.id("depression.blind_rage_chase");
    public static final SoundEvent DEPRESSION_BLIND_RAGE_CHASE = registrar.create("depression.blind_rage_chase");
    public static final Identifier DEPRESSION_RAGE_TO_DOCILE_ID = SparkTraits.id("depression.rage_to_docile");
    public static final SoundEvent DEPRESSION_RAGE_TO_DOCILE = registrar.create("depression.rage_to_docile");
    public static final Identifier DEPRESSION_PLAYER_WAS_SEEN_ID = SparkTraits.id("depression.player_was_seen");
    public static final SoundEvent DEPRESSION_PLAYER_WAS_SEEN = registrar.create("depression.player_was_seen");
    public static final Identifier DEPRESSION_MELEE_KILL_1_ID = SparkTraits.id("depression.melee_kill_1");
    public static final SoundEvent DEPRESSION_MELEE_KILL_1 = registrar.create("depression.melee_kill_1");
    public static final Identifier DEPRESSION_MELEE_KILL_2_ID = SparkTraits.id("depression.melee_kill_2");
    public static final SoundEvent DEPRESSION_MELEE_KILL_2 = registrar.create("depression.melee_kill_2");
    public static final Identifier DEPRESSION_SHYGUY_KILLED_ID = SparkTraits.id("depression.shyguy_killed");
    public static final SoundEvent DEPRESSION_SHYGUY_KILLED = registrar.create("depression.shyguy_killed");

    private SparkTraitsSounds() {
    }

    public static void initialize() {
        registrar.registerEntries();
        // English: Keep this explicit so /playsound visibility failures leave a clear startup clue.
        // 中文：显式校验注册结果，让 /playsound 不可见时启动日志能给出清晰线索。
        for (Identifier id : new Identifier[]{
                MUSIC_TAKEDISKRUSH_ID,
                DEPRESSION_DOCILE_TO_RAGE_ID,
                DEPRESSION_RAGE_LOOP_ID,
                DEPRESSION_BLIND_RAGE_ENRAGE_ID,
                DEPRESSION_BLIND_RAGE_CHASE_ID,
                DEPRESSION_RAGE_TO_DOCILE_ID,
                DEPRESSION_PLAYER_WAS_SEEN_ID,
                DEPRESSION_MELEE_KILL_1_ID,
                DEPRESSION_MELEE_KILL_2_ID,
                DEPRESSION_SHYGUY_KILLED_ID
        }) {
            if (!Registries.SOUND_EVENT.containsId(id)) {
                SparkTraits.LOGGER.warn("SparkTraits sound event {} was not registered.", id);
                return;
            }
        }
        SparkTraits.LOGGER.info("Registered SparkTraits sound events for /playsound.");
    }
}
