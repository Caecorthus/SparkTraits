package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.ArrogantAsfTrait;
import dev.caecorthus.sparktraits.impl.CorruptCopTraitService;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.NoellesrolesClient;

/**
 * Renders Arrogant ASF's owner-only active-skill state in the NoellesRoles HUD style.
 * 以 NoellesRoles 的 HUD 风格渲染“展示豪度”仅本人可见的主动技能状态。
 */
public final class ArrogantAsfHud {
    private ArrogantAsfHud() {
    }

    public static void render(TextRenderer renderer, ClientPlayerEntity player, DrawContext context) {
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        if (!traits.hasActiveTrait(ArrogantAsfTrait.ID)) {
            return;
        }

        String key = traits.isArrogantAsfActive()
                ? "tip.sparktraits.arrogant_asf.active"
                : "tip.sparktraits.arrogant_asf.inactive";
        Text text = Text.translatable(key, NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
        int y = context.getScaledWindowHeight() - renderer.fontHeight;
        drawRightAligned(renderer, context, text, y);

        int remainingSeconds = ArrogantAsfMusicController.remainingResumeSeconds();
        if (!traits.isArrogantAsfActive() && remainingSeconds > 0) {
            Text resumeText = Text.translatable(
                    "tip.sparktraits.arrogant_asf.music_resume_remaining",
                    remainingSeconds
            );
            drawRightAligned(renderer, context, resumeText, y - renderer.fontHeight);
        }
    }

    private static void drawRightAligned(TextRenderer renderer, DrawContext context, Text text, int y) {
        int x = context.getScaledWindowWidth() - renderer.getWidth(text);
        context.drawTextWithShadow(renderer, text, x, y, CorruptCopTraitService.ARROGANT_ASF_COLOR);
    }
}
