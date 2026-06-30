package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.ConscienceSerialKillerService;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.LastStandFinalMomentService;
import dev.caecorthus.sparktraits.impl.LastStandService;
import dev.caecorthus.sparktraits.impl.TraitAssignmentService;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mixin(value = MurderGameMode.class, remap = false)
public abstract class MurderGameModeMixin {
    @Redirect(
            method = "initializeGame",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/game/gamemode/MurderGameMode;assignRolesAndGetKillerCount(Lnet/minecraft/server/world/ServerWorld;Ljava/util/List;Ldev/doctor4t/wathe/cca/GameWorldComponent;)I"
            )
    )
    private int sparktraits$assignTraitsBeforeWelcome(
            ServerWorld world,
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent
    ) {
        Set<UUID> lockedRolePlayers = sparktraits$snapshotLockedRolePlayers(world);
        int publicKillerCount = sparktraits$assignRolesAndGetKillerCount(world, players, gameComponent);
        LastStandService.clearRoundState(world);
        TraitWorldComponent.KEY.get(world).clearRoundState();
        return TraitAssignmentService.assignForMurderGameBeforeWelcome(
                world,
                gameComponent,
                players,
                publicKillerCount,
                lockedRolePlayers
        );
    }

    private Set<UUID> sparktraits$snapshotLockedRolePlayers(ServerWorld world) {
        ScoreboardRoleSelectorComponent selector = ScoreboardRoleSelectorComponent.KEY.get(world.getScoreboard());
        Set<UUID> lockedRolePlayers = new LinkedHashSet<>();
        for (List<UUID> forcedPlayers : selector.forcedRoles.values()) {
            lockedRolePlayers.addAll(forcedPlayers);
        }
        return lockedRolePlayers;
    }

    @Invoker("assignRolesAndGetKillerCount")
    static int sparktraits$assignRolesAndGetKillerCount(
            ServerWorld world,
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent
    ) {
        throw new AssertionError();
    }

    @Redirect(
            method = "tickServerGameLoop",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;canUseKillerFeatures(Lnet/minecraft/entity/player/PlayerEntity;)Z"
            )
    )
    private boolean sparktraits$passiveMoneyOnlyForRealKillers(GameWorldComponent gameComponent, PlayerEntity player) {
        return ConscienceSerialKillerService.shouldReceiveKillerPassiveMoney(gameComponent, player);
    }

    @Inject(method = "tickServerGameLoop", at = @At("RETURN"))
    private void sparktraits$selfRealizeUnsupportedImpostors(
            ServerWorld serverWorld,
            GameWorldComponent gameWorldComponent,
            CallbackInfo ci
    ) {
        // Run after other win-condition hooks so neutral blockers can keep the round alive first.
        // 在其他胜利判定钩子之后运行，让中立阻塞者先正常阻止回合结束。
        EffectiveTraitService.killUnsupportedImpostorsIfNoRealKillers(serverWorld, gameWorldComponent);
    }

    @Inject(
            method = "tickServerGameLoop",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/api/event/GameEvents$OnWinDetermined;onWinDetermined(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/cca/GameWorldComponent;Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;Lnet/minecraft/server/network/ServerPlayerEntity;)V"
            ),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void sparktraits$cancelPendingLastStandRoundEnd(
            ServerWorld serverWorld,
            GameWorldComponent gameWorldComponent,
            CallbackInfo ci,
            GameFunctions.WinStatus winStatus,
            ServerPlayerEntity neutralWinner,
            boolean civilianAlive,
            CheckWinCondition.WinResult eventResult
    ) {
        // Cancel before Wathe announces and stores a winner; event ordering may already have skipped our blocker.
        // 在 wathe 宣布并写入胜利者前取消；事件顺序可能已经跳过了亡命徒阻止器。
        if (LastStandService.shouldCancelRoundEndFinalization(LastStandService.hasPendingInWorld(serverWorld))
                || LastStandFinalMomentService.shouldCancelRoundEndFinalization(serverWorld, gameWorldComponent, winStatus)) {
            EffectiveTraitService.killUnsupportedImpostorsIfNoRealKillers(serverWorld, gameWorldComponent);
            ci.cancel();
        }
    }
}
