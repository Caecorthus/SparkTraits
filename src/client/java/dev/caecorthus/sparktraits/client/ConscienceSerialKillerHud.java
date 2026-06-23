package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Renders the Conscience Serial Killer clue without exposing a player name.
 *  在不暴露玩家名的前提下渲染善良连环杀手的凶手身份线索。 */
public final class ConscienceSerialKillerHud {
    private static final int TEXT_COLOR = 0xFFDEF8;
    private static final int RIGHT_MARGIN = 10;
    private static final int BOTTOM_MARGIN = 34;

    private ConscienceSerialKillerHud() {
    }

    public static void render(TextRenderer renderer, ClientPlayerEntity player, DrawContext context) {
        Identifier murdererRole = TraitPlayerComponent.KEY.get(player).getSerialKillerMurdererRole();
        if (murdererRole == null) {
            return;
        }
        Text text = Text.literal("凶手身份：").append(Text.translatable("announcement.role." + murdererRole.getPath()));
        int x = context.getScaledWindowWidth() - renderer.getWidth(text) - RIGHT_MARGIN;
        int y = context.getScaledWindowHeight() - BOTTOM_MARGIN;
        context.drawTextWithShadow(renderer, text, Math.max(RIGHT_MARGIN, x), y, TEXT_COLOR);
    }
}
