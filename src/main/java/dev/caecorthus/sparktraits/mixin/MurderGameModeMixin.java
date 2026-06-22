package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.LastStandService;
import dev.caecorthus.sparktraits.impl.TraitAssignmentService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

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
        int publicKillerCount = sparktraits$assignRolesAndGetKillerCount(world, players, gameComponent);
        LastStandService.clearRoundState(world);
        TraitWorldComponent.KEY.get(world).clearRoundState();
        return TraitAssignmentService.assignForMurderGameBeforeWelcome(world, gameComponent, players, publicKillerCount);
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
        return gameComponent.canUseKillerFeatures(player) && !EffectiveTraitService.hasConscience(player);
    }
}
