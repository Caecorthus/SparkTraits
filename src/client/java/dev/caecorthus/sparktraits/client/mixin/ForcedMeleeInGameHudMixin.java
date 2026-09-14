package dev.caecorthus.sparktraits.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.caecorthus.sparktraits.client.killer.ForcedMeleeHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Run after Wathe's HEAD replacement: this is only the vanilla fallback, never a second pass.
@Mixin(value = InGameHud.class, priority = 900)
public abstract class ForcedMeleeInGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void sparktraits$lockedFallback(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ForcedMeleeHud.renderLockedCrosshair(context)) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "renderHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getAttackIndicator()Lnet/minecraft/client/option/SimpleOption;"),
                    to = @At(value = "FIELD", target = "Lnet/minecraft/client/option/AttackIndicator;HOTBAR:Lnet/minecraft/client/option/AttackIndicator;")))
    private Object sparktraits$hideContradictoryHotbarIndicator(Object original) {
        return ForcedMeleeHud.remainingTicks() > 0 ? AttackIndicator.OFF : original;
    }
}
