package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.TraitRemovalReason;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.mixin.GameWorldComponentAccessor;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheEntities;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime state machine for the Last Stand trait.
 * 背水一战天赋的运行时状态机：采样心情、拦截胜利、假死倒计时与复活。
 */
public final class LastStandService {
    private static final int PENDING_TICKS = 100;
    private static final int SAMPLE_INTERVAL_TICKS = 20 * 60;
    private static final int MAX_MOOD_SAMPLES = 5;
    private static final Identifier NOELLES_ASSASSINATED = Identifier.of("noellesroles", "assassinated");
    private static final Identifier NOELLES_NO_COLLISION = Identifier.of("noellesroles", "no_collision");
    private static final Text ACTIONBAR = Text.literal("Let’s not give up just yet…");
    private static final Text CANNOT_ASSASSINATE = Text.literal("This target cannot be assassinated.");
    private static final Text CANNOT_SWAP = Text.literal("This target cannot be swapped.");

    private static final Map<UUID, ReturnPoint> returnPoints = new HashMap<>();
    private static final Map<UUID, ArrayDeque<Double>> moodSamples = new HashMap<>();
    private static final Map<UUID, Long> lastSampleTicks = new HashMap<>();
    private static final Map<UUID, Double> preDeathMoodPercent = new HashMap<>();
    private static final Map<UUID, PendingState> pendingPlayers = new HashMap<>();
    // Approved kills are decided before Wathe drops items and consumed in AFTER.
    // 已批准的击杀会在 Wathe 掉落物品前确定，并在 AFTER 阶段消费。
    private static final Set<UUID> approvedLastStandDeaths = new HashSet<>();
    private static final Set<UUID> consumedPlayers = new HashSet<>();
    private static final Set<UUID> revivedImmunePlayers = new HashSet<>();

    private LastStandService() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(LastStandService::tickWorld);
        CheckWinCondition.EVENT.register((world, gameComponent, currentStatus) ->
                shouldBlockRoundEnd(hasPendingInWorld(world), currentStatus)
                        ? CheckWinCondition.WinResult.block()
                        : null);
        GetInstinctHighlight.EVENT.register(target -> {
            if (target instanceof PlayerEntity player) {
                if (!EffectiveTraitService.isHiddenFromKillerInstinct(player)) {
                    return null;
                }
                return GetInstinctHighlight.HighlightResult.skip();
            }
            return null;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cancelPendingRevive(handler.player));
        registerInteractionLocks();
    }

    public static void clearRoundState(ServerWorld world) {
        pendingPlayers.values().removeIf(state -> {
            if (state.deathWorldKey().equals(world.getRegistryKey())) {
                if (world.getPlayerByUuid(state.playerUuid()) instanceof ServerPlayerEntity player) {
                    restorePendingControl(player);
                    TraitPlayerComponent.KEY.get(player).setLastStandPending(false);
                }
                discardBody(world.getServer(), state);
                return true;
            }
            return false;
        });
        returnPoints.clear();
        moodSamples.clear();
        lastSampleTicks.clear();
        preDeathMoodPercent.clear();
        consumedPlayers.clear();
        approvedLastStandDeaths.clear();
        revivedImmunePlayers.clear();
    }

    public static void recordReturnPoint(ServerPlayerEntity player) {
        returnPoints.put(player.getUuid(), new ReturnPoint(
                player.getServerWorld().getRegistryKey(),
                player.getPos(),
                player.getYaw(),
                player.getPitch()
        ));
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        PendingState state = pendingPlayers.remove(uuid);
        if (state != null) {
            restorePendingControl(player);
        }
        returnPoints.remove(uuid);
        moodSamples.remove(uuid);
        lastSampleTicks.remove(uuid);
        preDeathMoodPercent.remove(uuid);
        approvedLastStandDeaths.remove(uuid);
        consumedPlayers.remove(uuid);
        revivedImmunePlayers.remove(uuid);
        TraitPlayerComponent.KEY.get(player).setLastStandPending(false);
        TraitPlayerComponent.KEY.get(player).setKillerInstinctHidden(false);
    }

    public static void cancelPendingRevive(ServerPlayerEntity player) {
        PendingState state = pendingPlayers.remove(player.getUuid());
        if (state == null) {
            return;
        }
        restorePendingControl(player);
        player.setInvulnerable(state.wasInvulnerable());
        state.effects().restore(player);
        TraitPlayerComponent.KEY.get(player).setLastStandPending(false);
        TraitPlayerComponent.KEY.get(player).clearActiveTraits(TraitRemovalReason.DEATH);
    }

    public static KillPlayer.KillResult beforeKill(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer, Identifier deathReason) {
        UUID uuid = victim.getUuid();
        approvedLastStandDeaths.remove(uuid);
        preDeathMoodPercent.put(uuid, currentMoodPercent(victim));
        if (isPending(victim)) {
            preDeathMoodPercent.remove(uuid);
            return KillPlayer.KillResult.cancel();
        }
        if (shouldBypassLastStandDeathReason(deathReason)) {
            preDeathMoodPercent.remove(uuid);
            return null;
        }
        if (NOELLES_ASSASSINATED.equals(deathReason) && isProtectedFromNoellesRoleUtility(victim)) {
            preDeathMoodPercent.remove(uuid);
            notifyCannotAssassinate(killer);
            return KillPlayer.KillResult.cancel();
        }
        if (!approveLastStandDeath(victim, killer)) {
            preDeathMoodPercent.remove(uuid);
        }
        return null;
    }

    public static boolean tryStartAfterKill(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer, Identifier deathReason) {
        UUID uuid = victim.getUuid();
        if (!approvedLastStandDeaths.remove(uuid)) {
            return false;
        }
        if (pendingPlayers.containsKey(uuid)) {
            return false;
        }
        TraitPlayerComponent playerTraits = TraitPlayerComponent.KEY.get(victim);
        if (!playerTraits.hasActiveTrait(LastStandTrait.ID)) {
            return false;
        }
        if (!(victim.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        if (hasTriggeredThisRound(world, uuid)) {
            return false;
        }

        markTriggeredThisRound(world, uuid);
        startPending(world, victim, deathReason);
        return true;
    }

    public static boolean shouldPreventDeathDrop(PlayerEntity victim, ItemStack stack) {
        return shouldPreventRevolverDeathDrop(
                approvedLastStandDeaths.contains(victim.getUuid()),
                stack.isOf(WatheItems.REVOLVER)
        );
    }

    public static boolean shouldHidePendingHeldItem(Entity entity) {
        return entity instanceof PlayerEntity player
                && isPendingClientOrServer(isPending(player), TraitPlayerComponent.KEY.get(player).isLastStandPending());
    }

    public static boolean shouldDisablePendingCollision(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            return shouldDisablePendingCollision(isPending(player), TraitPlayerComponent.KEY.get(player).isLastStandPending());
        }
        if (entity instanceof PlayerBodyEntity body) {
            UUID playerUuid = body.getPlayerUuid();
            PlayerEntity player = body.getWorld().getPlayerByUuid(playerUuid);
            boolean syncedPending = player != null && TraitPlayerComponent.KEY.get(player).isLastStandPending();
            return shouldDisablePendingCollision(isPending(playerUuid), syncedPending);
        }
        return false;
    }

    public static boolean shouldDisablePendingCollision(Entity first, Entity second) {
        return shouldDisablePendingCollision(first) || shouldDisablePendingCollision(second);
    }

    static boolean shouldDisablePendingCollision(boolean serverPending, boolean syncedPending) {
        return isPendingClientOrServer(serverPending, syncedPending);
    }

    static boolean shouldPreventRevolverDeathDrop(boolean approvedLastStandDeath, boolean revolver) {
        return approvedLastStandDeath && revolver;
    }

    static boolean isPendingClientOrServer(boolean serverPending, boolean syncedPending) {
        return serverPending || syncedPending;
    }

    static boolean shouldHideFromKillerInstinct(boolean lastStandPending, boolean killerInstinctHidden) {
        return EffectiveTraitService.shouldHideFromKillerInstinct(lastStandPending, killerInstinctHidden);
    }

    public static boolean shouldBlockRoundEnd(boolean pendingInWorld, GameFunctions.WinStatus winStatus) {
        return pendingInWorld && winStatus != GameFunctions.WinStatus.NONE;
    }

    public static boolean shouldCancelRoundEndFinalization(boolean pendingInWorld) {
        // The mixin calls this only after Wathe has a real winner ready to announce.
        // Mixin 只会在 wathe 已经准备宣布真实胜利时调用这里。
        return pendingInWorld;
    }

    public static int pendingSpectatorHighlightColor(
            boolean canSeeSpectatorInformation,
            boolean instinctEnabled,
            boolean lastStandPending,
            @Nullable Role targetRole
    ) {
        if (!canSeeSpectatorInformation || !instinctEnabled || !lastStandPending) {
            return -1;
        }
        return Objects.requireNonNullElse(targetRole, WatheRoles.CIVILIAN).color();
    }

    static Identifier noCollisionEffectId() {
        return NOELLES_NO_COLLISION;
    }

    static boolean canTriggerFromKill(
            Role victimRole,
            Collection<Identifier> victimTraits,
            @Nullable Role killerRole,
            Collection<Identifier> killerTraits
    ) {
        // Last Stand is a good-side comeback against any non-good attacker, not only native killers.
        // 背水一战是好人阵营面对任意非好人击杀者的翻盘机会，不只绑定原生杀手。
        return EffectiveTraitService.isEffectiveCivilian(victimRole, victimTraits)
                && killerRole != null
                && !EffectiveTraitService.isEffectiveCivilian(killerRole, killerTraits);
    }

    static boolean shouldBypassLastStandDeathReason(Identifier deathReason) {
        return GameConstants.DeathReasons.MENTAL_BREAKDOWN.equals(deathReason)
                || GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(deathReason)
                || GameConstants.DeathReasons.DROWNED.equals(deathReason)
                || GameConstants.DeathReasons.ESCAPED.equals(deathReason)
                || GameConstants.DeathReasons.VANILLA_DEATH.equals(deathReason);
    }

    private static boolean approveLastStandDeath(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer) {
        UUID uuid = victim.getUuid();
        if (pendingPlayers.containsKey(uuid) || consumedPlayers.contains(uuid)) {
            return false;
        }
        TraitPlayerComponent playerTraits = TraitPlayerComponent.KEY.get(victim);
        if (!playerTraits.hasActiveTrait(LastStandTrait.ID)) {
            return false;
        }
        if (!(victim.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        if (hasTriggeredThisRound(world, uuid)) {
            return false;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        Role victimRole = game.getRole(victim);
        Collection<Identifier> victimTraits = playerTraits.getActiveTraitIds();
        Role killerRole = killer == null ? null : game.getRole(killer);
        Collection<Identifier> killerTraits = killer == null
                ? Set.of()
                : TraitPlayerComponent.KEY.get(killer).getActiveTraitIds();
        if (!canTriggerFromKill(victimRole, victimTraits, killerRole, killerTraits)) {
            return false;
        }
        if (!meetsPopulationThresholdAfterDeath(game, uuid)) {
            return false;
        }

        double averageMood = roundedMoodAverage(victim);
        double chance = calculateTriggerChance(averageMood, victimRole.getMoodType());
        double roll = victim.getRandom().nextDouble() * 100.0;
        boolean success = roll < chance;
        SparkTraits.LOGGER.debug(
                "Last Stand roll for {}: averageMood={}, chance={}, roll={}, success={}",
                victim.getGameProfile().getName(),
                averageMood,
                chance,
                roll,
                success
        );
        preDeathMoodPercent.remove(uuid);
        if (!success) {
            return false;
        }

        approvedLastStandDeaths.add(uuid);
        return true;
    }

    public static boolean isPending(PlayerEntity player) {
        return isPending(player.getUuid());
    }

    public static boolean isPending(UUID uuid) {
        return pendingPlayers.containsKey(uuid);
    }

    public static boolean hasTriggeredThisRound(UUID uuid) {
        return consumedPlayers.contains(uuid);
    }

    public static boolean hasTriggeredThisRound(ServerWorld world, UUID uuid) {
        return hasTriggeredThisRound(
                consumedPlayers.contains(uuid),
                world != null && TraitWorldComponent.KEY.get(world).hasLastStandTriggered(uuid)
        );
    }

    static boolean hasTriggeredThisRound(boolean runtimeTriggered, boolean roundStateTriggered) {
        return runtimeTriggered || roundStateTriggered;
    }

    private static void markTriggeredThisRound(ServerWorld world, UUID uuid) {
        consumedPlayers.add(uuid);
        TraitWorldComponent.KEY.get(world).markLastStandTriggered(uuid);
    }

    public static boolean isProtectedFromNoellesRoleUtility(Entity entity) {
        return entity instanceof PlayerEntity player && isProtectedFromNoellesRoleUtility(player);
    }

    public static boolean isProtectedFromNoellesRoleUtility(PlayerEntity player) {
        UUID uuid = player.getUuid();
        return pendingPlayers.containsKey(uuid) || revivedImmunePlayers.contains(uuid);
    }

    public static void notifyCannotAssassinate(@Nullable ServerPlayerEntity player) {
        if (player != null) {
            player.sendMessage(CANNOT_ASSASSINATE, true);
        }
    }

    public static void notifyCannotSwap(ServerPlayerEntity player) {
        player.sendMessage(CANNOT_SWAP, true);
    }

    static double calculateTriggerChance(double averageMood) {
        if (averageMood >= 70.0) {
            return 100.0;
        }
        if (averageMood <= 0.0) {
            return 5.0;
        }
        return 5.0 + (averageMood / 70.0) * 95.0;
    }

    static double calculateTriggerChance(double averageMood, Role.MoodType moodType) {
        if (moodType != Role.MoodType.REAL) {
            return 100.0;
        }
        return calculateTriggerChance(averageMood);
    }

    private static void registerInteractionLocks() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                isPending(player) ? ActionResult.FAIL : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                isPending(player) || isPendingTarget(entity) ? ActionResult.FAIL : ActionResult.PASS);
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                isPending(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                isPending(player) || isPendingTarget(entity) ? ActionResult.FAIL : ActionResult.PASS);
        UseItemCallback.EVENT.register((player, world, hand) ->
                isPending(player)
                        ? TypedActionResult.fail(player.getStackInHand(hand))
                        : TypedActionResult.pass(player.getStackInHand(hand)));
    }

    private static boolean isPendingTarget(Entity entity) {
        return entity instanceof PlayerEntity player && isPending(player);
    }

    private static void tickWorld(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (game.getGameStatus() == GameWorldComponent.GameStatus.ACTIVE) {
            sampleMood(world, game);
        }
        tickPendingPlayers(world);
    }

    private static void sampleMood(ServerWorld world, GameWorldComponent game) {
        long time = world.getTime();
        for (UUID uuid : game.getAllPlayers()) {
            if (!(world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player)) {
                continue;
            }
            if (!TraitPlayerComponent.KEY.get(player).hasActiveTrait(LastStandTrait.ID)
                    || !GameFunctions.isPlayerPlayingAndAlive(player)) {
                continue;
            }
            Long lastSampleTick = lastSampleTicks.get(uuid);
            if (lastSampleTick == null) {
                lastSampleTicks.put(uuid, time);
                continue;
            }
            if (time - lastSampleTick >= SAMPLE_INTERVAL_TICKS) {
                addMoodSample(uuid, currentMoodPercent(player));
                lastSampleTicks.put(uuid, time);
            }
        }
    }

    private static void tickPendingPlayers(ServerWorld world) {
        long time = world.getTime();
        for (PendingState state : Set.copyOf(pendingPlayers.values())) {
            if (!state.deathWorldKey().equals(world.getRegistryKey())) {
                continue;
            }
            if (!(world.getPlayerByUuid(state.playerUuid()) instanceof ServerPlayerEntity player)) {
                pendingPlayers.remove(state.playerUuid());
                continue;
            }
            holdPendingPlayer(player, state);
            if (time >= state.reviveTime()) {
                revive(player, state);
            }
        }
    }

    private static void holdPendingPlayer(ServerPlayerEntity player, PendingState state) {
        player.changeGameMode(GameMode.SPECTATOR);
        Entity camera = pendingCameraEntity(player, state);
        if (player.getCameraEntity() != camera) {
            player.setCameraEntity(camera);
        }
        player.setInvulnerable(true);
        player.setVelocity(Vec3d.ZERO);
        player.teleport(player.getServerWorld(), state.deathPos().x, state.deathPos().y, state.deathPos().z, Set.of(), state.deathYaw(), state.deathPitch());
        applyPendingEffect(player, StatusEffects.BLINDNESS);
        applyPendingEffect(player, StatusEffects.INVISIBILITY);
        applyPendingEffect(player, noellesNoCollisionEffect());
        player.sendMessage(ACTIONBAR, true);
    }

    private static void startPending(ServerWorld world, ServerPlayerEntity player, Identifier deathReason) {
        UUID uuid = player.getUuid();
        TraitPlayerComponent.KEY.get(player).revealToOwner(LastStandTrait.ID);
        PlayerBodyEntity body = findNewestBody(world, uuid);
        if (body == null) {
            body = spawnFallbackBody(world, player, deathReason);
        }

        PendingState state = new PendingState(
                uuid,
                world.getRegistryKey(),
                player.getPos(),
                player.getYaw(),
                player.getPitch(),
                world.getTime() + PENDING_TICKS,
                body == null ? null : body.getUuid(),
                EffectSnapshot.capture(player),
                player.isInvulnerable()
        );
        pendingPlayers.put(uuid, state);
        TraitPlayerComponent.KEY.get(player).setLastStandPending(true);
        holdPendingPlayer(player, state);
    }

    private static void revive(ServerPlayerEntity player, PendingState state) {
        MinecraftServer server = player.getServer();
        pendingPlayers.remove(state.playerUuid());
        TraitPlayerComponent.KEY.get(player).setLastStandPending(false);

        ServerWorld targetWorld = player.getServerWorld();
        Vec3d targetPos = state.deathPos();
        float targetYaw = state.deathYaw();
        float targetPitch = state.deathPitch();
        ReturnPoint returnPoint = returnPoints.get(state.playerUuid());
        if (server != null && returnPoint != null) {
            ServerWorld returnWorld = server.getWorld(returnPoint.worldKey());
            if (returnWorld != null && canSafelyStandAt(player, returnWorld, returnPoint.pos())) {
                targetWorld = returnWorld;
                targetPos = returnPoint.pos();
                targetYaw = returnPoint.yaw();
                targetPitch = returnPoint.pitch();
            }
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(targetWorld);
        ((GameWorldComponentAccessor) game).sparktraits$getDeadPlayers().remove(player.getUuid());
        game.sync();

        restorePendingControl(player);
        player.setHealth(player.getMaxHealth());
        player.setAir(player.getMaxAir());
        player.setInvulnerable(state.wasInvulnerable());
        player.teleport(targetWorld, targetPos.x, targetPos.y, targetPos.z, Set.of(), targetYaw, targetPitch);
        state.effects().restore(player);
        if (isRealMoodRole(game.getRole(player))) {
            PlayerMoodComponent.KEY.get(player).setMood(0.5f);
        }
        ensureRevolver(player);
        TraitPlayerComponent.KEY.get(player).setKillerInstinctHidden(true);
        revivedImmunePlayers.add(player.getUuid());
        discardBody(player.getServer(), state);
        resetVoiceChatGroup(player.getUuid());
        playTotemEffects(player);
    }

    private static Entity pendingCameraEntity(ServerPlayerEntity player, PendingState state) {
        MinecraftServer server = player.getServer();
        if (server != null && state.bodyUuid() != null) {
            ServerWorld world = server.getWorld(state.deathWorldKey());
            if (world != null && world.getEntity(state.bodyUuid()) instanceof PlayerBodyEntity body) {
                return body;
            }
        }
        return player;
    }

    private static void restorePendingControl(ServerPlayerEntity player) {
        player.setCameraEntity(player);
        player.changeGameMode(GameMode.ADVENTURE);
    }

    private static void addMoodSample(UUID uuid, double moodPercent) {
        ArrayDeque<Double> samples = moodSamples.computeIfAbsent(uuid, ignored -> new ArrayDeque<>());
        samples.addLast(moodPercent);
        while (samples.size() > MAX_MOOD_SAMPLES) {
            samples.removeFirst();
        }
    }

    private static double roundedMoodAverage(ServerPlayerEntity player) {
        ArrayDeque<Double> samples = moodSamples.get(player.getUuid());
        return roundedMoodAverage(samples, preDeathMoodPercent.getOrDefault(player.getUuid(), currentMoodPercent(player)));
    }

    static double roundedMoodAverage(@Nullable Collection<Double> samples, double fallbackMoodPercent) {
        if (samples == null || samples.isEmpty()) {
            return roundToTwoDecimals(fallbackMoodPercent);
        }
        double sum = 0.0;
        for (double sample : samples) {
            sum += sample;
        }
        return roundToTwoDecimals(sum / samples.size());
    }

    private static double currentMoodPercent(ServerPlayerEntity player) {
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (!isRealMoodRole(role)) {
            // Roles without a real mood bar (for example Undercover) are treated as fully stable.
            // 没有真实理智条的身份（例如卧底）按满理智处理，避免隐藏 fallback 影响触发。
            return 100.0;
        }
        return PlayerMoodComponent.KEY.get(player).getMood() * 100.0;
    }

    private static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static boolean meetsPopulationThresholdAfterDeath(GameWorldComponent game, UUID dyingPlayerUuid) {
        int startingCivilians = 0;
        int aliveCiviliansAfterDeath = 0;
        for (Map.Entry<UUID, Role> entry : game.getRoles().entrySet()) {
            if (!isCivilian(entry.getValue())) {
                continue;
            }
            startingCivilians++;
            if (!entry.getKey().equals(dyingPlayerUuid) && !game.isPlayerDead(entry.getKey())) {
                aliveCiviliansAfterDeath++;
            }
        }
        return meetsCivilianPopulationThreshold(startingCivilians, aliveCiviliansAfterDeath);
    }

    static boolean meetsCivilianPopulationThresholdAfterDeath(int startingCivilians, int aliveCiviliansBeforeDeath) {
        return meetsCivilianPopulationThreshold(startingCivilians, Math.max(0, aliveCiviliansBeforeDeath - 1));
    }

    static boolean meetsCivilianPopulationThreshold(int startingCivilians, int aliveCiviliansAfterDeath) {
        return startingCivilians > 0 && aliveCiviliansAfterDeath <= Math.floorDiv(startingCivilians, 3);
    }

    private static boolean isCivilian(@Nullable Role role) {
        return role != null && role.getFaction() == Faction.CIVILIAN;
    }

    private static boolean isRealMoodRole(@Nullable Role role) {
        return role != null && role.getMoodType() == Role.MoodType.REAL;
    }

    public static boolean hasPendingInWorld(ServerWorld world) {
        for (PendingState state : pendingPlayers.values()) {
            if (state.deathWorldKey().equals(world.getRegistryKey())) {
                return true;
            }
        }
        return false;
    }

    private static void applyPendingEffect(ServerPlayerEntity player, RegistryEntry<StatusEffect> effect) {
        player.removeStatusEffect(effect);
        player.addStatusEffect(new StatusEffectInstance(effect, PENDING_TICKS + 10, 0, false, false, true));
    }

    private static RegistryEntry<StatusEffect> noellesNoCollisionEffect() {
        return Registries.STATUS_EFFECT.getEntry(NOELLES_NO_COLLISION).orElseThrow(() ->
                new IllegalStateException("Missing NoellesRoles status effect: " + NOELLES_NO_COLLISION));
    }

    private static PlayerBodyEntity findNewestBody(ServerWorld world, UUID playerUuid) {
        return world.getEntitiesByType(WatheEntities.PLAYER_BODY, body -> playerUuid.equals(body.getPlayerUuid()))
                .stream()
                .max(Comparator.comparingInt(PlayerBodyEntity::getDeathGameTime))
                .orElse(null);
    }

    private static PlayerBodyEntity spawnFallbackBody(ServerWorld world, ServerPlayerEntity player, Identifier deathReason) {
        PlayerBodyEntity body = WatheEntities.PLAYER_BODY.create(world);
        if (body == null) {
            return null;
        }
        body.refreshPositionAndAngles(player.getPos(), player.getYaw(), player.getPitch());
        body.setPlayerUuid(player.getUuid());
        body.setDeathReason(deathReason);
        body.setDeathGameTime(world.getTime());
        world.spawnEntity(body);
        return body;
    }

    private static void discardBody(@Nullable MinecraftServer server, PendingState state) {
        if (server == null || state.bodyUuid() == null) {
            return;
        }
        ServerWorld world = server.getWorld(state.deathWorldKey());
        if (world != null && world.getEntity(state.bodyUuid()) instanceof PlayerBodyEntity body) {
            body.discard();
        }
    }

    private static boolean canSafelyStandAt(ServerPlayerEntity player, ServerWorld world, Vec3d pos) {
        if (pos.y < world.getBottomY() || pos.y >= world.getTopY()) {
            return false;
        }
        BlockPos feet = BlockPos.ofFloored(pos);
        BlockPos below = feet.down();
        if (world.getBlockState(below).getCollisionShape(world, below).isEmpty()) {
            return false;
        }
        Box targetBox = player.getBoundingBox().offset(pos.subtract(player.getPos()));
        return world.isSpaceEmpty(player, targetBox);
    }

    private static void ensureRevolver(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (player.getInventory().getStack(slot).isOf(WatheItems.REVOLVER)) {
                return;
            }
        }
        ItemStack revolver = new ItemStack(WatheItems.REVOLVER);
        if (!player.giveItemStack(revolver)) {
            player.dropItem(revolver, false);
        }
    }

    private static void playTotemEffects(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        world.sendEntityStatus(player, EntityStatuses.USE_TOTEM_OF_UNDYING);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getBodyY(0.5), player.getZ(), 48, 0.5, 0.8, 0.5, 0.2);
    }

    private static void resetVoiceChatGroup(UUID uuid) {
        try {
            Class<?> plugin = Class.forName("dev.doctor4t.wathe.compat.TrainVoicePlugin");
            Method resetPlayer = plugin.getMethod("resetPlayer", UUID.class);
            resetPlayer.invoke(null, uuid);
        } catch (ReflectiveOperationException exception) {
            SparkTraits.LOGGER.debug("Unable to reset voice chat group for Last Stand revive", exception);
        }
    }

    private record ReturnPoint(RegistryKey<World> worldKey, Vec3d pos, float yaw, float pitch) {
    }

    private record PendingState(
            UUID playerUuid,
            RegistryKey<World> deathWorldKey,
            Vec3d deathPos,
            float deathYaw,
            float deathPitch,
            long reviveTime,
            @Nullable UUID bodyUuid,
            EffectSnapshot effects,
            boolean wasInvulnerable
    ) {
    }

    private record EffectSnapshot(
            @Nullable StatusEffectInstance blindness,
            @Nullable StatusEffectInstance invisibility,
            @Nullable StatusEffectInstance noCollision
    ) {
        static EffectSnapshot capture(ServerPlayerEntity player) {
            return new EffectSnapshot(
                    copy(player.getStatusEffect(StatusEffects.BLINDNESS)),
                    copy(player.getStatusEffect(StatusEffects.INVISIBILITY)),
                    copy(player.getStatusEffect(noellesNoCollisionEffect()))
            );
        }

        void restore(ServerPlayerEntity player) {
            restore(player, StatusEffects.BLINDNESS, blindness);
            restore(player, StatusEffects.INVISIBILITY, invisibility);
            restore(player, noellesNoCollisionEffect(), noCollision);
        }

        private static @Nullable StatusEffectInstance copy(@Nullable StatusEffectInstance effect) {
            return effect == null ? null : new StatusEffectInstance(effect);
        }

        private static void restore(ServerPlayerEntity player, RegistryEntry<StatusEffect> effect, @Nullable StatusEffectInstance snapshot) {
            player.removeStatusEffect(effect);
            if (snapshot != null) {
                player.addStatusEffect(new StatusEffectInstance(snapshot));
            }
        }
    }
}
