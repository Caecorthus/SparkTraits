package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.ArrogantAsfMusicRules;
import dev.caecorthus.sparktraits.impl.ArrogantAsfTrait;
import dev.caecorthus.sparktraits.impl.SparkTraitsSounds;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.SoundManager;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

/**
 * Drives the owner-only Arrogant ASF music from server-confirmed trait state.
 * 根据服务端确认的天赋状态驱动“展示豪度”仅本人可听音乐。
 */
public final class ArrogantAsfMusicController {
    private static ArrogantAsfMusicInstance music;
    private static boolean paused;
    private static int pausedTicks;
    private static boolean missingSoundResourceWarned;

    private ArrogantAsfMusicController() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            stopAndClear(client);
            return;
        }

        ClientPlayerEntity player = client.player;
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        if (!isEligiblePlayer(player, traits)) {
            stopAndClear(client);
            return;
        }

        if (traits.isArrogantAsfActive()) {
            playOrResume(client.getSoundManager());
            return;
        }

        pauseOrExpire(client.getSoundManager());
    }

    private static boolean isEligiblePlayer(ClientPlayerEntity player, TraitPlayerComponent traits) {
        return GameFunctions.isPlayerPlayingAndAlive(player)
                && !SwallowedPlayerComponent.isPlayerSwallowed(player)
                && traits.hasActiveTrait(ArrogantAsfTrait.ID);
    }

    private static void playOrResume(SoundManager soundManager) {
        if (!hasMusicResource(soundManager)) {
            return;
        }
        if (music == null || !soundManager.isPlaying(music)) {
            music = new ArrogantAsfMusicInstance();
            paused = false;
            pausedTicks = 0;
            soundManager.play(music);
            return;
        }

        if (paused) {
            ArrogantAsfSoundAccess.resume(soundManager, music);
            paused = false;
            pausedTicks = 0;
        }
    }

    private static boolean hasMusicResource(SoundManager soundManager) {
        if (soundManager.get(SparkTraitsSounds.MUSIC_TAKEDISKRUSH_ID) != null) {
            missingSoundResourceWarned = false;
            return true;
        }

        if (!missingSoundResourceWarned) {
            // English: The sound event is registered, but the client resource pack did not expose the sound set.
            // 中文：声音事件已注册，但客户端资源包没有暴露对应的声音集合。
            SparkTraits.LOGGER.warn("Missing client sound resource {}. Check assets/sparktraits/sounds.json and the installed client jar.",
                    SparkTraitsSounds.MUSIC_TAKEDISKRUSH_ID);
            missingSoundResourceWarned = true;
        }
        return false;
    }

    private static void pauseOrExpire(SoundManager soundManager) {
        if (music == null) {
            paused = false;
            pausedTicks = 0;
            return;
        }
        if (!soundManager.isPlaying(music)) {
            clearState();
            return;
        }

        if (!paused) {
            ArrogantAsfSoundAccess.pause(soundManager, music);
            paused = true;
            pausedTicks = 1;
        } else {
            pausedTicks++;
        }

        if (ArrogantAsfMusicRules.shouldDiscardPausedTrack(pausedTicks)) {
            stopAndClear(soundManager);
        }
    }

    private static void stopAndClear(MinecraftClient client) {
        if (client == null) {
            clearState();
            return;
        }
        stopAndClear(client.getSoundManager());
    }

    private static void stopAndClear(SoundManager soundManager) {
        if (music != null && soundManager != null) {
            music.stopLoop();
            soundManager.stop(music);
        }
        clearState();
    }

    private static void clearState() {
        music = null;
        paused = false;
        pausedTicks = 0;
    }
}
