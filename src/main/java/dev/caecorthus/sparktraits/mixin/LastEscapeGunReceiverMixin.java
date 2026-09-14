package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reject new shots before Niko scheduling, ammunition, records or effects; already emitted attacks remain valid.
 *  在 Niko 补发调度、弹药、记录及特效之前拒绝新射击，不取消此前已发出的攻击。 */
@Mixin(value = GunShootPayload.Receiver.class, priority = 2200, remap = false)
public abstract class LastEscapeGunReceiverMixin {
    @Inject(method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void sparktraits$blockPhaseShot(GunShootPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        if (LastEscapeService.isActive(context.player())) ci.cancel();
    }
}
