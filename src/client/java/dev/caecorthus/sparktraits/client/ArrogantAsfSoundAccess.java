package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.client.mixin.SoundManagerAccessor;
import dev.caecorthus.sparktraits.client.mixin.SoundSystemAccessor;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.Source;

/**
 * Narrow bridge for pausing and resuming one client-side Arrogant ASF sound source.
 * 仅用于暂停/恢复“展示豪度”客户端音乐声源的窄桥接层。
 */
public final class ArrogantAsfSoundAccess {
    private ArrogantAsfSoundAccess() {
    }

    public static boolean pause(SoundManager manager, SoundInstance instance) {
        return runOnSource(manager, instance, Source::pause);
    }

    public static boolean resume(SoundManager manager, SoundInstance instance) {
        return runOnSource(manager, instance, Source::resume);
    }

    private static boolean runOnSource(SoundManager manager, SoundInstance instance, java.util.function.Consumer<Source> action) {
        if (manager == null || instance == null) {
            return false;
        }
        SoundSystem soundSystem = ((SoundManagerAccessor) manager).sparktraits$getSoundSystem();
        Channel.SourceManager sourceManager = ((SoundSystemAccessor) soundSystem).sparktraits$getSources().get(instance);
        if (sourceManager == null || sourceManager.isStopped()) {
            return false;
        }
        sourceManager.run(action);
        return true;
    }
}
