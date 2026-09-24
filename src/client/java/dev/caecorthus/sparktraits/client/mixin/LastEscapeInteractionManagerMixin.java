package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.killer.LastEscapeInput;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Also covers GUI subclasses and callers that bypass MinecraftClient's normal input loop. */
@Mixin(value = ClientPlayerInteractionManager.class, priority = 2000)
public abstract class LastEscapeInteractionManagerMixin {
    @Inject(method = {"clickSlot", "attackEntity", "stopUsingItem"}, at = @At("HEAD"), cancellable = true)
    private void sparktraits$rejectEscapeAction(CallbackInfo ci) {
        if (LastEscapeInput.blocked()) {
            ci.cancel();
        }
    }

    @Inject(method = {"interactItem", "interactBlock", "interactEntity", "interactEntityAtLocation"},
            at = @At("HEAD"), cancellable = true)
    private void sparktraits$rejectEscapeUse(CallbackInfoReturnable<ActionResult> cir) {
        if (LastEscapeInput.blocked()) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(method = {"attackBlock", "updateBlockBreakingProgress"}, at = @At("HEAD"), cancellable = true)
    private void sparktraits$rejectEscapeMining(CallbackInfoReturnable<Boolean> cir) {
        if (LastEscapeInput.blocked()) {
            cir.setReturnValue(false);
        }
    }
}
