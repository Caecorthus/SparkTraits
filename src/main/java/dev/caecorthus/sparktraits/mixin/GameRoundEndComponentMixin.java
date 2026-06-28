package dev.caecorthus.sparktraits.mixin;

import com.mojang.authlib.GameProfile;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.LastStandFinalMomentService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            ) || EffectiveTraitService.didEffectiveTeamWin(
                    winStatus,
                    role,
                    sparktraits$roundEndTraits(serverWorld, traitWorld, uuid)
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

    private static Collection<Identifier> sparktraits$roundEndTraits(
            ServerWorld serverWorld,
            TraitWorldComponent traitWorld,
            UUID uuid
    ) {
        PlayerEntity player = serverWorld.getPlayerByUuid(uuid);
        if (player != null) {
            List<Identifier> activeTraits = TraitPlayerComponent.KEY.get(player).getActiveTraitIds();
            if (!activeTraits.isEmpty()) {
                return activeTraits;
            }
        }
        List<Identifier> deathTraits = traitWorld.getDeathTraitSnapshot(uuid);
        if (!deathTraits.isEmpty()) {
            return deathTraits;
        }
        return traitWorld.getRoundTraitSnapshot(uuid);
    }
}
