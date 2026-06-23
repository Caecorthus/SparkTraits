package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.ConscienceSerialKillerService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

/** Routes Serial Killer targets through effective alignment and keeps Conscience targets protected.
 *  让连环杀手目标使用有效阵营判定，并让善良连环杀手的目标保持为保护对象。 */
@Mixin(value = SerialKillerPlayerComponent.class, remap = false)
public abstract class SerialKillerPlayerComponentMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Inject(method = "initializeTarget", at = @At("TAIL"))
    private void sparktraits$normalizeConscienceProtectedTarget(GameWorldComponent gameWorldComponent, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer && player.getWorld() instanceof ServerWorld serverWorld
                && ConscienceSerialKillerService.isConscienceSerialKiller(player, gameWorldComponent)) {
            ConscienceSerialKillerService.normalizeTarget(serverWorld, gameWorldComponent, serverPlayer);
        }
    }

    @Inject(method = "getEligibleTargets", at = @At("HEAD"), cancellable = true)
    private void sparktraits$effectiveSerialKillerTargets(
            GameWorldComponent gameWorldComponent,
            ServerWorld serverWorld,
            CallbackInfoReturnable<List<UUID>> cir
    ) {
        cir.setReturnValue(ConscienceSerialKillerService.serialKillerTargets(serverWorld, gameWorldComponent, player));
    }

    @Inject(method = "isTargetValid", at = @At("HEAD"), cancellable = true)
    private void sparktraits$effectiveSerialKillerTargetStillValid(
            UUID targetUuid,
            GameWorldComponent gameWorldComponent,
            ServerWorld serverWorld,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(ConscienceSerialKillerService.isSerialKillerTargetValid(
                serverWorld,
                gameWorldComponent,
                player,
                targetUuid
        ));
    }

    @Inject(method = "onTargetDeath", at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockConscienceTargetDeathReassign(GameWorldComponent gameWorldComponent, CallbackInfo ci) {
        if (ConscienceSerialKillerService.shouldBlockSerialKillerRetarget(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "reassignTarget", at = @At("HEAD"), cancellable = true)
    private void sparktraits$blockConscienceReassign(GameWorldComponent gameWorldComponent, ServerWorld serverWorld, CallbackInfo ci) {
        if (ConscienceSerialKillerService.shouldBlockSerialKillerRetarget(player)) {
            ci.cancel();
        }
    }
}
