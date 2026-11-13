package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import dev.caecorthus.sparkfactionapi.impl.economy.FactionEconomyRules;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.economy.EffectiveEconomyRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.DoorInteraction;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.api.event.PsychoType;
import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheEntities;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraitService;
import dev.caecorthus.sparktraits.impl.resource.SparkTraitsSounds;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;

/**
 * Runtime of the Depression civilian-side trait: the sole owner of every player's fake-death and psycho state, with
 * the listeners, kill hooks, world tick, Last Escape exit, cleanup and the state queries mixins read. Its pure rules
 * live in {@link DepressionRules}, sounds in {@link DepressionAudio}, psycho player effects in
 * {@link DepressionPsychoLoadout} and the standing stamina penalty in {@link DepressionStaminaPenalty}.
 * 抑郁平民阵营天赋的运行时：唯一持有每名玩家假死与疯魔状态的类，负责监听注册、击杀钩子、世界 tick、绝处逢生退出、
 * 清理以及供 mixin 读取的状态查询。纯规则在 {@link DepressionRules}，音效在 {@link DepressionAudio}，
 * 疯魔期间对玩家的效果在 {@link DepressionPsychoLoadout}，常驻体力惩罚在 {@link DepressionStaminaPenalty}。
 */
public final class DepressionTraitService {
    public static final int COLOR = 0x6B7280;
    public static final int ATTACKER_HIGHLIGHT_COLOR = 0xFF0000;
    public static final int VICTIM_HIGHLIGHT_COLOR = 0xFF9F1C;
    public static final int SUICIDE_INTERVAL_TICKS = GameConstants.getInTicks(0, 30);
    public static final int FAKE_DEATH_TICKS = GameConstants.getInTicks(0, 5);
    // Depression psycho is held at Integer.MAX_VALUE; re-top it only after an hour has drained or something cut it short.
    // 抑郁疯魔维持在 Integer.MAX_VALUE；只在消耗超过一小时或被其他逻辑缩短后才重新补满。
    static final int PSYCHO_HOLD_TOP_UP_BELOW_TICKS = Integer.MAX_VALUE - GameConstants.getInTicks(60, 0);
    static final int PSYCHO_ACTIONBAR_REFRESH_TICKS = 10;
    private static final Text PSYCHO_ACTIONBAR = Text.literal("我们今天都会死。一个比另一个晚一点罢了").withColor(ATTACKER_HIGHLIGHT_COLOR);
    private static final Text ATTACKER_ACTIONBAR = Text.literal("！！反击或逃跑！！").withColor(ATTACKER_HIGHLIGHT_COLOR);
    private static final Text ATTACKER_TITLE = Text.literal("跑").withColor(ATTACKER_HIGHLIGHT_COLOR);
    private static final Map<UUID, PendingState> pendingPlayers = new HashMap<>();
    // Concurrent: Simple Voice Chat asks shouldMuteVoice from its own packet threads; every write stays on the server thread.
    // 并发表：Simple Voice Chat 会在自己的数据包线程上调用 shouldMuteVoice；所有写入仍只在服务端线程进行。
    private static final Map<UUID, ActiveState> activePlayers = new java.util.concurrent.ConcurrentHashMap<>();
    private static final DepressionFakeBodyTracker fakeBodies = new DepressionFakeBodyTracker();
    private static final Set<UUID> forceMentalBreakdownDeaths = new java.util.HashSet<>();

    private DepressionTraitService() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(DepressionTraitService::tickWorld);
        DepressionFakeKillCooldowns.register();
        DoorInteraction.EVENT.register(DepressionTraitService::onDoorInteraction);
        ShopPurchase.BEFORE.register((player, entry, index) ->
                isPsychoActive(player) ? ShopPurchase.PurchaseResult.deny() : null);
        // A singleplayer world can be reopened in the same JVM; never carry this server's round state into the next.
        // 单人存档可在同一 JVM 中重新打开；不能把本服务器的回合状态带到下一次。
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            pendingPlayers.clear();
            activePlayers.clear();
            fakeBodies.clear();
            DepressionFakeKillCooldowns.clear();
            forceMentalBreakdownDeaths.clear();
        });
    }

    public static boolean shouldMuteVoice(ServerPlayerEntity player) {
        return DepressionRules.shouldMuteVoice(isPsychoActive(player));
    }

    public static boolean shouldAllowLowMoodSprint(net.minecraft.entity.player.PlayerEntity player) {
        if (player == null) {
            return false;
        }
        return DepressionRules.shouldAllowLowMoodSprint(
                TraitPlayerComponent.KEY.get(player).isDepressionPsychoActive(),
                isPsychoActive(player)
        );
    }

    public static boolean shouldBlockInventoryInsert(ServerPlayerEntity player, ItemStack stack) {
        return DepressionRules.shouldBlockInventoryInsert(isPsychoActive(player), stack.isOf(WatheItems.BAT));
    }

    public static boolean shouldBlockInventoryInsert(net.minecraft.entity.player.PlayerEntity player, ItemStack stack) {
        return player instanceof ServerPlayerEntity serverPlayer && shouldBlockInventoryInsert(serverPlayer, stack);
    }

    public static boolean shouldBlockDrops(ServerPlayerEntity player) {
        return isPending(player);
    }

    public static boolean isPending(ServerPlayerEntity player) {
        return pendingPlayers.containsKey(player.getUuid());
    }

    /**
     * True while the victim is in a Depression fake death started by this attacker.
     * 受害者正处于由该攻击者触发的抑郁假死中时返回 true。
     */
    public static boolean isPendingFrom(ServerPlayerEntity victim, ServerPlayerEntity attacker) {
        PendingState state = pendingPlayers.get(victim.getUuid());
        return state != null && state.attackerUuid().equals(attacker.getUuid());
    }

    public static boolean isFakeDeathBody(PlayerBodyEntity body) {
        return body != null && fakeBodies.isTracked(
                body.getPlayerUuid(),
                body.getWorld().getRegistryKey().getValue(),
                body.getUuid()
        );
    }

    public static boolean isPsychoActive(net.minecraft.entity.player.PlayerEntity player) {
        return player != null && activePlayers.containsKey(player.getUuid());
    }

    public static boolean isWathePsychoActive(net.minecraft.entity.player.PlayerEntity player) {
        return player != null && PlayerPsychoComponent.KEY.get(player).getPsychoTicks() > 0;
    }

    public static boolean shouldSuppressMentalBreakdown(net.minecraft.entity.player.PlayerEntity player, Identifier deathReason) {
        return DepressionRules.shouldSuppressMentalBreakdown(
                player != null && TraitPlayerComponent.KEY.get(player).hasActiveTrait(CivilianTraits.DEPRESSION),
                isPsychoActive(player),
                isWathePsychoActive(player),
                deathReason
        );
    }

    public static KillPlayer.KillResult beforeKill(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer, Identifier deathReason) {
        if (forceMentalBreakdownDeaths.remove(victim.getUuid())) {
            return null;
        }
        if (isPending(victim)) {
            return KillPlayer.KillResult.cancel();
        }
        if (killer == null || isPsychoActive(victim) || activePlayers.containsKey(victim.getUuid())) {
            return null;
        }
        TraitPlayerComponent victimTraits = TraitPlayerComponent.KEY.get(victim);
        if (!victimTraits.hasActiveTrait(CivilianTraits.DEPRESSION)) {
            return null;
        }
        // Wathe ignores the BEFORE cancel for a forced kill and BEFORE does not expose the flag, so it is read from
        // the kill-attempt scope: the victim really dies, and starting a fake death would add the half reward and
        // the trigger cues on top of the real kill.
        // Wathe 对强制击杀会无视 BEFORE 的取消，而 BEFORE 不提供该标记，因此从击杀尝试作用域读取：受害者会真实死亡，
        // 此时启动假死只会在真实击杀之外多发一半奖励并播放触发提示。
        if (KillerTraitService.isCurrentKillAttemptForced()) {
            return null;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        Role killerRole = game.getRole(killer);
        Collection<Identifier> killerTraits = TraitPlayerComponent.KEY.get(killer).getActiveTraitIds();
        if (EffectiveTraitService.isEffectiveCivilian(killerRole, killerTraits)) {
            return null;
        }
        float mood = PlayerMoodComponent.KEY.get(victim).getMood();
        double roll = victim.getRandom().nextDouble() * 100.0;
        double chance = DepressionRules.psychoCounterChance(mood);
        if (roll >= chance) {
            return null;
        }
        startPending(victim, killer, deathReason, DepressionRules.initialPsychoArmour(
                killerRole,
                JesterPlayerComponent.KEY.get(killer).inPsychoMode
        ));
        rewardFakeDeathKill(victim, killer, game);
        DepressionFakeKillCooldowns.onFakeDeathStarted(killer, victim, deathReason);
        return KillPlayer.KillResult.cancel();
    }

    /**
     * Pays half of what the same kill would have paid, once per started fake death; AFTER-only bonuses stay real-kill only.
     * 每次假死只按同一击杀正常收益的一半发一次钱；只在 AFTER 中发放的额外奖励仍只属于真实击杀。
     */
    private static void rewardFakeDeathKill(ServerPlayerEntity victim, ServerPlayerEntity killer, GameWorldComponent game) {
        // Mirrors GameFunctionsMixin.sparktraits$rewardOnlyRealKillers for Wathe's own killer reward.
        // 与 GameFunctionsMixin.sparktraits$rewardOnlyRealKillers 对 Wathe 杀手奖励的判定保持一致。
        boolean originalKiller = EffectiveEconomyRules.shouldReceiveOriginalKillerReward(
                game.canUseKillerFeatures(killer),
                EffectiveTraitService.hasConscience(killer)
        );
        int impostorReward = EffectiveTraitService.hasImpostor(killer)
                ? EffectiveTraitService.impostorKillReward(
                        game.getRole(victim),
                        TraitPlayerComponent.KEY.get(victim).getActiveTraitIds(),
                        ShopUtils.canAccessShop(killer))
                : 0;
        int reward = DepressionRules.fakeDeathReward(DepressionRules.normalDirectKillReward(
                originalKiller,
                FactionEconomyRules.receivesCustomKillReward(killer, game),
                impostorReward
        ));
        if (reward > 0) {
            PlayerShopComponent.KEY.get(killer).addToBalance(reward);
        }
        if (!originalKiller) {
            return;
        }
        // Same teammate filter as Wathe's +15: alive, online, not the attacker, and not Conscience.
        // 与 Wathe 队友 +15 的筛选一致：存活、在线、不是攻击者本人且没有善良。
        int teammateReward = DepressionRules.fakeDeathReward(GameConstants.MONEY_PER_KILL_TEAMMATE);
        MinecraftServer server = killer.getServer();
        if (server == null || teammateReward <= 0) {
            return;
        }
        for (UUID uuid : game.getAllKillerTeamPlayers()) {
            if (uuid.equals(killer.getUuid()) || game.isPlayerDead(uuid)) {
                continue;
            }
            ServerPlayerEntity teammate = server.getPlayerManager().getPlayer(uuid);
            if (teammate != null && !EffectiveTraitService.hasConscience(teammate)) {
                PlayerShopComponent.KEY.get(teammate).addToBalance(teammateReward);
            }
        }
    }

    public static void handleAfterKill(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer, Identifier deathReason) {
        if (killer != null) {
            ActiveState killerState = activePlayers.get(killer.getUuid());
            if (killerState != null) {
                DepressionAudio.playRangeSound(victim, DepressionAudio.meleeKillSound(killer.getRandom().nextBoolean()));
                GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
                Role victimRole = game == null ? null : game.getRole(victim);
                Collection<Identifier> victimTraits = TraitPlayerComponent.KEY.get(victim).getActiveTraitIds();
                if (DepressionRules.shouldPunishDepressionPsychoBatKill(true, deathReason, victimRole, victimTraits)
                        && GameFunctions.isPlayerPlayingAndAlive(killer)) {
                    GameFunctions.killPlayer(killer, true, null, GameConstants.DeathReasons.SHOT_INNOCENT, true);
                }
                if (killerState.attackerUuid().equals(victim.getUuid())) {
                    endPsycho(killer, true, true);
                }
            }
        }
        endActivePsychosTargeting(victim);
        ActiveState victimState = activePlayers.get(victim.getUuid());
        if (victimState != null) {
            DepressionAudio.playRangeSound(victim, SparkTraitsSounds.DEPRESSION_SHYGUY_KILLED);
            // Wathe's death drop has just walked the live inventory, which startPsycho emptied into the stash; the
            // stash is discarded below, so its drop-on-death items (the round's revolver) are dropped here instead.
            // Wathe 的死亡掉落刚遍历过实时背包，而背包已在 startPsycho 时清空并存入暂存；暂存即将在下方丢弃，
            // 因此在这里补掉其中“死亡时掉落”的物品（本局的左轮）。
            victimState.inventory().dropDeathDrops(victim);
            endPsycho(victim, false, false);
        }
        clearPending(victim);
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        clearPending(player);
        if (activePlayers.containsKey(player.getUuid())) {
            endPsycho(player, false, false);
        }
        TraitPlayerComponent.KEY.get(player).setDepressionSuicideTicks(-1);
        TraitPlayerComponent.KEY.get(player).setDepressionPsychoState(false, null);
        TraitPlayerComponent.KEY.get(player).setDepressionCounterTarget(null);
    }

    /**
     * ResetPlayer variant: Wathe has already cleared this player's effects, inventory, psycho and stamina and moved
     * them to spawn, so the fake-death and psycho exits only drop Depression's own state. They must not put the
     * pre-fake-death effects or the pre-psycho Speed back, nor play the rage-to-docile cue at the lobby spawn. The
     * rest is the normal cleanup, which then finds neither state left.
     * ResetPlayer 专用：Wathe 已清空该玩家的效果、背包、疯魔与体力并将其传送到出生点，因此假死与疯魔的退出只清理抑郁自身的状态，
     * 不能把假死前的效果或疯魔前的速度加回去，也不能在大厅出生点播放“由怒转静”的提示音。其余部分沿用常规清理，
     * 届时这两种状态都已不存在。
     */
    public static void clearPlayerAfterReset(ServerPlayerEntity player) {
        clearPending(player, false);
        endPsycho(player, false, false, true);
        clearPlayer(player);
    }

    /**
     * Mid-round Depression removal from a living player: resets only state owned by this player's Depression. No-op
     * during a fake death or psycho, which own their own exit. The counter target stays: it marks another player's
     * psycho hunting this one.
     * 存活玩家在局中被移除抑郁时，只重置其自身抑郁拥有的状态；假死或疯魔期间不做任何事（由其自身流程收尾）。
     * 反制目标保留：它标记的是正在追杀本玩家的他人疯魔。
     */
    public static void clearOwnerStateForTraitRemoval(ServerPlayerEntity player) {
        if (isPending(player) || isPsychoActive(player)) {
            return;
        }
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        traits.setDepressionSuicideTicks(-1);
        traits.setDepressionPsychoState(false, null);
    }

    public static void clearRoundState(ServerWorld world) {
        for (UUID uuid : Set.copyOf(pendingPlayers.keySet())) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                clearPending(player);
            }
        }
        for (UUID uuid : Set.copyOf(activePlayers.keySet())) {
            if (world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player) {
                endPsycho(player, false, false);
            }
        }
        pendingPlayers.clear();
        activePlayers.clear();
        fakeBodies.clear();
        DepressionFakeKillCooldowns.clear();
        forceMentalBreakdownDeaths.clear();
    }

    private static DoorInteraction.DoorInteractionResult onDoorInteraction(DoorInteraction.DoorInteractionContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player) || !isPsychoActive(player)) {
            return DoorInteraction.DoorInteractionResult.PASS;
        }
        if (context.getHandItem().isOf(WatheItems.BAT)
                && (context.getDoorType() == DoorInteraction.DoorType.TRAIN_DOOR || context.requiresKey())) {
            return DoorInteraction.DoorInteractionResult.ALLOW;
        }
        return DoorInteraction.DoorInteractionResult.PASS;
    }

    private static void tickWorld(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (game == null || !game.isRunning()) {
            return;
        }
        tickPending(world);
        discardStaleFakeBodies(world);
        tickActive(world);
        tickSuicideCountdown(world, game);
    }

    private static void tickSuicideCountdown(ServerWorld world, GameWorldComponent game) {
        /*
         * Almost nobody holds Depression, so the holders are picked out with one component lookup per world player
         * instead of a role-list copy plus a getPlayerByUuid scan and five predicate inputs per role holder. A role
         * holder without Depression still gets its countdown hidden, as shouldPauseSuicideCountdown(false, ...) did.
         * The countdown itself runs on the collected holders, so a kill it triggers cannot disturb the player list.
         * 几乎没有人持有抑郁，因此改为对世界内每名玩家做一次组件查询来挑出持有者，而不是复制身份列表、逐个 getPlayerByUuid
         * 扫描并为每名有身份的玩家计算五个判定输入。没有抑郁但有身份的玩家仍会隐藏倒计时，
         * 与 shouldPauseSuicideCountdown(false, ...) 的结果一致。倒计时本身在收集到的持有者上执行，其触发的击杀不会影响玩家列表的遍历。
         */
        List<ServerPlayerEntity> holders = null;
        for (ServerPlayerEntity candidate : world.getPlayers()) {
            TraitPlayerComponent candidateTraits = TraitPlayerComponent.KEY.get(candidate);
            if (candidateTraits.hasActiveTrait(CivilianTraits.DEPRESSION)) {
                if (holders == null) {
                    holders = new java.util.ArrayList<>(2);
                }
                holders.add(candidate);
            } else if (candidateTraits.getDepressionSuicideTicks() != -1 && game.hasAnyRole(candidate)) {
                candidateTraits.setDepressionSuicideTicks(-1);
            }
        }
        if (holders == null) {
            return;
        }
        for (ServerPlayerEntity player : holders) {
            if (!game.hasAnyRole(player)) {
                continue;
            }
            TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
            if (DepressionRules.shouldPauseSuicideCountdown(
                    traits.hasActiveTrait(CivilianTraits.DEPRESSION),
                    GameFunctions.isPlayerPlayingAndAlive(player),
                    isPending(player),
                    isPsychoActive(player),
                    isWathePsychoActive(player))) {
                traits.setDepressionSuicideTicks(-1);
                continue;
            }
            PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
            if (!DepressionRules.shouldRunSuicideCountdown(mood.getMood())) {
                traits.setDepressionSuicideTicks(-1);
                continue;
            }
            int ticks = traits.getDepressionSuicideTicks();
            if (ticks <= 0) {
                traits.setDepressionSuicideTicks(SUICIDE_INTERVAL_TICKS);
                continue;
            }
            // The owner's client counts down locally, so only starts, resets and stops are synced.
            // 本人客户端会自行倒数，因此只在开始、重置与停止时同步。
            if (traits.countDownDepressionSuicideTicks() > 0) {
                continue;
            }
            double chance = DepressionRules.triggerChance(mood.getMood());
            double roll = player.getRandom().nextDouble() * 100.0;
            if (roll < chance) {
                forceMentalBreakdownDeaths.add(player.getUuid());
                try {
                    GameFunctions.killPlayer(player, true, null, GameConstants.DeathReasons.MENTAL_BREAKDOWN, true);
                } finally {
                    // beforeKill consumes the mark; if an earlier listener or a HEAD veto answered first it must
                    // not wait for this player's next, unrelated kill.
                    // 标记由 beforeKill 消费；若更早的监听或 HEAD 否决先行返回，它不能留到该玩家下一次无关的击杀。
                    forceMentalBreakdownDeaths.remove(player.getUuid());
                }
            } else {
                traits.setDepressionSuicideTicks(SUICIDE_INTERVAL_TICKS);
            }
        }
    }

    private static void tickPending(ServerWorld world) {
        if (pendingPlayers.isEmpty()) {
            return;
        }
        long time = world.getTime();
        for (PendingState state : Set.copyOf(pendingPlayers.values())) {
            if (!(world.getPlayerByUuid(state.playerUuid()) instanceof ServerPlayerEntity player)) {
                continue;
            }
            holdPending(player, state);
            if (time >= state.startPsychoTime()) {
                startPsycho(player, state);
            }
        }
    }

    private static void tickActive(ServerWorld world) {
        if (activePlayers.isEmpty()) {
            return;
        }
        for (ActiveState state : Set.copyOf(activePlayers.values())) {
            if (!(world.getPlayerByUuid(state.playerUuid()) instanceof ServerPlayerEntity player)) {
                activePlayers.remove(state.playerUuid());
                continue;
            }
            ServerPlayerEntity attacker = world.getServer().getPlayerManager().getPlayer(state.attackerUuid());
            boolean playerAlive = GameFunctions.isPlayerPlayingAndAlive(player);
            boolean attackerAlive = attacker != null && GameFunctions.isPlayerPlayingAndAlive(attacker);
            if (DepressionRules.shouldEndActivePsycho(playerAlive, attackerAlive)) {
                // Dead Depression psycho players must not have their synced skin/state maintained.
                // 已死亡的抑郁疯魔玩家不能继续被同步皮肤/状态维持住。
                endPsycho(player, playerAlive, playerAlive);
                continue;
            }
            ActiveState updatedState = maintainPsycho(player, attacker, state, false);
            if (activePlayers.containsKey(player.getUuid())) {
                activePlayers.put(player.getUuid(), updatedState);
            }
        }
    }

    private static void startPending(ServerPlayerEntity player, ServerPlayerEntity attacker, Identifier deathReason, int initialArmour) {
        ServerWorld world = player.getServerWorld();
        UUID uuid = player.getUuid();
        discardFakeBodies(player);
        PlayerBodyEntity body = WatheEntities.PLAYER_BODY.create(world);
        if (body != null) {
            body.setPlayerUuid(uuid);
            body.setDeathReason(deathReason);
            body.setDeathGameTime(world.getTime());
            Vec3d spawnPos = player.getPos().add(player.getRotationVector().normalize().multiply(1));
            body.refreshPositionAndAngles(spawnPos.x, player.getY(), spawnPos.z, player.getHeadYaw(), 0f);
            body.setYaw(player.getHeadYaw());
            body.setHeadYaw(player.getHeadYaw());
            world.spawnEntity(body);
            fakeBodies.track(uuid, world.getRegistryKey().getValue(), body.getUuid());
        }
        PendingState state = new PendingState(
                uuid,
                attacker.getUuid(),
                world.getTime() + FAKE_DEATH_TICKS,
                body == null ? null : body.getUuid(),
                EffectSnapshot.capture(player),
                player.isInvulnerable(),
                player.getPos(),
                player.getYaw(),
                player.getPitch(),
                initialArmour
        );
        pendingPlayers.put(uuid, state);
        TraitPlayerComponent.KEY.get(player).setTemporaryFakeDeathPending(true);
        TraitPlayerComponent.KEY.get(attacker).setDepressionCounterTarget(uuid);
        attacker.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(ATTACKER_TITLE));
        DepressionAudio.playRangeSound(player, SparkTraitsSounds.DEPRESSION_DOCILE_TO_RAGE);
        DepressionAudio.playPairMusicSound(player, attacker, SparkTraitsSounds.DEPRESSION_BLIND_RAGE_ENRAGE);
        DepressionAudio.playDirectSound(attacker, SparkTraitsSounds.DEPRESSION_PLAYER_WAS_SEEN);
        holdPending(player, state);
    }

    private static void holdPending(ServerPlayerEntity player, PendingState state) {
        player.changeGameMode(GameMode.SPECTATOR);
        Entity camera = pendingCameraEntity(player, state);
        if (player.getCameraEntity() != camera) {
            player.setCameraEntity(camera);
        }
        player.setInvulnerable(true);
        player.setVelocity(Vec3d.ZERO);
        player.teleport(player.getServerWorld(), state.deathPos().x, state.deathPos().y, state.deathPos().z, Set.of(), state.deathYaw(), state.deathPitch());
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false, false));
    }

    private static Entity pendingCameraEntity(ServerPlayerEntity player, PendingState state) {
        MinecraftServer server = player.getServer();
        if (server != null && state.bodyUuid() != null) {
            Entity body = server.getOverworld().getEntity(state.bodyUuid());
            if (body != null) {
                return body;
            }
            for (ServerWorld world : server.getWorlds()) {
                Entity entity = world.getEntity(state.bodyUuid());
                if (entity != null) {
                    return entity;
                }
            }
        }
        return player;
    }

    private static void startPsycho(ServerPlayerEntity player, PendingState state) {
        pendingPlayers.remove(player.getUuid());
        TraitPlayerComponent.KEY.get(player).setTemporaryFakeDeathPending(false);
        player.setCameraEntity(player);
        // The fake corpse only lasts through the fake-death window; it vanishes as the player gets up.
        // 假尸只存在于假死期间；玩家站起来时移除。
        discardFakeBodies(player);
        player.changeGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(state.wasInvulnerable());
        state.effects().restore(player);
        player.teleport(player.getServerWorld(), state.deathPos().x, state.deathPos().y, state.deathPos().z, Set.of(), state.deathYaw(), state.deathPitch());

        StatusEffectInstance prePsychoSpeed = DepressionPsychoLoadout.copyStatusEffect(player.getStatusEffect(StatusEffects.SPEED));
        InventorySnapshot inventory = InventorySnapshot.capture(player);
        DepressionPsychoLoadout.clearInventory(player);
        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
        if (holdDepressionPsycho(psycho)) {
            psycho.setArmour(Math.max(0, state.initialArmour()));
        }
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        stamina.setMaxSprintTime(-1);
        stamina.setSprintingTicks(Integer.MAX_VALUE);
        stamina.setExhausted(false);
        stamina.sync();
        ActiveState activeState = new ActiveState(
                player.getUuid(),
                state.attackerUuid(),
                inventory,
                Math.max(0, state.initialArmour()),
                DepressionAudio.CHASE_LOOP_INTERVAL_TICKS,
                prePsychoSpeed
        );
        activePlayers.put(player.getUuid(), activeState);
        TraitPlayerComponent.KEY.get(player).setDepressionPsychoState(true, state.attackerUuid());
        ServerPlayerEntity attacker = player.getServer().getPlayerManager().getPlayer(state.attackerUuid());
        DepressionAudio.playPairMusicSound(player, attacker, SparkTraitsSounds.DEPRESSION_BLIND_RAGE_CHASE);
        activePlayers.put(player.getUuid(), maintainPsycho(
                player,
                attacker,
                activeState,
                true
        ));
    }

    private static ActiveState maintainPsycho(
            ServerPlayerEntity player,
            @Nullable ServerPlayerEntity attacker,
            ActiveState state,
            boolean refreshActionbar
    ) {
        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
        if (holdDepressionPsycho(psycho)) {
            // English: The Jester bonus is starting armour only; consumed armour stays consumed.
            // 中文：小丑加成只提供初始护盾；已经消耗的护盾不会被每 tick 补回。
            int maintainedArmour = DepressionRules.maintainedPsychoArmour(psycho.getArmour(), state.maxArmour());
            if (psycho.getArmour() != maintainedArmour) {
                psycho.setArmour(maintainedArmour);
            }
        }
        DepressionPsychoLoadout.applyPsychoSpeed(player);
        DepressionPsychoLoadout.enforceBatOnly(player);
        DepressionPsychoLoadout.holdInfiniteStamina(PlayerStaminaComponent.KEY.get(player));
        // An action bar message stays up for 60 ticks, so a refresh every few ticks keeps it on screen.
        // 动作栏消息会停留 60 tick，每隔几 tick 刷新一次即可保持显示。
        if (refreshActionbar || player.getServerWorld().getTime() % PSYCHO_ACTIONBAR_REFRESH_TICKS == 0) {
            player.sendMessage(PSYCHO_ACTIONBAR, true);
            if (attacker != null) {
                attacker.sendMessage(ATTACKER_ACTIONBAR, true);
            }
        }
        ActiveState updatedState = tickChaseAudio(player, attacker, state);
        return updatedState;
    }

    /**
     * Keeps the running Wathe psycho, or starts Depression's uncounted VISIBLE_QUIET one; never forces ticks onto a failed start.
     * A failed start keeps Wathe's default PUBLIC type, so its stopPsycho would take another player's psychosActive count
     * and silence Wathe's psycho music.
     * 维持已在运行的 Wathe 疯魔，或启动抑郁自己的、不计数的 VISIBLE_QUIET 疯魔；启动失败时绝不强行写入疯魔时间。
     * 启动失败会保留 Wathe 默认的 PUBLIC 类型，结束时会扣掉其他玩家的 psychosActive 计数，导致 Wathe 疯魔音乐停止。
     */
    private static boolean holdDepressionPsycho(PlayerPsychoComponent psycho) {
        if (psycho.getPsychoTicks() <= 0 && !psycho.startPsycho(PsychoType.VISIBLE_QUIET)) {
            return false;
        }
        // Wathe's setter syncs to every tracker, so top up only after the hold has drained instead of every tick.
        // Wathe 的设置方法会同步给所有追踪者，因此只在维持时长被消耗后补满，而不是每 tick 重设。
        if (psycho.getPsychoTicks() < PSYCHO_HOLD_TOP_UP_BELOW_TICKS) {
            psycho.setPsychoTicks(Integer.MAX_VALUE);
        }
        return true;
    }

    /** Survivor exit used by Last Escape; never uses the destructive death cleanup. */
    public static boolean endPsychoForLastEscape(ServerPlayerEntity player) {
        if (!activePlayers.containsKey(player.getUuid())) return false;
        endPsycho(player, true, true);
        return true;
    }

    private static void endPsycho(ServerPlayerEntity player, boolean restoreInventory, boolean restoreMoodIfSurvived, boolean afterWatheReset) {
        ActiveState state = activePlayers.remove(player.getUuid());
        if (state == null) {
            return;
        }
        ServerPlayerEntity attacker = player.getServer().getPlayerManager().getPlayer(state.attackerUuid());
        clearActivePsychoState(player, state);
        try {
            DepressionAudio.stopPairMusicSound(player, attacker, SparkTraitsSounds.DEPRESSION_BLIND_RAGE_CHASE_ID);
            if (!afterWatheReset) {
                DepressionAudio.playRangeSound(player, SparkTraitsSounds.DEPRESSION_RAGE_TO_DOCILE);
            }
            PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
            // Wathe's killPlayer (death, or a terminal attempt Last Escape saved) may have stopped psycho already,
            // resetting its type to PUBLIC; a second stop would take another player's count and silence Wathe's psycho music.
            // Wathe 的 killPlayer（死亡，或被绝处逢生救下的致命判定）可能已结束疯魔并把类型重置为 PUBLIC；
            // 再停一次会扣掉其他玩家的疯魔计数，导致 Wathe 疯魔音乐停止。
            if (psycho.getPsychoTicks() > 0) psycho.stopPsycho();
            psycho.sync();
            if (restoreInventory) {
                state.inventory().restore(player);
            }
            if (!afterWatheReset) {
                DepressionPsychoLoadout.restorePrePsychoSpeed(player, state.prePsychoSpeed());
            }
            DepressionPsychoLoadout.restorePostPsychoStamina(player);
            DepressionPsychoLoadout.restorePostPsychoMood(player, restoreMoodIfSurvived);
        } finally {
            clearActivePsychoState(player, state);
        }
    }

    private static void endPsycho(ServerPlayerEntity player, boolean restoreInventory, boolean restoreMoodIfSurvived) {
        endPsycho(player, restoreInventory, restoreMoodIfSurvived, false);
    }

    private static void endActivePsychosTargeting(ServerPlayerEntity victim) {
        for (ActiveState state : Set.copyOf(activePlayers.values())) {
            if (!DepressionRules.shouldEndActivePsychoForDeadTarget(victim.getUuid(), state.attackerUuid())) {
                continue;
            }
            ServerPlayerEntity player = victim.getServer().getPlayerManager().getPlayer(state.playerUuid());
            if (player == null) {
                activePlayers.remove(state.playerUuid());
                TraitPlayerComponent.KEY.get(victim).setDepressionCounterTarget(null);
                continue;
            }
            if (activePlayers.containsKey(player.getUuid())) {
                endPsycho(player, true, true);
            }
        }
    }

    private static void clearActivePsychoState(ServerPlayerEntity player, ActiveState state) {
        // English: Wathe stopPsycho removes the bat, but SparkTraits must clear its own synced state first.
        // 中文：Wathe 的 stopPsycho 只会移除棒子；SparkTraits 自己同步的疯魔状态必须先独立清理。
        TraitPlayerComponent.KEY.get(player).setDepressionPsychoState(false, null);
        ServerPlayerEntity attacker = player.getServer().getPlayerManager().getPlayer(state.attackerUuid());
        if (attacker != null) {
            TraitPlayerComponent.KEY.get(attacker).setDepressionCounterTarget(null);
        }
    }

    private static void clearPending(ServerPlayerEntity player, boolean restoreEffects) {
        TraitPlayerComponent.KEY.get(player).setTemporaryFakeDeathPending(false);
        PendingState state = pendingPlayers.remove(player.getUuid());
        if (state == null) {
            return;
        }
        player.setCameraEntity(player);
        // Covers deaths during the fake-death window (forced kills ignore the pending cancel) and resets.
        // AFTER runs once Wathe has spawned the real body, so only the tracked fake body is removed.
        // 覆盖假死期间的死亡（强制击杀会无视假死取消）和重置；AFTER 时真尸已生成，只移除被追踪的假尸。
        discardFakeBodies(player);
        player.setInvulnerable(state.wasInvulnerable());
        if (restoreEffects) {
            state.effects().restore(player);
        }
        TraitPlayerComponent.KEY.get(player).setDepressionPsychoState(false, null);
        ServerPlayerEntity attacker = player.getServer().getPlayerManager().getPlayer(state.attackerUuid());
        if (attacker != null) {
            TraitPlayerComponent.KEY.get(attacker).setDepressionCounterTarget(null);
        }
    }

    private static void clearPending(ServerPlayerEntity player) {
        clearPending(player, true);
    }

    private static void discardFakeBodies(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        for (DepressionFakeBodyTracker.FakeBody fakeBody : fakeBodies.bodiesOf(player.getUuid())) {
            discardFakeBody(
                    server == null ? null : server.getWorld(RegistryKey.of(RegistryKeys.WORLD, fakeBody.worldId())),
                    fakeBody
            );
        }
    }

    /**
     * A fake body outlives its fake death only when its chunk was unloaded at the time; remove it once it loads again.
     * 假尸只会在假死结束时所在区块已卸载的情况下残留；区块重新加载后将其移除。
     */
    private static void discardStaleFakeBodies(ServerWorld world) {
        if (fakeBodies.isEmpty()) {
            return;
        }
        for (DepressionFakeBodyTracker.FakeBody fakeBody : fakeBodies.bodiesIn(world.getRegistryKey().getValue())) {
            if (!pendingPlayers.containsKey(fakeBody.playerUuid())) {
                discardFakeBody(world, fakeBody);
            }
        }
    }

    private static void discardFakeBody(@Nullable ServerWorld world, DepressionFakeBodyTracker.FakeBody fakeBody) {
        // Untrack only after a discard: a missing entity may just be unloaded and must stay a fake body when it returns.
        // Bagged or eaten bodies never return; their entries are dropped at round cleanup.
        // 仅在成功移除后取消追踪：找不到的实体可能只是区块卸载，重新加载后仍需被识别为假尸；
        // 被尸袋收走或被吃掉的假尸不会再出现，其记录在回合清理时丢弃。
        if (world != null
                && world.getEntity(fakeBody.bodyUuid()) instanceof PlayerBodyEntity body
                && fakeBody.playerUuid().equals(body.getPlayerUuid())) {
            body.discard();
            fakeBodies.untrack(fakeBody.bodyUuid());
        }
    }

    private static ActiveState tickChaseAudio(ServerPlayerEntity player, @Nullable ServerPlayerEntity attacker, ActiveState state) {
        if (DepressionAudio.shouldPlayChaseLoop(state.chaseLoopTicks())) {
            DepressionAudio.playPairMusicSound(player, attacker, SparkTraitsSounds.DEPRESSION_BLIND_RAGE_CHASE);
        }
        return state.withChaseLoopTicks(DepressionAudio.nextChaseLoopTicks(state.chaseLoopTicks()));
    }

    private record PendingState(
            UUID playerUuid,
            UUID attackerUuid,
            long startPsychoTime,
            @Nullable UUID bodyUuid,
            EffectSnapshot effects,
            boolean wasInvulnerable,
            Vec3d deathPos,
            float deathYaw,
            float deathPitch,
            int initialArmour
    ) {
    }

    private record ActiveState(
            UUID playerUuid,
            UUID attackerUuid,
            InventorySnapshot inventory,
            int maxArmour,
            int chaseLoopTicks,
            @Nullable StatusEffectInstance prePsychoSpeed
    ) {
        ActiveState withChaseLoopTicks(int chaseTicks) {
            return new ActiveState(playerUuid, attackerUuid, inventory, maxArmour, chaseTicks, prePsychoSpeed);
        }
    }

    private record EffectSnapshot(List<StatusEffectInstance> effects) {
        static EffectSnapshot capture(ServerPlayerEntity player) {
            return new EffectSnapshot(player.getStatusEffects().stream()
                    .map(StatusEffectInstance::new)
                    .toList());
        }

        void restore(ServerPlayerEntity player) {
            player.clearStatusEffects();
            for (StatusEffectInstance effect : effects) {
                player.addStatusEffect(new StatusEffectInstance(effect));
            }
        }
    }

    private record InventorySnapshot(
            List<ItemStack> main,
            List<ItemStack> armor,
            List<ItemStack> offHand,
            int selectedSlot
    ) {
        static InventorySnapshot capture(ServerPlayerEntity player) {
            PlayerInventory inventory = player.getInventory();
            return new InventorySnapshot(copy(inventory.main), copy(inventory.armor), copy(inventory.offHand), inventory.selectedSlot);
        }

        void restore(ServerPlayerEntity player) {
            PlayerInventory inventory = player.getInventory();
            restoreList(inventory.main, main);
            restoreList(inventory.armor, armor);
            restoreList(inventory.offHand, offHand);
            inventory.selectedSlot = selectedSlot;
            player.currentScreenHandler.sendContentUpdates();
        }

        /**
         * Applies Wathe's own death-drop rule ({@code GameFunctions.shouldDropOnDeath}, i.e. the revolver and every
         * ShouldDropOnDeath listener) to the stashed stacks, with the same drop call as Wathe's killPlayer loop.
         * 对暂存的物品应用 Wathe 自身的死亡掉落规则（{@code GameFunctions.shouldDropOnDeath}：左轮以及所有
         * ShouldDropOnDeath 监听），掉落方式与 Wathe killPlayer 中的循环一致。
         */
        void dropDeathDrops(ServerPlayerEntity victim) {
            for (List<ItemStack> stacks : List.of(main, armor, offHand)) {
                for (ItemStack stack : stacks) {
                    if (GameFunctions.shouldDropOnDeath(stack, victim)) {
                        victim.dropItem(stack.copy(), true, false);
                    }
                }
            }
        }

        private static List<ItemStack> copy(DefaultedList<ItemStack> stacks) {
            return stacks.stream().map(ItemStack::copy).toList();
        }

        private static void restoreList(DefaultedList<ItemStack> target, List<ItemStack> source) {
            for (int i = 0; i < target.size(); i++) {
                target.set(i, i < source.size() ? source.get(i).copy() : ItemStack.EMPTY);
            }
        }
    }
}
