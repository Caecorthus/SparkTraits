package dev.caecorthus.sparktraits.client;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.client.gui.TimeRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Renders Depression's private suicide countdown under Wathe's round timer.
 * 在 wathe 游戏时间正下方渲染抑郁玩家私有的自杀判定倒计时。
 */
public final class DepressionHud {
    private static final int TIMER_COLOR = 0xFFFF0000;
    private static final TimeRenderer.TimeNumberRenderer timer = new TimeRenderer.TimeNumberRenderer();

    private DepressionHud() {
    }

    public static void tick() {
        timer.update();
    }

    public static void render(TextRenderer renderer, ClientPlayerEntity player, DrawContext context, float delta) {
        int ticks = TraitPlayerComponent.KEY.get(player).getDepressionSuicideTicks();
        if (ticks <= 0) {
            return;
        }

        timer.setTarget(ticks);
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2f, 18, 0);
        timer.render(renderer, context, 0, 0, TIMER_COLOR, delta);
        context.getMatrices().pop();
    }
}
