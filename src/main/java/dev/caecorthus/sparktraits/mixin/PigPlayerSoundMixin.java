package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.PigTraitService;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives Pig players vanilla pig feedback sounds without changing non-Pig players.
 * 让猪天赋玩家使用原版猪的反馈声音，同时不影响非猪玩家。
 */
@Mixin(PlayerEntity.class)
public abstract class PigPlayerSoundMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void sparktraits$tickPigAmbientSound(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            PigTraitService.tickAmbientSound(player);
        }
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void sparktraits$usePigHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        if (PigTraitService.isPig((PlayerEntity) (Object) this)) {
            cir.setReturnValue(PigTraitService.hurtSound());
        }
    }
}
