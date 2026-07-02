package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.YuushaTrait;
import dev.caecorthus.sparktraits.impl.YuushaTraitService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;

/**
 * Renders Yuusha's Mankai ability status in the NoellesRoles ability-HUD style.
 * 以 NoellesRoles 技能提示风格渲染勇者“満開”的冷却与状态。
 */
public final class YuushaHud {
    private static final int EDGE_PADDING = 5;
    private static final int LINE_GAP = 2;

    private YuushaHud() {
    }

    public static void render(TextRenderer renderer, ClientPlayerEntity player, DrawContext context) {
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        if (!traits.hasActiveTrait(YuushaTrait.ID)) {
            return;
        }

        Text text;
        if (traits.isYuushaActive()) {
            text = Text.translatable("tip.sparktraits.yuusha.hud.active", seconds(traits.getYuushaActiveTicks()));
        } else if (traits.getYuushaCooldownTicks() > 0) {
            text = Text.translatable("tip.sparktraits.yuusha.hud.cooldown", seconds(traits.getYuushaCooldownTicks()));
        } else {
            text = Text.translatable("tip.sparktraits.yuusha.hud.ready", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
        }

        int x = context.getScaledWindowWidth() - renderer.getWidth(text) - EDGE_PADDING;
        int y = context.getScaledWindowHeight() - renderer.fontHeight - EDGE_PADDING;
        if (hasNativeNoellesRoleHud(player)) {
            y -= renderer.fontHeight + LINE_GAP;
        }
        context.drawTextWithShadow(renderer, text, x, y, YuushaTraitService.YUUSHA_COLOR);
    }

    private static boolean hasNativeNoellesRoleHud(ClientPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        return game.isRole(player, Noellesroles.ASSASSIN)
                || game.isRole(player, Noellesroles.CORONER)
                || game.isRole(player, Noellesroles.CORRUPT_COP)
                || game.isRole(player, Noellesroles.DETECTIVE)
                || game.isRole(player, Noellesroles.JESTER)
                || game.isRole(player, Noellesroles.MORPHLING)
                || game.isRole(player, Noellesroles.NOISEMAKER)
                || game.isRole(player, Noellesroles.PARTY_ANIMAL)
                || game.isRole(player, Noellesroles.PATHOGEN)
                || game.isRole(player, Noellesroles.PHANTOM)
                || game.isRole(player, Noellesroles.RECALLER)
                || game.isRole(player, Noellesroles.REPORTER)
                || game.isRole(player, Noellesroles.SHADOW_JESTER)
                || game.isRole(player, Noellesroles.SILENCER)
                || game.isRole(player, Noellesroles.SPIRIT_WALKER)
                || game.isRole(player, Noellesroles.SWAPPER)
                || game.isRole(player, Noellesroles.TAOTIE)
                || game.isRole(player, Noellesroles.VULTURE);
    }

    private static int seconds(int ticks) {
        return Math.max(1, (ticks + 19) / 20);
    }
}
