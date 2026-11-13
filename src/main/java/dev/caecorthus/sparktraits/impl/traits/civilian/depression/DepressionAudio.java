package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import dev.caecorthus.sparktraits.impl.resource.SparkTraitsSounds;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Depression's server-sent sound cues: which cue plays, when the chase loop replays, and how each cue reaches
 * players (category, volume, pitch). The client rage loop reuses the range volume.
 * 抑郁由服务端发送的音效提示：播放哪种提示、追逐循环何时重播，以及每种提示如何送达玩家（分类、音量、音调）。
 * 客户端的狂怒循环沿用其中的范围音量。
 */
public final class DepressionAudio {
    public static final int CHASE_LOOP_INTERVAL_TICKS = 1121;
    public static final float DEPRESSION_RANGE_SOUND_VOLUME = 5.0f;
    public static final float DEPRESSION_DIRECT_SOUND_VOLUME = 2.0f;
    public static final float DEPRESSION_MUSIC_SOUND_VOLUME = 2.0f;
    // English: Wathe locks MUSIC to 0.0, so Depression's owned psycho audio uses AMBIENT.
    // 中文：Wathe 会把 MUSIC 分类锁到 0.0，因此抑郁自管的疯魔音频改走 AMBIENT。
    static final SoundCategory DEPRESSION_AUDIO_CATEGORY = SoundCategory.AMBIENT;
    private static final float DEPRESSION_SOUND_PITCH = 1.0f;

    private DepressionAudio() {
    }

    public static boolean shouldPlayChaseLoop(int ticksUntilNextLoop) {
        return ticksUntilNextLoop <= 0;
    }

    public static int nextChaseLoopTicks(int ticksUntilNextLoop) {
        return ticksUntilNextLoop <= 0 ? CHASE_LOOP_INTERVAL_TICKS : ticksUntilNextLoop - 1;
    }

    public static SoundEvent meleeKillSound(boolean secondVariant) {
        return secondVariant ? SparkTraitsSounds.DEPRESSION_MELEE_KILL_2 : SparkTraitsSounds.DEPRESSION_MELEE_KILL_1;
    }

    static void playRangeSound(ServerPlayerEntity source, SoundEvent sound) {
        source.getWorld().playSound(
                null,
                source.getX(),
                source.getY(),
                source.getZ(),
                sound,
                DEPRESSION_AUDIO_CATEGORY,
                DEPRESSION_RANGE_SOUND_VOLUME,
                DEPRESSION_SOUND_PITCH
        );
    }

    static void stopPairMusicSound(ServerPlayerEntity player, @Nullable ServerPlayerEntity attacker, Identifier soundId) {
        stopDirectSound(player, soundId);
        if (attacker != null && attacker != player) {
            stopDirectSound(attacker, soundId);
        }
    }

    private static void stopDirectSound(ServerPlayerEntity player, Identifier soundId) {
        player.networkHandler.sendPacket(new StopSoundS2CPacket(soundId, DEPRESSION_AUDIO_CATEGORY));
    }

    static void playDirectSound(ServerPlayerEntity player, SoundEvent sound) {
        player.playSoundToPlayer(sound, DEPRESSION_AUDIO_CATEGORY, DEPRESSION_DIRECT_SOUND_VOLUME, DEPRESSION_SOUND_PITCH);
    }

    private static void playMusicSound(ServerPlayerEntity player, SoundEvent sound) {
        player.playSoundToPlayer(sound, DEPRESSION_AUDIO_CATEGORY, DEPRESSION_MUSIC_SOUND_VOLUME, DEPRESSION_SOUND_PITCH);
    }

    static void playPairMusicSound(ServerPlayerEntity player, @Nullable ServerPlayerEntity attacker, SoundEvent sound) {
        playMusicSound(player, sound);
        if (attacker != null && attacker != player) {
            playMusicSound(attacker, sound);
        }
    }
}
