package dev.caecorthus.sparktraits.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pixel geometry only; kept identical in the two independently installable mods. */
public final class InventoryCardLayout {
    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean intersects(Rect other) {
            return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }
    /** section=-1 denotes a compact summary of all sections; index=-1 denotes section overflow. */
    public record Row(int section, int index, int y, int height, int omitted, boolean heading) {}
    /**
     * {@code padding} is the vertical padding; sections are separated by their header rule, so separators stay empty.
     * padding 仅为上下内边距；分节由标题刻线区分，因此 separators 始终为空。
     */
    public record Layout(Rect bounds, int padding, List<Row> rows, List<Integer> separators) {
        public Layout { rows = List.copyOf(rows); separators = List.copyOf(separators); }
    }
    /**
     * contentWidth: stable content width measured from reserves (never live labels), so the rect cannot jitter
     * with the skill state. narrowWidth: the column right of the hotbar strip for the narrow retry (≤0 = none).
     * fillSlot: take the whole slot width (legacy placement, or a native card that shares the column).
     * 内容宽度取自预留值而非实时文本，避免卡片随状态抖动；narrowWidth 为热栏右侧的窄列宽度；fillSlot 占满整列。
     */
    public record CardMetrics(int contentWidth, int narrowWidth, boolean fillSlot) {
        public static CardMetrics slotWide() { return new CardMetrics(0, 0, true); }
    }
    private static final int SECTION_GAP = 6;
    private InventoryCardLayout() {}

    public static Layout arrange(int width, int height, int fontHeight, List<List<Integer>> sections, List<Rect> controls) {
        return arrange(width, height, fontHeight, sections, controls, false, false);
    }

    /**
     * Legacy overloads: slot-wide width; the top, padding and section gap follow the current rules.
     * 旧接口仅保持整列宽度；顶边、内边距与分节间距按现行规则。
     */
    public static Layout arrange(int width, int height, int fontHeight, List<List<Integer>> sections, List<Rect> controls,
                                 boolean traitsPresent, boolean ownsTraits) {
        return arrange(width, height, fontHeight, sections, CardMetrics.slotWide(), controls, traitsPresent, ownsTraits);
    }

    public static Layout arrange(int width, int height, int fontHeight, List<List<Integer>> sections, CardMetrics metrics,
                                 List<Rect> controls, boolean traitsPresent, boolean ownsTraits) {
        boolean compatibilityFallback = traitsPresent && !ownsTraits;
        int margin = Math.min(8, Math.max(0, Math.min(width, height) / 4));
        int slotWidth = Math.min(Math.max(0, width - margin * 2), Math.min(176, Math.max(64, width / 4 - 8)));
        // Explicit min/max, never a library clamp: the lower bound exceeds the upper one when the slot is under 96 px.
        // 显式 min/max，不用库函数钳制：列宽小于 96 时下界大于上界，钳制函数会抛异常。
        int cardWidth = metrics.fillSlot() ? slotWidth
                : Math.min(slotWidth, Math.max(Math.min(96, slotWidth), metrics.contentWidth() + 14));
        int slotX = Math.max(0, width - margin - slotWidth);
        int x;
        if (compatibilityFallback) {
            // Native Traits may fill the entire height of its slot. Use the slot's left neighbour (not the
            // content-fit card's), not a guessed vertical offset or the top-left occupied by legacy Traits.
            // Tiny windows shrink to the remaining strip; unreadably small viewports cannot fit two native cards.
            // 原生天赋卡可能占满整列高度：回退卡锚定在整列左侧而非内容宽度卡片左侧。
            int right = Math.max(0, slotX - 4);
            cardWidth = Math.min(cardWidth, right);
            x = right - cardWidth;
        } else x = Math.max(0, width - margin - cardWidth);
        // The top is the screen margin; in game the HUD band arrives as a control and pushes it to y = 20.
        // 顶边即屏幕边距；游戏内 HUD 顶带作为障碍物传入，把卡片推到 y = 20。
        int top = Math.min(margin, Math.max(0, height - margin - fontHeight));
        int bottom = Math.max(0, height - margin);
        Layout best = sweep(x, cardWidth, top, bottom, fontHeight, sections, controls, compatibilityFallback);
        if (!compatibilityFallback && metrics.narrowWidth() > 0 && shown(best) < entryCount(sections)) {
            // Narrow retry: a card that cannot show everything in its column (hotbar strip, heads, logo) tries the
            // column right of the strip. The fallback card keeps its slot anchor instead.
            // 窄列重试：整列放不下全部条目时，尝试热栏右侧的窄列；回退卡保持锚定整列。
            int narrow = Math.min(cardWidth, Math.max(Math.min(56, metrics.narrowWidth()), metrics.narrowWidth()));
            if (narrow < cardWidth) {
                Layout retry = sweep(Math.max(0, width - margin - narrow), narrow, top, bottom, fontHeight, sections, controls, false);
                if (shown(retry) > shown(best)) best = retry;
            }
        }
        return best;
    }

    private static Layout sweep(int x, int cardWidth, int top, int bottom, int fontHeight, List<List<Integer>> sections,
                                List<Rect> controls, boolean compatibilityFallback) {
        int cardLeft = x;
        int cardRight = x + cardWidth;
        List<Rect> blockers = controls.stream()
                .filter(control -> control.width() > 0 && control.height() > 0
                        && cardLeft < control.right() && cardRight > control.x())
                .sorted(Comparator.comparingInt(Rect::y)).toList();
        Layout best = null;
        int start = top;
        for (Rect control : blockers) {
            int stop = Math.min(bottom, control.y() - 4);
            if (stop > start) {
                Layout candidate = layoutAt(x, start, cardWidth, stop - start, fontHeight, sections, compatibilityFallback);
                if (better(candidate, best)) best = candidate;
            }
            start = Math.max(start, Math.min(bottom, control.bottom() + 4));
        }
        Layout candidate = layoutAt(x, start, cardWidth, Math.max(0, bottom - start), fontHeight, sections, compatibilityFallback);
        if (better(candidate, best)) best = candidate;
        return best;
    }

    private static long shown(Layout layout) { return layout.rows().stream().filter(row -> row.index() >= 0).count(); }
    private static int entryCount(List<List<Integer>> sections) { return sections.stream().mapToInt(List::size).sum(); }

    private static boolean better(Layout candidate, Layout current) {
        if (current == null) return true;
        long shown = shown(candidate);
        long previous = shown(current);
        if (shown != previous) return shown > previous;
        if (shown == 0 && candidate.bounds().height() != current.bounds().height()) {
            return candidate.bounds().height() > current.bounds().height();
        }
        return false;
    }

    private static Layout layoutAt(int x, int y, int cardWidth, int available, int fontHeight,
                                   List<List<Integer>> sections, boolean compatibilityFallback) {
        int padding = Math.min(6, Math.min(cardWidth / 4, available / 4));
        int line = Math.max(1, fontHeight) + 2;
        List<Integer> nonempty = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) if (!sections.get(i).isEmpty()) nonempty.add(i);
        if (nonempty.isEmpty() || cardWidth == 0 || available == 0) {
            return new Layout(new Rect(x, y, cardWidth, 0), padding, List.of(), List.of());
        }
        int minimum = padding * 2 + nonempty.size() * line * 2 + (nonempty.size() - 1) * SECTION_GAP;
        if (available < minimum || cardWidth - 2 * padding < 32) {
            // Preserve a readable summary before spending scarce height on decoration.
            padding = Math.min(padding, Math.max(0, (available - line) / 2));
            if (compatibilityFallback) padding = Math.min(padding, Math.max(0, (cardWidth - 24) / 2));
            int rowHeight = Math.min(line, Math.max(0, available - padding * 2));
            int count = sections.stream().mapToInt(List::size).sum();
            return new Layout(new Rect(x, y, cardWidth, padding * 2 + rowHeight), padding,
                    List.of(new Row(-1, -1, y + padding, rowHeight, count, false)), List.of());
        }
        List<Row> rows = new ArrayList<>();
        int cursor = y + padding;
        int bottom = y + available - padding;
        for (int n = 0; n < nonempty.size(); n++) {
            int section = nonempty.get(n);
            if (n > 0) cursor += SECTION_GAP;
            rows.add(new Row(section, -1, cursor, line, 0, true));
            cursor += line;
            int remainingSections = nonempty.size() - n - 1;
            int limit = bottom - remainingSections * (line * 2 + SECTION_GAP);
            List<Integer> entries = sections.get(section);
            for (int i = 0; i < entries.size(); i++) {
                int entryHeight = Math.max(1, entries.get(i)) * line;
                int overflowReserve = i < entries.size() - 1 ? line : 0;
                if (cursor + entryHeight + overflowReserve > limit) {
                    rows.add(new Row(section, -1, cursor, line, entries.size() - i, false));
                    cursor += line;
                    break;
                }
                rows.add(new Row(section, i, cursor, entryHeight, 0, false));
                cursor += entryHeight;
            }
        }
        return new Layout(new Rect(x, y, cardWidth, cursor + padding - y), padding, rows, List.of());
    }
}
