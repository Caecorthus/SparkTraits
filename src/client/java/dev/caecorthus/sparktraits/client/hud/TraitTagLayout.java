package dev.caecorthus.sparktraits.client.hud;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry for the trait tags under Wathe's crosshair name plate, in physical framebuffer pixels so every
 * hairline lands on whole pixels at any GUI scale. Text keeps Wathe's 0.6 scale; the chrome unit is that scale
 * rounded to whole pixels, so borders match the glyph stroke without blurring.
 * 准星名牌下方天赋标签的纯几何计算，单位为帧缓冲物理像素，任意界面缩放下描边都落在整像素上。
 * 文字沿用 Wathe 的 0.6 缩放；装饰单位为该缩放取整后的像素数，描边粗细与字形笔画一致且不发虚。
 */
final class TraitTagLayout {
    /** RoleNameRenderer's text scale. Wathe 名牌文字缩放。 */
    static final float TEXT_SCALE = 0.6f;
    /** Glyph rows of one font line, without the line gap. 单行字形高度（不含行距）。 */
    static final int GLYPH_ROWS = 8;

    private TraitTagLayout() {
    }

    /**
     * Per-scale sizes: {@code px} physical pixels per text unit, {@code unit} the whole-pixel chrome unit,
     * {@code height} of one tag.
     * 按缩放计算的尺寸：每文字单位的物理像素、整像素装饰单位与单个标签高度。
     */
    record Metrics(double px, int unit, int glyphs, int height) {
        static Metrics of(double guiScale) {
            double px = TEXT_SCALE * guiScale;
            int unit = Math.max(1, (int) Math.round(px));
            int glyphs = (int) Math.round(GLYPH_ROWS * px);
            // border, pad, glyphs, pad, border 描边、留白、字形、留白、描边
            return new Metrics(px, unit, glyphs, 4 * unit + glyphs);
        }

        /** A tag's width for text {@code advance} units wide (the font's trailing 1-unit spacing is dropped). */
        int tagWidth(int advance) {
            return 13 * unit + textWidth(advance);
        }

        int textWidth(int advance) {
            return (int) Math.round(Math.max(0, advance - 1) * px);
        }

        /** Gap between tags in a row and between rows. 标签与行之间的间距。 */
        int gap() {
            return 3 * unit;
        }
    }

    /** One tag: outer box, the 5-unit gem's top-left and the text origin. 单个标签：外框、宝石左上角与文字起点。 */
    record Tag(int x, int y, int width, int height, int gemX, int gemY, int textX, int textY) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }
    }

    /**
     * Lays tags out in centred rows starting at {@code top}, wrapping greedily before a row would pass {@code maxRow}.
     * A tag wider than {@code maxRow} takes a row of its own; names are never cut.
     * 自 top 起按行居中排列，行宽将超过 maxRow 时换行；单个超宽标签独占一行，名称从不截断。
     */
    static List<Tag> arrange(Metrics metrics, int[] advances, int centerX, int top, int maxRow) {
        List<Tag> tags = new ArrayList<>(advances.length);
        int unit = metrics.unit(), height = metrics.height(), gap = metrics.gap();
        int start = 0, y = top;
        while (start < advances.length) {
            int end = start, rowWidth = 0;
            while (end < advances.length) {
                int width = metrics.tagWidth(advances[end]);
                int next = end == start ? width : rowWidth + gap + width;
                if (end > start && next > maxRow) break;
                rowWidth = next;
                end++;
            }
            int x = centerX - rowWidth / 2;
            for (int i = start; i < end; i++) {
                int width = metrics.tagWidth(advances[i]);
                tags.add(new Tag(x, y, width, height,
                        x + 3 * unit, y + (height - 5 * unit) / 2,
                        x + 10 * unit, y + 2 * unit));
                x += width + gap;
            }
            y += height + gap;
            start = end;
        }
        return tags;
    }
}
