package dev.caecorthus.sparktraits.client.hud;

import dev.caecorthus.sparktraits.client.gui.InventoryCardPaint;
import dev.caecorthus.sparktraits.client.text.TraitClientTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

import java.util.List;

/**
 * Trait tags under Wathe's crosshair name plate: centred rows of small Harpy Express price tags (brass hairline, a
 * band in the trait colour holding a gem in the colour of the faction the trait belongs to, name in TEXT). The
 * inventory trait card keeps its trait-colour gem.
 * 准星名牌下方的天赋标签：居中排列的哈比特快小价签（黄铜描边、天赋色色条内嵌所属阵营色宝石、TEXT 色名称）。
 * 背包天赋卡仍使用天赋色宝石。
 */
public final class TraitNameplateTags {
    /**
     * Rows start 3 text units under the last line Wathe 1.5.6 RoleNameRenderer draws, in its 0.6-scaled space
     * (origin at the screen centre + 6): a player's name at 16 and cohort tip at 20 + font height, a body's death
     * info at 32.
     * 标签行起于 Wathe 1.5.6 RoleNameRenderer 最后一行文字下方 3 个文字单位（0.6 缩放空间，原点为屏幕中心下移 6）：
     * 玩家名在 16、同伙提示在 20 + 字高，尸体死亡信息在 32。
     */
    private static final int PLAYER_NAME_Y = 16, BODY_DEATH_INFO_Y = 32, LINE_GAP = 3;
    /** Widest row in text units before tags wrap. 换行前的最大行宽（文字单位）。 */
    private static final int MAX_ROW_UNITS = 200;

    private TraitNameplateTags() {
    }

    /** One cached layout per line, since the player and body lines can both show in one frame. 玩家行与尸体行各缓存一份布局。 */
    private static final LayoutCache PLAYER_LAYOUT = new LayoutCache();
    private static final LayoutCache BODY_LAYOUT = new LayoutCache();

    public static void renderUnderPlayer(DrawContext context, TextRenderer font, List<Identifier> traits, boolean cohortLine, float alpha) {
        int lastLine = cohortLine ? 20 + font.fontHeight : PLAYER_NAME_Y;
        render(PLAYER_LAYOUT, context, font, traits, lastLine + font.fontHeight + LINE_GAP, alpha);
    }

    public static void renderUnderBody(DrawContext context, TextRenderer font, List<Identifier> traits, float alpha) {
        render(BODY_LAYOUT, context, font, traits, BODY_DEATH_INFO_Y + font.fontHeight + LINE_GAP, alpha);
    }

    private static void render(LayoutCache cache, DrawContext context, TextRenderer font, List<Identifier> traits, int top, float alpha) {
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        Layout layout = cache.get(context, font, traits, top, guiScale);
        String[] names = layout.names();
        int[] colors = layout.colors();
        int[] factionColors = layout.factionColors();
        List<TraitTagLayout.Tag> tags = layout.tags();
        int unit = layout.unit();

        // Chrome in physical pixels; text afterwards, never inside the fill batch. 先以物理像素绘制装饰，文字在批次之后。
        context.getMatrices().push();
        context.getMatrices().scale((float) (1.0 / guiScale), (float) (1.0 / guiScale), 1.0f);
        InventoryCardPaint.batch(context, () -> {
            for (int i = 0; i < tags.size(); i++) {
                TraitTagLayout.Tag tag = tags.get(i);
                tag(context, tag, unit, colors[i], alpha);
                gem(context, tag.gemX(), tag.gemY(), unit, factionColors[i], alpha);
            }
        });
        context.getMatrices().pop();

        int text = fade(InventoryCardPaint.TEXT, alpha);
        for (int i = 0; i < tags.size(); i++) {
            TraitTagLayout.Tag tag = tags.get(i);
            context.getMatrices().push();
            context.getMatrices().translate((float) (tag.textX() / guiScale), (float) (tag.textY() / guiScale), 0.0f);
            context.getMatrices().scale(TraitTagLayout.TEXT_SCALE, TraitTagLayout.TEXT_SCALE, 1.0f);
            context.drawText(font, names[i], 0, 0, text, true);
            context.getMatrices().pop();
        }
    }

    /**
     * Resolved names, colours and tag geometry for one trait list. Everything it depends on is in the key: the list,
     * GUI scale and scaled window size, the row top, and the language and font settings that change names and widths
     * (a resource reload installs a new Language instance, so it rebuilds too). Only the fade alpha varies per frame.
     * 一个天赋列表解析后的名称、颜色与标签几何。其依赖全部在键中：列表、界面缩放与缩放后的窗口尺寸、行顶，以及影响名称与
     * 宽度的语言和字体设置（资源重载会安装新的 Language 实例，因此同样触发重建）。每帧只有淡入淡出透明度变化。
     */
    private record Layout(String[] names, int[] colors, int[] factionColors, int unit, List<TraitTagLayout.Tag> tags) {
    }

    private static final class LayoutCache {
        private List<Identifier> traits = List.of();
        private double guiScale = Double.NaN;
        private int scaledWidth = -1;
        private int scaledHeight = -1;
        private int top = Integer.MIN_VALUE;
        private Language language;
        private TextRenderer font;
        private boolean forceUnicodeFont;
        private boolean japaneseGlyphVariants;
        private Layout layout;

        Layout get(DrawContext context, TextRenderer font, List<Identifier> traits, int top, double guiScale) {
            int scaledWidth = context.getScaledWindowWidth();
            int scaledHeight = context.getScaledWindowHeight();
            Language language = Language.getInstance();
            GameOptions options = MinecraftClient.getInstance().options;
            boolean forceUnicodeFont = options.getForceUnicodeFont().getValue();
            boolean japaneseGlyphVariants = options.getJapaneseGlyphVariants().getValue();
            if (layout == null
                    || guiScale != this.guiScale
                    || scaledWidth != this.scaledWidth
                    || scaledHeight != this.scaledHeight
                    || top != this.top
                    || language != this.language
                    || font != this.font
                    || forceUnicodeFont != this.forceUnicodeFont
                    || japaneseGlyphVariants != this.japaneseGlyphVariants
                    || !traits.equals(this.traits)) {
                // Copied, so a list changed in place later still misses the cache. 复制一份，原列表之后被原地修改时仍会失效。
                this.traits = List.copyOf(traits);
                this.guiScale = guiScale;
                this.scaledWidth = scaledWidth;
                this.scaledHeight = scaledHeight;
                this.top = top;
                this.language = language;
                this.font = font;
                this.forceUnicodeFont = forceUnicodeFont;
                this.japaneseGlyphVariants = japaneseGlyphVariants;
                this.layout = build(font, this.traits, top, guiScale, scaledWidth, scaledHeight);
            }
            return layout;
        }
    }

    private static Layout build(TextRenderer font, List<Identifier> traits, int top, double guiScale, int scaledWidth, int scaledHeight) {
        TraitTagLayout.Metrics metrics = TraitTagLayout.Metrics.of(guiScale);
        String[] names = new String[traits.size()];
        int[] colors = new int[traits.size()];
        int[] factionColors = new int[traits.size()];
        int[] advances = new int[traits.size()];
        for (int i = 0; i < names.length; i++) {
            Identifier id = traits.get(i);
            // Names draw from their plain string in TEXT; the trait colour fills the band and the gem shows the faction.
            // 名称以纯文本 TEXT 色绘制；天赋色填充色条，宝石显示所属阵营。
            names[i] = TraitClientTexts.name(id).getString();
            colors[i] = TraitClientTexts.color(id);
            factionColors[i] = TraitClientTexts.factionColor(id);
            advances[i] = font.getWidth(names[i]);
        }
        int centerX = (int) Math.floor(scaledWidth * guiScale / 2.0);
        int topPx = (int) Math.round((scaledHeight / 2.0 + 6.0 + top * TraitTagLayout.TEXT_SCALE) * guiScale);
        List<TraitTagLayout.Tag> tags = TraitTagLayout.arrange(metrics, advances, centerX, topPx,
                (int) Math.round(MAX_ROW_UNITS * metrics.px()));
        return new Layout(names, colors, factionColors, metrics.unit(), tags);
    }

    /**
     * Raised price tag (InventoryCardPaint.pill's READY chrome) at chrome unit {@code u}: L-shaped drop shadow, cut
     * corners, brass rim lit on top, and a band in the trait colour {@code rgb} along the inside of the left rim.
     * Each pixel is painted once, so the fade never double-blends.
     * 凸起价签（同 InventoryCardPaint.pill 的可用态外观），按装饰单位 u 绘制：L 形投影、切角、上亮下暗的黄铜边，
     * 左描边内侧为天赋色 rgb 的色条。每个像素只绘制一次，淡入淡出时不会叠色。
     */
    private static void tag(DrawContext c, TraitTagLayout.Tag t, int u, int rgb, float alpha) {
        int x = t.x(), y = t.y(), r = t.right(), b = t.bottom();
        int shadow = fade(InventoryCardPaint.SHADOW, alpha);
        int top = fade(InventoryCardPaint.BRASS_HI, alpha), bottom = fade(InventoryCardPaint.BRASS_LO, alpha);
        c.fill(r, y + 2 * u, r + u, b, shadow);
        c.fill(x + 2 * u, b, r, b + u, shadow);
        c.fill(x + u, y + u, t.bandRight(), b - u, fade(0xFF000000 | rgb, alpha));
        c.fill(t.bandRight(), y + u, r - u, b - u, fade(InventoryCardPaint.TIP_BG, alpha));
        c.fill(x + u, y, r - u, y + u, top);
        c.fill(x + u, b - u, r - u, b, bottom);
        c.fillGradient(x, y + u, x + u, b - u, top, bottom);
        c.fillGradient(r - u, y + u, r, b - u, top, bottom);
    }

    /**
     * InventoryCardPaint.gem at chrome unit {@code u}: bezel with cut corners, 3x3 colour {@code rgb} (the trait's
     * faction here), glint and shade.
     * 按装饰单位 u 放大的身份宝石：切角宝石托、3x3 颜色 rgb（此处为天赋所属阵营色）、左上高光与右下暗部。
     */
    private static void gem(DrawContext c, int x, int y, int u, int rgb, float alpha) {
        int id = 0xFF000000 | rgb;
        int bezel = fade(InventoryCardPaint.BRASS_LO, alpha);
        c.fill(x + u, y, x + 4 * u, y + u, bezel);
        c.fill(x + u, y + 4 * u, x + 4 * u, y + 5 * u, bezel);
        c.fill(x, y + u, x + u, y + 4 * u, bezel);
        c.fill(x + 4 * u, y + u, x + 5 * u, y + 4 * u, bezel);
        // Identity in three bands so the glint and shade pixels are not painted over it.
        // 身份色分三段绘制，高光与暗部像素不与其重叠。
        int fill = fade(id, alpha);
        c.fill(x + u, y + u, x + 2 * u, y + u * 2, fade(InventoryCardPaint.mix(id, 0xFFFFFFFF, 0.45), alpha));
        c.fill(x + 2 * u, y + u, x + 4 * u, y + 2 * u, fill);
        c.fill(x + u, y + 2 * u, x + 4 * u, y + 3 * u, fill);
        c.fill(x + u, y + 3 * u, x + 3 * u, y + 4 * u, fill);
        c.fill(x + 3 * u, y + 3 * u, x + 4 * u, y + 4 * u, fade(InventoryCardPaint.mix(id, 0xFF000000, 0.35), alpha));
    }

    private static int fade(int argb, float alpha) {
        int a = Math.round((argb >>> 24) * Math.max(0.0f, Math.min(1.0f, alpha)));
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
