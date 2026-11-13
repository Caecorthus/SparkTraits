package dev.caecorthus.sparktraits.impl.lifecycle;

import dev.caecorthus.sparktraits.api.SparkTraitsApi;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.api.event.ResetPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.UUID;
import dev.caecorthus.sparktraits.impl.compatibility.noellesroles.SilencedKillerRestrictionService;
import dev.caecorthus.sparktraits.impl.compatibility.sparkfactionapi.SparkFactionApiEffectiveFactionBridge;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.effective.death.EffectiveDeathConsequenceRules;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.BluePoisonMarkerSync;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBombService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceEconomyService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConsciencePoisonerService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceSerialKillerService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceWalkieTalkieService;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.AnxietyService;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.DopamineService;
import dev.caecorthus.sparktraits.impl.traits.global.EconomyService;
import dev.caecorthus.sparktraits.impl.traits.global.SensitiveService;
import dev.caecorthus.sparktraits.impl.traits.global.StandsFirmService;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorBodyguardService;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorRevolverService;
import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraitService;
import dev.caecorthus.sparktraits.impl.traits.killer.ExhilaratedService;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.CloseQuartersService;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.ForcedMeleeCooldownService;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService;
import dev.caecorthus.sparktraits.impl.traits.global.pig.PigTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.VigilanteVeteranTraitService;

public final class TraitGameHooks {
    // Set while a server runs; ON_GAME_START has no world to reach it through. 服务器运行期间有效；ON_GAME_START 不携带世界。
    private static MinecraftServer runningServer;

    private TraitGameHooks() {
    }

    public static void register() {
        EffectiveTraitService.register();
        SparkFactionApiEffectiveFactionBridge.register();
        GlobalTraitService.register();
        CivilianTraitService.register();
        KillerTraitService.register();
        ForcedMeleeCooldownService.register();
        CloseQuartersService.register();
        LastEscapeService.register();
        VigilanteVeteranTraitService.register();
        ImpostorRevolverService.register();
        ConscienceSerialKillerService.register();
        ConsciencePoisonerService.register();
        BluePoisonMarkerSync.register();
        ConscienceWalkieTalkieService.register();
        SilencedKillerRestrictionService.register();
        DepressionTraitService.register();
        EconomyService.register();
        AnxietyService.register();
        DopamineService.register();
        SensitiveService.register();
        // Ahead of the Last Stand and Depression kill listeners below: a blocked same-faction kill must not spend them.
        // 必须排在下方背水一战与抑郁的击杀监听之前：被拦下的同阵营击杀不应消耗它们。
        StandsFirmService.register();
        ResetPlayer.EVENT.register(player -> {
            boolean wraithActive = SparkTraitsApi.isWraithActive(player);
            ConscienceBombService.clearTimedBomb(player);
            ConscienceSerialKillerService.clearPlayer(player);
            LastStandService.clearPlayer(player);
            // Wathe has just reset this player; the reset variant drops state without replaying a "restore".
            // Wathe 刚重置过该玩家；重置专用版本只清理状态，不再重放“恢复”。
            DepressionTraitService.clearPlayerAfterReset(player);
            DopamineService.clear(player);
            if (shouldClearTraitsOnReset(wraithActive)) {
                TraitPlayerComponent.KEY.get(player).clearActiveTraits(TraitRemovalReason.RESET);
            }
        });

        KillPlayer.BEFORE.register((victim, killer, reason) -> TerminalDeathRules.contains(reason)
                ? null : LastStandService.beforeKill(victim, killer, reason));
        KillPlayer.BEFORE.register((victim, killer, reason) -> TerminalDeathRules.contains(reason)
                ? null : DepressionTraitService.beforeKill(victim, killer, reason));

        KillPlayer.AFTER.register((victim, killer, deathReason) -> {
            LastEscapeService.onDeath(victim);
            TraitPlayerComponent playerTraits = TraitPlayerComponent.KEY.get(victim);
            TraitWorldComponent.KEY.get(victim.getWorld()).snapshotDeathTraits(victim.getUuid(), playerTraits.getActiveTraitIds());
            boolean terminal = TerminalDeathRules.contains(deathReason);
            if (terminal) LastStandService.clearPlayer(victim);
            boolean lastStandStarted = !terminal && LastStandService.tryStartAfterKill(victim, killer, deathReason);
            PigTraitService.playDeathSound(victim);
            EffectiveTraitService.handleAfterKill(victim, killer, deathReason);
            DepressionTraitService.handleAfterKill(victim, killer, deathReason);
            if (lastStandStarted) {
                syncPlayerTraitsToNewSpectators(victim);
                return;
            }
            ConscienceEconomyService.rewardAfterConfirmedRealDeath(victim);
            KillerTraitService.handleAfterRealKill(victim, killer, deathReason);
            ExhilaratedService.afterRealKill(victim, killer);
            ImpostorBodyguardService.handleAfterKill(victim);
            ConscienceSerialKillerService.handleAfterKill(victim, killer, deathReason);
            playerTraits.clearActiveTraits(TraitRemovalReason.DEATH);
            ConscienceSerialKillerService.clearPlayer(victim);
            DepressionTraitService.clearPlayer(victim);
            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(victim.getWorld());
            // Publish Wathe's confirmed-death set before syncing spectator-visible trait state.
            // 在同步旁观者可见的天赋状态前，先发布 Wathe 的已确认死亡集合。
            gameComponent.sync();
            syncPlayerTraitsToNewSpectators(victim);
        });

        /*
         * Round-scoped TraitWorldComponent state (Final Moment flag and loose ends, Last Stand triggers, round and
         * death trait snapshots, unique-trait occupancy) was cleared only by Murder's role assignment, so a Loose Ends
         * or Discovery round that followed a Murder round inherited it. ON_GAME_START precedes every mode's
         * initialize, but carries no world, hence the server reference (as LastEscapeService does). Only worlds
         * without a running round are cleared: the starting world is still STARTING here. Not done at finalize,
         * because the finished round's state stays readable until the next round starts.
         * 回合级的 TraitWorldComponent 状态（终局时刻标记与亡命徒、背水一战触发记录、本局与死亡天赋快照、唯一天赋占用）
         * 此前只在谋杀模式分配身份时清理，因此紧随其后的 Loose Ends 或 Discovery 回合会继承这些状态。ON_GAME_START
         * 早于所有模式的初始化，但不携带世界，所以与 LastEscapeService 一样保存服务器引用。只清理没有进行中回合的世界：
         * 正在开始的世界此时仍处于 STARTING。不在结算时清理，因为已结束回合的状态需保留到下一回合开始前仍可读取。
         */
        ServerLifecycleEvents.SERVER_STARTED.register(server -> runningServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> runningServer = null);
        dev.doctor4t.wathe.api.event.GameEvents.ON_GAME_START.register(mode -> {
            MinecraftServer server = runningServer;
            if (server == null) {
                return;
            }
            for (ServerWorld serverWorld : server.getWorlds()) {
                if (shouldClearRoundStateOnGameStart(GameWorldComponent.KEY.get(serverWorld).isRunning())) {
                    TraitWorldComponent.KEY.get(serverWorld).clearRoundState();
                }
            }
        });

        dev.doctor4t.wathe.api.event.GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }
            LastEscapeService.clearRound(serverWorld);
            ConscienceBombService.clearAll();
            ConscienceSerialKillerService.clearAll();
            LastStandService.clearRoundState(serverWorld);
            DepressionTraitService.clearRoundState(serverWorld);
            DopamineService.clearAll();
            // Attributions for victims who never died this round must not reach a death in the next one.
            // 本局未死亡受害者的毒源归因不能带到下一局的死亡判定。
            EffectiveDeathConsequenceRules.clearPoisonSources();
            clearActiveTraits(serverWorld, gameComponent);
        });
    }

    /**
     * A world whose round is ACTIVE or STOPPING keeps its round state when another world's game starts.
     * 其他世界开始对局时，回合处于 ACTIVE 或 STOPPING 的世界保留自己的回合状态。
     */
    static boolean shouldClearRoundStateOnGameStart(boolean roundRunning) {
        return !roundRunning;
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

    /**
     * A death changes only the victim's own view, so only the victim needs everyone's spectator-visible traits re-sent.
     * 死亡只改变死者自己的视角，因此只需把所有人的旁观者可见天赋重发给死者。
     */
    private static void syncPlayerTraitsToNewSpectators(ServerPlayerEntity victim) {
        TraitPlayerComponent.KEY.get(victim).resyncFilteredStateNow();
    }
}
