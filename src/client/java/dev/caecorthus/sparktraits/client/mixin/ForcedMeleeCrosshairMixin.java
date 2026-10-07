package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.killer.ForcedMeleeHud;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CrosshairRenderer.class, remap = false)
public abstract class ForcedMeleeCrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void sparktraits$replaceLockedIndicator(MinecraftClient client, ClientPlayerEntity player,
                                                           DrawContext context, RenderTickCounter tickCounter,
                                                           CallbackInfo ci) {
        if (ForcedMeleeHud.renderLockedCrosshair(context)) {
            ci.cancel();
        }
    }
}
