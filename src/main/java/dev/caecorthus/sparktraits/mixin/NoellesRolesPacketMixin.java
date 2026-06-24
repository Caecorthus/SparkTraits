package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.LastStandService;
import dev.caecorthus.sparktraits.impl.SilencedKillerRestrictionService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.packet.AbilityC2SPacket;
import org.agmas.noellesroles.packet.AssassinGuessRoleC2SPacket;
import org.agmas.noellesroles.packet.DetectiveInvestigateC2SPacket;
import org.agmas.noellesroles.packet.MorphC2SPacket;
import org.agmas.noellesroles.packet.MorphCorpseToggleC2SPacket;
import org.agmas.noellesroles.packet.PartyAnimalBuzzC2SPacket;
import org.agmas.noellesroles.packet.ReporterMarkC2SPacket;
import org.agmas.noellesroles.packet.SilencerSilenceC2SPacket;
import org.agmas.noellesroles.packet.SpiritProjectC2SPacket;
import org.agmas.noellesroles.packet.SwapperC2SPacket;
import org.agmas.noellesroles.packet.TaotieSwallowC2SPacket;
import org.agmas.noellesroles.packet.VultureEatC2SPacket;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Noellesroles.class, remap = false)
public abstract class NoellesRolesPacketMixin {
    @Inject(method = "lambda$registerPackets$30", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockSilencedMorph(MorphC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        sparktraits$blockSilencedKillerAbility(context.player(), ci);
    }

    @Inject(method = "lambda$registerPackets$31", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockSilencedMorphCorpseToggle(MorphCorpseToggleC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        sparktraits$blockSilencedKillerAbility(context.player(), ci);
    }

    @Inject(method = "lambda$registerPackets$33", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockSilencedVultureEat(VultureEatC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        sparktraits$blockSilencedKillerAbility(context.player(), ci);
    }

    @Inject(method = "lambda$registerPackets$35", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockSilencedSharedAbility(AbilityC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        sparktraits$blockSilencedKillerAbility(context.player(), ci);
    }

    @Inject(method = "lambda$registerPackets$36", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockLastStandAssassination(AssassinGuessRoleC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity assassin = context.player();
        if (sparktraits$blockSilencedKillerAbility(assassin, ci)) {
            return;
        }
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
        if (sparktraits$blockSilencedKillerAbility(swapper, ci)) {
            return;
        }
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

    @Inject(method = "lambda$registerPackets$37", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockSilencedReporterMark(ReporterMarkC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        sparktraits$blockSilencedKillerAbility(context.player(), ci);
    }

    @Inject(method = "lambda$registerPackets$38", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockSilencedDetectiveInvestigate(DetectiveInvestigateC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        sparktraits$blockSilencedKillerAbility(context.player(), ci);
    }

    @Inject(method = "lambda$registerPackets$39", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockSilencedTaotieSwallow(TaotieSwallowC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        sparktraits$blockSilencedKillerAbility(context.player(), ci);
    }

    @Inject(method = "lambda$registerPackets$40", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockSilencedSilencerSilence(SilencerSilenceC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        sparktraits$blockSilencedKillerAbility(context.player(), ci);
    }

    @Inject(method = "lambda$registerPackets$41", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockSilencedPartyAnimalBuzz(PartyAnimalBuzzC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        sparktraits$blockSilencedKillerAbility(context.player(), ci);
    }

    @Redirect(
            method = "lambda$registerPackets$41",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;canUseKillerFeatures(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            ),
            remap = false
    )
    private static boolean sparktraits$partyAnimalRewardIgnoresRawKillerTarget(GameWorldComponent gameComponent, PlayerEntity target) {
        return EffectiveTraitService.shouldBlockPartyAnimalTargetReward(
                gameComponent.getRole(target),
                TraitPlayerComponent.KEY.get(target).getActiveTraitIds()
        );
    }

    @Redirect(
            method = "lambda$registerPackets$41",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isRole(Lnet/minecraft/entity/player/PlayerEntity;Ldev/doctor4t/wathe/api/Role;)Z",
                    ordinal = 1
            ),
            remap = false
    )
    private static boolean sparktraits$partyAnimalRewardIgnoresUndercoverTarget(
            GameWorldComponent gameComponent,
            PlayerEntity target,
            Role role
    ) {
        return EffectiveTraitService.shouldBlockPartyAnimalTargetReward(
                gameComponent.getRole(target),
                TraitPlayerComponent.KEY.get(target).getActiveTraitIds()
        );
    }

    @Inject(method = "lambda$registerPackets$42", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparktraits$blockSilencedSpiritProject(SpiritProjectC2SPacket payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        sparktraits$blockSilencedKillerAbility(context.player(), ci);
    }

    private static boolean sparktraits$blockSilencedKillerAbility(ServerPlayerEntity player, CallbackInfo ci) {
        if (!SilencedKillerRestrictionService.denyActiveAbilityIfRestricted(player)) {
            return false;
        }
        ci.cancel();
        return true;
    }
}
