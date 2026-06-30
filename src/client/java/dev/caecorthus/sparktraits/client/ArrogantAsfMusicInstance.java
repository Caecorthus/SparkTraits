package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.impl.SparkTraitsSounds;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

/**
 * Local-only looping music for the Arrogant ASF active state.
 * “展示豪度”开启状态的本地循环音乐实例。
 */
public final class ArrogantAsfMusicInstance extends AbstractSoundInstance implements TickableSoundInstance {
    private boolean done;

    public ArrogantAsfMusicInstance() {
        super(SparkTraitsSounds.MUSIC_TAKEDISKRUSH, SoundCategory.MUSIC, Random.create());
        this.repeat = true;
        this.repeatDelay = 0;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        this.relative = true;
        this.volume = 1.0f;
        this.pitch = 1.0f;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void tick() {
    }

    public void stopLoop() {
        this.done = true;
    }
}
