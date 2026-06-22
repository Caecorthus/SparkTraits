package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.LastStandService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.packet.AssassinGuessRoleC2SPacket;
import org.agmas.noellesroles.packet.SwapperC2SPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Noellesroles.class, remap = false)
public abstract class NoellesRolesPacketMixin {
    @Inject(method = "lambda$registerPackets$36", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockLastStandAssassination(AssassinGuessRoleC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity assassin = context.player();
        ServerWorld world = assassin.getServerWorld();
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(world);

        if (!gameWorldComponent.isRole(assassin, Noellesroles.ASSASSIN)) {
            return;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(assassin) || SwallowedPlayerComponent.isPlayerSwallowed(assassin)) {
            return;
        }
        if (!AssassinPlayerComponent.KEY.get(assassin).canGuess()) {
            return;
        }
        if (payload.targetPlayer() == null || payload.targetPlayer().equals(assassin.getUuid())) {
            return;
        }
        if (!(world.getPlayerByUuid(payload.targetPlayer()) instanceof ServerPlayerEntity target)) {
            return;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(target)) {
            return;
        }
        if (LastStandService.isProtectedFromNoellesRoleUtility(target)) {
            LastStandService.notifyCannotAssassinate(assassin);
            ci.cancel();
        }
    }

    @Inject(method = "lambda$registerPackets$34", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockLastStandSwap(SwapperC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity swapper = context.player();
        ServerWorld world = swapper.getServerWorld();
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(world);

        if (!gameWorldComponent.isRole(swapper, Noellesroles.SWAPPER)) {
            return;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(swapper) || SwallowedPlayerComponent.isPlayerSwallowed(swapper)) {
            return;
        }
        if (payload.player() == null || payload.player2() == null) {
            return;
        }

        PlayerEntity player1 = world.getPlayerByUuid(payload.player());
        PlayerEntity player2 = world.getPlayerByUuid(payload.player2());
        if (player1 == null || player2 == null) {
            return;
        }
        if (SwallowedPlayerComponent.isPlayerSwallowed(player1) || SwallowedPlayerComponent.isPlayerSwallowed(player2)) {
            return;
        }
        if (LastStandService.isProtectedFromNoellesRoleUtility(player1)
                || LastStandService.isProtectedFromNoellesRoleUtility(player2)) {
            LastStandService.notifyCannotSwap(swapper);
            ci.cancel();
        }
    }
}
