package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.traits.global.CautiousSoundRules;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Covers client-side sound replays after mods such as Sound Physics Remastered bypass vanilla step hooks.
 * 兜底处理物理音效等客户端重放声音并绕过原版脚步钩子的路径。
 */
@Mixin(ClientWorld.class)
public abstract class CautiousClientWorldSoundMixin {
    @Inject(
            method = "playSoundFromEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sparktraits$skipCautiousClientEntityStepSound(
            Entity source,
            SoundEvent sound,
            SoundCategory category,
            float volume,
            float pitch,
            CallbackInfo ci
    ) {
        if (SparkTraitsServerConnection.isConfirmedServer()
                && CautiousSoundRules.shouldSuppressClientEntityStepSound(source, sound, category)) {
            ci.cancel();
        }
    }
}
