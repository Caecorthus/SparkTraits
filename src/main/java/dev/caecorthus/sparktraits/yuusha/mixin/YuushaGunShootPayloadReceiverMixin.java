package dev.caecorthus.sparktraits.mixin.yuusha.mixin;

import dev.caecorthus.sparktraits.yuusha.YuushaTrait;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.doctor4t.wathe.util.GunShootPayload$Receiver")
public abstract class YuushaGunShootPayloadReceiverMixin {
    @Redirect(
        method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
        at = @At(
            value = "INVOKE",
            target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;setMood(F)V",
            ordinal = 0
        ),
        remap = false
    )
    private void sparktraits$skipOnlyYuushaGunSanLoss(
        PlayerMoodComponent moodComponent,
        float newMood,
        GunShootPayload payload,
        ServerPlayNetworking.Context context
    ) {
        ServerPlayerEntity shooter = context.player();
        if (YuushaTrait.isYuushaShooter(shooter)) {
            // Keep Wathe's normal innocent-shot/backfire/小脑 flow intact; only skip the final sanity loss.
            return;
        }
        moodComponent.setMood(newMood);
    }
}
