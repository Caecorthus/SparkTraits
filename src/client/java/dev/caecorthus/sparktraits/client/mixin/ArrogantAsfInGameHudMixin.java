package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.ArrogantAsfHud;
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
 * Adds Arrogant ASF's ability toggle text to the in-game HUD.
 * 将“展示豪度”的技能开关文字加入游戏内 HUD。
 */
@Mixin(InGameHud.class)
public abstract class ArrogantAsfInGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderMainHud", at = @At("TAIL"))
    private void sparktraits$renderArrogantAsfHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientPlayerEntity player = this.client.player;
        if (player == null) {
            return;
        }
        TextRenderer renderer = this.client.textRenderer;
        ArrogantAsfHud.render(renderer, player, context);
    }
}
