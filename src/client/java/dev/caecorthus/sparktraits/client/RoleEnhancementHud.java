package dev.caecorthus.sparktraits.client;

import com.mojang.authlib.GameProfile;
import dev.caecorthus.sparktraits.component.RoleEnhancementPlayerComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public final class RoleEnhancementHud {
    private RoleEnhancementHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderCriminologistTracking(context));
    }

    private static void renderCriminologistTracking(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        RoleEnhancementPlayerComponent component = RoleEnhancementPlayerComponent.KEY.get(client.player);
        UUID targetUuid = component.getCriminologistTargetUuid();
        if (targetUuid == null && !shouldShowCriminologistLine(client, component)) {
            return;
        }

        int y = context.getScaledWindowHeight() - 74;
        if (targetUuid == null) {
            Text text = component.getCriminologistCooldownTicks() > 0
                    ? Text.literal("犯罪行为学家：" + (component.getCriminologistCooldownTicks() + 19) / 20 + "s")
                    : Text.literal("犯罪行为学家：就绪");
            int textWidth = client.textRenderer.getWidth(text);
            context.drawTextWithShadow(client.textRenderer, text, context.getScaledWindowWidth() - textWidth - 6, y + 1, 0xFFD96A);
            return;
        }
        String name = component.getCriminologistTargetName().isBlank() ? "Unknown" : component.getCriminologistTargetName();
        Text text = Text.literal("正在追踪：" + name);
        int textWidth = client.textRenderer.getWidth(text);
        int x = context.getScaledWindowWidth() - textWidth - 24;
        PlayerSkinDrawer.draw(context, skinTextures(targetUuid, name), x, y - 3, 16);
        context.drawTextWithShadow(client.textRenderer, text, x + 20, y + 1, 0xFF5555);
    }

    private static boolean shouldShowCriminologistLine(MinecraftClient client, RoleEnhancementPlayerComponent component) {
        if (component.getCriminologistCooldownTicks() > 0) {
            return true;
        }
        return client.player != null
                && GameFunctions.isPlayerPlayingAndAlive(client.player)
                && GameWorldComponent.KEY.get(client.player.getWorld()).isRole(client.player, Noellesroles.DETECTIVE);
    }

    private static SkinTextures skinTextures(UUID uuid, String name) {
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(uuid);
        if (entry != null) {
            return entry.getSkinTextures();
        }
        return DefaultSkinHelper.getSkinTextures(new GameProfile(uuid, name));
    }
}
