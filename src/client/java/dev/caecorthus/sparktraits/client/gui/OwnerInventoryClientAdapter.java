package dev.caecorthus.sparktraits.client.gui;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.client.text.TraitClientTexts;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.presentation.OwnerInventoryPresentation;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public final class OwnerInventoryClientAdapter implements OwnerInventoryPresentation.Adapter {
    private static final OwnerInventoryClientAdapter INSTANCE = new OwnerInventoryClientAdapter();
    private static boolean nativeCardFailed;
    private BooleanSupplier presenter;
    private OwnerInventoryClientAdapter() {}
    public static void install() { OwnerInventoryPresentation.install(INSTANCE); }

    @Override
    public void visit(PlayerEntity player, BiConsumer<Text, List<Text>> visitor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (player == null || player != client.player || client.world == null || player.getWorld() != client.world
                || !player.getWorld().isClient || !SparkTraitsServerConnection.isConfirmedServer()) return;
        var component = TraitPlayerComponent.KEY.get(player);
        // Complete collection first; neither a downstream callback nor mutable Text can affect iteration.
        // The name (no brackets) carries the trait colour on its root style; cards read it only as the gem accent.
        // 导出不带括号的名称，根样式携带天赋颜色；卡片仅将其用作宝石身份色。
        List<InventoryInfoCard.Entry> entries = new ArrayList<>();
        for (var id : component.getActiveTraitIds()) {
            if (component.isVisibleToOwner(id)) entries.add(new InventoryInfoCard.Entry(
                    List.of(TraitClientTexts.name(id).copy().styled(style -> style.withColor(TraitClientTexts.color(id)))),
                    TraitClientTexts.tooltip(id)));
        }
        for (var entry : entries) visitor.accept(entry.lines().getFirst().copy(), entry.tooltip().stream().map(t -> (Text) t.copy()).toList());
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

    public static void resetNativeCard() { nativeCardFailed = false; }

    public static List<InventoryInfoCard.Entry> collect(PlayerEntity player) {
        List<InventoryInfoCard.Entry> entries = new ArrayList<>();
        INSTANCE.visit(player, (tag, tooltip) -> entries.add(new InventoryInfoCard.Entry(List.of(tag), tooltip)));
        return List.copyOf(entries);
    }
}
