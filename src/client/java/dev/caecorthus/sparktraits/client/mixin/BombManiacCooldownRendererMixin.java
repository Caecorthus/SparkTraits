package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyService;
import dev.doctor4t.wathe.client.gui.CooldownRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CooldownRenderer.class, remap = false)
public abstract class BombManiacCooldownRendererMixin {
    @Redirect(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ItemCooldownManager;isCoolingDown(Lnet/minecraft/item/Item;)Z"
            )
    )
    private static boolean sparktraits$hideMarkedGrenadeCooldownText(
            ItemCooldownManager manager,
            Item item,
            TextRenderer textRenderer,
            ClientPlayerEntity player,
            DrawContext drawContext,
            RenderTickCounter tickCounter
    ) {
        ItemStack heldStack = player.getMainHandStack();
        return !ConscienceBomberFrenzyService.canUseMarkedGrenade(player, heldStack)
                && manager.isCoolingDown(item);
    }
}
