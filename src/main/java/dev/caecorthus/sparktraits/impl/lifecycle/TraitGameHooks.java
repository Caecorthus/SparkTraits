package dev.caecorthus.sparktraits.impl.lifecycle;

import dev.caecorthus.sparktraits.api.SparkTraitsApi;
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
import dev.caecorthus.sparktraits.impl.compatibility.noellesroles.SilencedKillerRestrictionService;
import dev.caecorthus.sparktraits.impl.compatibility.sparkfactionapi.SparkFactionApiEffectiveFactionBridge;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBombService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceEconomyService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConsciencePoisonerService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceSerialKillerService;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorBodyguardService;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorRevolverService;
import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService;
import dev.caecorthus.sparktraits.impl.traits.global.pig.PigTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.VigilanteVeteranTraitService;

public final class TraitGameHooks {
    private TraitGameHooks() {
    }

    public static void register() {
        EffectiveTraitService.register();
        SparkFactionApiEffectiveFactionBridge.register();
        GlobalTraitService.register();
        CivilianTraitService.register();
        KillerTraitService.register();
        VigilanteVeteranTraitService.register();
        ImpostorRevolverService.register();
        ConscienceSerialKillerService.register();
        ConsciencePoisonerService.register();
        ConscienceBomberFrenzyService.register();
        SilencedKillerRestrictionService.register();
        DepressionTraitService.register();
        ResetPlayer.EVENT.register(player -> {
            boolean wraithActive = SparkTraitsApi.isWraithActive(player);
            ConscienceBombService.clearTimedBomb(player);
            ConscienceBomberFrenzyService.clearPlayer(player);
            ConscienceSerialKillerService.clearPlayer(player);
            LastStandService.clearPlayer(player);
            DepressionTraitService.clearPlayer(player);
            if (shouldClearTraitsOnReset(wraithActive)) {
                TraitPlayerComponent.KEY.get(player).clearActiveTraits(TraitRemovalReason.RESET);
            }
        });

        KillPlayer.BEFORE.register(LastStandService::beforeKill);
        KillPlayer.BEFORE.register(DepressionTraitService::beforeKill);

        KillPlayer.AFTER.register((victim, killer, deathReason) -> {
            TraitPlayerComponent playerTraits = TraitPlayerComponent.KEY.get(victim);
            TraitWorldComponent.KEY.get(victim.getWorld()).snapshotDeathTraits(victim.getUuid(), playerTraits.getActiveTraitIds());
            boolean lastStandStarted = LastStandService.tryStartAfterKill(victim, killer, deathReason);
            PigTraitService.playDeathSound(victim);
            EffectiveTraitService.handleAfterKill(victim, killer, deathReason);
            DepressionTraitService.handleAfterKill(victim, killer, deathReason);
            if (lastStandStarted) {
                syncPlayerTraitsToNewSpectators((ServerWorld) victim.getWorld(), GameWorldComponent.KEY.get(victim.getWorld()));
                return;
            }
            ConscienceEconomyService.rewardAfterConfirmedRealDeath(victim);
            KillerTraitService.handleAfterRealKill(victim, killer, deathReason);
            ImpostorBodyguardService.handleAfterKill(victim);
            ConscienceSerialKillerService.handleAfterKill(victim, killer, deathReason);
            ConscienceBomberFrenzyService.clearPlayer(victim);
            playerTraits.clearActiveTraits(TraitRemovalReason.DEATH);
            ConscienceSerialKillerService.clearPlayer(victim);
            DepressionTraitService.clearPlayer(victim);
            syncPlayerTraitsToNewSpectators((ServerWorld) victim.getWorld(), GameWorldComponent.KEY.get(victim.getWorld()));
        });

        dev.doctor4t.wathe.api.event.GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }
            ConscienceBombService.clearAll();
            ConscienceBomberFrenzyService.clearAll(serverWorld);
            ConscienceSerialKillerService.clearAll();
            LastStandService.clearRoundState(serverWorld);
            DepressionTraitService.clearRoundState(serverWorld);
            clearActiveTraits(serverWorld, gameComponent);
        });
    }

    /**
     * Active Wraith trait cleanup is deferred to SparkWitch so listener order cannot downgrade GAME_END to RESET.
     * 激活冤魂的天赋清理由 SparkWitch 接管，避免监听顺序把 GAME_END 降级为 RESET。
     */
    static boolean shouldClearTraitsOnReset(boolean wraithActive) {
        return !wraithActive;
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
