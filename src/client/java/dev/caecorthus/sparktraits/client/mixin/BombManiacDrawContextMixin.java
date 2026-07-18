package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DrawContext.class)
public abstract class BombManiacDrawContextMixin {
    @Redirect(
            method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ItemCooldownManager;getCooldownProgress(Lnet/minecraft/item/Item;F)F"
            )
    )
    private float sparktraits$hideMarkedGrenadeCooldown(
            ItemCooldownManager manager,
            Item item,
            float tickDelta,
            TextRenderer textRenderer,
            ItemStack stack,
            int x,
            int y,
            String countOverride
    ) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && ConscienceBomberFrenzyService.canUseMarkedGrenade(player, stack)) {
            return 0.0F;
        }
        return manager.getCooldownProgress(item, tickDelta);
    }
}
