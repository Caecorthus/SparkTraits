package dev.caecorthus.sparktraits.client.gui;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Language;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

/** Owner inventory only. No input handlers; tooltips deliberately avoid vanilla's z=400 path. */
public final class InventoryInfoCard {
    /** Skill state. Shape and glyph encode it as well as colour (spec-final §3.5). 技能状态：形状与图形同样编码状态。 */
    public enum Kind { READY, ACTIVE, COOLDOWN, LOCKED, NO_MANA }

    /**
     * Live gauge fill, re-read every frame: a cached snapshot keeps its texts and layout while the gauge still
     * interpolates between the 1 s syncs. Returning NaN falls back to {@link Status#progress()}.
     * 实时进度：每帧重新读取，缓存的快照保留文本与排版，进度条仍在两次同步之间平滑插值；返回 NaN 时使用 progress。
     */
    @FunctionalInterface
    public interface Gauge { float progress(); }

    /**
     * Hero (skill) status. {@code widest}/{@code widestShort} are per-skill reserve labels for every kind at the
     * widest countdown: the pill and the card width are measured from them, never from the live label, so the rect
     * stays identical in every state. {@code progress} is already oriented (ACTIVE drains, COOLDOWN fills, NO_MANA
     * = mana / cost); {@code gauge}, when set, supplies it live each frame. LOCKED draws {@code pips} task pips with
     * {@code pipsOn} done (an empty track when {@code pips} is 0); ACTIVE with {@code charges >= 0} draws
     * {@code pips} (3 when unset) charge pips with {@code charges} lit before the bar. {@code cost} is shown only
     * for READY and NO_MANA; its own styles win, {@code costShort} picks the fallback colour. {@code facts} counts
     * the tooltip lines appended AFTER the status line (mana, charges, dash), so the +2 gap lands before the status.
     * 技能状态：宽度按预留文本测量（从不使用实时文本），卡片在所有状态下尺寸一致；gauge 每帧提供实时进度；
     * facts 为提示中状态行之后追加的附加行数，使 2 像素间距始终位于状态行之前。
     */
    public record Status(Kind kind, Text label, Text shortLabel, Map<Kind, Text> widest, Map<Kind, Text> widestShort,
                         float progress, int pips, int pipsOn, int charges, @Nullable Text cost, boolean costShort,
                         @Nullable Gauge gauge, int facts) {
        public Status {
            label = copyText(label);
            shortLabel = copyText(shortLabel);
            widest = copyTexts(widest);
            widestShort = copyTexts(widestShort);
            cost = cost == null ? null : copyText(cost);
            facts = Math.max(0, facts);
        }

        /**
         * Static progress; the appended facts are the presenter's legacy pair: the mana line in NO_MANA and the
         * charges line while {@code charges >= 0}. 静态进度；附加行为魔力不足时的魔力行与有发数时的发数行。
         */
        public Status(Kind kind, Text label, Text shortLabel, Map<Kind, Text> widest, Map<Kind, Text> widestShort,
                      float progress, int pips, int pipsOn, int charges, @Nullable Text cost, boolean costShort) {
            this(kind, label, shortLabel, widest, widestShort, progress, pips, pipsOn, charges, cost, costShort, null,
                    (kind == Kind.NO_MANA ? 1 : 0) + (charges >= 0 ? 1 : 0));
        }

        /** This frame's fill: the live gauge when present, else {@code progress}. 本帧填充比例。 */
        public float fill() {
            if (gauge == null) return progress;
            float live = gauge.progress();
            return Float.isNaN(live) ? progress : live;
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

    /**
     * {@code tooltipBlockers}: the price tags, the only z >= 300 draws made before the card tooltip. {@code storeFrames}:
     * the 30x30 shop-slot frames under them; a tooltip top never cuts through one (no frame sliver above the box).
     * 价签为唯一遮挡物；storeFrames 为价签下方的 30x30 商店槽边框，提示框顶边不会横切边框（不留边框碎条）。
     */
    public record Snapshot(List<Section> sections, InventoryCardLayout.Layout layout, List<InventoryCardLayout.Rect> tooltipBlockers,
                           List<InventoryCardLayout.Rect> storeFrames) {
        public Snapshot {
            sections = List.copyOf(sections);
            tooltipBlockers = List.copyOf(tooltipBlockers);
            storeFrames = List.copyOf(storeFrames);
        }
        public Snapshot(List<Section> sections, InventoryCardLayout.Layout layout, List<InventoryCardLayout.Rect> tooltipBlockers) {
            this(sections, layout, tooltipBlockers, List.of());
        }
        public Snapshot(List<Section> sections, InventoryCardLayout.Layout layout) { this(sections, layout, List.of(), List.of()); }
    }

    /**
     * Hero ladder step, shared by every hero of a section (spec-final §6.5 + spec-v2 §2): FULL = A (full pill, gauge
     * under it), SHORT = A' (short pill + gauge), BELOW = B (full pill on line B), NARROW = C (short pill under the gem).
     * 技能行阶梯（同一分节共用）：A 完整牌 + 进度条，A' 短牌 + 进度条，B 牌在第二行，C 短牌位于宝石下方。
     */
    enum Step { FULL, SHORT, BELOW, NARROW }

    /** One status column per section: the step and the pill widths every hero of that section uses. 分节状态列。 */
    record Column(Step step, int full, int compact) {
        int pill() { return step == Step.SHORT || step == Step.NARROW ? compact : full; }
    }

    /** One code point with its full style (legacy formatting codes already applied, as vanilla wraps). 码点与样式。 */
    record Cell(int codePoint, Style style) {}

    private record TipBlock(Text text, int color, boolean shadow, int gapBefore) {}
    private record Tip(List<OrderedText> lines, List<TipBlock> owners, int[] gaps, int width, int height, int x, int y, @Nullable Integer accent) {}

    private static final int PAD_X = 7, LINE = 11;
    /** Widest wrap a tall tooltip may grow to, in {@link #TOOLTIP_WRAP_STEP} steps. 过高提示可加宽到的上限及步长。 */
    static final int MAX_TOOLTIP_WRAP = 320, TOOLTIP_WRAP_STEP = 20;
    private static final Kind[] KINDS = Kind.values();
    /** Closing punctuation (spec-final §2.4 tokens) that must not start a line. 不可位于行首的收尾标点。 */
    static final String CLOSING = "，。、；：？！）」』》】〉…—,.;:?!)％”’]";
    /** Opening punctuation that sticks to the token after it. 附着后一记号的开启标点。 */
    static final String OPENING = "（「『《【〈([“‘";
    private static final String WORD_SYMBOLS = "+-.%/×÷_'#";
    private static final int BRACKET_SPAN = 8;

    // Render-thread caches, each a single entry keyed by everything its value depends on (spec-v2 §4).
    // 渲染线程缓存：各一项，键包含其值所依赖的全部输入。
    private static @Nullable Prepared prepared;
    private static @Nullable Plan plan;
    private static @Nullable TipCache tipCache;

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

    /** Drops every cached snapshot, plan and tooltip (connection reset). 清空全部缓存（连接重置时）。 */
    public static void invalidateCaches() {
        prepared = null;
        plan = null;
        tipCache = null;
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

    /**
     * The last prepared snapshot and its key: the language instance (a new one after every language change or resource
     * reload), the glyph fingerprint ({@link #glyphFingerprint()}), the font, the screen size, the widget signature, the
     * sections (identity first, then value equality) and the placement flags. A hit returns the same immutable
     * snapshot, so draw's plan and tooltip caches hit too.
     * 上次的快照及其键：语言实例（切换语言或重载资源后会变化）、字形指纹、字体、界面尺寸、控件签名、分节（先比同一性再比值）与放置标志；
     * 命中时返回同一个不可变快照，绘制计划与提示缓存随之命中。
     */
    record Prepared(Language language, int glyphs, TextRenderer font, int width, int height, int[] controls, List<Section> sections,
                    boolean fillSlot, boolean traitsPresent, boolean ownsTraits, Snapshot snapshot) {
        boolean matches(Language language, int glyphs, TextRenderer font, int width, int height, int[] controls, List<Section> sections,
                        boolean fillSlot, boolean traitsPresent, boolean ownsTraits) {
            return this.language == language && this.glyphs == glyphs && this.font == font && this.width == width
                    && this.height == height && this.fillSlot == fillSlot && this.traitsPresent == traitsPresent
                    && this.ownsTraits == ownsTraits && Arrays.equals(this.controls, controls) && this.sections.equals(sections);
        }
    }

    /**
     * Glyph-metrics fingerprint: the two font options that swap glyph providers WITHOUT a resource reload. Toggling
     * Force Unicode Font or Japanese Glyph Variants only runs MinecraftClient.onFontOptionsChanged (it re-filters the
     * font storages), so the Language instance, the TextRenderer and the screen size all stay the same while the
     * advances change; without this key the stale snapshot (widths, ellipses, price-tag rects) would be reused.
     * 字形度量指纹：两个无需重载资源即可切换字形来源的字体选项。切换“强制使用 Unicode 字体”或“日文字形变体”只会重新过滤字体存储，
     * 语言实例、字体对象与界面尺寸都不变，但字宽会变；缺少此键时会继续使用过期快照（宽度、省略号、价签矩形）。
     */
    static int glyphFingerprint() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return 0;
        return (client.options.getForceUnicodeFont().getValue() ? 1 : 0) | (client.options.getJapaneseGlyphVariants().getValue() ? 2 : 0);
    }

    private static Snapshot prepare(Screen screen, TextRenderer font, List<Section> sections, boolean fillSlot,
                                    boolean traitsPresent, boolean ownsTraits) {
        int[] signature = controlSignature(screen);
        Language language = Language.getInstance();
        int glyphs = glyphFingerprint();
        Prepared cached = prepared;
        if (cached != null && cached.matches(language, glyphs, font, screen.width, screen.height, signature, sections,
                fillSlot, traitsPresent, ownsTraits)) return cached.snapshot();
        List<InventoryCardLayout.Rect> controls = new ArrayList<>();
        List<InventoryCardLayout.Rect> priceTags = new ArrayList<>();
        List<InventoryCardLayout.Rect> storeFrames = new ArrayList<>();
        int manaPriceWidth = -1;
        for (var child : screen.children()) {
            if (child instanceof ClickableWidget widget && widget.visible) {
                if (widget instanceof LimitedInventoryScreen.StoreItemWidget store) {
                    // Wathe draws "price+coin"; mana-priced shops relabel entries "N ✦", so reserve "888 ✦" too. Keep the
                    // escapes: raw private-use glyphs are invisible in editors, and deleting one would shrink the reserve.
                    // Wathe 价签为“价格+金币”；魔力定价的商店会改写为“N ✦”，因此同时预留“888 ✦”的宽度。保留转义：私用区字符不可见，误删会缩小预留宽度。
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
                    // Wathe's shop_slot sprite: 30x30 at (X - 7, Y - 7). 商店槽边框：30x30，位于 (X - 7, Y - 7)。
                    storeFrames.add(new InventoryCardLayout.Rect(widget.getX() - 7, widget.getY() - 7, 30, 30));
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
        Snapshot snapshot = new Snapshot(nonempty, InventoryCardLayout.arrange(screen.width, screen.height, font.fontHeight,
                lineCounts(nonempty), metrics, controls, traitsPresent, ownsTraits), priceTags, storeFrames);
        prepared = new Prepared(language, glyphs, font, screen.width, screen.height, signature, List.copyOf(sections),
                fillSlot, traitsPresent, ownsTraits, snapshot);
        return snapshot;
    }

    /**
     * Cheap per-frame signature of everything the obstacle scan reads: per visible widget its kind, bounds and price.
     * 障碍扫描所读取内容的廉价签名：每个可见控件的类型、边界与价格。
     */
    static int[] controlSignature(Screen screen) {
        var children = screen.children();
        int[] signature = new int[children.size() * 6];
        int n = 0;
        for (var child : children) {
            if (!(child instanceof ClickableWidget widget) || !widget.visible) continue;
            boolean store = widget instanceof LimitedInventoryScreen.StoreItemWidget;
            signature[n++] = store ? 1 : 2;
            signature[n++] = widget.getX();
            signature[n++] = widget.getY();
            signature[n++] = widget.getWidth();
            signature[n++] = widget.getHeight();
            signature[n++] = store ? ((LimitedInventoryScreen.StoreItemWidget) widget).entry.price() : 0;
        }
        return n == signature.length ? signature : Arrays.copyOf(signature, n);
    }

    private static List<List<Integer>> lineCounts(List<Section> sections) {
        return sections.stream().map(s -> s.entries().stream()
                .map(e -> Math.max(e.lines().size(), e.status() != null ? 2 : 1)).toList()).toList();
    }

    /**
     * Stable content width (spec-final §6.4) from reserves only, with the section's status column (every hero of a
     * section uses its widest pill reserve). The overflow label ("+N more") is not measured: it is always narrower
     * than the 82 px floor, and whether a row overflows is only known after the layout.
     * 稳定内容宽度（仅用预留值，按分节状态列计算）。溢出行文本始终窄于 82 像素下限，且是否溢出要到排版后才知道，因此不测量。
     */
    static int contentWidth(ToIntFunction<StringVisitable> width, List<Section> sections) {
        int content = 0;
        for (Section section : sections) {
            int header = width.applyAsInt(plain(section.title()));
            if (section.tail() != null) {
                header += 4 + 12 + 4 + width.applyAsInt(section.tailReserve() != null ? section.tailReserve() : section.tail());
            }
            content = Math.max(content, header);
            int pill = columnPill(section, width, false);
            for (Entry entry : section.entries()) {
                if (entry.lines().isEmpty()) continue;
                Status status = entry.status();
                if (status != null) {
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

    /** The section's status-column width: the widest pill reserve of its heroes (0 without heroes). 分节状态列宽度。 */
    static int columnPill(Section section, ToIntFunction<StringVisitable> width, boolean shortLabels) {
        int pill = 0;
        for (Entry entry : section.entries()) {
            if (entry.status() != null && !entry.lines().isEmpty()) pill = Math.max(pill, pillReserve(entry.status(), width, shortLabels));
        }
        return pill;
    }

    /**
     * One status column per section (spec-v2 §2): every hero shares the pill width and the ladder step, so pills and
     * gauges line up and no row degrades on its own. Decided on names, reserves and the inner width only, never on the
     * live state, so it never flips with a countdown. Null when the section has no hero.
     * 每个分节一条状态列：所有技能行共用状态牌宽度与阶梯，状态牌与进度条对齐、不会单独降级；只取决于名称、预留宽度与内宽，
     * 不随倒计时切换。无技能行时为 null。
     */
    static @Nullable Column column(Section section, ToIntFunction<StringVisitable> width, int inner) {
        int name = -1;
        for (Entry entry : section.entries()) {
            if (entry.status() != null && !entry.lines().isEmpty()) name = Math.max(name, width.applyAsInt(plain(entry.lines().getFirst())));
        }
        if (name < 0) return null;
        int full = columnPill(section, width, false), compact = columnPill(section, width, true);
        Step step = name + 6 + full <= inner ? Step.FULL : name + 6 + compact <= inner ? Step.SHORT
                : full <= inner ? Step.BELOW : Step.NARROW;
        return new Column(step, full, compact);
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
            if (codePoints[i] == ' ' && previous >= '0' && previous <= '9' && ideograph(next)) {
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
        Plan current = plan(font, snapshot);
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
                    if (row.section() < 0) compactRow(context, font, snapshot, current, x0, row.y(), now);
                    else if (row.heading()) header(context, font, snapshot.sections().get(row.section()), current.headers[row.section()], x0, x1, row.y());
                    else if (row.omitted() > 0) overflowRow(context, font, current, row, x0, hover);
                    else entryRow(context, font, current, row.section(), row.index(), x0, x1, row.y(), hover, now);
                }
            } finally { context.disableScissor(); }
        } finally { context.getMatrices().pop(); }
        if (hovered != null) drawTooltip(context, font, snapshot, hovered, width, height);
    }

    /**
     * What draw derives from a snapshot and does not animate: each section's status column, header label and tail
     * reserve, every drawn name (already ellipsized), cost widths and the overflow labels. Built on the first frame that
     * draws a snapshot; the prepare cache hands back the same instance while nothing changed.
     * 快照中不随动画变化的绘制数据（状态列、标题、已省略的名称、消耗宽度、溢出文本）：首次绘制时计算，快照不变则复用。
     */
    private static final class Plan {
        final Snapshot snapshot;
        final TextRenderer font;
        final @Nullable Column[] columns;
        final Header[] headers;
        final String[][] names;
        final int[][] costWidths;
        final Map<Integer, Text> overflow = new HashMap<>();

        Plan(TextRenderer font, Snapshot snapshot) {
            this.snapshot = snapshot;
            this.font = font;
            var box = snapshot.layout().bounds();
            int x0 = box.x() + PAD_X, x1 = box.right() - PAD_X, inner = x1 - (x0 + 8);
            int count = snapshot.sections().size();
            columns = new Column[count];
            headers = new Header[count];
            names = new String[count][];
            costWidths = new int[count][];
            for (int s = 0; s < count; s++) {
                Section section = snapshot.sections().get(s);
                Column column = column(section, font::getWidth, inner);
                columns[s] = column;
                headers[s] = Header.of(font, section, x1 - x0);
                names[s] = new String[section.entries().size()];
                costWidths[s] = new int[section.entries().size()];
                for (int i = 0; i < section.entries().size(); i++) {
                    Entry entry = section.entries().get(i);
                    if (entry.lines().isEmpty()) { names[s][i] = ""; continue; }
                    int room = inner;
                    if (entry.status() != null && column != null && (column.step() == Step.FULL || column.step() == Step.SHORT)) {
                        room = x1 - column.pill() - 6 - (x0 + 8);
                    }
                    names[s][i] = InventoryCardPaint.ellipsize(font, entry.lines().getFirst().getString(), room);
                    Status status = entry.status();
                    costWidths[s][i] = status != null && status.cost() != null ? font.getWidth(status.cost()) : 0;
                }
            }
        }

        Text overflow(int omitted) {
            return overflow.computeIfAbsent(omitted, n -> Text.translatable("gui.sparktraits.inventory.more", n));
        }
    }

    /** Header ladder resolved on the tail RESERVE: full title + tail → short title + tail → tail only. 标题降级。 */
    private record Header(String label, int reserve, boolean tailShadow) {
        static Header of(TextRenderer font, Section section, int room) {
            Text tail = section.tail();
            int reserve = tail == null ? 0 : font.getWidth(section.tailReserve() != null ? section.tailReserve() : tail);
            String label = section.title().getString();
            if (tail != null && font.getWidth(label) + 4 + reserve > room) label = section.shortTitle().getString();
            if (tail != null && font.getWidth(label) + 4 + reserve > room) label = "";
            return new Header(label, reserve, tail != null && InventoryCardPaint.shadowed(tail.getStyle().getColor()));
        }
    }

    private static Plan plan(TextRenderer font, Snapshot snapshot) {
        Plan current = plan;
        if (current == null || current.snapshot != snapshot || current.font != font) plan = current = new Plan(font, snapshot);
        return current;
    }

    private static void header(DrawContext context, TextRenderer font, Section section, Header header, int x0, int x1, int y) {
        InventoryCardPaint.sectionHeader(context, font, header.label(), x0, y, x1, section.tail(), header.reserve(), header.tailShadow());
    }

    private static void entryRow(DrawContext context, TextRenderer font, Plan plan, int section, int index, int x0, int x1,
                                 int y, boolean hover, long now) {
        Entry entry = plan.snapshot.sections().get(section).entries().get(index);
        InventoryCardPaint.gem(context, x0, y + 3, entry.accent(), hover);
        Column column = plan.columns[section];
        if (entry.status() != null && !entry.lines().isEmpty() && column != null) {
            hero(context, font, plan.names[section][index], plan.costWidths[section][index], entry.status(), column, x0, x1, y, now);
            return;
        }
        // Names draw from their plain string in TEXT; a style colour is only the gem accent (spec-final §3.3).
        // 名称以纯文本 TEXT 色绘制；样式颜色仅用作宝石身份色。
        for (int i = 0; i < entry.lines().size(); i++) {
            String line = i == 0 ? plan.names[section][index]
                    : InventoryCardPaint.ellipsize(font, entry.lines().get(i).getString(), x1 - (x0 + 8));
            int color = i > 0 ? InventoryCardPaint.MUTED : hover ? InventoryCardPaint.TEXT_HI : InventoryCardPaint.TEXT;
            context.drawText(font, line, x0 + 8, y + 2 + i * LINE, color, true);
        }
    }

    /**
     * Hero row at its section's ladder step (spec-final §6.5): A full pill + gauge under it; A' short pill + gauge;
     * B pill on line B; C short pill under the gem. The step and widths come from the section's column.
     * 技能行按所在分节的阶梯绘制，阶梯与宽度来自分节状态列。
     */
    private static void hero(DrawContext context, TextRenderer font, String name, int costWidth, Status status, Column column,
                             int x0, int x1, int y, long now) {
        int inner = x1 - (x0 + 8);
        context.drawText(font, name, x0 + 8, y + 2, InventoryCardPaint.TEXT_HI, true);
        switch (column.step()) {
            case FULL, SHORT -> {
                boolean full = column.step() == Step.FULL;
                int pillWidth = column.pill(), pillX = x1 - pillWidth;
                InventoryCardPaint.pill(context, font, status.kind(), full ? status.label() : status.shortLabel(), pillX, y, pillWidth, now);
                if (showCost(status) && 8 + costWidth + 5 <= pillX - x0) cost(context, font, status, x0 + 8, y + 13);
                lineB(context, status, x0, pillX, pillWidth, y + LINE);
            }
            case BELOW -> {
                InventoryCardPaint.pill(context, font, status.kind(), status.label(), x1 - column.full(), y + LINE, column.full(), now);
                if (showCost(status) && costWidth + 6 <= inner - column.full()) cost(context, font, status, x0 + 8, y + 13);
            }
            case NARROW -> InventoryCardPaint.pill(context, font, status.kind(), status.shortLabel(), x0, y + LINE,
                    Math.min(column.compact(), x1 - x0), now);
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
     * Line B under the pill: task pips (LOCKED; an empty track when there are none, so every hero of a section has a
     * bar under its pill), charge pips + bar (ACTIVE with charges), else the gauge. When the pill is too narrow for
     * pips and a readable bar (the short A' pill), the pips take the cost slot under the name, which ACTIVE leaves
     * empty, and the bar keeps the full pill width.
     * 状态牌下方一行：锁定为任务点（无任务点时为空槽，使分节每行状态牌下都有进度槽），带发数的生效为发数点 + 进度条，
     * 否则为进度条。状态牌过窄（A' 短牌）时，发数点移到名称下方的消耗位（生效时该位置为空），进度条保持整个状态牌宽度。
     */
    private static void lineB(DrawContext context, Status status, int x0, int pillX, int pillWidth, int y) {
        switch (status.kind()) {
            case LOCKED -> {
                if (status.pips() > 0) {
                    int span = status.pips() * 7 - 2;
                    InventoryCardPaint.pips(context, pillX + (pillWidth - span) / 2, y + 3, status.pips(), status.pipsOn(),
                            InventoryCardPaint.BRASS_HI, InventoryCardPaint.BRASS_LO);
                } else InventoryCardPaint.gauge(context, pillX, y + 5, pillWidth, 0f, Kind.LOCKED);
            }
            case ACTIVE -> {
                if (status.charges() >= 0) {
                    int n = status.pips() > 0 ? status.pips() : 3, span = n * 7 - 2;
                    if (chargePipsUnderName(pillWidth, span, pillX - x0)) {
                        InventoryCardPaint.pips(context, x0 + 8, y + 3, n, status.charges(), InventoryCardPaint.COIN, InventoryCardPaint.BRASS_LO);
                        InventoryCardPaint.gauge(context, pillX, y + 5, pillWidth, status.fill(), Kind.ACTIVE);
                    } else {
                        int drawn = InventoryCardPaint.pips(context, pillX, y + 3, n, status.charges(),
                                InventoryCardPaint.COIN, InventoryCardPaint.BRASS_LO);
                        InventoryCardPaint.gauge(context, pillX + drawn + 3, y + 5, pillWidth - drawn - 3, status.fill(), Kind.ACTIVE);
                    }
                } else InventoryCardPaint.gauge(context, pillX, y + 5, pillWidth, status.fill(), Kind.ACTIVE);
            }
            default -> InventoryCardPaint.gauge(context, pillX, y + 5, pillWidth, status.fill(), status.kind());
        }
    }

    /**
     * True when the bar beside the pips would be under 24 px and the pips fit left of the pill (8 + span + 5).
     * 发数点旁的进度条不足 24 像素、且状态牌左侧放得下发数点时为真。
     */
    static boolean chargePipsUnderName(int pillWidth, int pipSpan, int roomLeftOfPill) {
        return pillWidth - pipSpan - 3 < MIN_CHARGE_BAR && 8 + pipSpan + 5 <= roomLeftOfPill;
    }

    private static void overflowRow(DrawContext context, TextRenderer font, Plan plan, InventoryCardLayout.Row row, int x0, boolean hover) {
        int y = row.y();
        InventoryCardPaint.batch(context, () -> {
            for (int k = 0; k < 3; k++) context.fill(x0 + 1 + k * 2, y + 6, x0 + 2 + k * 2, y + 7, InventoryCardPaint.FAINT);
        });
        int color = hover ? InventoryCardPaint.TEXT_HI : InventoryCardPaint.FAINT;
        context.drawText(font, plan.overflow(row.omitted()), x0 + 8, y + 2, color, true);
    }

    /** One-row summary "技能 [short pill] · 天赋 3": the skill state survives compaction. 压缩摘要保留技能状态。 */
    private static void compactRow(DrawContext context, TextRenderer font, Snapshot snapshot, Plan plan, int x0, int y, long now) {
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
            Column column = plan.columns[s];
            if (hero != null && column != null) {
                InventoryCardPaint.pill(context, font, hero.status().kind(), hero.status().shortLabel(), cursor, y, column.compact(), now);
                cursor += column.compact();
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

    /** The laid-out tooltip of one hovered row, kept while the snapshot, the row and the screen size stay the same. */
    private record TipCache(Snapshot snapshot, TextRenderer font, InventoryCardLayout.Row row, int width, int height, @Nullable Tip tip) {}

    /**
     * Brass tooltip left of the card at z=300 (below Assist's modal and Wathe's z=400 price tags), never scaled.
     * Content-top candidates: the hovered label's row, above the first price tag it would hit, below the price band;
     * only price tags block it. The wrapped layout is cached per hovered row, so hovering costs no re-wrap per frame.
     * 黄铜提示框位于卡片左侧 z=300，绝不缩放；仅价签视为遮挡，依次尝试三个候选位置；排版按悬停行缓存，悬停时不再逐帧换行。
     */
    private static void drawTooltip(DrawContext context, TextRenderer font, Snapshot snapshot, InventoryCardLayout.Row row,
                                    int width, int height) {
        if (width < 8 || height < 8) return;
        TipCache cached = tipCache;
        Tip tip;
        if (cached != null && cached.snapshot() == snapshot && cached.font() == font && cached.row().equals(row)
                && cached.width() == width && cached.height() == height) tip = cached.tip();
        else {
            tip = layoutTooltip(font, snapshot, row, width, height);
            tipCache = new TipCache(snapshot, font, row, width, height, tip);
        }
        if (tip == null) return;
        int x = tip.x(), y = tip.y();
        context.getMatrices().push();
        try {
            // Vanilla drawTooltip internally adds 400. Drawing wrapped text ourselves keeps BOTH
            // background and glyphs below Assist's modal layer, including the default text offset.
            context.getMatrices().translate(x, y, 300);
            InventoryCardPaint.brassTooltip(context, 0, 0, tip.width(), tip.height());
            int lineY = 0;
            for (int i = 0; i < tip.lines().size(); i++) {
                lineY += tip.gaps()[i];
                int lineX = 0;
                if (i == 0 && tip.accent() != null) { InventoryCardPaint.gem(context, 0, lineY + 1, tip.accent(), false); lineX = 8; }
                context.drawText(font, tip.lines().get(i), lineX, lineY, tip.owners().get(i).color(), tip.owners().get(i).shadow());
                lineY += LINE;
            }
        } finally { context.getMatrices().pop(); }
    }

    private static @Nullable Tip layoutTooltip(TextRenderer font, Snapshot snapshot, InventoryCardLayout.Row row, int width, int height) {
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
        if (blocks.isEmpty()) return null;
        var box = snapshot.layout().bounds();
        int base = tooltipWrapWidth(width, box.x());
        int widest = Math.max(base, Math.min(MAX_TOOLTIP_WRAP, box.x() - 8 - 6 - 8));
        int room = tooltipRoom(height, snapshot.tooltipBlockers());
        Map<Cell, Integer> advances = new HashMap<>();
        // Widen a tall tooltip in 20 px steps (up to the room left of the card, at most 320) until it fits below the
        // price band, so long texts never end up under Wathe's z=400 price tags. fitWrap IS the search (its tests cover
        // this path); each width is wrapped once and the chosen layout is reused. 过高的提示逐步加宽直到能放在价签带下方；
        // 搜索即 fitWrap（测试覆盖此路径），每个宽度只换行一次，选中的排版直接复用。
        Map<Integer, Wrapped> byWidth = new HashMap<>();
        List<TipBlock> tipBlocks = blocks;
        Integer tipAccent = accent;
        IntFunction<Wrapped> at = wrapWidth -> byWidth.computeIfAbsent(wrapWidth,
                w -> wrapTooltip(font, tipBlocks, tipAccent, w, advances));
        Wrapped laid = at.apply(fitWrap(wrapWidth -> at.apply(wrapWidth).height(), base, widest, room));
        if (laid.lines().isEmpty()) return null;
        int x = Math.max(6, box.x() - 8 - laid.width());
        int y = tooltipTop(x, row.y() + 2, laid.width(), laid.height(), snapshot.tooltipBlockers(), snapshot.storeFrames(), height);
        return new Tip(laid.lines(), laid.owners(), laid.gaps(), laid.width(), laid.height(), x, y, accent);
    }

    /** One tooltip wrapped at one width: lines, their owning blocks and gaps, and the content size. 某一宽度下的排版。 */
    private record Wrapped(List<OrderedText> lines, List<TipBlock> owners, int[] gaps, int width, int height) {}

    private static Wrapped wrapTooltip(TextRenderer font, List<TipBlock> blocks, @Nullable Integer accent, int wrapWidth,
                                       Map<Cell, Integer> advances) {
        List<OrderedText> lines = new ArrayList<>();
        List<TipBlock> owners = new ArrayList<>();
        List<Integer> lineGaps = new ArrayList<>();
        for (TipBlock block : blocks) {
            List<OrderedText> wrapped = wrapBlock(font, block.text(), wrapWidth, advances);
            for (int k = 0; k < wrapped.size(); k++) {
                lines.add(InventoryCardPaint.untintIcons(wrapped.get(k)));
                owners.add(block);
                lineGaps.add(k == 0 ? block.gapBefore() : 0);
            }
        }
        int[] gaps = lineGaps.stream().mapToInt(Integer::intValue).toArray();
        int tipWidth = 0, tipHeight = 0;
        for (int i = 0; i < lines.size(); i++) {
            tipWidth = Math.max(tipWidth, font.getWidth(lines.get(i)) + (i == 0 && accent != null ? 8 : 0));
            tipHeight += gaps[i] + (i == lines.size() - 1 ? 9 : LINE);
        }
        return new Wrapped(List.copyOf(lines), List.copyOf(owners), gaps, tipWidth, tipHeight);
    }

    /**
     * Height a tooltip can use without covering a price tag: below the price band (or the whole screen without tags),
     * keeping the 6 px screen margin. 不遮挡价签时提示可用的高度：价签带下方（无价签时为整屏），保留 6 像素边距。
     */
    static int tooltipRoom(int screenHeight, List<InventoryCardLayout.Rect> blockers) {
        int top = 6;
        for (var tag : blockers) top = Math.max(top, tag.bottom() + 6);
        return screenHeight - 6 - top;
    }

    /**
     * One tooltip block: rule 7a (glueUnits), then the line breaker, then the widow retry. The widths are measured
     * per (code point, style) and shared by every block and retry of one tooltip.
     * 单个提示段：先做 7a 单位粘连，再断行，最后避免孤字；字宽按（码点，样式）缓存，整个提示共享。
     */
    private static List<OrderedText> wrapBlock(TextRenderer font, Text block, int wrapWidth, Map<Cell, Integer> advances) {
        Text text = glueUnits(block);
        List<OrderedText> wrapped = wrapTooltipText(font, text, wrapWidth, advances);
        return avoidStrandedPunctuation(wrapped, narrower -> wrapTooltipText(font, text, narrower, advances), wrapWidth);
    }

    /**
     * Tooltip line breaking. Text without CJK keeps vanilla's word wrap, identical to item tooltips (none of the rules
     * below can change it). CJK text is broken by {@link #wrap}: numbers stay glued to their unit ("300格"), closing
     * punctuation never starts a line and opening punctuation never ends one; styles and icon glyphs survive.
     * 提示换行：不含中日韩文字的文本沿用原版按词换行（与物品提示一致，下列规则不会改变其结果）；中文按 {@link #wrap} 断行：
     * 数字与单位不分开（“300格”），收尾标点不在行首、开启标点不在行尾，样式与图标字形保留。
     */
    static List<OrderedText> wrapTooltipText(TextRenderer font, Text text, int wrapWidth, Map<Cell, Integer> advances) {
        List<Cell> cells = cells(text);
        if (cells.stream().noneMatch(cell -> cjk(cell.codePoint()))) return font.wrapLines(text, wrapWidth);
        ToIntFunction<Cell> advance = cell -> advances.computeIfAbsent(cell,
                key -> font.getWidth(OrderedText.styled(key.codePoint(), key.style())));
        return wrap(normalise(cells), wrapWidth, advance).stream().map(InventoryInfoCard::ordered).toList();
    }

    /** Styled cells as vanilla sees them: legacy § codes are applied, not drawn. 与原版一致地展开样式单元。 */
    static List<Cell> cells(StringVisitable text) {
        List<Cell> out = new ArrayList<>();
        TextVisitFactory.visitFormatted(text, Style.EMPTY, (index, style, codePoint) -> {
            out.add(new Cell(codePoint, style));
            return true;
        });
        return out;
    }

    /**
     * Display rule 7b (spec-final §2.4), for CJK text only: drop an ASCII space next to full-width punctuation
     * ("不能杀人。 词条" → "不能杀人。词条"). Lang files stay untouched. 规则 7b：删除紧邻全角标点的半角空格，仅渲染期。
     */
    static List<Cell> normalise(List<Cell> cells) {
        List<Cell> out = new ArrayList<>(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).codePoint() == ' ' && i > 0 && i + 1 < cells.size()
                    && (fullWidthPunctuation(cells.get(i - 1).codePoint()) || fullWidthPunctuation(cells.get(i + 1).codePoint()))) continue;
            out.add(cells.get(i));
        }
        return out;
    }

    /**
     * Greedy fill over tokens (a port of SparkAssist's guide CjkLineBreaker, spec-final §2.4): a space is a break
     * opportunity dropped at line ends; a word run of ASCII letters/digits and {@code + - . % / × ÷ _ ' #} is never
     * split, and one containing a digit is glued to the next ideograph (optionally after one space); closing punctuation
     * attaches to the token before it, opening punctuation to the token after it; {@code 【…】} of at most 8 glyphs is
     * one token; any other glyph is its own token; a token wider than the line falls back to glyph breaking.
     * Always returns at least one (possibly empty) line.
     * 按记号贪心填充（移植自 SparkAssist 指南的 CjkLineBreaker）：空格为断点且行尾丢弃；ASCII 单词不拆开，含数字的单词与其后汉字粘连；
     * 收尾标点附着前一记号、开启标点附着后一记号；不超过 8 字的【…】整体不拆；其余每字一个记号；超宽记号退回逐字断行。
     */
    static List<List<Cell>> wrap(List<Cell> cells, int maxWidth, ToIntFunction<Cell> advance) {
        List<List<Cell>> lines = new ArrayList<>();
        List<Cell> current = new ArrayList<>();
        int currentWidth = 0;
        for (List<Cell> token : tokens(cells)) {
            int first = token.getFirst().codePoint();
            if (first == '\n') {
                lines.add(trim(current));
                current = new ArrayList<>();
                currentWidth = 0;
                continue;
            }
            int tokenWidth = 0;
            for (Cell cell : token) tokenWidth += advance.applyAsInt(cell);
            if (first == ' ') {
                if (!current.isEmpty()) { current.addAll(token); currentWidth += tokenWidth; }
                continue;
            }
            if (currentWidth + tokenWidth > maxWidth && !current.isEmpty()) {
                lines.add(trim(current));
                current = new ArrayList<>();
                currentWidth = 0;
            }
            if (tokenWidth > maxWidth) {
                for (Cell cell : token) {
                    int glyph = advance.applyAsInt(cell);
                    if (currentWidth + glyph > maxWidth && !current.isEmpty()) {
                        lines.add(trim(current));
                        current = new ArrayList<>();
                        currentWidth = 0;
                    }
                    current.add(cell);
                    currentWidth += glyph;
                }
                continue;
            }
            current.addAll(token);
            currentWidth += tokenWidth;
        }
        lines.add(trim(current));
        return lines;
    }

    static List<List<Cell>> tokens(List<Cell> cells) {
        List<List<Cell>> tokens = new ArrayList<>();
        int i = 0, n = cells.size();
        while (i < n) {
            int codePoint = cells.get(i).codePoint();
            if (codePoint == ' ' || codePoint == '\n') { tokens.add(List.of(cells.get(i++))); continue; }
            List<Cell> token = new ArrayList<>();
            while (i < n && OPENING.indexOf(cells.get(i).codePoint()) >= 0) token.add(cells.get(i++));
            int close = -1;
            if (!token.isEmpty() && token.getLast().codePoint() == '【') {
                for (int j = i; j < n && j <= i + BRACKET_SPAN; j++) if (cells.get(j).codePoint() == '】') { close = j; break; }
            }
            if (close >= 0) {
                while (i <= close) token.add(cells.get(i++));
            } else if (i < n && word(cells.get(i).codePoint())) {
                boolean number = false;
                while (i < n && word(cells.get(i).codePoint())) { number |= digit(cells.get(i).codePoint()); token.add(cells.get(i++)); }
                if (number) {
                    int j = i;
                    if (j < n && cells.get(j).codePoint() == ' ') j++;
                    if (j < n && ideograph(cells.get(j).codePoint())) while (i <= j) token.add(cells.get(i++));
                }
            } else if (i < n && cells.get(i).codePoint() != ' ' && cells.get(i).codePoint() != '\n') {
                token.add(cells.get(i++));
            }
            while (i < n && CLOSING.indexOf(cells.get(i).codePoint()) >= 0) token.add(cells.get(i++));
            if (token.isEmpty()) token.add(cells.get(i++));
            tokens.add(token);
        }
        return tokens;
    }

    /** One OrderedText per line: equal-style neighbours become one forwards-visited run. 同样式相邻单元合并为一段。 */
    static OrderedText ordered(List<Cell> line) {
        if (line.isEmpty()) return OrderedText.EMPTY;
        List<OrderedText> runs = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        Style style = line.getFirst().style();
        for (Cell cell : line) {
            if (!cell.style().equals(style)) {
                runs.add(OrderedText.styledForwardsVisitedString(run.toString(), style));
                run.setLength(0);
                style = cell.style();
            }
            run.appendCodePoint(cell.codePoint());
        }
        runs.add(OrderedText.styledForwardsVisitedString(run.toString(), style));
        return OrderedText.concat(runs);
    }

    private static List<Cell> trim(List<Cell> line) {
        int start = 0, end = line.size();
        while (start < end && line.get(start).codePoint() == ' ') start++;
        while (end > start && line.get(end - 1).codePoint() == ' ') end--;
        return new ArrayList<>(line.subList(start, end));
    }

    static boolean word(int codePoint) {
        return codePoint >= '0' && codePoint <= '9' || codePoint >= 'A' && codePoint <= 'Z'
                || codePoint >= 'a' && codePoint <= 'z' || WORD_SYMBOLS.indexOf(codePoint) >= 0;
    }

    static boolean digit(int codePoint) { return codePoint >= '0' && codePoint <= '9'; }

    static boolean ideograph(int codePoint) {
        return codePoint >= 0x3400 && codePoint <= 0x9FFF || codePoint >= 0xF900 && codePoint <= 0xFAFF;
    }

    /** CJK script or full-width forms; the private-use icon glyphs (coin, mana) do not count. 中日韩文字与全角符号。 */
    static boolean cjk(int codePoint) { return codePoint >= 0x2E80 && !InventoryCardPaint.iconGlyph(codePoint); }

    static boolean fullWidthPunctuation(int codePoint) {
        return codePoint >= 0x3000 && codePoint <= 0x303F || codePoint >= 0xFF01 && codePoint <= 0xFF0F
                || codePoint >= 0xFF1A && codePoint <= 0xFF20 || codePoint >= 0xFF3B && codePoint <= 0xFF40
                || codePoint >= 0xFF5B && codePoint <= 0xFF65;
    }

    /**
     * Index of a hero tooltip's status line: the presenter appends {@code status.facts()} extra lines AFTER the shared
     * status line (mana, charges, dash). Other tooltips end with their status.
     * 技能提示的状态行下标：展示方在共享状态行之后追加 facts 行附加信息（魔力、发数、冲刺）；其余提示以末行为状态行。
     */
    static int statusLine(Entry entry) {
        Status status = entry.status();
        return entry.tooltip().size() - 1 - (status == null ? 0 : status.facts());
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
     * The narrowest wrap in [base, widest] (20 px steps) whose height fits {@code room}; {@code widest} when none does.
     * This is layoutTooltip's own search, not a copy. 在 [base, widest] 中按 20 像素步长取能放下的最窄宽度，均放不下时取最宽；
     * 即 layoutTooltip 实际使用的搜索。
     */
    static int fitWrap(IntUnaryOperator heightAt, int base, int widest, int room) {
        for (int wrap = base; ; wrap = Math.min(widest, wrap + TOOLTIP_WRAP_STEP)) {
            if (heightAt.applyAsInt(wrap) <= room || wrap >= widest) return wrap;
        }
    }

    /**
     * Post-pass over the wrap: if punctuation is stranded ({@link #strandsPunctuation}), re-wrap up to three glyph
     * advances (9 px each) narrower so the glyphs before it move down too ("…降低理" / "智。" instead of a lone "。").
     * Keeps the original wrap if none helps.
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

    /**
     * Content top (spec-final §6.7): the first candidate clear of every price tag — (a) the hovered row, (b) above the
     * first tag it would hit, (c) below the price band — clamped to the screen, then {@link #clearFrameTops}.
     * 内容顶端：依次取不与价签相交的候选位置（悬停行、所碰价签上方、价签带下方），夹在屏幕内，再避免横切商店槽边框。
     */
    static int tooltipTop(int x, int preferred, int tipWidth, int tipHeight, List<InventoryCardLayout.Rect> blockers,
                          List<InventoryCardLayout.Rect> frames, int height) {
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
        return clearFrameTops(x, Math.max(6, Math.min(y, height - tipHeight - 6)), tipWidth, tipHeight, blockers, frames);
    }

    /**
     * A box whose outer top cuts through a shop-slot frame (typically after the bottom clamp pushed it up into the shop
     * row) leaves a band of frame tops showing between the price tags and the box. Raise it so the outer top meets the
     * highest cut frame's top, which also covers that band (Wathe's tag ends 2 px above its frame, so the tag stays
     * clear). Only ever a move up, and only when the raised box still clears every tag and the 6 px margin.
     * 提示框外框顶边横切商店槽边框时（通常是底部夹取把它推入商店行），价签与提示框之间会露出一条边框碎条：把外框顶边上移到被切边框的顶边，
     * 将碎条完全遮住（Wathe 价签底边比边框高 2 像素，价签不受影响）。只会上移，且上移后仍须避开所有价签并保留 6 像素边距。
     */
    static int clearFrameTops(int x, int y, int tipWidth, int tipHeight, List<InventoryCardLayout.Rect> blockers,
                              List<InventoryCardLayout.Rect> frames) {
        int outerTop = y - 4, outerLeft = x - 4, outerRight = x + tipWidth + 4, target = Integer.MAX_VALUE;
        for (var frame : frames) {
            boolean covered = frame.x() < outerRight && outerLeft < frame.right();
            if (covered && frame.y() < outerTop && outerTop < frame.bottom()) target = Math.min(target, frame.y());
        }
        if (target == Integer.MAX_VALUE) return y;
        int raised = target + 4;
        var rect = new InventoryCardLayout.Rect(outerLeft, target, tipWidth + 8, tipHeight + 8);
        return raised >= 6 && raised < y && blockers.stream().noneMatch(rect::intersects) ? raised : y;
    }
}
