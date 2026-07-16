package dev.caecorthus.sparktraits.client.audio;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.resource.SparkTraitsSounds;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

/**
 * Client-owned Depression rage loop that follows one psycho player.
 * 客户端持有的抑郁疯魔喊叫循环，始终跟随对应玩家。
 */
public final class DepressionRageLoopSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {
    private final PlayerEntity player;
    private boolean done;

    public DepressionRageLoopSoundInstance(PlayerEntity player) {
        super(SparkTraitsSounds.DEPRESSION_RAGE_LOOP, SoundCategory.AMBIENT, Random.create());
        this.player = player;
        this.repeat = true;
        this.repeatDelay = 0;
        this.attenuationType = SoundInstance.AttenuationType.LINEAR;
        this.relative = false;
        this.volume = DepressionTraitService.DEPRESSION_RANGE_SOUND_VOLUME;
        this.pitch = 1.0F;
        updatePosition();
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public void tick() {
        if (player.isRemoved() || !TraitPlayerComponent.KEY.get(player).isDepressionPsychoActive()) {
            stopLoop();
            return;
        }
        updatePosition();
    }

    public void stopLoop() {
        repeat = false;
        done = true;
    }

    boolean isFollowing(PlayerEntity player) {
        return this.player == player;
    }

    private void updatePosition() {
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }
}
