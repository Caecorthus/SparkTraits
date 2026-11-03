package dev.caecorthus.sparktraits.client.jester;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.agmas.noellesroles.jester.JesterPlayerComponent;

/**
 * Client side of the Jester moment's inventory lock: closes an inventory that was already open when the moment
 * started. Opening a new one is blocked by the screen mixin.
 * 小丑时刻背包锁定的客户端部分：关闭时刻开始时已经打开的背包；新背包的打开由界面 mixin 拦截。
 */
public final class JesterMomentClientHooks {
    private JesterMomentClientHooks() {
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || !JesterPlayerComponent.KEY.get(client.player).inPsychoMode) {
            return;
        }
        Screen screen = client.currentScreen;
        if (screen instanceof LimitedInventoryScreen
                || screen instanceof InventoryScreen
                || screen instanceof CreativeInventoryScreen) {
            client.setScreen(null);
        }
    }
}
