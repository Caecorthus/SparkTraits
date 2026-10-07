package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.killer.LastEscapeInput;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Run before role-specific click replacements, without blocking movement or camera input. */
@Mixin(value = MinecraftClient.class, priority = 2000)
public abstract class LastEscapeMinecraftClientMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void sparktraits$drainEscapeClicks(CallbackInfo ci) {
        LastEscapeInput.suppress((MinecraftClient) (Object) this);
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockEscapeAttack(CallbackInfoReturnable<Boolean> cir) {
        if (LastEscapeInput.blocked()) {
            LastEscapeInput.suppress((MinecraftClient) (Object) this);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockEscapeUse(CallbackInfo ci) {
        if (LastEscapeInput.blocked()) {
            LastEscapeInput.suppress((MinecraftClient) (Object) this);
            ci.cancel();
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockEscapeBreaking(boolean breaking, CallbackInfo ci) {
        if (LastEscapeInput.blocked()) {
            LastEscapeInput.suppress((MinecraftClient) (Object) this);
            ci.cancel();
        }
    }
}
