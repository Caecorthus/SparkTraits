package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.gui.InventoryInfoCard;
import dev.caecorthus.sparktraits.client.gui.OwnerInventoryClientAdapter;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(LimitedInventoryScreen.class)
public abstract class TraitInventoryScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;
    public TraitInventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "method_25394(Lnet/minecraft/class_332;IIF)V", at = @At("TAIL"))
    private void sparktraits$renderOwnerTraits(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (OwnerInventoryClientAdapter.externallyPresented() || OwnerInventoryClientAdapter.nativeCardDisabled()) return;
        // A failure must not escape Wathe's render (it would crash the client): the card is disabled for this
        // connection and logged once. 失败不得抛出 Wathe 的渲染方法（会导致客户端崩溃）：本连接内停用卡片并只记录一次。
        try {
            var entries = OwnerInventoryClientAdapter.collect(player);
            // With SparkWitch installed its fallback card anchors to this slot, so fill the slot to sit flush beside it.
            // 安装 SparkWitch 时其回退卡锚定在本列左侧，因此原生卡占满整列以紧贴排列。
            var snapshot = InventoryInfoCard.prepareNative(this, textRenderer,
                    List.of(new InventoryInfoCard.Section(Text.translatable("gui.sparktraits.traits"), entries)),
                    FabricLoader.getInstance().isModLoaded("sparkwitch"));
            InventoryInfoCard.draw(context, textRenderer, snapshot, mouseX, mouseY, width, height);
        } catch (RuntimeException | LinkageError failure) {
            OwnerInventoryClientAdapter.disableNativeCard(failure);
        }
    }
}
