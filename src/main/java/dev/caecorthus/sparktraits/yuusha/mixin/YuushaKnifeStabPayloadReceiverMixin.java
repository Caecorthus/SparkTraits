package dev.caecorthus.sparktraits.mixin.yuusha.mixin;

import dev.caecorthus.sparktraits.yuusha.YuushaTrait;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.doctor4t.wathe.util.KnifeStabPayload$Receiver")
public abstract class YuushaKnifeStabPayloadReceiverMixin {
    @Inject(method = "receive(Ldev/doctor4t/wathe/util/KnifeStabPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V", at = @At("TAIL"), remap = false)
    private void sparktraits$overrideTemporaryYuushaKnifeCooldown(KnifeStabPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity player = context.player();
        if (YuushaTrait.hasTemporaryYuushaKnife(player)) {
            player.getItemCooldownManager().set(WatheItems.KNIFE, 20 * 20);
        }
    }
}
