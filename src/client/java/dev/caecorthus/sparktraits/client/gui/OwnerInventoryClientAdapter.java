package dev.caecorthus.sparktraits.client.gui;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.client.text.TraitClientTexts;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.presentation.OwnerInventoryPresentation;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public final class OwnerInventoryClientAdapter implements OwnerInventoryPresentation.Adapter {
    private static final OwnerInventoryClientAdapter INSTANCE = new OwnerInventoryClientAdapter();
    private static boolean nativeCardFailed;
    // Last visible trait ids and their detached entries (render thread). 上次的可见天赋 id 与其独立条目（渲染线程）。
    private static List<Identifier> cachedIds;
    private static List<InventoryInfoCard.Entry> cachedEntries = List.of();
    private BooleanSupplier presenter;
    private OwnerInventoryClientAdapter() {}
    public static void install() { OwnerInventoryPresentation.install(INSTANCE); }

    @Override
    public void visit(PlayerEntity player, BiConsumer<Text, List<Text>> visitor) {
        // Detached deep copies per visit: a downstream consumer can never reach the cached entries.
        // 每次访问都交出深拷贝，下游无法触及缓存的条目。
        for (var entry : entries(player)) {
            visitor.accept(InventoryInfoCard.copyText(entry.lines().getFirst()), entry.tooltip().stream().map(InventoryInfoCard::copyText).toList());
        }
    }

    /**
     * The owner's visible traits as card entries, rebuilt only when the visible id list changes: names and tooltips
     * are pure functions of the id, and their translatable texts follow a language change by themselves (the card
     * re-measures on it). 拥有者可见天赋的卡片条目，仅在可见 id 列表变化时重建：名称与提示只取决于 id，可翻译文本随语言自动更新。
     */
    private static List<InventoryInfoCard.Entry> entries(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (player == null || player != client.player || client.world == null || player.getWorld() != client.world
                || !player.getWorld().isClient || !SparkTraitsServerConnection.isConfirmedServer()) return List.of();
        var component = TraitPlayerComponent.KEY.get(player);
        List<Identifier> visible = new ArrayList<>();
        for (var id : component.getActiveTraitIds()) {
            if (component.isVisibleToOwner(id)) visible.add(id);
        }
        if (visible.equals(cachedIds)) return cachedEntries;
        // Complete collection first; neither a downstream callback nor mutable Text can affect iteration.
        // The name (no brackets) carries the trait colour on its root style; cards read it only as the gem accent.
        // 导出不带括号的名称，根样式携带天赋颜色；卡片仅将其用作宝石身份色。
        List<InventoryInfoCard.Entry> entries = new ArrayList<>();
        for (var id : visible) {
            entries.add(new InventoryInfoCard.Entry(
                    List.of(TraitClientTexts.name(id).copy().styled(style -> style.withColor(TraitClientTexts.color(id)))),
                    TraitClientTexts.tooltip(id)));
        }
        cachedIds = List.copyOf(visible);
        cachedEntries = List.copyOf(entries);
        return cachedEntries;
    }

    @Override
    public boolean register(BooleanSupplier candidate) {
        if (presenter != null && presenter != candidate) return false;
        presenter = candidate;
        return true;
    }

    public static boolean externallyPresented() {
        try { return INSTANCE.presenter != null && INSTANCE.presenter.getAsBoolean(); }
        catch (RuntimeException | LinkageError ignored) { return false; }
    }

    /**
     * Connection-scoped latch for the native card: after a collect/prepare/draw failure it stops drawing (no
     * exception, log or half-drawn card every frame) until {@link #resetNativeCard()} on disconnect.
     * 原生卡的按连接失败锁存：失败后停止绘制（不再每帧抛异常、记日志或绘制残缺卡片），断开连接时重置。
     */
    public static boolean nativeCardDisabled() { return nativeCardFailed; }

    public static void disableNativeCard(Throwable failure) {
        if (nativeCardFailed) return;
        nativeCardFailed = true;
        SparkTraits.LOGGER.error("Owner inventory traits card failed; disabling it for this connection", failure);
    }

    /** Disconnect: re-enables the native card and drops the cached entries and card layout. 断开连接时重置并清空缓存。 */
    public static void resetNativeCard() {
        nativeCardFailed = false;
        cachedIds = null;
        cachedEntries = List.of();
        InventoryInfoCard.invalidateCaches();
    }

    /** The native card's entries: the cached, immutable entries themselves (the card never mutates them). 原生卡条目。 */
    public static List<InventoryInfoCard.Entry> collect(PlayerEntity player) {
        return entries(player);
    }
}
