package dev.caecorthus.sparktraits.client.audio;

import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.resource.SparkTraitsSounds;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;

/**
 * Stops the final moment BGM as soon as the moment is no longer active or the round has ended. The server plays the
 * stream once at trigger time; this controller only observes the synced final-moment flag and the game status, so a
 * round end, a world unload or a disconnect also cuts the music even when the sound is longer than the moment.
 * 终局时刻失效或回合结束时立即停止终局 BGM。服务端只在触发时播放一次流式音乐；本控制器只观察同步的终局标记与游戏状态，
 * 因此回合结束、世界卸载或断线时，即使音乐还没放完也会被截断。
 */
public final class FinalMomentMusicController {
    private static boolean wasActive;

    private FinalMomentMusicController() {
    }

    public static void tick(MinecraftClient client) {
        boolean active = false;
        if (client.world != null && SparkTraitsServerConnection.isConfirmedServer()) {
            TraitWorldComponent traitWorld = TraitWorldComponent.KEY.maybeGet(client.world).orElse(null);
            GameWorldComponent game = GameWorldComponent.KEY.maybeGet(client.world).orElse(null);
            active = traitWorld != null
                    && traitWorld.isFinalMomentActive()
                    && hasLivingLooseEnd(client, traitWorld)
                    && game != null
                    && game.getGameStatus() == GameWorldComponent.GameStatus.ACTIVE;
        }
        if (wasActive && !active) {
            stop(client);
        }
        wasActive = active;
    }

    /**
     * The moment is over once no living loose end remains; the synced world component still keeps the flag set until
     * the round itself resets, so the music must not wait for that.
     * 最后一名亡命徒死亡后终局时刻即结束；同步的世界组件要到回合重置才清标记，因此音乐不能等它。
     */
    private static boolean hasLivingLooseEnd(MinecraftClient client, TraitWorldComponent traitWorld) {
        if (client.world == null) {
            return false;
        }
        for (PlayerEntity player : client.world.getPlayers()) {
            if (traitWorld.isFinalMomentLooseEnd(player.getUuid())
                    && GameFunctions.isPlayerPlayingAndAlive(player)) {
                return true;
            }
        }
        return false;
    }

    public static void stop(MinecraftClient client) {
        client.getSoundManager().stopSounds(SparkTraitsSounds.FINAL_MOMENT_MUSIC_ID, SoundCategory.MASTER);
    }

    /** Disconnect/world unload path: stop and forget the previous flag. / 断线或世界卸载：停止并清除上一状态。 */
    public static void reset(MinecraftClient client) {
        stop(client);
        wasActive = false;
    }
}
