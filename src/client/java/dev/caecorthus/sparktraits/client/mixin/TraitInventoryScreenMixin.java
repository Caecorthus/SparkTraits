package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.TraitClientTexts;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LimitedInventoryScreen.class)
public abstract class TraitInventoryScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    private static final int SPARKTRAITS_PANEL_X = 8;
    private static final int SPARKTRAITS_PANEL_Y = 8;
    private static final int SPARKTRAITS_LINE_GAP = 2;

    @Shadow
    @Final
    public ClientPlayerEntity player;

    public TraitInventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void sparktraits$renderOwnerTraits(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        List<Identifier> visibleTraits = traits.getActiveTraitIds().stream()
                .filter(traits::isVisibleToOwner)
                .toList();
        if (visibleTraits.isEmpty()) {
            return;
        }

        int x = SPARKTRAITS_PANEL_X;
        int y = SPARKTRAITS_PANEL_Y;
        Text title = Text.translatable("gui.sparktraits.traits");
        context.drawTextWithShadow(this.textRenderer, title, x, y, 0xFFFFFF);
        y += this.textRenderer.fontHeight + 4;

        Identifier hoveredTrait = null;
        for (Identifier traitId : visibleTraits) {
            Text tag = TraitClientTexts.tag(traitId);
            int width = this.textRenderer.getWidth(tag);
            context.drawTextWithShadow(this.textRenderer, tag, x, y, TraitClientTexts.color(traitId));
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + this.textRenderer.fontHeight) {
                hoveredTrait = traitId;
            }
            y += this.textRenderer.fontHeight + SPARKTRAITS_LINE_GAP;
        }

        if (hoveredTrait != null) {
            context.drawTooltip(this.textRenderer, TraitClientTexts.tooltip(hoveredTrait), mouseX, mouseY);
        }
    }
}
