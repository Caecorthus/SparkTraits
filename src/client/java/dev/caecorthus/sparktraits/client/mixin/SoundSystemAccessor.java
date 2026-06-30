package dev.caecorthus.sparktraits.client.mixin;

import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exposes active sound sources so Arrogant ASF can pause one exact instance.
 * 暴露正在播放的声音源，让“展示豪度”只暂停自己的音乐实例。
 */
@Mixin(SoundSystem.class)
public interface SoundSystemAccessor {
    @Accessor("sources")
    Map<SoundInstance, Channel.SourceManager> sparktraits$getSources();
}
