package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.compatibility.noellesroles.SilencedKillerRestrictionService;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.agmas.noellesroles.demonhunter.DemonHunterShootC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DemonHunterShootC2SPacket.Receiver.class, priority = 2200, remap = false)
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
        // Phase rejection must precede even restriction feedback, not just ammo, cooldown and hit resolution.
        // 脱险禁用先于限制提示、弹药、冷却及命中结算，不影响既有延迟攻击。
        if (LastEscapeService.isActive(context.player())
                || SilencedKillerRestrictionService.denyActiveAbilityIfRestricted(context.player())) {
            ci.cancel();
        }
    }
}
