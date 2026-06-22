package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.doctor4t.wathe.client.gui.RoundTextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

/**
 * Replaces the welcome goal line for Conscience killers.
 * 为善良杀手替换开局欢迎登车的第三行目标文字。
 */
@Mixin(value = RoundTextRenderer.class, remap = false)
public abstract class RoundTextRendererMixin {
    @Redirect(
            method = "renderHud",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 1)
    )
    private static Object sparktraits$conscienceGoalText(Function<Integer, Text> goalText, Object targets) {
        if (!EffectiveTraitService.hasConscience(MinecraftClient.getInstance().player)) {
            return goalText.apply((Integer) targets);
        }
        return Text.translatable("announcement.goal.sparktraits.conscience")
                .withColor(EffectiveTraitService.CONSCIENCE_COLOR);
    }
}
