package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.BluePoisonPresentationRules;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerPoisonComponent.class, remap = false)
public abstract class PlayerPoisonComponentBluePoisonMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    public int poisonTicks;

    @Shadow
    public boolean pulsing;

    @Shadow
    public float pulseProgress;

    @Unique
    private int sparktraits$bluePulseCooldown;

    @Unique
    private boolean sparktraits$presentingBluePoison;

    @Inject(method = "clientTick", at = @At("HEAD"))
    private void sparktraits$releaseBluePulseBeforeNativeTick(CallbackInfo ci) {
        if (sparktraits$presentingBluePoison && poisonTicks > 0) {
            pulsing = false;
            pulseProgress = 0.0F;
            sparktraits$presentingBluePoison = false;
            sparktraits$bluePulseCooldown = 0;
        }
    }

    @Inject(method = "clientTick", at = @At("TAIL"))
    private void sparktraits$bluePoisonHeartbeat(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != player) {
            return;
        }

        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        traits.tickConsciencePoisonClient();
        int nativePoisonTicks = poisonTicks;
        boolean bluePoisoned = traits.hasConsciencePoison();
        if (!SparkTraitsServerConnection.isConfirmedServer() || !player.isAlive() || player.isSpectator()) {
            sparktraits$clearBluePulse(nativePoisonTicks);
            return;
        }
        if (BluePoisonPresentationRules.owner(nativePoisonTicks > 0, bluePoisoned)
                != BluePoisonPresentationRules.Owner.BLUE_POISON) {
            sparktraits$clearBluePulse(nativePoisonTicks);
            return;
        }

        sparktraits$presentingBluePoison = true;
        int remainingTicks = traits.getConsciencePoisonTicks();
        int initialTicks = traits.getConsciencePoisonInitialTicks();
        if (!BluePoisonPresentationRules.canPulse(initialTicks, remainingTicks)) {
            return;
        }
        if (sparktraits$bluePulseCooldown > 0) {
            sparktraits$bluePulseCooldown--;
            return;
        }

        int maximumPoisonTicks = PlayerPoisonComponent.clampTime.getRight();
        sparktraits$bluePulseCooldown = BluePoisonPresentationRules.cooldownAfterPulse(remainingTicks, maximumPoisonTicks);
        pulsing = true;
        player.playSound(
                SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                BluePoisonPresentationRules.heartbeatVolume(remainingTicks, maximumPoisonTicks),
                1.0F
        );
    }

    @Unique
    private void sparktraits$clearBluePulse(int nativePoisonTicks) {
        sparktraits$bluePulseCooldown = 0;
        if (sparktraits$presentingBluePoison && nativePoisonTicks <= 0) {
            pulsing = false;
            pulseProgress = 0.0F;
        }
        sparktraits$presentingBluePoison = false;
    }
}
