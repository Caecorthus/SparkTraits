package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.api.event.ResetPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.UUID;

public final class TraitGameHooks {
    private TraitGameHooks() {
    }

    public static void register() {
        EffectiveTraitService.register();
        GlobalTraitService.register();
        GoodTraitService.register();
        DepressionTraitService.register();
        KillerTraitService.register();
        VigilanteVeteranTraitService.register();
        ImpostorRevolverService.register();
        ConscienceSerialKillerService.register();
        ConsciencePoisonerService.register();
        SilencedKillerRestrictionService.register();
        ResetPlayer.EVENT.register(player -> {
            ConscienceBombService.clearTimedBomb(player);
            ConscienceSerialKillerService.clearPlayer(player);
            LastStandService.clearPlayer(player);
            DepressionTraitService.clearPlayer(player);
            TraitPlayerComponent.KEY.get(player).clearActiveTraits(TraitRemovalReason.RESET);
        });

        KillPlayer.BEFORE.register(DepressionTraitService::beforeKill);
        KillPlayer.BEFORE.register(LastStandService::beforeKill);

        KillPlayer.AFTER.register((victim, killer, deathReason) -> {
            TraitPlayerComponent playerTraits = TraitPlayerComponent.KEY.get(victim);
            TraitWorldComponent.KEY.get(victim.getWorld()).snapshotDeathTraits(victim.getUuid(), playerTraits.getActiveTraitIds());
            boolean lastStandStarted = LastStandService.tryStartAfterKill(victim, killer, deathReason);
            DepressionTraitService.handleAfterKill(victim, killer);
            EffectiveTraitService.handleAfterKill(victim, killer, deathReason);
            if (lastStandStarted) {
                syncPlayerTraitsToNewSpectators((ServerWorld) victim.getWorld(), GameWorldComponent.KEY.get(victim.getWorld()));
                return;
            }
            KillerTraitService.handleAfterRealKill(victim, killer, deathReason);
            ImpostorBodyguardService.handleAfterKill(victim);
            ConscienceSerialKillerService.handleAfterKill(victim, killer, deathReason);
            playerTraits.clearActiveTraits(TraitRemovalReason.DEATH);
            ConscienceSerialKillerService.clearPlayer(victim);
            syncPlayerTraitsToNewSpectators((ServerWorld) victim.getWorld(), GameWorldComponent.KEY.get(victim.getWorld()));
        });

        dev.doctor4t.wathe.api.event.GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }
            ConscienceBombService.clearAll();
            ConscienceSerialKillerService.clearAll();
            LastStandService.clearRoundState(serverWorld);
            DepressionTraitService.clearRoundState(serverWorld);
            clearActiveTraits(serverWorld, gameComponent);
        });
    }

    private static void clearActiveTraits(ServerWorld world, GameWorldComponent gameComponent) {
        for (UUID uuid : new ArrayList<>(gameComponent.getAllPlayers())) {
            if (world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player) {
                TraitPlayerComponent.KEY.get(player).clearActiveTraits(TraitRemovalReason.GAME_END);
            }
        }
    }

    private static void syncPlayerTraitsToNewSpectators(ServerWorld world, GameWorldComponent gameComponent) {
        for (UUID uuid : gameComponent.getAllPlayers()) {
            if (world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player) {
                TraitPlayerComponent.KEY.sync(player);
            }
        }
    }
}
