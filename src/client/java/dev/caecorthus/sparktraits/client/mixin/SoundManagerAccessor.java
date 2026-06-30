package dev.caecorthus.sparktraits.client.mixin;

import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes Minecraft's sound system only for Arrogant ASF music pause/resume.
 * 仅为“展示豪度”音乐暂停/恢复暴露 Minecraft 声音系统。
 */
@Mixin(SoundManager.class)
public interface SoundManagerAccessor {
    @Accessor("soundSystem")
    SoundSystem sparktraits$getSoundSystem();
}
