package dev.caecorthus.sparktraits.client.audio;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Maintains one independent rage-loop sound for every synced Depression psycho.
 * 根据同步的抑郁疯魔状态，为每名玩家分别维护独立的喊叫循环。
 */
public final class DepressionRageLoopController {
    private static final Map<UUID, DepressionRageLoopSoundInstance> activeLoops = new HashMap<>();

    private DepressionRageLoopController() {
    }

    public static void tick(MinecraftClient client) {
        SoundManager soundManager = client.getSoundManager();
        if (!SparkTraitsServerConnection.isConfirmedServer()
                || client.world == null
                || !canHearRageLoop(client)) {
            stopAll(soundManager);
            return;
        }

        Set<UUID> activePlayers = new HashSet<>();
        for (PlayerEntity player : client.world.getPlayers()) {
            if (!TraitPlayerComponent.KEY.get(player).isDepressionPsychoActive()) {
                continue;
            }

            UUID playerUuid = player.getUuid();
            activePlayers.add(playerUuid);
            DepressionRageLoopSoundInstance instance = activeLoops.get(playerUuid);
            if (instance == null || instance.isDone() || !instance.isFollowing(player)) {
                if (instance != null) {
                    stop(soundManager, instance);
                }
                instance = new DepressionRageLoopSoundInstance(player);
                activeLoops.put(playerUuid, instance);
            }
            if (!soundManager.isPlaying(instance)) {
                instance.tick();
                soundManager.play(instance);
            }
        }

        Iterator<Map.Entry<UUID, DepressionRageLoopSoundInstance>> iterator = activeLoops.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DepressionRageLoopSoundInstance> entry = iterator.next();
            if (activePlayers.contains(entry.getKey())) {
                continue;
            }
            stop(soundManager, entry.getValue());
            iterator.remove();
        }
    }

    private static void stopAll(SoundManager soundManager) {
        for (DepressionRageLoopSoundInstance instance : activeLoops.values()) {
            stop(soundManager, instance);
        }
        activeLoops.clear();
    }

    private static void stop(SoundManager soundManager, DepressionRageLoopSoundInstance instance) {
        instance.stopLoop();
        soundManager.stop(instance);
    }

    private static boolean canHearRageLoop(MinecraftClient client) {
        return client.options.getSoundVolume(SoundCategory.MASTER) > 0.0F
                && client.options.getSoundVolume(SoundCategory.AMBIENT) > 0.0F;
    }
}
