package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.VigilanteVeteranTraitService;
import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Publishes blackout-only Going Dark state through SparkTraits' synced player component.
 * 通过 SparkTraits 的玩家同步组件发布隐蔽行动的关灯状态。
 */
@Mixin(value = WorldBlackoutComponent.class, remap = false)
public abstract class WorldBlackoutComponentMixin {
    @Shadow
    public abstract boolean isBlackoutActive();

    @Inject(method = "applyBlackoutEffects", at = @At("TAIL"))
    private void sparktraits$syncGoingDarkDuringBlackout(ServerWorld serverWorld, CallbackInfo ci) {
        VigilanteVeteranTraitService.syncGoingDarkInstinct(serverWorld, isBlackoutActive());
    }

    @Inject(method = "removeBlackoutEffects", at = @At("TAIL"))
    private void sparktraits$clearGoingDarkAfterBlackout(ServerWorld serverWorld, CallbackInfo ci) {
        VigilanteVeteranTraitService.syncGoingDarkInstinct(serverWorld, false);
    }
}
