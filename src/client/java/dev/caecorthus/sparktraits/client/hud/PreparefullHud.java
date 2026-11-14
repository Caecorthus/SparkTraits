package dev.caecorthus.sparktraits.client.hud;

import dev.caecorthus.sparktraits.component.PreparefullComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

/**
 * Draws 养精蓄锐 charge percentage in the bottom-left corner.
 * 在左下角绘制养精蓄锐的蓄力百分比。
 */
public final class PreparefullHud {
    /** Full charge turns the label gold so it reads as "ready". 充满时标签转金色，读作“就绪”。 */
    private static final int CHARGING_COLOR = 0xFFE0C060;
    private static final int CHARGED_COLOR = 0xFFFFD24A;
    private static final int MARGIN = 4;

    private PreparefullHud() {
    }

    public static void render(TextRenderer renderer, ClientPlayerEntity player, DrawContext context) {
        PreparefullComponent component = PreparefullComponent.KEY.get(player);
        float progress = component.getProgress();
        if (progress <= 0.0F) {
            return;
        }
        Text label = Text.translatable("hud.sparktraits.preparefull", component.percent());
        // Bottom-left, one line above the hotbar edge. 左下角，热键栏上沿之上留一行。
        int y = context.getScaledWindowHeight() - 40;
        context.drawTextWithShadow(renderer, label, MARGIN, y, component.isCharged() ? CHARGED_COLOR : CHARGING_COLOR);
    }
}
