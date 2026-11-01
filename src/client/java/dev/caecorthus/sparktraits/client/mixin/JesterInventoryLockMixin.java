package dev.caecorthus.sparktraits.client.mixin;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Jester cannot open its inventory while its psycho moment is running.
 * 小丑疯魔时刻期间无法打开背包。
 */
@Mixin(value = MinecraftClient.class, priority = 2000)
public abstract class JesterInventoryLockMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockJesterInventory(Screen screen, CallbackInfo ci) {
        if (screen == null) {
            return;
        }
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player == null || !JesterPlayerComponent.KEY.get(client.player).inPsychoMode) {
            return;
        }
        if (screen instanceof LimitedInventoryScreen
                || screen instanceof InventoryScreen
                || screen instanceof CreativeInventoryScreen) {
            ci.cancel();
        }
    }
}
