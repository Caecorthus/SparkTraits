package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies Well Supplied after every finish-initialize listener, since some roles write their starting balance there.
 * 在所有初始化完成监听之后应用物资充沛，因为部分身份在该事件中才写入开局余额。
 */
@Mixin(value = GameFunctions.class, remap = false)
public abstract class WellSuppliedStartingMoneyMixin {
    @Inject(
            method = "initializeGame",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;setGameStatus(Ldev/doctor4t/wathe/cca/GameWorldComponent$GameStatus;)V"
            )
    )
    private static void sparktraits$boostWellSuppliedStartingMoney(ServerWorld serverWorld, CallbackInfo ci) {
        GlobalTraitService.applyWellSuppliedStartingMoney(serverWorld);
    }
}
