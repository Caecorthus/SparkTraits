package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.hud.PreparefullHud;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the 养精蓄锐 charge percentage to Wathe's in-game HUD.
 * 将养精蓄锐的蓄力百分比接入 wathe 游戏 HUD。
 */
@Mixin(InGameHud.class)
public abstract class PreparefullInGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderMainHud", at = @At("TAIL"))
    private void sparktraits$renderPreparefullHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!SparkTraitsServerConnection.isConfirmedServer()) {
            return;
        }
        ClientPlayerEntity player = this.client.player;
        if (player == null) {
            return;
        }
        PreparefullHud.render(this.client.textRenderer, player, context);
    }
}
