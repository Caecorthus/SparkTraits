package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandFinalMomentService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService;
import dev.caecorthus.sparktraits.impl.assignment.TraitAssignmentService;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.ScoreboardRoleSelectorComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mixin(value = MurderGameMode.class, remap = false)
public abstract class MurderGameModeMixin {
    @Inject(method = "assignRolesAndGetKillerCount", at = @At("HEAD"))
    private static void sparktraits$snapshotLockedRolePlayers(
            ServerWorld world,
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent,
            CallbackInfoReturnable<Integer> cir,
            @Share("lockedRolePlayers") LocalRef<Set<UUID>> lockedRolePlayers
    ) {
        // Forced roles are consumed during selection, so record /forcerole targets first.
        // 强制身份会在抽选中被消耗，因此先记录 /forcerole 目标。
        ScoreboardRoleSelectorComponent selector = ScoreboardRoleSelectorComponent.KEY.get(world.getScoreboard());
        Set<UUID> forced = new LinkedHashSet<>();
        for (List<UUID> forcedPlayers : selector.forcedRoles.values()) {
            forced.addAll(forcedPlayers);
        }
        lockedRolePlayers.set(forced);
    }

    @Inject(
            method = "assignRolesAndGetKillerCount",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/ScoreboardRoleSelectorComponent;assignCivilians(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/cca/GameWorldComponent;Ljava/util/List;)I",
                    shift = At.Shift.AFTER
            )
    )
    private static void sparktraits$planTraitsBeforeRoleAssigned(
            ServerWorld world,
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent,
            CallbackInfoReturnable<Integer> cir,
            @Local(ordinal = 0) int killerCount,
            @Share("lockedRolePlayers") LocalRef<Set<UUID>> lockedRolePlayers,
            @Share("traitPlan") LocalRef<TraitAssignmentService.RoundPlan> traitPlan
    ) {
        // Every role is picked but none announced: Conscience compensation must convert its civilian here, before
        // RoleAssigned hands out kits and Wathe records role history.
        // 身份已全部选定但尚未公布：善良补偿必须在此转换好人，早于 RoleAssigned 发放道具与 Wathe 记录身份历史。
        LastStandService.clearRoundState(world);
        traitPlan.set(TraitAssignmentService.planBeforeRoleAssigned(
                world,
                gameComponent,
                players,
                killerCount,
                lockedRolePlayers.get()
        ));
    }

    @Inject(method = "assignRolesAndGetKillerCount", at = @At("RETURN"), cancellable = true)
    private static void sparktraits$applyTraitsBeforeWelcome(
            ServerWorld world,
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent,
            CallbackInfoReturnable<Integer> cir,
            @Share("traitPlan") LocalRef<TraitAssignmentService.RoundPlan> traitPlan
    ) {
        cir.setReturnValue(TraitAssignmentService.applyAfterRoleAssigned(world, gameComponent, players, traitPlan.get()));
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

    @ModifyVariable(
            method = "tickServerGameLoop",
            at = @At(
                    value = "FIELD",
                    target = "Ldev/doctor4t/wathe/api/event/CheckWinCondition;EVENT:Lnet/fabricmc/fabric/api/event/Event;",
                    opcode = Opcodes.GETSTATIC
            ),
            ordinal = 0
    )
    private GameFunctions.WinStatus sparktraits$resolveFinalMomentTimeout(
            GameFunctions.WinStatus winStatus,
            ServerWorld serverWorld,
            GameWorldComponent gameWorldComponent
    ) {
        // Rewrite Wathe's TIME before any listener sees it, and before the null-result fallback reuses it.
        // 在任何监听器看到之前改写 wathe 的 TIME，也覆盖监听器全返回 null 时沿用的本地状态。
        return LastStandFinalMomentService.resolveFinalMomentTimeout(serverWorld, gameWorldComponent, winStatus);
    }

    @Inject(
            method = "tickServerGameLoop",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/api/event/CheckWinCondition;checkWin(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/cca/GameWorldComponent;Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;)Ldev/doctor4t/wathe/api/event/CheckWinCondition$WinResult;"
            ),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void sparktraits$startFinalMomentBeforeWinConditionHooks(
            ServerWorld serverWorld,
            GameWorldComponent gameWorldComponent,
            CallbackInfo ci,
            GameFunctions.WinStatus winStatus,
            ServerPlayerEntity neutralWinner,
            boolean civilianAlive
    ) {
        // Start Final Moment before earlier neutral blockers, such as NoellesRoles Jester Moment, can short-circuit the event chain.
        // 在 NoellesRoles 小丑时刻等更早的中立阻塞者截断胜利事件前，先启动背水一战终局时刻。
        if (LastStandFinalMomentService.triggerFinalMomentIfEligible(serverWorld, gameWorldComponent, winStatus)) {
            ci.cancel();
        }
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
