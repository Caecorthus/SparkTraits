package dev.caecorthus.sparktraits.client.killer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.caecorthus.sparktraits.api.SparkTraitsApi;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class ForcedMeleeHud {
    private static final Identifier AIM = Identifier.of("wathe", "hud/crosshair");
    private static final int RED = 0xffff4040;

    private ForcedMeleeHud() {
    }

    public static int remainingTicks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!SparkTraitsServerConnection.isConfirmedServer() || client.player == null || client.world == null
                || !GameWorldComponent.KEY.get(client.world).isRunning()
                || !GameFunctions.isPlayerPlayingAndAlive(client.player)) {
            return 0;
        }
        return SparkTraitsApi.getForcedMeleeCooldownTicks(client.player, client.player.getMainHandStack());
    }

    /** Replaces only the crosshair path while locked; no ordinary cooldown state is rewritten. */
    public static boolean renderLockedCrosshair(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        int ticks = remainingTicks();
        if (ticks <= 0) {
            return false;
        }
        if (!ForcedMeleeHudRules.visible(ticks, client.options.getPerspective().isFirstPerson(),
                client.options.hudHidden, client.player.isAlive() && !client.player.isSpectator(), true)) {
            return true;
        }
        int centerX = context.getScaledWindowWidth() / 2;
        int centerY = context.getScaledWindowHeight() / 2;
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2.0f - 1.5f,
                context.getScaledWindowHeight() / 2.0f - 1.5f, 0);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        context.drawGuiTexture(AIM, 0, 0, 3, 3);
        context.draw();
        context.getMatrices().pop();

        // Wathe's inverse-color blend must NOT leak into the red warning.
        RenderSystem.defaultBlendFunc();
        for (int pixel = 0; pixel < 7; pixel++) {
            context.fill(centerX - 3 + pixel, centerY + 5 + pixel,
                    centerX - 2 + pixel, centerY + 6 + pixel, RED);
            context.fill(centerX + 3 - pixel, centerY + 5 + pixel,
                    centerX + 4 - pixel, centerY + 6 + pixel, RED);
        }
        String text = ForcedMeleeHudRules.seconds(ticks) + "s";
        context.drawTextWithShadow(client.textRenderer, text,
                centerX - client.textRenderer.getWidth(text) / 2, centerY + 14, RED);
        context.draw();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        return true;
    }
}
