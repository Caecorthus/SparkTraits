package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

/**
 * Keeps a Last Stand survivor's revolver in their inventory during Wathe death drops.
 * 在 Wathe 死亡掉落阶段保留背水一战幸存者原本携带的左轮手枪。
 */
@Mixin(value = GameFunctions.class, priority = 2000, remap = false)
public abstract class GameFunctionsMixin {
    @WrapMethod(method = "killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V")
    private static void sparktraits$guardAndScopeKillAttempt(
            ServerPlayerEntity victim, boolean spawnBody, ServerPlayerEntity killer,
            Identifier deathReason, boolean force, Operation<Void> original
    ) {
        // WrapMethod encloses injected cancellation returns, unlike HEAD/RETURN pairing.
        if (LastEscapeService.blocksDeath(victim, deathReason)) return;
        boolean trainEscape = LastEscapeService.isActive(victim) && LastEscapeService.isTrainDeath(deathReason);
        KillerTraitService.beginKillAttempt(victim, killer, force || trainEscape);
        boolean completed = false;
        try (var scope = new dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeKillScope()) {
            original.call(victim, spawnBody, killer, deathReason, force || trainEscape);
            completed = true;
        } finally {
            if (completed) KillerTraitService.finishKillAttempt(victim, spawnBody, killer, deathReason, force || trainEscape);
            else KillerTraitService.abortKillAttempt();
        }
    }

    @Inject(method = "shouldDropOnDeath", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$keepLastStandRevolver(ItemStack stack, PlayerEntity victim, CallbackInfoReturnable<Boolean> cir) {
        if (LastStandService.shouldPreventDeathDrop(victim, stack)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;canUseKillerFeatures(Lnet/minecraft/entity/player/PlayerEntity;)Z")
    )
    private static boolean sparktraits$rewardOnlyRealKillers(GameWorldComponent gameComponent, PlayerEntity player) {
        return gameComponent.canUseKillerFeatures(player) && !EffectiveTraitService.hasConscience(player);
    }

    @Redirect(
            method = "killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;getAllKillerTeamPlayers()Ljava/util/List;")
    )
    private static List<UUID> sparktraits$rewardPoolOnlyRealKillers(GameWorldComponent gameComponent) {
        if (!(((GameWorldComponentAccessor) gameComponent).sparktraits$getWorld() instanceof ServerWorld world)) {
            return gameComponent.getAllKillerTeamPlayers();
        }
        return gameComponent.getAllKillerTeamPlayers().stream()
                .filter(uuid -> world.getPlayerByUuid(uuid) instanceof PlayerEntity player && !EffectiveTraitService.hasConscience(player))
                .toList();
    }
}
