package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.audio.CautiousSoundDebug;
import dev.caecorthus.sparktraits.impl.traits.global.CautiousSoundRules;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adapts Presence Footsteps, which sends movement audio directly through SoundManager and bypasses ClientWorld sound hooks.
 * 适配 Presence Footsteps：其移动音效会直接经由 SoundManager 播放，从而绕过 ClientWorld 声音钩子。
 */
@Pseudo
@Mixin(targets = "eu.ha3.presencefootsteps.sound.SoundEngine", remap = false)
public abstract class CautiousPresenceFootstepsVolumeMixin {
    @Inject(
            method = "getVolumeForSource",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void sparktraits$suppressCautiousPresenceFootstepsVolume(
            LivingEntity source,
            CallbackInfoReturnable<Float> cir
    ) {
        boolean confirmedServer = SparkTraitsServerConnection.isConfirmedServer();
        if (!confirmedServer && !CautiousSoundDebug.isEnabled()) {
            return;
        }
        boolean cautiousRule = source instanceof PlayerEntity player
                && CautiousSoundRules.shouldSuppressSounds(player);
        boolean zeroed = confirmedServer && cautiousRule;
        CautiousSoundDebug.trace(
                "PresenceFootsteps.SoundEngine.getVolumeForSource",
                source,
                confirmedServer,
                cautiousRule,
                zeroed ? "zeroed" : "preserved",
                null,
                null,
                zeroed ? 0.0F : null
        );
        if (zeroed) {
            cir.setReturnValue(0.0F);
        }
    }
}
