package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;
import dev.doctor4t.wathe.api.event.ShouldShowCohort;
import dev.doctor4t.wathe.api.event.TaskComplete;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Central effective-team rules for alignment-flipping traits.
 *  阵营翻转天赋的统一有效阵营规则入口。 */
public final class EffectiveTraitService {
    public static final int CONSCIENCE_COLOR = 0xFFDEF8;
    public static final int IMPOSTOR_COLOR = 0x7D0000;
    public static final int IMPOSTOR_INSTINCT_COLOR = 0x0013FF;
    public static final int CIVILIAN_INSTINCT_COLOR = 0x4EDD35;
    public static final double CONSCIENCE_INSTINCT_RANGE_SQUARED = 100.0;
    public static final int TASK_MONEY_REWARD = 50;
    public static final Identifier SELF_REALIZATION = SparkTraits.id("self_realization");

    private EffectiveTraitService() {
    }

    public static void register() {
        CheckWinCondition.EVENT.register(EffectiveTraitService::checkWin);
        TaskComplete.EVENT.register((player, taskType) -> {
            if (hasConscience(player)) {
                PlayerShopComponent.KEY.get(player).addToBalance(TASK_MONEY_REWARD);
            }
        });
        ShouldPunishGunShooter.EVENT.register((shooter, victim) -> {
            GameWorldComponent game = GameWorldComponent.KEY.get(shooter.getWorld());
            if (isEffectiveKiller(shooter, game) && isEffectiveCivilian(victim, game)) {
                return ShouldPunishGunShooter.PunishResult.cancel();
            }
            return null;
        });
        ShouldShowCohort.EVENT.register((viewer, target) -> {
            GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
            Boolean override = cohortOverride(
                    game.getRole(viewer),
                    TraitPlayerComponent.KEY.get(viewer).getActiveTraitIds(),
                    game.getRole(target),
                    instinctVisibleTraitIds(target)
            );
            if (override == null) {
                return null;
            }
            return override
                    ? ShouldShowCohort.CohortResult.show(ShouldShowCohort.CohortResult.PRIORITY_HIGH)
                    : ShouldShowCohort.CohortResult.hide();
        });
    }

    public static boolean hasConscience(PlayerEntity player) {
        return player != null && hasConscience(TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    public static boolean hasImpostor(PlayerEntity player) {
        return player != null && hasImpostor(TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    public static boolean isConscienceVisibleToInstinct(PlayerEntity player) {
        return player != null && TraitPlayerComponent.KEY.get(player).isConscienceInstinctVisible();
    }

    public static boolean isImpostorVisibleToInstinct(PlayerEntity player) {
        return player != null && TraitPlayerComponent.KEY.get(player).isImpostorInstinctVisible();
    }

    public static boolean isHiddenFromKillerInstinct(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        return shouldHideFromKillerInstinct(traits.isLastStandPending(), traits.isKillerInstinctHidden());
    }

    public static boolean shouldHideFromKillerInstinct(boolean lastStandPending, boolean killerInstinctHidden) {
        return lastStandPending || killerInstinctHidden;
    }

    public static boolean shouldConscienceInstinctHighlightTarget(
            boolean instinctEnabled,
            boolean targetPlayingAndAlive,
            boolean targetSpectatingOrCreative,
            double targetDistanceSquared,
            boolean lastStandPending,
            boolean killerInstinctHidden
    ) {
        return instinctEnabled
                && !targetSpectatingOrCreative
                && targetDistanceSquared <= CONSCIENCE_INSTINCT_RANGE_SQUARED
                && (targetPlayingAndAlive || lastStandPending || killerInstinctHidden);
    }

    private static Collection<Identifier> instinctVisibleTraitIds(PlayerEntity player) {
        boolean conscience = isConscienceVisibleToInstinct(player);
        boolean impostor = isImpostorVisibleToInstinct(player);
        if (conscience && impostor) {
            return List.of(ConscienceTrait.ID, ImpostorTrait.ID);
        }
        if (conscience) {
            return List.of(ConscienceTrait.ID);
        }
        if (impostor) {
            return List.of(ImpostorTrait.ID);
        }
        return List.of();
    }

    public static boolean hasConscience(Collection<Identifier> traits) {
        return traits.contains(ConscienceTrait.ID);
    }

    public static boolean hasImpostor(Collection<Identifier> traits) {
        return traits.contains(ImpostorTrait.ID);
    }

    public static boolean canSelectConscience(Role role, GameWorldComponent gameComponent, Collection<Identifier> selectedTraits) {
        return canSelectConscience(role, originalKillerCount(gameComponent), selectedTraits);
    }

    public static boolean canSelectConscience(Role role, int originalKillerCount, Collection<Identifier> selectedTraits) {
        return originalKillerCount >= 2
                && isOriginalKiller(role)
                && !hasImpostor(selectedTraits);
    }

    public static boolean canSelectImpostor(Role role, GameWorldComponent gameComponent, Collection<Identifier> selectedTraits) {
        return canSelectImpostor(role, originalKillerCount(gameComponent), selectedTraits);
    }

    public static boolean canSelectImpostor(Role role, int originalKillerCount, Collection<Identifier> selectedTraits) {
        return originalKillerCount >= 2
                && isOriginalCivilian(role)
                && !isUndercover(role)
                && !selectedTraits.contains(LastStandTrait.ID)
                && !hasConscience(selectedTraits);
    }

    public static int originalKillerCount(GameWorldComponent gameComponent) {
        if (gameComponent == null) {
            return 0;
        }
        int count = 0;
        for (UUID uuid : gameComponent.getAllPlayers()) {
            if (isOriginalKiller(gameComponent.getRole(uuid))) {
                count++;
            }
        }
        return count;
    }

    public static boolean isOriginalKiller(Role role) {
        return role != null && role.canUseKiller();
    }

    public static boolean isOriginalCivilian(Role role) {
        return role != null && role.isInnocent();
    }

    public static boolean isUndercover(Role role) {
        return role != null && role.identifier().equals(Noellesroles.UNDERCOVER_ID);
    }

    public static boolean countsAsPublicKiller(Role role, Collection<Identifier> traits) {
        return isOriginalKiller(role) && !hasConscience(traits);
    }

    public static Boolean cohortOverride(
            Role viewerRole,
            Collection<Identifier> viewerTraits,
            Role targetRole,
            Collection<Identifier> targetTraits
    ) {
        if (hasConscience(viewerTraits)) {
            return Boolean.FALSE;
        }
        if (!isEffectiveKiller(viewerRole, viewerTraits)) {
            return null;
        }
        if (hasConscience(targetTraits)) {
            return Boolean.FALSE;
        }
        if (hasImpostor(targetTraits)) {
            return Boolean.TRUE;
        }
        return null;
    }

    public static boolean sharesBlackoutCooldown(
            Role purchaserRole,
            Collection<Identifier> purchaserTraits,
            Role targetRole,
            Collection<Identifier> targetTraits
    ) {
        if (!isOriginalKiller(purchaserRole) || !isOriginalKiller(targetRole)) {
            return false;
        }
        return hasConscience(purchaserTraits) == hasConscience(targetTraits);
    }

    public static int publicKillerCount(GameWorldComponent gameComponent, Collection<ServerPlayerEntity> players) {
        int count = 0;
        for (ServerPlayerEntity player : players) {
            if (countsAsPublicKiller(gameComponent.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds())) {
                count++;
            }
        }
        return count;
    }

    public static boolean isEffectiveKiller(PlayerEntity player, GameWorldComponent gameComponent) {
        if (player == null) {
            return false;
        }
        return isEffectiveKiller(gameComponent.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    public static boolean isEffectiveKiller(Role role, Collection<Identifier> traits) {
        if (hasImpostor(traits)) {
            return true;
        }
        if (hasConscience(traits)) {
            return false;
        }
        return isOriginalKiller(role);
    }

    public static boolean isRealOriginalKiller(PlayerEntity player, GameWorldComponent gameComponent) {
        return player != null && isOriginalKiller(gameComponent.getRole(player)) && !hasConscience(player);
    }

    public static boolean isEffectiveCivilian(PlayerEntity player, GameWorldComponent gameComponent) {
        if (player == null) {
            return false;
        }
        return isEffectiveCivilian(gameComponent.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    public static boolean isEffectiveCivilian(Role role, Collection<Identifier> traits) {
        if (hasConscience(traits)) {
            return true;
        }
        if (hasImpostor(traits)) {
            return false;
        }
        return isOriginalCivilian(role);
    }

    public static Role.MoodType effectiveMoodType(PlayerEntity player, Role role) {
        if (hasConscience(player)) {
            return Role.MoodType.REAL;
        }
        if (hasImpostor(player)) {
            return Role.MoodType.FAKE;
        }
        return role == null ? Role.MoodType.NONE : role.getMoodType();
    }

    public static Role.MoodType effectiveMoodType(Role role, Collection<Identifier> traits) {
        if (hasConscience(traits)) {
            return Role.MoodType.REAL;
        }
        if (hasImpostor(traits)) {
            return Role.MoodType.FAKE;
        }
        return role == null ? Role.MoodType.NONE : role.getMoodType();
    }

    public static int requiredExtraKillersForConscience(int intendedPublicKillerCount, int originalKillerRoleCount, int conscienceCount) {
        return Math.max(0, intendedPublicKillerCount + conscienceCount - originalKillerRoleCount);
    }

    public static boolean shouldRewardConscienceKill(Role victimRole, Collection<Identifier> victimTraits) {
        return victimRole != null && !isEffectiveCivilian(victimRole, victimTraits);
    }

    public static void handleAfterKill(ServerPlayerEntity victim, ServerPlayerEntity killer) {
        if (victim == null || killer == null || victim.getUuid().equals(killer.getUuid())) {
            return;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        Role victimRole = game.getRole(victim);
        Collection<Identifier> victimTraits = TraitPlayerComponent.KEY.get(victim).getActiveTraitIds();
        boolean victimIsEffectiveCivilian = isEffectiveCivilian(victimRole, victimTraits);
        if (hasConscience(killer)) {
            if (victimIsEffectiveCivilian && GameFunctions.isPlayerPlayingAndAlive(killer)) {
                GameFunctions.killPlayer(killer, true, null, GameConstants.DeathReasons.SHOT_INNOCENT, true);
            } else if (shouldRewardConscienceKill(victimRole, victimTraits)) {
                PlayerShopComponent.KEY.get(killer).addToBalance(GameConstants.MONEY_PER_KILL);
            }
        } else if (hasImpostor(killer) && victimIsEffectiveCivilian && ShopUtils.canAccessShop(killer)) {
            PlayerShopComponent.KEY.get(killer).addToBalance(TASK_MONEY_REWARD);
        }
    }

    private static CheckWinCondition.WinResult checkWin(
            ServerWorld world,
            GameWorldComponent gameComponent,
            GameFunctions.WinStatus currentStatus
    ) {
        List<ServerPlayerEntity> players = world.getPlayers();
        boolean realKillerAlive = false;
        boolean effectiveCivilianAlive = false;

        for (ServerPlayerEntity player : players) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player) || !gameComponent.hasAnyRole(player)) {
                continue;
            }
            if (isRealOriginalKiller(player, gameComponent)) {
                realKillerAlive = true;
            }
            if (isEffectiveCivilian(player, gameComponent)) {
                effectiveCivilianAlive = true;
            }
        }

        if (!realKillerAlive) {
            killUnsupportedImpostors(players);
            return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.PASSENGERS);
        }
        if (!effectiveCivilianAlive) {
            return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.KILLERS);
        }
        if (currentStatus == GameFunctions.WinStatus.PASSENGERS || currentStatus == GameFunctions.WinStatus.KILLERS) {
            return CheckWinCondition.WinResult.block();
        }
        return null;
    }

    private static void killUnsupportedImpostors(List<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            if (hasImpostor(player) && GameFunctions.isPlayerPlayingAndAlive(player)) {
                GameFunctions.killPlayer(player, true, null, SELF_REALIZATION, true);
            }
        }
    }
}
