package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lower priority than Witch's explicit priority-1000 Tofana terminal protection. */
@Mixin(value = GameFunctions.class, priority = 900, remap = false)
public abstract class LastEscapeTerminalMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
            method = "killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/PlayerPsychoComponent;stopPsycho()V"))
    private static void sparktraits$snapshotBeforePsychoCleanup(
            dev.doctor4t.wathe.cca.PlayerPsychoComponent psycho,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original,
            ServerPlayerEntity victim, boolean spawnBody, ServerPlayerEntity killer, Identifier reason, boolean force) {
        dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeKillScope.beforePsychoStop(victim);
        original.call(psycho);
    }

    @Inject(method = "killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;changeGameMode(Lnet/minecraft/world/GameMode;)Z", remap = true),
            cancellable = true, require = 1, allow = 1)
    private static void sparktraits$lastSave(ServerPlayerEntity victim, boolean spawnBody,
            ServerPlayerEntity killer, Identifier reason, boolean force, CallbackInfo ci) {
        if (!LastEscapeService.isTrainDeath(reason) && !GameConstants.DeathReasons.ESCAPED.equals(reason)
                && LastEscapeService.tryActivate(victim)) ci.cancel();
    }
}
