package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.SilencedKillerRestrictionService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.agmas.noellesroles.demonhunter.DemonHunterShootC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DemonHunterShootC2SPacket.Receiver.class, remap = false)
public abstract class DemonHunterShootPacketMixin {
    @Inject(
            method = "receive(Lorg/agmas/noellesroles/demonhunter/DemonHunterShootC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void sparktraits$blockSilencedDemonHunterShot(
            DemonHunterShootC2SPacket payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        if (SilencedKillerRestrictionService.denyActiveAbilityIfRestricted(context.player())) {
            ci.cancel();
        }
    }
}
