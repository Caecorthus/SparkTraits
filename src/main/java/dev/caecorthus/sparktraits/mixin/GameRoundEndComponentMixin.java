package dev.caecorthus.sparktraits.mixin;

import com.mojang.authlib.GameProfile;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.lifecycle.RoundEndTraitResolver;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandFinalMomentService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import dev.caecorthus.sparktraits.SparkTraits;

/**
 * Applies SparkTraits effective-team winners to Wathe's round-end data.
 * 将 SparkTraits 的有效阵营胜负关系写入 Wathe 回合结束数据。
 */
@Mixin(value = GameRoundEndComponent.class, remap = false)
public abstract class GameRoundEndComponentMixin {
    @Shadow
    @Final
    private List<GameRoundEndComponent.RoundEndData> players;

    @Shadow
    private GameFunctions.WinStatus winStatus;

    @Shadow
    private @Nullable Identifier gameMode;

    @Shadow
    public abstract void sync();

    @Inject(method = "setRoundEndData(Lnet/minecraft/server/world/ServerWorld;Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void sparktraits$setEffectiveRoundEndData(
            ServerWorld serverWorld,
            GameFunctions.WinStatus winStatus,
            CallbackInfo ci
    ) {
        this.players.clear();
        GameWorldComponent game = GameWorldComponent.KEY.get(serverWorld);
        TraitWorldComponent traitWorld = TraitWorldComponent.KEY.get(serverWorld);
        this.gameMode = game.getGameMode() != null ? game.getGameMode().identifier : null;

        for (Map.Entry<UUID, Role> entry : game.getRoles().entrySet()) {
            UUID uuid = entry.getKey();
            Role role = entry.getValue();
            GameProfile profile = game.getGameProfiles().get(uuid);

            if (profile == null || role == WatheRoles.NO_ROLE) {
                continue;
            }

            boolean wasDead = game.isPlayerDead(uuid);
            boolean isOnline = serverWorld.getPlayerByUuid(uuid) != null;
            GameRoundEndComponent.PlayerEndStatus endStatus = sparktraits$endStatus(wasDead, isOnline);
            boolean isWinner = LastStandFinalMomentService.didFinalMomentPlayerWin(
                    winStatus,
                    role,
                    traitWorld.isFinalMomentLooseEnd(uuid)
            ) || isWinningJester(serverWorld, uuid, role)
                    || EffectiveTraitService.didEffectiveTeamWin(
                    winStatus,
                    role,
                    RoundEndTraitResolver.resolve(serverWorld, traitWorld, uuid)
            );

            this.players.add(new GameRoundEndComponent.RoundEndData(profile, role.identifier(), endStatus, isWinner));
        }

        this.winStatus = winStatus;
        this.sync();
        ci.cancel();
    }

    @Inject(method = "didWin", at = @At("HEAD"), cancellable = true)
    private void sparktraits$didWinFromRoundEndData(UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        if (GameFunctions.WinStatus.NONE == this.winStatus) {
            cir.setReturnValue(false);
            return;
        }
        for (GameRoundEndComponent.RoundEndData detail : this.players) {
            if (detail.player().getId().equals(uuid)) {
                cir.setReturnValue(detail.isWinner());
                return;
            }
        }
        cir.setReturnValue(false);
    }

    /**
     * A triggered hidden Jester is the real NoellesRoles Jester role; NoellesRoles marks the win on its component,
     * and that flag must survive SparkTraits' replacement of the round-end data so the player gets the Jester screen.
     * 触发后的隐藏小丑就是真正的 NoellesRoles 小丑；NoellesRoles 会把胜利写在组件上，SparkTraits 替换回合结束数据时必须
     * 保留该标志，玩家才能看到小丑结算界面。
     */
    private static boolean isWinningJester(ServerWorld world, UUID uuid, @Nullable Role role) {
        if (role == null || !Noellesroles.JESTER_ID.equals(role.identifier())) {
            return false;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
        return player != null && JesterPlayerComponent.KEY.get(player).won;
    }

    private static GameRoundEndComponent.PlayerEndStatus sparktraits$endStatus(boolean wasDead, boolean isOnline) {
        if (wasDead) {
            return isOnline
                    ? GameRoundEndComponent.PlayerEndStatus.DEAD
                    : GameRoundEndComponent.PlayerEndStatus.LEFT_DEAD;
        }
        return isOnline
                ? GameRoundEndComponent.PlayerEndStatus.ALIVE
                : GameRoundEndComponent.PlayerEndStatus.LEFT;
    }
}
