package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.civilian.jester.JesterMomentLoadoutService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When the Jester successfully starts its psycho moment, closes its inventory and locks the baseball bat to its hand.
 * 小丑成功进入疯魔时刻时，关闭其背包并把棒球棍锁进主手。
 */
@Mixin(value = JesterPlayerComponent.class, remap = false)
public abstract class NoellesRolesJesterLoadoutMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Inject(method = "startJesterPsychoMode", at = @At("RETURN"))
    private void sparktraits$applyMomentLoadout(CallbackInfo ci) {
        JesterPlayerComponent jester = (JesterPlayerComponent) (Object) this;
        if (jester.inPsychoMode && this.player instanceof ServerPlayerEntity serverPlayer) {
            JesterMomentLoadoutService.onPsychoStarted(serverPlayer);
        }
    }
}
