package dev.caecorthus.sparktraits.client.gui;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/** Owner inventory only. No input handlers; tooltips deliberately avoid vanilla's z=400 path. */
public final class InventoryInfoCard {
    /** Skill state. Shape and glyph encode it as well as colour (spec-final §3.5). 技能状态：形状与图形同样编码状态。 */
    public enum Kind { READY, ACTIVE, COOLDOWN, LOCKED, NO_MANA }

    /**
     * Hero (skill) status. {@code widest}/{@code widestShort} are per-skill reserve labels for every kind at the
     * widest countdown: the pill and the card width are measured from them, never from the live label, so the rect
     * stays identical in every state. {@code progress} is already oriented (ACTIVE drains, COOLDOWN fills, NO_MANA
     * = mana / cost). LOCKED draws {@code pips} task pips with {@code pipsOn} done; ACTIVE with {@code charges >= 0}
     * draws {@code pips} (3 when unset) charge pips with {@code charges} lit before the bar. {@code cost} is shown
     * only for READY and NO_MANA; its own styles win, {@code costShort} picks the fallback colour.
     * 技能状态：宽度按预留文本测量（从不使用实时文本），卡片在所有状态下尺寸一致。
     */
    public record Status(Kind kind, Text label, Text shortLabel, Map<Kind, Text> widest, Map<Kind, Text> widestShort,
                         float progress, int pips, int pipsOn, int charges, @Nullable Text cost, boolean costShort) {
        public Status {
            label = copyText(label);
            shortLabel = copyText(shortLabel);
            widest = copyTexts(widest);
            widestShort = copyTexts(widestShort);
            cost = cost == null ? null : copyText(cost);
        }
    }

    /**
     * Trait rows: {@code lines = [name]}. Hero rows: {@code lines = [name, status.label()]} plus a status, so the
     * layout's line-count contract (2 lines = 22 px) is unchanged. {@code accent} is the identity RGB for the gem.
     * 天赋行只有名称；技能行为名称 + 状态（两行 22 像素）；accent 为宝石的身份色。
     */
    public record Entry(List<Text> lines, List<Text> tooltip, int accent, @Nullable Status status) {
        public Entry { lines = copies(lines); tooltip = copies(tooltip); }
        /** Plain entry; the accent comes from the first line's style colour (white when unstyled). */
        public Entry(List<Text> lines, List<Text> tooltip) { this(lines, tooltip, accentOf(lines), null); }
    }

    /**
     * {@code tail} is drawn right-aligned in the header; the header ladder and the card width are decided on
     * {@code tailReserve} (never the live tail). {@code shortTitle} replaces the title on narrow cards.
     * 标题尾注右对齐；标题降级与卡片宽度按预留宽度决定，从不按实时数值。
     */
    public record Section(Text title, List<Entry> entries, @Nullable Text tail, @Nullable Text tailReserve, Text shortTitle) {
        public Section {
            title = copyText(title);
            entries = List.copyOf(entries);
            tail = tail == null ? null : copyText(tail);
            tailReserve = tailReserve == null ? null : copyText(tailReserve);
            shortTitle = shortTitle == null ? title : copyText(shortTitle);
        }
        /** Count tail in FAINT with a three-digit reserve; the short title is the title. 计数尾注，三位数预留。 */
        public Section(Text title, List<Entry> entries) {
            this(title, entries, Text.literal(String.valueOf(entries.size())).withColor(InventoryCardPaint.FAINT & 0xFFFFFF),
                    Text.literal("888"), title);
        }
    }

    /** {@code tooltipBlockers}: the price tags, the only z >= 300 draws made before the card tooltip. 价签为唯一遮挡物。 */
    public record Snapshot(List<Section> sections, InventoryCardLayout.Layout layout, List<InventoryCardLayout.Rect> tooltipBlockers) {
        public Snapshot { sections = List.copyOf(sections); tooltipBlockers = List.copyOf(tooltipBlockers); }
        public Snapshot(List<Section> sections, InventoryCardLayout.Layout layout) { this(sections, layout, List.of()); }
    }

    private record TipBlock(Text text, int color, boolean shadow, int gapBefore) {}

    private static final int PAD_X = 7, LINE = 11;
    private static final Kind[] KINDS = Kind.values();
    /** Closing punctuation (spec-final §2.4 tokens) that must not be stranded by a wrap. 换行时不可落单的收尾标点。 */
    private static final String CLOSING = "，。、；：？！）」』》】〉…—,.;:?!)％";
    private InventoryInfoCard() {}
    private static List<Text> copies(List<Text> texts) { return texts.stream().map(InventoryInfoCard::copyText).toList(); }
    private static Map<Kind, Text> copyTexts(@Nullable Map<Kind, Text> texts) {
        if (texts == null || texts.isEmpty()) return Map.of();
        Map<Kind, Text> copy = new EnumMap<>(Kind.class);
        texts.forEach((kind, text) -> copy.put(kind, copyText(text)));
        return Map.copyOf(copy);
    }
    private static int accentOf(List<Text> lines) {
        TextColor color = lines.isEmpty() ? null : lines.getFirst().getStyle().getColor();
        return color == null ? 0xFFFFFF : color.getRgb();
    }

    /** Text.copy() only copies the sibling list, not the mutable sibling texts or translation args. */
    public static Text copyText(Text text) {
        MutableText copy;
        if (text.getContent() instanceof net.minecraft.text.TranslatableTextContent translation) {
            Object[] args = translation.getArgs().clone();
            for (int i = 0; i < args.length; i++) if (args[i] instanceof Text nested) args[i] = copyText(nested);
            copy = Text.translatableWithFallback(translation.getKey(), translation.getFallback(), args);
        } else copy = text.copyContentOnly();
        copy.setStyle(text.getStyle());
        for (Text sibling : text.getSiblings()) copy.append(copyText(sibling));
        return copy;
    }

    public static Snapshot prepare(Screen screen, TextRenderer font, List<Section> sections) {
        return prepare(screen, font, sections, false, false);
    }

    /** Content-fit card in the right column (or beside the native Traits slot when the handoff failed). */
    public static Snapshot prepare(Screen screen, TextRenderer font, List<Section> sections, boolean traitsPresent, boolean ownsTraits) {
        return prepare(screen, font, sections, false, traitsPresent, ownsTraits);
    }

    /**
     * Native Traits card. {@code fillSlot} makes it take its whole slot, so a Witch fallback card anchored to the
     * slot sits flush beside it; the caller decides (the shared code never names the other mod).
     * 原生天赋卡：fillSlot 时占满整列，使魔女回退卡紧贴其左侧；由调用方决定，共享代码不引用另一个模组。
     */
    public static Snapshot prepareNative(Screen screen, TextRenderer font, List<Section> sections, boolean fillSlot) {
        return prepare(screen, font, sections, fillSlot, false, false);
    }

    private static Snapshot prepare(Screen screen, TextRenderer font, List<Section> sections, boolean fillSlot,
                                    boolean traitsPresent, boolean ownsTraits) {
        List<InventoryCardLayout.Rect> controls = new ArrayList<>();
        List<InventoryCardLayout.Rect> priceTags = new ArrayList<>();
        int manaPriceWidth = -1;
        for (var child : screen.children()) {
            if (child instanceof ClickableWidget widget && widget.visible) {
                if (widget instanceof LimitedInventoryScreen.StoreItemWidget store) {
                    // Wathe draws "price+coin"; mana-priced shops relabel entries "N ✦", so reserve "888 ✦" too.
                    // Wathe 价签为“价格+金币”；魔力定价的商店会改写为“N ✦”，因此同时预留“888 ✦”的宽度。
                    if (manaPriceWidth < 0) manaPriceWidth = font.getWidth("888 \uE782");
                    int priceWidth = Math.max(font.getWidth(Text.literal(store.entry.price() + "\uE781")), manaPriceWidth);
                    var price = HoveredTooltipPositioner.INSTANCE.getPosition(screen.width, screen.height,
                            widget.getX() - 4 - priceWidth / 2, widget.getY() - 9, priceWidth, 8);
                    int left = Math.min(widget.getX() - 7, price.x() - 4) - 2;
                    int top = Math.min(widget.getY() - 7, price.y() - 4) - 2;
                    int right = Math.max(widget.getX() + 23, price.x() + priceWidth + 4) + 2;
                    int bottom = Math.max(widget.getY() + 23, price.y() + 12) + 2;
                    controls.add(new InventoryCardLayout.Rect(left, top, right - left, bottom - top));
                    priceTags.add(new InventoryCardLayout.Rect(price.x() - 4, price.y() - 4, priceWidth + 8, 16));
                } else if (widget.getWidth() == 16 && widget.getHeight() == 16) {
                    // 16x16 target heads draw a 30x30 shop-slot frame (+7 halo), then 2 px of padding.
                    // 16x16 目标头像会绘制 30x30 边框（外扩 7），再留 2 像素。
                    controls.add(new InventoryCardLayout.Rect(widget.getX() - 9, widget.getY() - 9, 34, 34));
                } else {
                    controls.add(new InventoryCardLayout.Rect(widget.getX() - 2, widget.getY() - 2,
                            widget.getWidth() + 4, widget.getHeight() + 4));
                }
            }
        }
        // Mirrors Wathe 1.5.6: the HUD band (money, timer, first task line end at y = 15), LimitedHandledScreen's
        // 176x32 hotbar strip, and LimitedInventoryScreen.drawBackground's logo transform (0.28 scale).
        // 对应 Wathe 1.5.6：HUD 顶带、LimitedHandledScreen 的 176x32 热栏框、drawBackground 的标志变换。
        controls.add(new InventoryCardLayout.Rect(0, 0, screen.width, 16));
        controls.add(new InventoryCardLayout.Rect((screen.width - 176) / 2, (screen.height - 32) / 2, 176, 32));
        controls.add(new InventoryCardLayout.Rect((int) Math.floor(screen.width / 2.0 - 69.4),
                (int) Math.floor(screen.height - 99.96), 140, 72));
        List<Section> nonempty = sections.stream().filter(s -> !s.entries().isEmpty()).toList();
        var metrics = new InventoryCardLayout.CardMetrics(contentWidth(font::getWidth, nonempty),
                screen.width - 8 - ((screen.width + 176) / 2 + 4), fillSlot);
        return new Snapshot(nonempty, InventoryCardLayout.arrange(screen.width, screen.height, font.fontHeight,
                lineCounts(nonempty), metrics, controls, traitsPresent, ownsTraits), priceTags);
    }

    private static List<List<Integer>> lineCounts(List<Section> sections) {
        return sections.stream().map(s -> s.entries().stream()
                .map(e -> Math.max(e.lines().size(), e.status() != null ? 2 : 1)).toList()).toList();
    }

    /**
     * Stable content width (spec-final §6.4) from reserves only. The overflow label ("+N more") is not measured:
     * it is always narrower than the 82 px floor, and whether a row overflows is only known after the layout.
     * 稳定内容宽度（仅用预留值）。溢出行文本始终窄于 82 像素下限，且是否溢出要到排版后才知道，因此不测量。
     */
    static int contentWidth(ToIntFunction<StringVisitable> width, List<Section> sections) {
        int content = 0;
        for (Section section : sections) {
            int header = width.applyAsInt(plain(section.title()));
            if (section.tail() != null) {
                header += 4 + 12 + 4 + width.applyAsInt(section.tailReserve() != null ? section.tailReserve() : section.tail());
            }
            content = Math.max(content, header);
            for (Entry entry : section.entries()) {
                if (entry.lines().isEmpty()) continue;
                Status status = entry.status();
                if (status != null) {
                    int pill = pillReserve(status, width, false);
                    content = Math.max(content, 8 + width.applyAsInt(plain(entry.lines().getFirst())) + 6 + pill);
                    if (status.cost() != null) content = Math.max(content, 8 + width.applyAsInt(status.cost()) + 5 + pill);
                } else {
                    for (Text line : entry.lines()) content = Math.max(content, 8 + width.applyAsInt(plain(line)));
                }
            }
        }
        return content;
    }

    /**
     * The fixed pill width: the widest reserve label over every kind, plus the live label as a safety net (if the
     * reserves are ever wrong, the card grows instead of the label overflowing its pill).
     * 状态牌固定宽度：各状态预留标签的最大宽度，并以实时标签兜底（预留值有误时卡片变宽，而不是文字溢出）。
     */
    static int pillReserve(Status status, ToIntFunction<StringVisitable> width, boolean shortLabels) {
        Map<Kind, Text> widest = shortLabels ? status.widestShort() : status.widest();
        int reserve = InventoryCardPaint.pillWidth(status.kind(), width, shortLabels ? status.shortLabel() : status.label());
        for (Kind kind : KINDS) {
            Text label = widest.get(kind);
            if (label != null) reserve = Math.max(reserve, InventoryCardPaint.pillWidth(kind, width, label));
        }
        return reserve;
    }

    private static StringVisitable plain(Text text) { return StringVisitable.plain(text.getString()); }

    /**
     * Display rule 7a (spec-final §2.4): remove the ASCII space between a digit and a following ideograph
     * ("100 金币" → "100金币"), across styled runs. The space before a number stays, so it remains the break point.
     * 显示规则 7a：删除数字与其后汉字之间的半角空格（跨样式段），数字前的空格保留为换行点。
     */
    static Text glueUnits(Text text) {
        List<Style> styles = new ArrayList<>();
        List<String> runs = new ArrayList<>();
        text.visit((style, string) -> {
            if (!string.isEmpty()) { styles.add(style); runs.add(string); }
            return Optional.empty();
        }, Style.EMPTY);
        int[] codePoints = String.join("", runs).codePoints().toArray();
        boolean[] drop = new boolean[codePoints.length];
        boolean changed = false;
        for (int i = 1; i + 1 < codePoints.length; i++) {
            int previous = codePoints[i - 1], next = codePoints[i + 1];
            if (codePoints[i] == ' ' && previous >= '0' && previous <= '9'
                    && ((next >= 0x3400 && next <= 0x9FFF) || (next >= 0xF900 && next <= 0xFAFF))) {
                drop[i] = true;
                changed = true;
            }
        }
        if (!changed) return text;
        MutableText glued = Text.empty();
        int index = 0;
        for (int r = 0; r < runs.size(); r++) {
            StringBuilder kept = new StringBuilder();
            int count = runs.get(r).codePointCount(0, runs.get(r).length());
            for (int j = 0; j < count; j++, index++) if (!drop[index]) kept.appendCodePoint(codePoints[index]);
            if (!kept.isEmpty()) glued.append(Text.literal(kept.toString()).setStyle(styles.get(r)));
        }
        return glued;
    }

    public static void draw(DrawContext context, TextRenderer font, Snapshot snapshot, int mouseX, int mouseY, int width, int height) {
        var layout = snapshot.layout();
        var box = layout.bounds();
        if (box.width() < 6 || box.height() < 6) return;
        InventoryCardLayout.Row hovered = null;
        long now = Util.getMeasuringTimeMs();
        context.getMatrices().push();
        try {
            context.getMatrices().translate(0, 0, 100);
            InventoryCardPaint.panel(context, box.x(), box.y(), box.width(), box.height());
            int x0 = box.x() + PAD_X, x1 = box.right() - PAD_X;
            context.enableScissor(box.x(), box.y(), box.right(), box.bottom());
            try {
                for (var row : layout.rows()) {
                    // Headings are not hoverable; every other row gets the wash and its tooltip. 标题行不可悬停。
                    boolean hover = !row.heading() && mouseX >= box.x() && mouseX < box.right()
                            && mouseY >= row.y() && mouseY < row.y() + row.height();
                    if (hover) {
                        context.fill(box.x() + 4, row.y(), box.right() - 4, row.y() + row.height(), InventoryCardPaint.HOVER);
                        hovered = row;
                    }
                    if (row.section() < 0) compactRow(context, font, snapshot, x0, row.y(), now);
                    else if (row.heading()) header(context, font, snapshot.sections().get(row.section()), x0, x1, row.y());
                    else if (row.omitted() > 0) overflowRow(context, font, row, x0, hover);
                    else entryRow(context, font, snapshot.sections().get(row.section()).entries().get(row.index()), x0, x1, row.y(), hover, now);
                }
            } finally { context.disableScissor(); }
        } finally { context.getMatrices().pop(); }
        if (hovered != null) drawTooltip(context, font, snapshot, hovered, width, height);
    }

    /** Narrow ladder decided on the tail RESERVE: full title + tail → short title + tail → tail only. */
    private static void header(DrawContext context, TextRenderer font, Section section, int x0, int x1, int y) {
        Text tail = section.tail();
        int reserve = tail == null ? 0 : font.getWidth(section.tailReserve() != null ? section.tailReserve() : tail);
        String label = section.title().getString();
        if (tail != null && font.getWidth(label) + 4 + reserve > x1 - x0) label = section.shortTitle().getString();
        if (tail != null && font.getWidth(label) + 4 + reserve > x1 - x0) label = "";
        boolean tailShadow = tail != null && InventoryCardPaint.shadowed(tail.getStyle().getColor());
        InventoryCardPaint.sectionHeader(context, font, label, x0, y, x1, tail, reserve, tailShadow);
    }

    private static void entryRow(DrawContext context, TextRenderer font, Entry entry, int x0, int x1, int y, boolean hover, long now) {
        InventoryCardPaint.gem(context, x0, y + 3, entry.accent(), hover);
        if (entry.status() != null && !entry.lines().isEmpty()) {
            hero(context, font, entry.lines().getFirst().getString(), entry.status(), x0, x1, y, now);
            return;
        }
        // Names draw from their plain string in TEXT; a style colour is only the gem accent (spec-final §3.3).
        // 名称以纯文本 TEXT 色绘制；样式颜色仅用作宝石身份色。
        for (int i = 0; i < entry.lines().size(); i++) {
            String line = InventoryCardPaint.ellipsize(font, entry.lines().get(i).getString(), x1 - (x0 + 8));
            int color = i > 0 ? InventoryCardPaint.MUTED : hover ? InventoryCardPaint.TEXT_HI : InventoryCardPaint.TEXT;
            context.drawText(font, line, x0 + 8, y + 2 + i * LINE, color, true);
        }
    }

    /**
     * Hero ladder (spec-final §6.5), decided only on the name, the reserves and the card width so it never flips
     * with the state: A full pill + gauge under it; A' short pill + gauge; B pill on line B; C short pill under the gem.
     * 技能行阶梯只取决于名称、预留宽度与卡片宽度，不随状态切换。
     */
    private static void hero(DrawContext context, TextRenderer font, String name, Status status, int x0, int x1, int y, long now) {
        ToIntFunction<StringVisitable> width = font::getWidth;
        int inner = x1 - (x0 + 8);
        int full = pillReserve(status, width, false), compact = pillReserve(status, width, true);
        int nameWidth = font.getWidth(name);
        boolean fullFits = nameWidth + 6 + full <= inner;
        if (fullFits || nameWidth + 6 + compact <= inner) {
            int pillWidth = fullFits ? full : compact, pillX = x1 - pillWidth;
            context.drawText(font, InventoryCardPaint.ellipsize(font, name, pillX - 6 - (x0 + 8)), x0 + 8, y + 2, InventoryCardPaint.TEXT_HI, true);
            InventoryCardPaint.pill(context, font, status.kind(), fullFits ? status.label() : status.shortLabel(), pillX, y, pillWidth, now);
            if (showCost(status) && 8 + font.getWidth(status.cost()) + 5 <= pillX - x0) cost(context, font, status, x0 + 8, y + 13);
            lineB(context, status, x0, pillX, pillWidth, y + LINE);
        } else if (full <= inner) {
            context.drawText(font, InventoryCardPaint.ellipsize(font, name, inner), x0 + 8, y + 2, InventoryCardPaint.TEXT_HI, true);
            InventoryCardPaint.pill(context, font, status.kind(), status.label(), x1 - full, y + LINE, full, now);
            if (showCost(status) && font.getWidth(status.cost()) + 6 <= inner - full) cost(context, font, status, x0 + 8, y + 13);
        } else {
            context.drawText(font, InventoryCardPaint.ellipsize(font, name, inner), x0 + 8, y + 2, InventoryCardPaint.TEXT_HI, true);
            InventoryCardPaint.pill(context, font, status.kind(), status.shortLabel(), x0, y + LINE, Math.min(compact, x1 - x0), now);
        }
    }

    /** Cost is hidden while ACTIVE, COOLDOWN or LOCKED. 生效、冷却、锁定时隐藏消耗。 */
    private static boolean showCost(Status status) {
        return status.cost() != null && (status.kind() == Kind.READY || status.kind() == Kind.NO_MANA);
    }

    private static void cost(DrawContext context, TextRenderer font, Status status, int x, int y) {
        int color = status.costShort() ? InventoryCardPaint.ALERT_TEXT : InventoryCardPaint.MUTED;
        InventoryCardPaint.iconText(context, font, status.cost(), x, y, color, true);
    }

    /** Death-ray bar floor beside the charge pips; below it the pips move under the name. 射线进度条的最小宽度。 */
    static final int MIN_CHARGE_BAR = 24;

    /**
     * Line B under the pill: task pips (LOCKED), charge pips + bar (ACTIVE with charges), else the gauge. When the
     * pill is too narrow for pips and a readable bar (the short A' pill), the pips take the cost slot under the name,
     * which ACTIVE leaves empty, and the bar keeps the full pill width.
     * 状态牌下方一行：锁定为任务点，带发数的生效为发数点 + 进度条，否则为进度条。状态牌过窄（A' 短牌）时，
     * 发数点移到名称下方的消耗位（生效时该位置为空），进度条保持整个状态牌宽度。
     */
    private static void lineB(DrawContext context, Status status, int x0, int pillX, int pillWidth, int y) {
        switch (status.kind()) {
            case LOCKED -> {
                int span = status.pips() * 7 - 2;
                InventoryCardPaint.pips(context, pillX + (pillWidth - span) / 2, y + 3, status.pips(), status.pipsOn(),
                        InventoryCardPaint.BRASS_HI, InventoryCardPaint.BRASS_LO);
            }
            case ACTIVE -> {
                if (status.charges() >= 0) {
                    int n = status.pips() > 0 ? status.pips() : 3, span = n * 7 - 2;
                    if (chargePipsUnderName(pillWidth, span, pillX - x0)) {
                        InventoryCardPaint.pips(context, x0 + 8, y + 3, n, status.charges(), InventoryCardPaint.COIN, InventoryCardPaint.BRASS_LO);
                        InventoryCardPaint.gauge(context, pillX, y + 5, pillWidth, status.progress(), Kind.ACTIVE);
                    } else {
                        int drawn = InventoryCardPaint.pips(context, pillX, y + 3, n, status.charges(),
                                InventoryCardPaint.COIN, InventoryCardPaint.BRASS_LO);
                        InventoryCardPaint.gauge(context, pillX + drawn + 3, y + 5, pillWidth - drawn - 3, status.progress(), Kind.ACTIVE);
                    }
                } else InventoryCardPaint.gauge(context, pillX, y + 5, pillWidth, status.progress(), Kind.ACTIVE);
            }
            default -> InventoryCardPaint.gauge(context, pillX, y + 5, pillWidth, status.progress(), status.kind());
        }
    }

    /**
     * True when the bar beside the pips would be under 24 px and the pips fit left of the pill (8 + span + 5).
     * 发数点旁的进度条不足 24 像素、且状态牌左侧放得下发数点时为真。
     */
    static boolean chargePipsUnderName(int pillWidth, int pipSpan, int roomLeftOfPill) {
        return pillWidth - pipSpan - 3 < MIN_CHARGE_BAR && 8 + pipSpan + 5 <= roomLeftOfPill;
    }

    private static void overflowRow(DrawContext context, TextRenderer font, InventoryCardLayout.Row row, int x0, boolean hover) {
        int y = row.y();
        InventoryCardPaint.batch(context, () -> {
            for (int k = 0; k < 3; k++) context.fill(x0 + 1 + k * 2, y + 6, x0 + 2 + k * 2, y + 7, InventoryCardPaint.FAINT);
        });
        int color = hover ? InventoryCardPaint.TEXT_HI : InventoryCardPaint.FAINT;
        context.drawText(font, Text.translatable("gui.sparktraits.inventory.more", row.omitted()), x0 + 8, y + 2, color, true);
    }

    /** One-row summary "技能 [short pill] · 天赋 3": the skill state survives compaction. 压缩摘要保留技能状态。 */
    private static void compactRow(DrawContext context, TextRenderer font, Snapshot snapshot, int x0, int y, long now) {
        int cursor = x0;
        for (int s = 0; s < snapshot.sections().size(); s++) {
            Section section = snapshot.sections().get(s);
            if (s > 0) {
                context.drawText(font, " · ", cursor, y + 2, InventoryCardPaint.FAINT, true);
                cursor += font.getWidth(" · ");
            }
            String title = section.shortTitle().getString();
            context.drawText(font, title, cursor, y + 2, InventoryCardPaint.HEADING, false);
            cursor += font.getWidth(title) + 3;
            Entry hero = section.entries().stream().filter(e -> e.status() != null).findFirst().orElse(null);
            if (hero != null) {
                int pillWidth = pillReserve(hero.status(), font::getWidth, true);
                InventoryCardPaint.pill(context, font, hero.status().kind(), hero.status().shortLabel(), cursor, y, pillWidth, now);
                cursor += pillWidth;
            } else {
                String count = String.valueOf(section.entries().size());
                context.drawText(font, count, cursor, y + 2, InventoryCardPaint.FAINT, true);
                cursor += font.getWidth(count);
            }
        }
    }

    /** Overflow and compact tooltips: section titles plus one name line per omitted entry, no descriptions. */
    private static List<TipBlock> omitted(Snapshot snapshot, InventoryCardLayout.Row row) {
        List<TipBlock> tooltip = new ArrayList<>();
        for (int s = 0; s < snapshot.sections().size(); s++) {
            if (row.section() >= 0 && row.section() != s) continue;
            Section section = snapshot.sections().get(s);
            tooltip.add(new TipBlock(Text.literal(section.title().getString()), InventoryCardPaint.HEADING, false, tooltip.isEmpty() ? 0 : 2));
            int start = row.section() < 0 ? 0 : Math.max(0, section.entries().size() - row.omitted());
            for (int i = start; i < section.entries().size(); i++) {
                Entry entry = section.entries().get(i);
                if (entry.lines().isEmpty()) continue;
                tooltip.add(new TipBlock(Text.literal(entry.lines().getFirst().getString()), InventoryCardPaint.TEXT, true, 0));
            }
        }
        return tooltip;
    }

    /**
     * Brass tooltip left of the card at z=300 (below Assist's modal and Wathe's z=400 price tags), never scaled.
     * Content-top candidates: the hovered label's row, above the first price tag it would hit, below the price band;
     * only price tags block it. 黄铜提示框位于卡片左侧 z=300，绝不缩放；仅价签视为遮挡，依次尝试三个候选位置。
     */
    private static void drawTooltip(DrawContext context, TextRenderer font, Snapshot snapshot, InventoryCardLayout.Row row,
                                    int width, int height) {
        if (width < 8 || height < 8) return;
        List<TipBlock> blocks;
        Integer accent = null;
        if (row.section() < 0 || row.omitted() > 0) blocks = omitted(snapshot, row);
        else {
            Entry entry = snapshot.sections().get(row.section()).entries().get(row.index());
            blocks = new ArrayList<>();
            int count = entry.tooltip().size(), status = statusLine(entry);
            for (int i = 0; i < count; i++) {
                // +2 after the title, +2 before the status line. 标题后与状态行前各留 2 像素。
                int gap = i > 0 && (i == 1 || i == status) ? 2 : 0;
                blocks.add(new TipBlock(entry.tooltip().get(i), i == 0 ? InventoryCardPaint.TEXT_HI : InventoryCardPaint.TIP_DESC, true, gap));
            }
            accent = entry.accent();
        }
        if (blocks.isEmpty()) return;
        var box = snapshot.layout().bounds();
        int wrapWidth = tooltipWrapWidth(width, box.x());
        List<OrderedText> lines = new ArrayList<>();
        List<TipBlock> owners = new ArrayList<>();
        List<Integer> gaps = new ArrayList<>();
        for (TipBlock block : blocks) {
            Text text = glueUnits(block.text());
            List<OrderedText> wrapped = font.wrapLines(text, wrapWidth);
            wrapped = avoidStrandedPunctuation(wrapped, narrower -> font.wrapLines(text, narrower), wrapWidth);
            for (int k = 0; k < wrapped.size(); k++) {
                lines.add(InventoryCardPaint.untintIcons(wrapped.get(k)));
                owners.add(block);
                gaps.add(k == 0 ? block.gapBefore() : 0);
            }
        }
        if (lines.isEmpty()) return;
        int tipWidth = 0, tipHeight = 0;
        for (int i = 0; i < lines.size(); i++) {
            tipWidth = Math.max(tipWidth, font.getWidth(lines.get(i)) + (i == 0 && accent != null ? 8 : 0));
            tipHeight += gaps.get(i) + (i == lines.size() - 1 ? 9 : LINE);
        }
        int x = Math.max(6, box.x() - 8 - tipWidth);
        int y = tooltipTop(x, row.y() + 2, tipWidth, tipHeight, snapshot.tooltipBlockers(), height);
        context.getMatrices().push();
        try {
            // Vanilla drawTooltip internally adds 400. Drawing wrapped text ourselves keeps BOTH
            // background and glyphs below Assist's modal layer, including the default text offset.
            context.getMatrices().translate(x, y, 300);
            InventoryCardPaint.brassTooltip(context, 0, 0, tipWidth, tipHeight);
            int lineY = 0;
            for (int i = 0; i < lines.size(); i++) {
                lineY += gaps.get(i);
                int lineX = 0;
                if (i == 0 && accent != null) { InventoryCardPaint.gem(context, 0, lineY + 1, accent, false); lineX = 8; }
                context.drawText(font, lines.get(i), lineX, lineY, owners.get(i).color(), owners.get(i).shadow());
                lineY += LINE;
            }
        } finally { context.getMatrices().pop(); }
    }

    /**
     * Index of a hero tooltip's status line. The presenter appends extra facts AFTER the shared status line: one
     * mana line in NO_MANA and one charges line when {@code charges >= 0}. Other tooltips end with their status.
     * 技能提示的状态行下标：展示方在共享状态行之后追加附加信息（魔力不足时的魔力行、有发数时的发数行）；其余提示以末行为状态行。
     */
    static int statusLine(Entry entry) {
        Status status = entry.status();
        int extras = status == null ? 0 : (status.kind() == Kind.NO_MANA ? 1 : 0) + (status.charges() >= 0 ? 1 : 0);
        return entry.tooltip().size() - 1 - extras;
    }

    /**
     * Wrap width: vanilla's 200 capped by the screen, and by the room left of the card when that room is usable
     * (>= 80 px), so on narrow screens the tooltip ends 4 px left of its card instead of covering the hovered row.
     * The room keeps the 6 px screen margin, the 4 px clearance + 4 px frame and the title gem's 8 px.
     * 换行宽度：原版 200 并受屏幕宽度限制；卡片左侧空间可用（≥ 80）时再受其限制，窄屏下提示框不再遮挡所悬停的行。
     */
    static int tooltipWrapWidth(int screenWidth, int cardX) {
        int wrap = Math.min(200, screenWidth - 16);
        int room = cardX - 8 - 6 - 8;
        if (room >= 80) wrap = Math.min(wrap, room);
        return Math.max(1, wrap);
    }

    /**
     * Post-pass over the vanilla wrap (the pinned {@code font.wrapLines} stays the wrapper): if punctuation is
     * stranded ({@link #strandsPunctuation}), re-wrap up to three glyph advances (9 px each) narrower so the glyphs
     * before it move down too ("…降低理" / "智。" instead of a lone "。"). Keeps the original wrap if none helps.
     * 避免标点落单：若换行后标点落单，则逐次缩窄一个字宽（9 像素，最多三次）重新换行，让前面的字随标点下移；都无效时保持原样。
     */
    static List<OrderedText> avoidStrandedPunctuation(List<OrderedText> wrapped, IntFunction<List<OrderedText>> rewrap, int wrapWidth) {
        if (!strandsPunctuation(wrapped)) return wrapped;
        for (int narrower = wrapWidth - 9; narrower >= wrapWidth - 27 && narrower >= 36; narrower -= 9) {
            List<OrderedText> retry = rewrap.apply(narrower);
            if (!strandsPunctuation(retry)) return retry;
        }
        return wrapped;
    }

    /**
     * True when a line after the first starts with closing punctuation ("。") or is a single ideograph followed only
     * by closing punctuation ("响。"). 首行之后任一行以收尾标点开头，或只有一个汉字加收尾标点时为真。
     */
    static boolean strandsPunctuation(List<OrderedText> lines) {
        StringBuilder line = new StringBuilder();
        for (int k = 1; k < lines.size(); k++) {
            line.setLength(0);
            lines.get(k).accept((index, style, codePoint) -> { line.appendCodePoint(codePoint); return true; });
            if (line.isEmpty()) continue;
            int first = line.codePointAt(0), rest = Character.charCount(first);
            if (CLOSING.indexOf(first) >= 0) return true;
            if (first >= 0x2E80 && rest < line.length() && line.codePoints().skip(1).allMatch(cp -> CLOSING.indexOf(cp) >= 0)) return true;
        }
        return false;
    }

    private static int tooltipTop(int x, int preferred, int tipWidth, int tipHeight, List<InventoryCardLayout.Rect> blockers, int height) {
        InventoryCardLayout.Rect firstHit = null;
        int bandBottom = Integer.MIN_VALUE;
        var outer = new InventoryCardLayout.Rect(x - 4, preferred - 4, tipWidth + 8, tipHeight + 8);
        for (var tag : blockers) {
            if (outer.intersects(tag) && (firstHit == null || tag.y() < firstHit.y())) firstHit = tag;
            bandBottom = Math.max(bandBottom, tag.bottom());
        }
        int[] candidates = {preferred, firstHit == null ? preferred : firstHit.y() - 8 - tipHeight,
                blockers.isEmpty() ? preferred : bandBottom + 6};
        int y = preferred;
        for (int candidate : candidates) {
            if (candidate < 6) continue;
            var rect = new InventoryCardLayout.Rect(x - 4, candidate - 4, tipWidth + 8, tipHeight + 8);
            if (blockers.stream().noneMatch(rect::intersects)) { y = candidate; break; }
        }
        return Math.max(6, Math.min(y, height - tipHeight - 6));
    }
}
