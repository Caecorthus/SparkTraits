package dev.caecorthus.sparktraits.client.audio;

import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.resource.SparkTraitsSounds;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;

import java.util.Map;
import java.util.UUID;

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
                    && game != null
                    && game.getGameStatus() == GameWorldComponent.GameStatus.ACTIVE
                    && hasLivingLooseEnd(traitWorld, game);
        }
        if (wasActive && !active) {
            stop(client);
        }
        wasActive = active;
    }

    /**
     * The moment is over once no living loose end remains; the synced world component still keeps the flag set until
     * the round itself resets, so the music must not wait for that. Reads the synced role map and dead set rather than
     * {@code client.world.getPlayers()}, which only holds players inside this client's entity tracking range.
     * 最后一名亡命徒死亡后终局时刻即结束；同步的世界组件要到回合重置才清标记，因此音乐不能等它。这里读同步的身份表与
     * 死亡名单，而不是 {@code client.world.getPlayers()}：后者只包含本客户端实体追踪范围内的玩家。
     */
    private static boolean hasLivingLooseEnd(TraitWorldComponent traitWorld, GameWorldComponent game) {
        for (Map.Entry<UUID, Role> entry : game.getRoles().entrySet()) {
            UUID uuid = entry.getKey();
            Role role = entry.getValue();
            if (traitWorld.isFinalMomentLooseEnd(uuid)
                    && role != null
                    && WatheRoles.LOOSE_END.identifier().equals(role.identifier())
                    && !game.isPlayerDead(uuid)) {
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
