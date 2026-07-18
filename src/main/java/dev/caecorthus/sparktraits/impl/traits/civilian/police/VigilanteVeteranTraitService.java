package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandFinalMomentService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import dev.doctor4t.wathe.util.GunDropPayload;
import dev.doctor4t.wathe.util.Scheduler;
import dev.doctor4t.wathe.util.ShootMuzzleS2CPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import dev.caecorthus.sparktraits.SparkTraits;

/**
 * Shared rules for Vigilante and Veteran-only traits.
 * 义警与老兵专属天赋的共享规则入口，避免影响其他角色或阵营翻转逻辑。
 */
public final class VigilanteVeteranTraitService {
    public static final double REVOLVER_RANGE = 30.0;
    public static final double DERRINGER_RANGE = 7.0;
    public static final double MARKSMAN_RANGE_MULTIPLIER = 1.3;
    public static final double HEAVY_ARTILLERY_RANGE = 5.0;
    public static final double NIKO_GUN_RANGE = 300.0;
    public static final int NIKO_REVOLVER_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);
    public static final int NIKO_BURST_INTERVAL_TICKS = 2;
    public static final int NIKO_BURST_SHOTS = 3;
    public static final int NIKO_NIGHT_VISION_TICKS = 240;
    public static final float NIKO_REVOLVER_RECOIL_MULTIPLIER = 0.1f;
    public static final float FAST_RELOAD_MULTIPLIER = 0.7f;
    public static final float WELL_TRAINED_DRAIN_MULTIPLIER = 0.7f;
    private static final int NIKO_REPEAT_SHOT_PUNISHMENT_DELAY_TICKS = 4;
    private static final int NIKO_NIGHT_VISION_AMPLIFIER = 0;
    private static final int NIKO_NIGHT_VISION_TOLERANCE_TICKS = 5;
    private static final Map<UUID, NikoNightVisionState> NIKO_NIGHT_VISION = new HashMap<>();

    private VigilanteVeteranTraitService() {
    }

    public enum NikoNightVisionAction {
        APPLY,
        REMOVE,
        CLEAR_MARKER,
        KEEP
    }

    enum NikoRepeatShotPunishment {
        NONE(false),
        CUSTOM(false),
        PREVENT_GUN_PICKUP(true),
        KILL_SHOOTER(true);

        private final boolean attemptsBackfire;

        NikoRepeatShotPunishment(boolean attemptsBackfire) {
            this.attemptsBackfire = attemptsBackfire;
        }

        boolean attemptsBackfire() {
            return attemptsBackfire;
        }
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(VigilanteVeteranTraitService::tickWorld);
    }

    public static boolean canSelectVigilanteTrait(Role role) {
        return role == WatheRoles.VIGILANTE;
    }

    public static boolean canSelectVeteranTrait(Role role) {
        return role == WatheRoles.VETERAN;
    }

    static Role runtimeVigilanteRole(Role role, boolean finalMomentLooseEnd) {
        return finalMomentLooseEnd && role == WatheRoles.LOOSE_END ? WatheRoles.VIGILANTE : role;
    }

    public static double gunRange(double baseRange, Role role, Collection<Identifier> traits) {
        return gunRange(baseRange, role, traits, false);
    }

    public static double gunRange(double baseRange, Role role, Collection<Identifier> traits, boolean sneaking) {
        if (canUseNikoTrait(role, traits, sneaking)) {
            return NIKO_GUN_RANGE;
        }
        if (canUseVigilanteTrait(role, traits, PoliceTraits.MARKSMAN)) {
            return baseRange * MARKSMAN_RANGE_MULTIPLIER;
        }
        return baseRange;
    }

    public static double gunRange(PlayerEntity player, double baseRange) {
        return gunRange(baseRange, roleOf(player), traitsOf(player), isSneaking(player));
    }

    public static int fastReloadCooldown(Item item, int duration, Role role, Collection<Identifier> traits) {
        return fastReloadCooldown(item == WatheItems.REVOLVER, duration, role, traits, false);
    }

    public static int fastReloadCooldown(
            boolean revolver,
            int duration,
            Role role,
            Collection<Identifier> traits
    ) {
        return fastReloadCooldown(revolver, duration, role, traits, false);
    }

    public static int fastReloadCooldown(
            boolean revolver,
            int duration,
            Role role,
            Collection<Identifier> traits,
            boolean sneaking
    ) {
        if (duration <= 0 || !revolver) {
            return duration;
        }
        if (canUseNikoTrait(role, traits, sneaking)) {
            return NIKO_REVOLVER_COOLDOWN_TICKS;
        }
        if (!canUseVigilanteTrait(role, traits, PoliceTraits.FAST_RELOAD)) {
            return duration;
        }
        return Math.max(1, (int) (duration * FAST_RELOAD_MULTIPLIER));
    }

    public static int fastReloadCooldown(Item item, int duration, PlayerEntity player) {
        return fastReloadCooldown(item == WatheItems.REVOLVER, duration, roleOf(player), traitsOf(player), isSneaking(player));
    }

    public static boolean shouldPreserveNikoRevolverCooldown(int duration, Role role, Collection<Identifier> traits, boolean sneaking) {
        return duration == NIKO_REVOLVER_COOLDOWN_TICKS && canUseNikoTrait(role, traits, sneaking);
    }

    public static boolean shouldPreserveNikoRevolverCooldown(int duration, PlayerEntity player) {
        return shouldPreserveNikoRevolverCooldown(duration, roleOf(player), traitsOf(player), isSneaking(player));
    }

    public static boolean shouldPreserveNikoRevolverCooldown(Item item, int duration, PlayerEntity player) {
        return item == WatheItems.REVOLVER && shouldPreserveNikoRevolverCooldown(duration, player);
    }

    public static double serverGunTargetRange(double originalRange, PlayerEntity player, Item item) {
        if ((item == WatheItems.REVOLVER || item == WatheItems.DERRINGER) && canUseNikoTrait(player)) {
            return NIKO_GUN_RANGE;
        }
        return originalRange;
    }

    public static float adjustedNikoRevolverRecoil(
            float recoil,
            boolean revolver,
            boolean coolingDown,
            boolean gameRunning,
            boolean playerPlayingAndAlive,
            Role role,
            Collection<Identifier> traits,
            boolean sneaking
    ) {
        if (shouldStartNikoRevolverBurst(revolver, coolingDown, gameRunning, playerPlayingAndAlive, role, traits, sneaking)) {
            return recoil * NIKO_REVOLVER_RECOIL_MULTIPLIER;
        }
        return recoil;
    }

    public static float adjustedNikoRevolverRecoil(float recoil, PlayerEntity player) {
        if (player == null || player.getWorld() == null) {
            return recoil;
        }
        return adjustedNikoRevolverRecoil(
                recoil,
                player.getMainHandStack().isOf(WatheItems.REVOLVER),
                player.getItemCooldownManager().isCoolingDown(WatheItems.REVOLVER),
                GameWorldComponent.KEY.get(player.getWorld()).isRunning(),
                !player.isSpectator() && GameFunctions.isPlayerPlayingAndAlive(player),
                roleOf(player),
                traitsOf(player),
                isSneaking(player)
        );
    }

    public static boolean shouldStartNikoRevolverBurst(
            boolean revolver,
            boolean coolingDown,
            boolean gameRunning,
            boolean playerPlayingAndAlive,
            Role role,
            Collection<Identifier> traits,
            boolean sneaking
    ) {
        return revolver
                && !coolingDown
                && gameRunning
                && playerPlayingAndAlive
                && canUseNikoTrait(role, traits, sneaking);
    }

    public static boolean shouldContinueNikoRevolverBurst(
            boolean revolver,
            boolean gameRunning,
            boolean playerPlayingAndAlive,
            Role role,
            Collection<Identifier> traits,
            boolean sneaking
    ) {
        return revolver
                && gameRunning
                && playerPlayingAndAlive
                && canUseNikoTrait(role, traits, sneaking);
    }

    static NikoRepeatShotPunishment decideNikoRepeatShotPunishment(
            boolean victimIsEffectiveCivilian,
            ShouldPunishGunShooter.PunishResult eventResult,
            boolean shooterCreative,
            GameWorldComponent.ShootInnocentPunishment configuredPunishment
    ) {
        if (eventResult != null && eventResult.hasCustomPunishment()) {
            return NikoRepeatShotPunishment.CUSTOM;
        }
        if (!victimIsEffectiveCivilian
                || shooterCreative
                || (eventResult != null && !eventResult.shouldPunish())) {
            return NikoRepeatShotPunishment.NONE;
        }
        return configuredPunishment == GameWorldComponent.ShootInnocentPunishment.PREVENT_GUN_PICKUP
                ? NikoRepeatShotPunishment.PREVENT_GUN_PICKUP
                : NikoRepeatShotPunishment.KILL_SHOOTER;
    }

    public static boolean shouldStartNikoRevolverBurst(PlayerEntity player) {
        return player != null
                && !player.isSpectator()
                && player.getWorld() != null
                && player.getMainHandStack().isOf(WatheItems.REVOLVER)
                && !player.getItemCooldownManager().isCoolingDown(WatheItems.REVOLVER)
                && shouldStartNikoRevolverBurst(
                true,
                false,
                GameWorldComponent.KEY.get(player.getWorld()).isRunning(),
                GameFunctions.isPlayerPlayingAndAlive(player),
                roleOf(player),
                traitsOf(player),
                isSneaking(player)
        );
    }

    private static boolean shouldContinueNikoRevolverBurst(PlayerEntity player) {
        return player != null
                && !player.isSpectator()
                && player.getWorld() != null
                && shouldContinueNikoRevolverBurst(
                player.getMainHandStack().isOf(WatheItems.REVOLVER),
                GameWorldComponent.KEY.get(player.getWorld()).isRunning(),
                GameFunctions.isPlayerPlayingAndAlive(player),
                roleOf(player),
                traitsOf(player),
                isSneaking(player)
        );
    }

    public static boolean isHeavyArtilleryShot(
            Role role,
            Collection<Identifier> traits,
            Identifier deathReason,
            double distanceSquared
    ) {
        return GameConstants.DeathReasons.GUN.equals(deathReason)
                && distanceSquared <= HEAVY_ARTILLERY_RANGE * HEAVY_ARTILLERY_RANGE
                && canUseVigilanteTrait(role, traits, PoliceTraits.HEAVY_ARTILLERY);
    }

    public static boolean isHeavyArtilleryShot(
            ServerPlayerEntity shooter,
            ServerPlayerEntity victim,
            Identifier deathReason
    ) {
        return shooter != null
                && victim != null
                && shooter != victim
                && isHeavyArtilleryShot(roleOf(shooter), traitsOf(shooter), deathReason, shooter.squaredDistanceTo(victim));
    }

    public static boolean shouldRetryHeavyArtilleryDamage(boolean eligibleShot, boolean victimStillAlive) {
        return eligibleShot && victimStillAlive;
    }

    public static boolean shouldRetryHeavyArtilleryDamage(
            boolean eligibleShot,
            boolean victimStillAlive,
            boolean victimInJesterMomentTransition
    ) {
        return shouldRetryHeavyArtilleryDamage(eligibleShot, victimStillAlive)
                && !victimInJesterMomentTransition;
    }

    public static void killPlayerWithHeavyArtillery(
            ServerPlayerEntity victim,
            boolean spawnBody,
            ServerPlayerEntity shooter,
            Identifier deathReason
    ) {
        boolean eligibleShot = isHeavyArtilleryShot(shooter, victim, deathReason);
        GameFunctions.killPlayer(victim, spawnBody, shooter, deathReason);
        if (shouldRetryHeavyArtilleryDamage(
                eligibleShot,
                GameFunctions.isPlayerPlayingAndAlive(victim),
                isJesterMomentActiveOrTransitioning(victim)
        )) {
            GameFunctions.killPlayer(victim, spawnBody, shooter, deathReason);
        }
    }

    private static boolean isJesterMomentActiveOrTransitioning(ServerPlayerEntity victim) {
        if (victim == null || victim.getWorld() == null) {
            return false;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        if (!game.isRole(victim, Noellesroles.JESTER)) {
            return false;
        }
        JesterPlayerComponent jester = JesterPlayerComponent.KEY.get(victim);
        // Heavy Artillery must not convert NoellesRoles' fake-death handoff into a real death.
        // 重炮不能把 NoellesRoles 的小丑假死交接阶段再次结算成真死亡。
        return jester.inPsychoMode || jester.isTransitioning();
    }

    public static void killPlayerWithPoliceGunTraits(
            ServerPlayerEntity victim,
            boolean spawnBody,
            ServerPlayerEntity shooter,
            Identifier deathReason
    ) {
        killPlayerWithHeavyArtillery(victim, spawnBody, shooter, deathReason);
    }

    public static float wellTrainedAdjustedMood(
            float currentMood,
            float proposedMood,
            Role role,
            Collection<Identifier> traits
    ) {
        if (proposedMood >= currentMood || !canUseVeteranTrait(role, traits, PoliceTraits.WELL_TRAINED)) {
            return proposedMood;
        }
        return currentMood - (currentMood - proposedMood) * WELL_TRAINED_DRAIN_MULTIPLIER;
    }

    public static float wellTrainedAdjustedMood(float currentMood, float proposedMood, PlayerEntity player) {
        return wellTrainedAdjustedMood(currentMood, proposedMood, roleOf(player), traitsOf(player));
    }

    public static boolean ignoresLowMood(Role role, Collection<Identifier> traits) {
        return canUseVeteranTrait(role, traits, PoliceTraits.WELL_TRAINED);
    }

    public static boolean ignoresLowMood(PlayerEntity player) {
        return ignoresLowMood(roleOf(player), traitsOf(player));
    }

    public static boolean shouldRefreshNikoNightVision(
            boolean playerPlayingAndAlive,
            Role role,
            Collection<Identifier> traits,
            boolean sneaking
    ) {
        return playerPlayingAndAlive && canUseNikoTrait(role, traits, sneaking);
    }

    public static NikoNightVisionAction nextNikoNightVisionAction(
            boolean eligible,
            boolean ownedByNiko,
            int currentNightVisionDurationTicks,
            int currentNightVisionAmplifier,
            long currentWorldTick,
            long ownedExpiresAtTick
    ) {
        boolean hasNightVision = currentNightVisionDurationTicks > 0;
        if (ownedByNiko && !hasNightVision) {
            return eligible ? NikoNightVisionAction.APPLY : NikoNightVisionAction.CLEAR_MARKER;
        }
        if (ownedByNiko && !matchesNikoOwnedNightVision(
                currentNightVisionDurationTicks,
                currentNightVisionAmplifier,
                currentWorldTick,
                ownedExpiresAtTick
        )) {
            return NikoNightVisionAction.CLEAR_MARKER;
        }
        if (!eligible) {
            return ownedByNiko && hasNightVision ? NikoNightVisionAction.REMOVE : NikoNightVisionAction.KEEP;
        }
        if (!hasNightVision) {
            return NikoNightVisionAction.APPLY;
        }
        if (!ownedByNiko) {
            return NikoNightVisionAction.KEEP;
        }
        if (currentNightVisionDurationTicks <= NIKO_NIGHT_VISION_TICKS / 2) {
            return NikoNightVisionAction.APPLY;
        }
        return NikoNightVisionAction.KEEP;
    }

    public static void scheduleNikoRevolverBurstRepeats(ServerPlayerEntity shooter) {
        if (!shouldStartNikoRevolverBurst(shooter)
                || !(shooter.getWorld() instanceof ServerWorld scheduledWorld)) {
            return;
        }
        GameWorldComponent scheduledGame = GameWorldComponent.KEY.get(scheduledWorld);
        // Do not replay Wathe's full gun packet handler: it owns inventory, punishment, and cooldown side effects.
        // 不重复执行 Wathe 的完整枪械包处理；那里负责扣枪、惩罚和冷却，重复调用会扩大副作用。
        for (int shot = 1; shot < NIKO_BURST_SHOTS; shot++) {
            int repeatDelay = NIKO_BURST_INTERVAL_TICKS * shot;
            AtomicReference<Runnable> pendingPunishment = new AtomicReference<>();
            Scheduler.schedule(
                    () -> repeatNikoBurstShot(shooter, scheduledWorld, scheduledGame, pendingPunishment),
                    repeatDelay
            );
            Scheduler.schedule(
                    () -> runNikoBurstContinuation(shooter, scheduledWorld, scheduledGame, pendingPunishment),
                    repeatDelay + NIKO_REPEAT_SHOT_PUNISHMENT_DELAY_TICKS
            );
        }
    }

    private static void repeatNikoBurstShot(
            ServerPlayerEntity shooter,
            ServerWorld scheduledWorld,
            GameWorldComponent scheduledGame,
            AtomicReference<Runnable> pendingPunishment
    ) {
        if (!isCurrentNikoBurstContext(shooter, scheduledWorld, scheduledGame)
                || !canContinueNikoBurst(shooter)) {
            return;
        }
        playNikoBurstFeedback(shooter);
        ServerPlayerEntity target = currentNikoGunTarget(shooter);
        if (target == null) {
            return;
        }
        resolveNikoBurstShot(shooter, target, pendingPunishment);
    }

    /** Mirrors only Wathe's per-hit punishment for synthetic Niko shots, not its packet side effects.
     *  仅为 Niko 合成补发复用 Wathe 的单发命中惩罚，不重复执行完整枪械包副作用。 */
    private static void resolveNikoBurstShot(
            ServerPlayerEntity shooter,
            ServerPlayerEntity target,
            AtomicReference<Runnable> pendingPunishment
    ) {
        GameWorldComponent game = GameWorldComponent.KEY.get(shooter.getWorld());
        ShouldPunishGunShooter.PunishResult eventResult = ShouldPunishGunShooter.EVENT.invoker()
                .shouldPunish(shooter, target);
        NikoRepeatShotPunishment punishment = decideNikoRepeatShotPunishment(
                EffectiveTraitService.shouldTreatGunVictimAsInnocent(
                        game.getRole(target),
                        TraitPlayerComponent.KEY.get(target).getActiveTraitIds()
                ),
                eventResult,
                shooter.isCreative(),
                game.getShootInnocentPunishment()
        );

        if (punishment == NikoRepeatShotPunishment.CUSTOM) {
            pendingPunishment.set(eventResult::executeCustomPunishment);
        } else if (punishment.attemptsBackfire()) {
            if (game.isInnocent(shooter) && shooter.getRandom().nextFloat() <= game.getBackfireChance()) {
                GameFunctions.killPlayer(shooter, true, shooter, GameConstants.DeathReasons.GUN_BACKFIRE);
                return;
            }
            pendingPunishment.set(() -> executeNikoRepeatShotPunishment(shooter, game, punishment));
        }

        killPlayerWithHeavyArtillery(target, true, shooter, GameConstants.DeathReasons.GUN);
    }

    private static void runNikoBurstContinuation(
            ServerPlayerEntity shooter,
            ServerWorld scheduledWorld,
            GameWorldComponent scheduledGame,
            AtomicReference<Runnable> pendingPunishment
    ) {
        Runnable punishment = pendingPunishment.getAndSet(null);
        if (punishment != null && isCurrentNikoBurstContext(shooter, scheduledWorld, scheduledGame)) {
            punishment.run();
        }
    }

    private static boolean isCurrentNikoBurstContext(
            ServerPlayerEntity shooter,
            ServerWorld scheduledWorld,
            GameWorldComponent scheduledGame
    ) {
        return scheduledWorld.getServer().getPlayerManager().getPlayer(shooter.getUuid()) == shooter
                && shooter.getWorld() == scheduledWorld
                && GameWorldComponent.KEY.get(scheduledWorld) == scheduledGame
                && scheduledGame.isRunning()
                && !shooter.isSpectator()
                && GameFunctions.isPlayerPlayingAndAlive(shooter);
    }

    private static void executeNikoRepeatShotPunishment(
            ServerPlayerEntity shooter,
            GameWorldComponent game,
            NikoRepeatShotPunishment punishment
    ) {
        if (!shooter.getInventory().contains(stack -> stack.isIn(WatheItemTags.GUNS))) {
            return;
        }
        shooter.getInventory().remove(
                stack -> stack.isOf(WatheItems.REVOLVER),
                1,
                shooter.getInventory()
        );
        if (game.canUseKillerFeatures(shooter)) {
            return;
        }

        ItemEntity droppedGun = shooter.dropItem(WatheItems.REVOLVER.getDefaultStack(), false, false);
        if (droppedGun != null) {
            droppedGun.setPickupDelay(10);
            droppedGun.setThrower(shooter);
        }
        ServerPlayNetworking.send(shooter, new GunDropPayload());
        PlayerMoodComponent.KEY.get(shooter).setMood(0);
        game.addToPreventGunPickup(shooter);
        if (game.isInnocent(shooter) && punishment == NikoRepeatShotPunishment.KILL_SHOOTER) {
            GameFunctions.killPlayer(shooter, true, null, GameConstants.DeathReasons.SHOT_INNOCENT);
        }
    }

    private static boolean canContinueNikoBurst(ServerPlayerEntity shooter) {
        return shooter != null && shooter.getWorld() instanceof ServerWorld && shouldContinueNikoRevolverBurst(shooter);
    }

    private static ServerPlayerEntity currentNikoGunTarget(ServerPlayerEntity shooter) {
        if (ProjectileUtil.getCollision(
                shooter,
                entity -> entity instanceof ServerPlayerEntity target
                        && target != shooter
                        && GameFunctions.isPlayerAliveAndSurvival(target)
                        && GameFunctions.isPlayerPlayingAndAlive(target),
                NIKO_GUN_RANGE
        ) instanceof EntityHitResult hit && hit.getEntity() instanceof ServerPlayerEntity target) {
            return target;
        }
        return null;
    }

    private static void playNikoBurstFeedback(ServerPlayerEntity shooter) {
        shooter.getWorld().playSound(
                null,
                shooter.getX(),
                shooter.getEyeY(),
                shooter.getZ(),
                WatheSounds.ITEM_REVOLVER_SHOOT,
                SoundCategory.PLAYERS,
                5.0f,
                1.0f + shooter.getRandom().nextFloat() * 0.1f - 0.05f
        );
        ShootMuzzleS2CPayload payload = new ShootMuzzleS2CPayload(shooter.getUuidAsString());
        for (ServerPlayerEntity tracking : PlayerLookup.tracking(shooter)) {
            ServerPlayNetworking.send(tracking, payload);
        }
        ServerPlayNetworking.send(shooter, payload);
    }

    private static void tickWorld(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (game == null || !game.isRunning()) {
            clearNikoNightVision(world);
            return;
        }
        long worldTick = world.getTime();
        for (ServerPlayerEntity player : world.getPlayers()) {
            TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
            boolean eligible = shouldRefreshNikoNightVision(
                    GameFunctions.isPlayerPlayingAndAlive(player),
                    roleOf(player),
                    traits.getActiveTraitIds(),
                    player.isSneaking()
            );
            syncNikoNightVision(player, eligible, worldTick);
        }
    }

    private static void syncNikoNightVision(ServerPlayerEntity player, boolean eligible, long worldTick) {
        UUID playerUuid = player.getUuid();
        NikoNightVisionState state = NIKO_NIGHT_VISION.get(playerUuid);
        StatusEffectInstance current = player.getStatusEffect(StatusEffects.NIGHT_VISION);
        int duration = current == null ? 0 : current.getDuration();
        int amplifier = current == null ? NIKO_NIGHT_VISION_AMPLIFIER : current.getAmplifier();
        long expiresAtTick = state == null ? 0L : state.expiresAtTick();
        NikoNightVisionAction action = nextNikoNightVisionAction(
                eligible,
                state != null,
                duration,
                amplifier,
                worldTick,
                expiresAtTick
        );
        if (action == NikoNightVisionAction.APPLY) {
            applyNikoNightVision(player, worldTick);
        } else if (action == NikoNightVisionAction.REMOVE) {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            NIKO_NIGHT_VISION.remove(playerUuid);
        } else if (action == NikoNightVisionAction.CLEAR_MARKER) {
            NIKO_NIGHT_VISION.remove(playerUuid);
        }
    }

    private static void applyNikoNightVision(ServerPlayerEntity player, long worldTick) {
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                NIKO_NIGHT_VISION_TICKS,
                NIKO_NIGHT_VISION_AMPLIFIER,
                false,
                false,
                true
        ));
        NIKO_NIGHT_VISION.put(player.getUuid(), new NikoNightVisionState(worldTick + NIKO_NIGHT_VISION_TICKS));
    }

    private static void clearNikoNightVision(ServerWorld world) {
        long worldTick = world.getTime();
        for (ServerPlayerEntity player : world.getPlayers()) {
            syncNikoNightVision(player, false, worldTick);
        }
    }

    private static boolean matchesNikoOwnedNightVision(
            int currentNightVisionDurationTicks,
            int currentNightVisionAmplifier,
            long currentWorldTick,
            long ownedExpiresAtTick
    ) {
        if (currentNightVisionDurationTicks <= 0 || currentNightVisionAmplifier != NIKO_NIGHT_VISION_AMPLIFIER) {
            return false;
        }
        long expectedRemainingTicks = ownedExpiresAtTick - currentWorldTick;
        if (expectedRemainingTicks <= 0) {
            return false;
        }
        // Only remove effects that still match SparkTraits' own Niko refresh window.
        // 只移除仍匹配 SparkTraits Niko 刷新窗口的夜视，避免误删关灯、金酒、人鱼等来源。
        return Math.abs(currentNightVisionDurationTicks - expectedRemainingTicks) <= NIKO_NIGHT_VISION_TOLERANCE_TICKS;
    }

    private static boolean canUseNikoTrait(PlayerEntity player) {
        return canUseNikoTrait(roleOf(player), traitsOf(player), isSneaking(player));
    }

    private static boolean canUseNikoTrait(Role role, Collection<Identifier> traits, boolean sneaking) {
        return sneaking && canUseVigilanteTrait(role, traits, PoliceTraits.NIKO);
    }

    private static boolean canUseVigilanteTrait(Role role, Collection<Identifier> traits, Identifier traitId) {
        return canSelectVigilanteTrait(role) && safeTraits(traits).contains(traitId);
    }

    private static boolean canUseVeteranTrait(Role role, Collection<Identifier> traits, Identifier traitId) {
        return canSelectVeteranTrait(role) && safeTraits(traits).contains(traitId);
    }

    private static Role roleOf(PlayerEntity player) {
        if (player == null || player.getWorld() == null) {
            return null;
        }
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        return runtimeVigilanteRole(role, LastStandFinalMomentService.isFinalMomentLooseEnd(player));
    }

    private static Collection<Identifier> traitsOf(PlayerEntity player) {
        if (player == null) {
            return List.of();
        }
        return TraitPlayerComponent.KEY.get(player).getActiveTraitIds();
    }

    private static boolean isSneaking(PlayerEntity player) {
        return player != null && player.isSneaking();
    }

    private static Collection<Identifier> safeTraits(Collection<Identifier> traits) {
        return traits == null ? List.of() : traits;
    }

    private record NikoNightVisionState(long expiresAtTick) {
    }
}
