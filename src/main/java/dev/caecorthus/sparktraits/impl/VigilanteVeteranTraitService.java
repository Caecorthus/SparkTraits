package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

/**
 * Shared rules for Vigilante and Veteran-only traits.
 * 义警与老兵专属天赋的共享规则入口，避免影响其他角色或阵营翻转逻辑。
 */
public final class VigilanteVeteranTraitService {
    public static final double REVOLVER_RANGE = 30.0;
    public static final double DERRINGER_RANGE = 7.0;
    public static final double MARKSMAN_RANGE_MULTIPLIER = 1.3;
    public static final double HEAVY_ARTILLERY_RANGE = 5.0;
    public static final float FAST_RELOAD_MULTIPLIER = 0.7f;
    public static final float WELL_TRAINED_DRAIN_MULTIPLIER = 0.7f;

    private VigilanteVeteranTraitService() {
    }

    public static boolean canSelectVigilanteTrait(Role role) {
        return role == WatheRoles.VIGILANTE;
    }

    public static boolean canSelectVeteranTrait(Role role) {
        return role == WatheRoles.VETERAN;
    }

    public static double gunRange(double baseRange, Role role, Collection<Identifier> traits) {
        if (canUseVigilanteTrait(role, traits, PoliceTraits.MARKSMAN)) {
            return baseRange * MARKSMAN_RANGE_MULTIPLIER;
        }
        return baseRange;
    }

    public static double gunRange(PlayerEntity player, double baseRange) {
        return gunRange(baseRange, roleOf(player), traitsOf(player));
    }

    public static int fastReloadCooldown(Item item, int duration, Role role, Collection<Identifier> traits) {
        return fastReloadCooldown(item == WatheItems.REVOLVER, duration, role, traits);
    }

    public static int fastReloadCooldown(
            boolean revolver,
            int duration,
            Role role,
            Collection<Identifier> traits
    ) {
        if (duration <= 0 || !revolver || !canUseVigilanteTrait(role, traits, PoliceTraits.FAST_RELOAD)) {
            return duration;
        }
        return Math.max(1, (int) (duration * FAST_RELOAD_MULTIPLIER));
    }

    public static int fastReloadCooldown(Item item, int duration, PlayerEntity player) {
        return fastReloadCooldown(item, duration, roleOf(player), traitsOf(player));
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

    public static void killPlayerWithHeavyArtillery(
            ServerPlayerEntity victim,
            boolean spawnBody,
            ServerPlayerEntity shooter,
            Identifier deathReason
    ) {
        boolean eligibleShot = isHeavyArtilleryShot(shooter, victim, deathReason);
        GameFunctions.killPlayer(victim, spawnBody, shooter, deathReason);
        if (shouldRetryHeavyArtilleryDamage(eligibleShot, GameFunctions.isPlayerPlayingAndAlive(victim))) {
            GameFunctions.killPlayer(victim, spawnBody, shooter, deathReason);
        }
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

    private static boolean canUseVigilanteTrait(Role role, Collection<Identifier> traits, Identifier traitId) {
        return canSelectVigilanteTrait(role) && traits.contains(traitId);
    }

    private static boolean canUseVeteranTrait(Role role, Collection<Identifier> traits, Identifier traitId) {
        return canSelectVeteranTrait(role) && traits.contains(traitId);
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
}
