package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.util.Scheduler;
import dev.doctor4t.wathe.util.ShootMuzzleS2CPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import java.util.List;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.jester.JesterPlayerComponent;

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
    public static final int NIKO_NIGHT_VISION_TICKS = 60;
    public static final float NIKO_REVOLVER_RECOIL_MULTIPLIER = 0.1f;
    public static final float FAST_RELOAD_MULTIPLIER = 0.7f;
    public static final float WELL_TRAINED_DRAIN_MULTIPLIER = 0.7f;

    private VigilanteVeteranTraitService() {
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

    public static boolean goingDarkInstinctHidden(
            boolean blackoutActive,
            boolean playerPlayingAndAlive,
            Role role,
            Collection<Identifier> traits
    ) {
        return blackoutActive
                && playerPlayingAndAlive
                && canUseVeteranTrait(role, traits, PoliceTraits.GOING_DARK);
    }

    public static boolean shouldSkipGoingDarkDefaultInstinct(
            boolean targetGoingDarkInstinctHidden,
            boolean defaultInstinctBranch,
            boolean spectatorBranch
    ) {
        return targetGoingDarkInstinctHidden && defaultInstinctBranch && !spectatorBranch;
    }

    public static void syncGoingDarkInstinct(ServerWorld world, boolean blackoutActive) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
            traits.setGoingDarkInstinctHidden(goingDarkInstinctHidden(
                    blackoutActive,
                    GameFunctions.isPlayerPlayingAndAlive(player),
                    game.getRole(player),
                    traits.getActiveTraitIds()
            ));
        }
    }

    public static boolean shouldRefreshNikoNightVision(
            boolean playerPlayingAndAlive,
            Role role,
            Collection<Identifier> traits,
            boolean sneaking
    ) {
        return playerPlayingAndAlive && canUseNikoTrait(role, traits, sneaking);
    }

    public static void scheduleNikoRevolverBurstRepeats(ServerPlayerEntity shooter) {
        if (!shouldStartNikoRevolverBurst(shooter)) {
            return;
        }
        // Do not replay Wathe's full gun packet handler: it owns inventory, punishment, and cooldown side effects.
        // 不重复执行 Wathe 的完整枪械包处理；那里负责扣枪、惩罚和冷却，重复调用会扩大副作用。
        for (int shot = 1; shot < NIKO_BURST_SHOTS; shot++) {
            Scheduler.schedule(
                    () -> repeatNikoBurstShot(shooter),
                    NIKO_BURST_INTERVAL_TICKS * shot
            );
        }
    }

    private static void repeatNikoBurstShot(ServerPlayerEntity shooter) {
        if (!canContinueNikoBurst(shooter)) {
            return;
        }
        playNikoBurstFeedback(shooter);
        ServerPlayerEntity target = currentNikoGunTarget(shooter);
        if (target == null) {
            return;
        }
        killPlayerWithHeavyArtillery(target, true, shooter, GameConstants.DeathReasons.GUN);
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
            return;
        }
        for (ServerPlayerEntity player : world.getPlayers()) {
            TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
            if (shouldRefreshNikoNightVision(
                    GameFunctions.isPlayerPlayingAndAlive(player),
                    game.getRole(player),
                    traits.getActiveTraitIds(),
                    player.isSneaking()
            )) {
                refreshNikoNightVision(player);
            }
        }
    }

    private static void refreshNikoNightVision(ServerPlayerEntity player) {
        StatusEffectInstance current = player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (current != null && current.getDuration() > NIKO_NIGHT_VISION_TICKS / 2) {
            return;
        }
        // Refresh a short effect without removing it later, so other night-vision sources stay intact.
        // 只刷新短时夜视，不在失效时主动移除，避免误删其他模组或关灯给予的夜视。
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                NIKO_NIGHT_VISION_TICKS,
                0,
                false,
                false,
                true
        ));
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
        return GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
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
}
