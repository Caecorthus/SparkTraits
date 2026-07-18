package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.audio.CautiousSoundDebug;
import dev.caecorthus.sparktraits.impl.traits.global.CautiousSoundRules;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
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
        boolean confirmedServer = SparkTraitsServerConnection.isConfirmedServer();
        if (!confirmedServer && !CautiousSoundDebug.isEnabled()) {
            return;
        }
        boolean cautiousRule = CautiousSoundRules.shouldSuppressClientEntityStepSound(source, sound, category);
        boolean cancelled = confirmedServer && cautiousRule;
        CautiousSoundDebug.trace(
                "ClientWorld.playSoundFromEntity/5",
                source,
                confirmedServer,
                cautiousRule,
                cancelled ? "cancelled" : "preserved",
                sound == null ? null : sound.getId(),
                category,
                volume
        );
        if (cancelled) {
            ci.cancel();
        }
    }

    @Inject(
            method = "playSoundFromEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/sound/SoundCategory;FFJ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sparktraits$skipCautiousClientEntityMovementSound(
            PlayerEntity player,
            Entity source,
            RegistryEntry<SoundEvent> sound,
            SoundCategory category,
            float volume,
            float pitch,
            long seed,
            CallbackInfo ci
    ) {
        SoundEvent soundEvent = sound.value();
        boolean confirmedServer = SparkTraitsServerConnection.isConfirmedServer();
        if (!confirmedServer && !CautiousSoundDebug.isEnabled()) {
            return;
        }
        boolean cautiousRule = CautiousSoundRules.shouldSuppressClientEntityStepSound(source, soundEvent, category);
        boolean cancelled = confirmedServer && cautiousRule;
        CautiousSoundDebug.trace(
                "ClientWorld.playSoundFromEntity/7",
                source,
                confirmedServer,
                cautiousRule,
                cancelled ? "cancelled" : "preserved",
                soundEvent.getId(),
                category,
                volume
        );
        if (cancelled) {
            ci.cancel();
        }
    }
}
