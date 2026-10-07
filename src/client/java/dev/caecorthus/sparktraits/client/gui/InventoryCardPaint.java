package dev.caecorthus.sparktraits.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Harpy Express dark tokens and primitives, kept identical in SparkWitch and SparkTraits (package aside) and
 * value-identical to SparkAssist's ExpressPalette/ExpressPaint. Pure DrawContext fills: no textures, no namespace.
 * Composite primitives batch their own fills with {@code context.draw(Runnable)}; text is never drawn inside a
 * batch, because batched vertices are flushed per render layer rather than in call order.
 * 哈比特快暗色令牌与绘制原语：两个模组逐字节一致（仅包名不同），与 SparkAssist 的 ExpressPalette/ExpressPaint 同值。
 * 纯 fill 绘制，无贴图、无命名空间。组合原语自行批量提交填充；文字从不放进批次（批次按渲染层而非调用顺序提交）。
 */
public final class InventoryCardPaint {
    private InventoryCardPaint() {}

    // ---- tokens (spec-final §3.1, dark subset) ----
    public static final int SHADOW = 0x66000000, EDGE = 0xFF0B0402, RIM_HI = 0xFF5A2D19, RIM_LO = 0xFF2C1204, RIM_MITER = 0xFF43200F;
    public static final int BRASS_LO = 0xFF815A15, BRASS = 0xFFA58224, BRASS_HI = 0xFFC5A244, POLISHED = 0xFFD4AF37, COIN = 0xFFFFBF49;
    public static final int BODY = 0xFF1C0C05, WELL = 0xFF0C0502, WELL_SHADE = 0xFF050200, HOVER = 0x16FFBF49, TIP_BG = 0xFF160902;
    public static final int TEXT = 0xFFEFE2C8, TEXT_HI = 0xFFFFF7E6, MUTED = 0xFFB4A080, FAINT = 0xFF9A8565, TIP_DESC = 0xFFCDBB9C;
    public static final int HEADING = BRASS_HI, TITLE = POLISHED, MANA = 0xFFD6B0FF, GLYPH = 0xFFFFFFFF;
    public static final int READY_TEXT = 0xFFA9E98C, ACTIVE_TEXT = 0xFFFFD68A, COOL_TEXT = 0xFFE3CC94, ALERT_TEXT = 0xFFF2838B;
    public static final int VELVET = 0xFFDC001E, VELVET_LO = 0xFF8A1B29;
    private static final String ELLIPSIS = "…";
    private static final TextColor GLYPH_COLOR = TextColor.fromRgb(GLYPH & 0xFFFFFF);

    /**
     * Submits fill-only work as one draw call. DrawContext.draw(Runnable) is deprecated in 1.21.1 but is what
     * vanilla's own tooltip path uses; without it every fill flushes separately (hundreds of calls per card).
     * 仅用于纯填充的批量提交；1.21.1 中该方法标记为弃用，但原版提示框同样使用它。
     */
    @SuppressWarnings("deprecation")
    public static void batch(DrawContext c, Runnable fills) { c.draw(fills); }

    // ---- primitives (spec-final §3.4); fill(x1, y1, x2, y2) has exclusive x2/y2 ----
    public static void roundedFill(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x + 1, y, x + w - 1, y + 1, col); c.fill(x, y + 1, x + w, y + h - 1, col); c.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
    }

    public static void roundedOutline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x + 1, y, x + w - 1, y + 1, col); c.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
        c.fill(x, y + 1, x + 1, y + h - 1, col); c.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }

    /** L-shaped drop shadow; the corner pixel stays empty so every corner cut survives. */
    public static void dropShadow(DrawContext c, int x, int y, int w, int h) {
        c.fill(x + w, y + 2, x + w + 1, y + h, SHADOW);
        c.fill(x + 2, y + h, x + w, y + h + 1, SHADOW);
    }

    private static void bevel(DrawContext c, int x, int y, int w, int h) {
        c.fill(x + 1, y + 1, x + w - 1, y + 2, RIM_HI); c.fill(x + 1, y + 2, x + 2, y + h - 1, RIM_HI);
        c.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, RIM_LO); c.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, RIM_LO);
        c.fill(x + w - 2, y + 1, x + w - 1, y + 2, RIM_MITER); c.fill(x + 1, y + h - 2, x + 2, y + h - 1, RIM_MITER);
    }

    private static void ring(DrawContext c, int x, int y, int w, int h, int top, int bottom) {
        c.fill(x + 2, y + 2, x + w - 2, y + 3, top); c.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, bottom);
        c.fillGradient(x + 2, y + 3, x + 3, y + h - 3, top, bottom);
        c.fillGradient(x + w - 3, y + 3, x + w - 2, y + h - 3, top, bottom);
    }

    /** Mahogany + brass hairline panel; content area (x+3, y+3)..(x+w-3, y+h-3). */
    public static void panel(DrawContext c, int x, int y, int w, int h) {
        batch(c, () -> {
            dropShadow(c, x, y, w, h);
            roundedOutline(c, x, y, w, h, EDGE);
            c.fill(x + 1, y + 1, x + w - 1, y + h - 1, BODY);
            bevel(c, x, y, w, h);
            ring(c, x, y, w, h, BRASS_HI, BRASS_LO);
        });
    }

    /**
     * Section header on dark (row 11): brass label without shadow, engraved rule, right tail in its own styles.
     * The rule is drawn only with a label and only when at least 12 px long. Returns the label's end x.
     * Whether the tail and the rule show is decided on {@code tailReserve} (never below the live width), so neither
     * flickers when the live tail changes width (mana 99 → 100); the rule still ends 4 px before the live tail.
     * 尾注与刻线是否绘制按尾注预留宽度（不小于实时宽度）决定，魔力位数变化时不会闪烁；刻线仍画到实时尾注前 4 像素。
     */
    public static int sectionHeader(DrawContext c, TextRenderer f, String label, int x, int y, int x2,
                                    @Nullable StringVisitable tail, int tailReserve, boolean tailShadow) {
        int tw = tail == null ? 0 : f.getWidth(tail);
        boolean showTail = tail != null && f.getWidth(label) + 4 + Math.max(tw, tailReserve) <= x2 - x;
        int end = x - 4;
        if (!label.isEmpty()) {
            String shown = ellipsize(f, label, x2 - x);
            c.drawText(f, shown, x, y + 1, HEADING, false);
            end = x + f.getWidth(shown);
        }
        int ruleEnd = x2;
        if (showTail) { iconText(c, f, tail, x2 - tw, y + 1, GLYPH, tailShadow); ruleEnd = x2 - tw - 4; }
        int ruleStart = end + 4, ruleStop = ruleEnd;
        if (headerRuleShown(!label.isEmpty(), end, x2, showTail, tw, tailReserve)) {
            batch(c, () -> { c.fill(ruleStart, y + 5, ruleStop, y + 6, BRASS_LO); c.fill(ruleStart, y + 6, ruleStop, y + 7, EDGE); });
        }
        return end;
    }

    /**
     * The header rule shows only with a label and when [labelEnd + 4, x2 - max(tail, reserve) - 4) is >= 12 px.
     * 仅在有标题且刻线（按预留宽度计算）不短于 12 像素时绘制。
     */
    static boolean headerRuleShown(boolean hasLabel, int labelEnd, int x2, boolean tailShown, int tailWidth, int tailReserve) {
        int fitEnd = tailShown ? x2 - Math.max(tailWidth, tailReserve) - 4 : x2;
        return hasLabel && fitEnd - (labelEnd + 4) >= 12;
    }

    // ---- icons: procedural 1 px stroke family (spec-final §3.4 icon table) ----
    /** Identity gem 5x5: bezel (corners cut), 3x3 identity, glint top-left, shade bottom-right. */
    public static void gem(DrawContext c, int x, int y, int rgb, boolean bright) {
        int id = 0xFF000000 | rgb, bezel = bright ? BRASS_HI : BRASS_LO;
        batch(c, () -> {
            c.fill(x + 1, y, x + 4, y + 1, bezel); c.fill(x + 1, y + 4, x + 4, y + 5, bezel);
            c.fill(x, y + 1, x + 1, y + 4, bezel); c.fill(x + 4, y + 1, x + 5, y + 4, bezel);
            c.fill(x + 1, y + 1, x + 4, y + 4, id);
            c.fill(x + 1, y + 1, x + 2, y + 2, mix(id, 0xFFFFFFFF, 0.45));
            c.fill(x + 3, y + 3, x + 4, y + 4, mix(id, 0xFF000000, 0.35));
        });
    }

    /** '#' = one pixel; horizontal runs are merged into single fills (same pixels, fewer quads). */
    public static void bitmap(DrawContext c, int x, int y, int col, String... rows) {
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int k = 0;
            while (k < row.length()) {
                if (row.charAt(k) != '#') { k++; continue; }
                int start = k;
                while (k < row.length() && row.charAt(k) == '#') k++;
                c.fill(x + start, y + r, x + k, y + r + 1, col);
            }
        }
    }

    public static void check(DrawContext c, int x, int y, int col) { bitmap(c, x, y, col, "....#", "...#.", "#.#..", ".#...", "....."); }
    public static void play(DrawContext c, int x, int y, int col) { bitmap(c, x, y, col, "#..", "##.", "###", "##.", "#.."); }
    public static void hourglass(DrawContext c, int x, int y, int col) { bitmap(c, x, y, col, "#####", ".#.#.", "..#..", ".###.", "#####"); }
    public static void padlock(DrawContext c, int x, int y, int col, int hole) {
        bitmap(c, x, y, col, ".###.", ".#.#.", ".#.#.", "#####", "#####", "#####", "#####");
        c.fill(x + 2, y + 4, x + 3, y + 6, hole);
    }
    public static void diamond(DrawContext c, int x, int y, int col) { bitmap(c, x, y, col, "..#..", ".###.", "#####", ".###.", "..#.."); }
    public static void diamondOutline(DrawContext c, int x, int y, int col) { bitmap(c, x, y, col, "..#..", ".#.#.", "#...#", ".#.#.", "..#.."); }

    // ---- status pill / gauge / pips (spec-final §3.5, §3.6) ----
    public static boolean sunken(InventoryInfoCard.Kind kind) {
        return kind == InventoryInfoCard.Kind.COOLDOWN || kind == InventoryInfoCard.Kind.LOCKED;
    }

    public static int kindText(InventoryInfoCard.Kind kind) {
        return switch (kind) {
            case READY -> READY_TEXT;
            case ACTIVE -> ACTIVE_TEXT;
            case COOLDOWN -> COOL_TEXT;
            case LOCKED -> MUTED;
            case NO_MANA -> ALERT_TEXT;
        };
    }

    /** Icon advance inside a pill; the mana glyph is measured because its advance comes from the font. */
    public static int iconWidth(InventoryInfoCard.Kind kind, ToIntFunction<StringVisitable> width) {
        return switch (kind) {
            case ACTIVE -> 3;
            case NO_MANA -> width.applyAsInt(StringVisitable.plain("\uE782")) - 1;
            default -> 5;
        };
    }

    /** 3 + icon + 2 + label + 3. */
    public static int pillWidth(InventoryInfoCard.Kind kind, ToIntFunction<StringVisitable> width, StringVisitable label) {
        return 3 + iconWidth(kind, width) + 2 + width.applyAsInt(label) + 3;
    }

    /**
     * Status pill, 11 tall, FIXED width w (the entry's reserve), content centred. Raised tag (READY / ACTIVE /
     * NO_MANA) = a small Wathe price tag; sunken slot (COOLDOWN / LOCKED) = a groove. Each kind has its own glyph,
     * so it survives grayscale and colour-vision deficiency. Only ACTIVE animates (ring hue, not alpha).
     * 状态牌：凸起（可用/生效/魔力不足）或凹槽（冷却/锁定），每种状态都有独立图形，灰度与色弱下仍可辨认。
     */
    public static void pill(DrawContext c, TextRenderer f, InventoryInfoCard.Kind kind, StringVisitable label,
                            int x, int y, int w, long timeMs) {
        int iconW = iconWidth(kind, f::getWidth);
        int cx = x + (w - (iconW + 2 + f.getWidth(label))) / 2;
        boolean sunken = sunken(kind);
        int bg = sunken ? WELL : TIP_BG;
        batch(c, () -> {
            if (sunken) {
                roundedFill(c, x, y, w, 11, WELL);
                c.fill(x + 1, y, x + w - 1, y + 1, WELL_SHADE);
                c.fill(x + 1, y + 10, x + w - 1, y + 11, RIM_HI);
            } else {
                int top = switch (kind) {
                    case NO_MANA -> VELVET;
                    case ACTIVE -> mix(BRASS_HI, COIN, 0.5 + 0.5 * Math.sin(2 * Math.PI * (timeMs % 1600L) / 1600.0));
                    default -> BRASS_HI;
                };
                int bottom = switch (kind) { case NO_MANA -> VELVET_LO; case ACTIVE -> BRASS; default -> BRASS_LO; };
                roundedFill(c, x, y, w, 11, TIP_BG);
                c.fill(x + 1, y, x + w - 1, y + 1, top); c.fill(x + 1, y + 10, x + w - 1, y + 11, bottom);
                c.fillGradient(x, y + 1, x + 1, y + 10, top, bottom); c.fillGradient(x + w - 1, y + 1, x + w, y + 10, top, bottom);
            }
            switch (kind) {
                case READY -> check(c, cx, y + 3, READY_TEXT);
                case ACTIVE -> play(c, cx, y + 3, COIN);
                case COOLDOWN -> hourglass(c, cx, y + 3, BRASS);
                case LOCKED -> padlock(c, cx, y + 2, MUTED, bg);
                case NO_MANA -> { }
            }
        });
        if (kind == InventoryInfoCard.Kind.NO_MANA) c.drawText(f, "\uE782", cx, y + 2, GLYPH, false);
        c.drawText(f, Language.getInstance().reorder(label), cx + iconW + 2, y + 2, kindText(kind), false);
    }

    /** 3 px track + 1 px lit lip below; READY is a flat charged rail without sheen. */
    public static void gauge(DrawContext c, int x, int y, int w, float progress, InventoryInfoCard.Kind kind) {
        if (w <= 0) return;
        batch(c, () -> {
            c.fill(x, y, x + w, y + 3, WELL);
            c.fill(x, y + 3, x + w, y + 4, RIM_HI);
            if (kind == InventoryInfoCard.Kind.READY) { c.fill(x, y, x + w, y + 3, BRASS_LO); return; }
            int col = switch (kind) { case ACTIVE -> COIN; case NO_MANA -> MANA; default -> BRASS_HI; };
            int fw = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * w);
            if (fw > 0) {
                c.fill(x, y, x + fw, y + 3, col);
                c.fill(x, y, x + fw, y + 1, mix(col, 0xFFFFFFFF, 0.35));
                c.fill(x, y + 2, x + fw, y + 3, mix(col, 0xFF000000, 0.35));
            }
        });
    }

    /** n 5x5 diamonds on a 7 px pitch; filled = on, outline = pending. Returns the drawn width. */
    public static int pips(DrawContext c, int x, int y, int n, int on, int onColor, int offColor) {
        batch(c, () -> {
            for (int i = 0; i < n; i++) {
                if (i < on) diamond(c, x + i * 7, y, onColor); else diamondOutline(c, x + i * 7, y, offColor);
            }
        });
        return n * 7 - 2;
    }

    /** Vanilla TooltipBackgroundRenderer geometry in Wathe colours; (x, y, w, h) is the content box. */
    public static void brassTooltip(DrawContext c, int x, int y, int w, int h) {
        batch(c, () -> {
            c.fill(x - 3, y - 4, x + w + 3, y - 3, TIP_BG); c.fill(x - 3, y + h + 3, x + w + 3, y + h + 4, TIP_BG);
            c.fill(x - 3, y - 3, x + w + 3, y + h + 3, TIP_BG);
            c.fill(x - 4, y - 3, x - 3, y + h + 3, TIP_BG); c.fill(x + w + 3, y - 3, x + w + 4, y + h + 3, TIP_BG);
            c.fillGradient(x - 3, y - 2, x - 2, y + h + 2, BRASS_HI, BRASS_LO);
            c.fillGradient(x + w + 2, y - 2, x + w + 3, y + h + 2, BRASS_HI, BRASS_LO);
            c.fill(x - 3, y - 3, x + w + 3, y - 2, BRASS_HI); c.fill(x - 3, y + h + 2, x + w + 3, y + h + 3, BRASS_LO);
        });
    }

    // ---- text helpers ----
    /** Private-use glyphs are font icons (Wathe coin U+E781, Witch mana U+E782). 私用区字符为字体图标。 */
    public static boolean iconGlyph(int codePoint) { return codePoint >= 0xE000 && codePoint <= 0xF8FF; }

    /**
     * Spec-final §2.5: brass, mana and glyph colours draw without shadow on dark; text tokens keep theirs. Header
     * tails rely on this: a FAINT count keeps its shadow, a MANA/white mana tail draws without one.
     * 暗底上黄铜、魔力与图标色不带阴影，文字令牌保留阴影；标题尾注依赖此规则（FAINT 计数有阴影，魔力尾注无阴影）。
     */
    public static boolean shadowed(@Nullable TextColor color) {
        if (color == null) return false;
        int rgb = color.getRgb() & 0xFFFFFF;
        return rgb == (TEXT & 0xFFFFFF) || rgb == (TEXT_HI & 0xFFFFFF) || rgb == (MUTED & 0xFFFFFF)
                || rgb == (FAINT & 0xFFFFFF) || rgb == (TIP_DESC & 0xFFFFFF) || rgb == (ALERT_TEXT & 0xFFFFFF);
    }

    /**
     * Draws styled text run by run: icon glyphs untinted and without shadow (their native pixels are the art),
     * every other run with its own style colour (default {@code color}) and the requested shadow. Returns the width.
     * 逐段绘制：图标字形保持原色且无阴影，其余文字使用自身样式颜色与所请求的阴影。
     */
    public static int iconText(DrawContext c, TextRenderer f, StringVisitable text, int x, int y, int color, boolean shadow) {
        int[] cursor = {x};
        StringBuilder run = new StringBuilder();
        Style[] runStyle = {Style.EMPTY};
        boolean[] runIcon = {false};
        Runnable flush = () -> {
            if (run.isEmpty()) return;
            boolean icon = runIcon[0];
            Style style = icon ? runStyle[0].withColor(GLYPH_COLOR) : runStyle[0];
            OrderedText ordered = OrderedText.styledForwardsVisitedString(run.toString(), style);
            c.drawText(f, ordered, cursor[0], y, icon ? GLYPH : color, !icon && shadow);
            cursor[0] += f.getWidth(ordered);
            run.setLength(0);
        };
        text.visit((style, string) -> {
            string.codePoints().forEach(cp -> {
                boolean icon = iconGlyph(cp);
                if (!run.isEmpty() && (icon != runIcon[0] || !style.equals(runStyle[0]))) flush.run();
                runStyle[0] = style;
                runIcon[0] = icon;
                run.appendCodePoint(cp);
            });
            return Optional.empty();
        }, Style.EMPTY);
        flush.run();
        return cursor[0] - x;
    }

    /** Wraps a tooltip line so icon glyphs keep their native colours (Wathe draws its coin the same way). */
    public static OrderedText untintIcons(OrderedText line) {
        return visitor -> line.accept((index, style, cp) ->
                visitor.accept(index, iconGlyph(cp) ? style.withColor(GLYPH_COLOR) : style, cp));
    }

    /** Plain-string ellipsis ("…"); styles are dropped because card labels are drawn in token colours. */
    public static String ellipsize(TextRenderer f, String text, int width) {
        if (f.getWidth(text) <= width) return text;
        int suffix = f.getWidth(ELLIPSIS);
        if (suffix > width) return "";
        return f.trimToWidth(text, width - suffix) + ELLIPSIS;
    }

    /**
     * Per-channel ARGB lerp with rounding, identical to the mockup renderer and SparkAssist's ExpressPaint.mix
     * (ColorHelper floors instead); keep the rounding in step with Assist.
     * 逐通道四舍五入插值，与模型渲染器及 SparkAssist 的 ExpressPaint.mix 取整一致（ColorHelper 为向下取整）。
     */
    public static int mix(int a, int b, double t) {
        int alpha = (int) Math.round((a >>> 24) * (1 - t) + (b >>> 24) * t);
        int red = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int green = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int blue = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

}
