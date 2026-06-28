package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.DepressionHud;
import dev.caecorthus.sparktraits.client.DepressionScreenEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
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
 * Adds Depression's private HUD timer and grayscale post effect to Wathe's in-game HUD.
 * 将抑郁的私有倒计时与灰阶后处理接入 wathe 游戏 HUD。
 */
@Mixin(InGameHud.class)
public abstract class DepressionInGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderMainHud", at = @At("TAIL"))
    private void sparktraits$renderDepressionHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientPlayerEntity player = this.client.player;
        if (player == null) {
            return;
        }
        float delta = tickCounter.getTickDelta(true);
        DepressionScreenEffects.render(player, delta);
        TextRenderer renderer = this.client.textRenderer;
        DepressionHud.render(renderer, player, context, delta);
    }
}
