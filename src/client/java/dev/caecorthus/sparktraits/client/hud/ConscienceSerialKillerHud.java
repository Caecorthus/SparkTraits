package dev.caecorthus.sparktraits.client.hud;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

/** Renders the Conscience Serial Killer clue without exposing a player name.
 *  在不暴露玩家名的前提下渲染善良连环杀手的凶手身份线索。 */
public final class ConscienceSerialKillerHud {
    private static final int TEXT_COLOR = 0xFFDEF8;
    private static final int RIGHT_MARGIN = 10;
    private static final int BOTTOM_MARGIN = 34;
    private static final String MURDERER_ROLE_KEY = "gui.sparktraits.conscience_serial_killer.murderer_role";

    // The clue and its width only change with the role, language or font, so they are built once per change.
    // 线索文本及其宽度只随职业、语言或字体变化，因此仅在变化时构建一次。
    private static Identifier cachedRole;
    private static Language cachedLanguage;
    private static TextRenderer cachedRenderer;
    private static boolean cachedForceUnicodeFont;
    private static boolean cachedJapaneseGlyphVariants;
    private static Text cachedText;
    private static int cachedWidth;

    private ConscienceSerialKillerHud() {
    }

    public static void render(TextRenderer renderer, ClientPlayerEntity player, DrawContext context) {
        Identifier murdererRole = TraitPlayerComponent.KEY.get(player).getSerialKillerMurdererRole();
        if (murdererRole == null) {
            return;
        }
        Language language = Language.getInstance();
        GameOptions options = MinecraftClient.getInstance().options;
        boolean forceUnicodeFont = options.getForceUnicodeFont().getValue();
        boolean japaneseGlyphVariants = options.getJapaneseGlyphVariants().getValue();
        if (!murdererRole.equals(cachedRole)
                || language != cachedLanguage
                || renderer != cachedRenderer
                || forceUnicodeFont != cachedForceUnicodeFont
                || japaneseGlyphVariants != cachedJapaneseGlyphVariants) {
            cachedText = Text.translatable(MURDERER_ROLE_KEY,
                    Text.translatable("announcement.role." + murdererRole.getPath()));
            cachedWidth = renderer.getWidth(cachedText);
            cachedRole = murdererRole;
            cachedLanguage = language;
            cachedRenderer = renderer;
            cachedForceUnicodeFont = forceUnicodeFont;
            cachedJapaneseGlyphVariants = japaneseGlyphVariants;
        }
        int x = context.getScaledWindowWidth() - cachedWidth - RIGHT_MARGIN;
        int y = context.getScaledWindowHeight() - BOTTOM_MARGIN;
        context.drawTextWithShadow(renderer, cachedText, Math.max(RIGHT_MARGIN, x), y, TEXT_COLOR);
    }
}
