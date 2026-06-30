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
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.spiritualist.SpiritPlayerComponent;
import org.ladysnake.cca.api.v3.component.ComponentKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Central effective-team rules for alignment-flipping traits.
 *  阵营翻转天赋的统一有效阵营规则入口。 */
public final class EffectiveTraitService {
    public static final int CONSCIENCE_COLOR = 0xFFDEF8;
    public static final int IMPOSTOR_COLOR = 0x7D0000;
    public static final int KILLER_INSTINCT_COLOR = 0x990000;
    public static final int IMPOSTOR_INSTINCT_COLOR = 0x0013FF;
    public static final int CIVILIAN_INSTINCT_COLOR = 0x4EDD35;
    public static final double CONSCIENCE_INSTINCT_RANGE_SQUARED = 100.0;
    public static final int TASK_MONEY_REWARD = 50;
    public static final Identifier SELF_REALIZATION = SparkTraits.id("self_realization");
    private static final Identifier SPARKWITCH_GRAND_WITCH_ID = Identifier.of("sparkwitch", "grand_witch");
    private static final Identifier SPARKWITCH_ACCOMPLICE_ID = Identifier.of("sparkwitch", "accomplice");
    private static final Identifier SPARKWITCH_MURDEROUS_WITCH_ID = Identifier.of("sparkwitch", "murderous_witch");
    private static final Identifier NOELLES_SHADOW_JESTER_ID = Identifier.of("noellesroles", "shadow_jester");
    private static final Map<UUID, Identifier> poisonSources = new HashMap<>();

    private EffectiveTraitService() {
    }

    public static void register() {
        CheckWinCondition.EVENT.register(EffectiveTraitService::checkWin);
        TaskComplete.EVENT.register((player, taskType) -> {
            GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
            if (shouldRewardTaskMoney(game.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds())) {
                PlayerShopComponent.KEY.get(player).addToBalance(TASK_MONEY_REWARD);
            }
        });
        ShouldPunishGunShooter.EVENT.register((shooter, victim) -> {
            GameWorldComponent game = GameWorldComponent.KEY.get(shooter.getWorld());
            if (shouldCancelInnocentShotPunishment(
                    game.getRole(shooter),
                    TraitPlayerComponent.KEY.get(shooter).getActiveTraitIds(),
                    game.getRole(victim),
                    TraitPlayerComponent.KEY.get(victim).getActiveTraitIds()
            )) {
                return ShouldPunishGunShooter.PunishResult.cancel();
            }
            return null;
        });
        ShouldShowCohort.EVENT.register((viewer, target) -> {
            GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
            Collection<Identifier> viewerTraits = TraitPlayerComponent.KEY.get(viewer).getActiveTraitIds();
            Boolean morphlingOverride = conscienceMorphlingCohortOverride(viewer, target, game, viewerTraits);
            if (morphlingOverride != null) {
                return morphlingOverride
                        ? ShouldShowCohort.CohortResult.show(ShouldShowCohort.CohortResult.PRIORITY_HIGH)
                        : ShouldShowCohort.CohortResult.hide();
            }
            Boolean disguiseTargetOverride = conscienceMorphlingDisguiseTargetCohortOverride(viewer, target, game, viewerTraits);
            if (disguiseTargetOverride != null) {
                return disguiseTargetOverride
                        ? ShouldShowCohort.CohortResult.show(ShouldShowCohort.CohortResult.PRIORITY_HIGH)
                        : ShouldShowCohort.CohortResult.hide();
            }
            Boolean override = cohortOverride(
                    game.getRole(viewer),
                    viewerTraits,
                    game.getRole(target),
                    publicEffectiveTraitIds(target)
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
        return shouldHideFromKillerInstinct(traits.isLastStandPending(), traits.isKillerInstinctHidden(), isSpiritProjecting(player));
    }

    public static boolean shouldHideFromKillerInstinct(boolean lastStandPending, boolean killerInstinctHidden) {
        return shouldHideFromKillerInstinct(lastStandPending, killerInstinctHidden, false);
    }

    public static boolean shouldHideFromKillerInstinct(boolean lastStandPending, boolean killerInstinctHidden, boolean spiritProjecting) {
        return lastStandPending || killerInstinctHidden || spiritProjecting;
    }

    /** Keeps Phantom invisibility from leaking through SparkTraits instinct overrides.
     *  防止幽灵隐身被 SparkTraits 的本能透视覆盖逻辑暴露。 */
    public static boolean shouldSkipInvisibleTargetFromEffectiveInstinct(
            boolean targetInvisible,
            boolean targetHasConscienceOverlay,
            boolean lastStandPending,
            boolean killerInstinctHidden
    ) {
        return targetInvisible
                && targetHasConscienceOverlay
                && !lastStandPending
                && !killerInstinctHidden;
    }

    /** Keeps Impostor instinct from seeing Survival Master through walls while preserving visible highlights.
     *  防止内鬼本能穿墙透视生存大师，同时保留视野内的正常描边。 */
    public static boolean shouldSkipSurvivalMasterForImpostorInstinct(
            Role targetRole,
            boolean viewerHasImpostor,
            boolean targetVisibleToViewer
    ) {
        return targetRole != null
                && targetRole.identifier().equals(Noellesroles.SURVIVAL_MASTER_ID)
                && viewerHasImpostor
                && !targetVisibleToViewer;
    }

    /** Mirrors NoellesRoles' Jester Moment skip: other players cannot highlight the active Jester, except Demon Hunter.
     *  同步 NoellesRoles 的小丑时刻规则：除猎魔人外，其他玩家不能高亮小丑时刻中的小丑。 */
    public static boolean shouldSkipJesterMomentHighlight(
            Role viewerRole,
            Role targetRole,
            boolean targetInJesterPsychoMode,
            boolean viewerIsTarget
    ) {
        return targetRole != null
                && targetRole.identifier().equals(Noellesroles.JESTER_ID)
                && targetInJesterPsychoMode
                && !viewerIsTarget
                && (viewerRole == null || !viewerRole.identifier().equals(Noellesroles.DEMON_HUNTER_ID));
    }

    public static boolean isSpiritProjecting(PlayerEntity player) {
        return player != null && SpiritPlayerComponent.KEY.get(player).isProjecting();
    }

    public static boolean shouldConscienceInstinctHighlightTarget(
            boolean instinctEnabled,
            boolean targetPlayingAndAlive,
            boolean targetSpectatingOrCreative,
            double targetDistanceSquared,
            boolean lastStandPending,
            boolean killerInstinctHidden,
            boolean spiritProjecting
    ) {
        return shouldConscienceInstinctHighlightTarget(
                instinctEnabled,
                targetPlayingAndAlive,
                targetSpectatingOrCreative,
                targetDistanceSquared,
                lastStandPending,
                killerInstinctHidden,
                spiritProjecting,
                false
        );
    }

    public static boolean shouldConscienceInstinctHighlightTarget(
            boolean instinctEnabled,
            boolean targetPlayingAndAlive,
            boolean targetSpectatingOrCreative,
            double targetDistanceSquared,
            boolean lastStandPending,
            boolean killerInstinctHidden,
            boolean spiritProjecting,
            boolean ignoreRangeLimit
    ) {
        return instinctEnabled
                && !targetSpectatingOrCreative
                && !spiritProjecting
                && (ignoreRangeLimit || targetDistanceSquared <= CONSCIENCE_INSTINCT_RANGE_SQUARED)
                && (targetPlayingAndAlive || lastStandPending || killerInstinctHidden);
    }

    /** Conscience Morphling copies only the disguise target's effective alignment.
     *  善良变形者只复制伪装目标的有效阵营，不复制临时高亮或隐藏状态。 */
    public static boolean shouldHideConscienceMorphlingFromInstinct(
            boolean targetHasConscience,
            boolean targetIsMorphling,
            boolean targetCorpseMode
    ) {
        return targetHasConscience && targetIsMorphling && targetCorpseMode;
    }

    public static boolean shouldUseConscienceMorphlingDisguiseInstinct(
            boolean targetHasConscience,
            boolean targetIsMorphling,
            boolean targetCorpseMode,
            boolean targetMorphing,
            boolean hasDisguise
    ) {
        return targetHasConscience && targetIsMorphling && !targetCorpseMode && targetMorphing && hasDisguise;
    }

    public static int effectiveKillerInstinctColor(Role targetRole, Collection<Identifier> targetTraits) {
        return effectiveKillerInstinctColor(
                appearsAsKillerToKillerInstinct(targetRole, isOriginalKiller(targetRole)),
                hasConscience(targetTraits),
                hasImpostor(targetTraits)
        );
    }

    public static int effectiveKillerInstinctColor(
            Role targetRole,
            boolean targetHasConscience,
            boolean targetHasImpostor
    ) {
        return effectiveKillerInstinctColor(
                appearsAsKillerToKillerInstinct(targetRole, isOriginalKiller(targetRole)),
                targetHasConscience,
                targetHasImpostor
        );
    }

    public static int effectiveKillerInstinctColor(
            boolean targetAppearsAsKiller,
            boolean targetHasConscience,
            boolean targetHasImpostor
    ) {
        if (targetHasImpostor) {
            return IMPOSTOR_INSTINCT_COLOR;
        }
        if (targetHasConscience) {
            return CIVILIAN_INSTINCT_COLOR;
        }
        return targetAppearsAsKiller ? KILLER_INSTINCT_COLOR : CIVILIAN_INSTINCT_COLOR;
    }

    /** Mirrors NoellesRoles' Undercover deception for SparkTraits' Impostor instinct.
     *  为 SparkTraits 的内鬼本能同步 NoellesRoles 卧底伪装成杀手同伙的规则。 */
    public static boolean appearsAsKillerToKillerInstinct(Role targetRole, boolean targetCanUseKillerFeatures) {
        return targetCanUseKillerFeatures || isUndercover(targetRole);
    }

    public static Boolean conscienceMorphlingCohortOverride(
            Role viewerRole,
            Collection<Identifier> viewerTraits,
            boolean targetHasConscience,
            boolean targetIsMorphling,
            boolean targetCorpseMode,
            boolean targetMorphing,
            Role disguiseRole,
            Collection<Identifier> disguiseTraits
    ) {
        return conscienceMorphlingCohortOverride(
                viewerRole,
                viewerTraits,
                targetHasConscience,
                targetIsMorphling,
                targetCorpseMode,
                targetMorphing,
                disguiseRole != null,
                isOriginalKiller(disguiseRole),
                hasConscience(disguiseTraits),
                hasImpostor(disguiseTraits)
        );
    }

    public static Boolean conscienceMorphlingCohortOverride(
            Role viewerRole,
            Collection<Identifier> viewerTraits,
            boolean targetHasConscience,
            boolean targetIsMorphling,
            boolean targetCorpseMode,
            boolean targetMorphing,
            Role disguiseRole,
            boolean disguiseHasConscience,
            boolean disguiseHasImpostor
    ) {
        return conscienceMorphlingCohortOverride(
                viewerRole,
                viewerTraits,
                targetHasConscience,
                targetIsMorphling,
                targetCorpseMode,
                targetMorphing,
                disguiseRole != null,
                isOriginalKiller(disguiseRole),
                disguiseHasConscience,
                disguiseHasImpostor
        );
    }

    public static Boolean conscienceMorphlingCohortOverride(
            Role viewerRole,
            Collection<Identifier> viewerTraits,
            boolean targetHasConscience,
            boolean targetIsMorphling,
            boolean targetCorpseMode,
            boolean targetMorphing,
            boolean hasDisguise,
            boolean disguiseCanUseKillerFeatures,
            boolean disguiseHasConscience,
            boolean disguiseHasImpostor
    ) {
        if (shouldHideConscienceMorphlingFromInstinct(targetHasConscience, targetIsMorphling, targetCorpseMode)) {
            return Boolean.FALSE;
        }
        if (!shouldUseConscienceMorphlingDisguiseInstinct(
                targetHasConscience,
                targetIsMorphling,
                targetCorpseMode,
                targetMorphing,
                hasDisguise
        )) {
            return null;
        }
        if (!isEffectiveKiller(viewerRole, viewerTraits)) {
            return null;
        }
        return disguiseHasImpostor || (!disguiseHasConscience && disguiseCanUseKillerFeatures)
                ? Boolean.TRUE
                : Boolean.FALSE;
    }

    private static Boolean conscienceMorphlingCohortOverride(
            PlayerEntity viewer,
            PlayerEntity target,
            GameWorldComponent game,
            Collection<Identifier> viewerTraits
    ) {
        MorphlingPlayerComponent morphling = MorphlingPlayerComponent.KEY.get(target);
        UUID disguise = morphling.disguise;
        Role disguiseRole = disguise == null ? null : game.getRole(disguise);
        PlayerEntity disguisePlayer = disguise == null ? null : target.getWorld().getPlayerByUuid(disguise);
        // Clients can know the disguise target's killer capability even when its full Role lookup is missing.
        // 客户端可能拿得到伪装目标是否可用杀手功能，但拿不到完整 Role；同伙提示要跟随这个公开状态。
        boolean disguiseCanUseKillerFeatures = disguisePlayer != null
                ? game.canUseKillerFeatures(disguisePlayer)
                : isOriginalKiller(disguiseRole);

        return conscienceMorphlingCohortOverride(
                game.getRole(viewer),
                viewerTraits,
                isConscienceVisibleToInstinct(target),
                game.isRole(target, Noellesroles.MORPHLING),
                morphling.corpseMode,
                morphling.getMorphTicks() > 0,
                disguise != null,
                disguiseCanUseKillerFeatures,
                disguisePlayer != null && isConscienceVisibleToInstinct(disguisePlayer),
                disguisePlayer != null && isImpostorVisibleToInstinct(disguisePlayer)
        );
    }

    /** Keeps the real disguise target visible as a killer cohort while a Conscience Morphling copies them.
     *  当善良变形者伪装成某个真实杀手时，那个真实目标杀手仍应对其他杀手显示“杀手同伙”。 */
    public static Boolean conscienceMorphlingDisguiseTargetCohortOverride(
            Role viewerRole,
            Collection<Identifier> viewerTraits,
            Role targetRole,
            Collection<Identifier> targetTraits,
            boolean targetIsDisguiseForConscienceMorphling
    ) {
        if (!targetIsDisguiseForConscienceMorphling) {
            return null;
        }
        if (!isEffectiveKiller(viewerRole, viewerTraits)) {
            return null;
        }
        return isEffectiveKiller(targetRole, targetTraits) ? Boolean.TRUE : null;
    }

    private static Boolean conscienceMorphlingDisguiseTargetCohortOverride(
            PlayerEntity viewer,
            PlayerEntity target,
            GameWorldComponent game,
            Collection<Identifier> viewerTraits
    ) {
        return conscienceMorphlingDisguiseTargetCohortOverride(
                game.getRole(viewer),
                viewerTraits,
                game.getRole(target),
                publicEffectiveTraitIds(target),
                isDisguiseTargetForConscienceMorphling(target, game)
        );
    }

    private static boolean isDisguiseTargetForConscienceMorphling(PlayerEntity target, GameWorldComponent game) {
        UUID targetUuid = target.getUuid();
        for (PlayerEntity player : target.getWorld().getPlayers()) {
            if (player.getUuid().equals(targetUuid)) {
                continue;
            }
            MorphlingPlayerComponent morphling = MorphlingPlayerComponent.KEY.get(player);
            if (targetUuid.equals(morphling.disguise)
                    && isConscienceVisibleToInstinct(player)
                    && game.isRole(player, Noellesroles.MORPHLING)
                    && !morphling.corpseMode
                    && morphling.getMorphTicks() > 0) {
                return true;
            }
        }
        return false;
    }

    /** Uses client-synced public alignment flags without exposing hidden trait text.
     *  使用客户端已同步的公开阵营标记，不暴露隐藏天赋文本。 */
    private static Collection<Identifier> publicEffectiveTraitIds(PlayerEntity player) {
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
        return canSelectConscience(
                role,
                originalKillerCount(gameComponent),
                isRoleEnabled(gameComponent, role),
                selectedTraits
        );
    }

    public static boolean canSelectConscience(Role role, int originalKillerCount, Collection<Identifier> selectedTraits) {
        return canSelectConscience(role, originalKillerCount, true, selectedTraits);
    }

    public static boolean canSelectConscience(
            Role role,
            int originalKillerCount,
            boolean roleEnabled,
            Collection<Identifier> selectedTraits
    ) {
        return originalKillerCount >= 2
                && roleEnabled
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
                && !isImpostorBlockedRole(role)
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

    private static boolean isRoleEnabled(GameWorldComponent gameComponent, Role role) {
        return gameComponent != null && role != null && gameComponent.isRoleEnabled(role);
    }

    public static boolean isOriginalCivilian(Role role) {
        return role != null && role.isInnocent();
    }

    public static boolean isUndercover(Role role) {
        return role != null && role.identifier().equals(Noellesroles.UNDERCOVER_ID);
    }

    /** Keeps high-agency innocent roles from being converted into Impostor.
     *  防止强机制无辜者角色被转换成内鬼。 */
    private static boolean isImpostorBlockedRole(Role role) {
        return role != null
                && (role == WatheRoles.VIGILANTE
                || role == WatheRoles.VETERAN
                || role.identifier().equals(Noellesroles.SURVIVAL_MASTER_ID));
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
        if (hasImpostor(viewerTraits) && isUndercover(targetRole)) {
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

    /** Resolves round-end winner membership through effective alignment.
     *  通过有效阵营判断回合结束时玩家是否属于胜利方。 */
    public static boolean didEffectiveTeamWin(
            GameFunctions.WinStatus winStatus,
            Role role,
            Collection<Identifier> traits
    ) {
        return switch (winStatus) {
            case KILLERS -> isEffectiveKiller(role, traits);
            case PASSENGERS, TIME -> isEffectiveCivilian(role, traits);
            default -> false;
        };
    }

    /** Defers ordinary team wins so downstream blockers and custom faction wins can resolve their hooks.
     *  延后普通队伍胜利，让后续阻塞角色与自定义阵营胜利用自己的钩子结算。 */
    public static boolean shouldDeferTeamWinForBlockingNeutral(
            GameFunctions.WinStatus proposedWinStatus,
            Collection<Role> livingRoles
    ) {
        return shouldDeferTeamWinForBlockingNeutral(proposedWinStatus, livingRoles, false);
    }

    public static boolean shouldDeferTeamWinForBlockingNeutral(
            GameFunctions.WinStatus proposedWinStatus,
            Collection<Role> livingRoles,
            boolean noellesJesterBlocksTeamWin
    ) {
        if (proposedWinStatus != GameFunctions.WinStatus.KILLERS
                && proposedWinStatus != GameFunctions.WinStatus.PASSENGERS) {
            return false;
        }
        if (noellesJesterBlocksTeamWin) {
            return true;
        }
        if (livingRoles == null) {
            return false;
        }
        for (Role role : livingRoles) {
            if (isBlockingTeamWinNeutral(role)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlockingTeamWinNeutral(Role role) {
        return role != null
                && (SPARKWITCH_GRAND_WITCH_ID.equals(role.identifier())
                || SPARKWITCH_ACCOMPLICE_ID.equals(role.identifier())
                || SPARKWITCH_MURDEROUS_WITCH_ID.equals(role.identifier())
                || Noellesroles.CORRUPT_COP_ID.equals(role.identifier())
                || Noellesroles.TAOTIE_ID.equals(role.identifier()));
    }

    /** Mirrors NoellesRoles' Jester blocker without making ordinary Jester block team wins.
     *  同步 NoellesRoles 的小丑阻止规则，但普通小丑不会阻止队伍胜利。 */
    public static boolean shouldDeferTeamWinForNoellesJester(
            Role role,
            boolean inPsychoMode,
            boolean transitioning
    ) {
        return role != null
                && Noellesroles.JESTER_ID.equals(role.identifier())
                && (inPsychoMode || transitioning);
    }

    /** Defers ordinary team wins while NoellesRoles' bound Shadow Jesters need showdown routing.
     *  当 NoellesRoles 命运绑定双影小丑需要进入/维持谢幕时，延后普通队伍胜利。 */
    public static boolean shouldDeferTeamWinForNoellesShadowJester(
            Role role,
            boolean allied,
            boolean showdownActive
    ) {
        return role != null
                && NOELLES_SHADOW_JESTER_ID.equals(role.identifier())
                && (allied || showdownActive);
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

    /**
     * Grants SparkTraits task money only when the base role does not already pay for tasks.
     * 仅在原职业没有自带任务金币时，由 SparkTraits 给阵营翻转玩家补发任务金币。
     */
    public static boolean shouldRewardTaskMoney(Role role, Collection<Identifier> traits) {
        return traits != null && (hasConscience(traits) || hasImpostor(traits)) && !hasNativeTaskMoneyReward(role);
    }

    public static boolean hasNativeTaskMoneyReward(Role role) {
        return role != null
                && (role.equals(Noellesroles.BARTENDER)
                || role.equals(Noellesroles.RECALLER)
                || role.equals(Noellesroles.TIMEKEEPER)
                || role.equals(Noellesroles.REPORTER)
                || role.equals(Noellesroles.WAITER));
    }

    /** Lets Party Animal reward any non-self target; NoellesRoles keeps self-buzz and repeat-level gates.
     *  允许派对狂对任意非自己目标发放变声奖励；自变声和重复等级限制仍由 NoellesRoles 原逻辑处理。 */
    public static boolean shouldBlockPartyAnimalTargetReward(Role targetRole, Collection<Identifier> targetTraits) {
        return false;
    }

    public static boolean shouldRewardConscienceKill(Role victimRole, Collection<Identifier> victimTraits) {
        return victimRole != null && !isEffectiveCivilian(victimRole, victimTraits);
    }

    /** Rewards Impostors for killing public non-killers, including neutral roles.
     *  内鬼击杀公开非杀手时获得击杀奖励，包含好人与中立角色。 */
    public static boolean shouldRewardImpostorKill(Role victimRole, Collection<Identifier> victimTraits) {
        return victimRole != null
                && victimRole != WatheRoles.NO_ROLE
                && !isEffectiveKiller(victimRole, victimTraits);
    }

    public static int impostorKillReward(Role victimRole, Collection<Identifier> victimTraits, boolean canAccessShop) {
        return canAccessShop && shouldRewardImpostorKill(victimRole, victimTraits) ? GameConstants.MONEY_PER_KILL : 0;
    }

    /**
     * Keeps NoellesRoles Jester Moment tied to unflipped original innocents.
     * 让 NoellesRoles 的小丑时刻只由未被内鬼翻阵营的原始好人触发。
     */
    public static boolean shouldTriggerJesterMoment(Role killerRole, Collection<Identifier> killerTraits) {
        return isOriginalCivilian(killerRole) && !hasImpostor(killerTraits);
    }

    /** Treat gun victims by their effective alignment for innocent-shot penalties.
     *  枪击惩罚按目标的有效阵营判定，确保善良杀手被当作好人。 */
    public static boolean shouldTreatGunVictimAsInnocent(Role victimRole, Collection<Identifier> victimTraits) {
        return isEffectiveCivilian(victimRole, victimTraits);
    }

    /** Cancels Wathe's innocent-shot punishment only for Impostor shots.
     *  只在内鬼开枪时取消 Wathe 的好人枪击惩罚，普通杀手保留原版消耗枪械逻辑。 */
    public static boolean shouldCancelInnocentShotPunishment(
            Role shooterRole,
            Collection<Identifier> shooterTraits,
            Role victimRole,
            Collection<Identifier> victimTraits
    ) {
        return isEffectiveKiller(shooterRole, shooterTraits)
                && hasImpostor(shooterTraits)
                && shouldTreatGunVictimAsInnocent(victimRole, victimTraits);
    }

    public static boolean shouldPunishConscienceKill(boolean victimIsEffectiveCivilian, Identifier deathReason) {
        return shouldPunishConscienceKill(victimIsEffectiveCivilian, deathReason, null);
    }

    public static boolean shouldPunishConscienceKill(boolean victimIsEffectiveCivilian, Identifier deathReason, Identifier poisonSource) {
        return victimIsEffectiveCivilian && !isAreaDamageDeathReason(deathReason, poisonSource);
    }

    public static void rememberPoisonSource(PlayerEntity player, Identifier poisonSource) {
        if (player != null && poisonSource != null) {
            poisonSources.put(player.getUuid(), poisonSource);
        }
    }

    private static boolean isAreaDamageDeathReason(Identifier deathReason, Identifier poisonSource) {
        return GameConstants.DeathReasons.GRENADE.equals(deathReason)
                || (GameConstants.DeathReasons.POISON.equals(deathReason)
                && Noellesroles.POISON_SOURCE_GAS_BOMB.equals(poisonSource));
    }

    public static void handleAfterKill(ServerPlayerEntity victim, ServerPlayerEntity killer, Identifier deathReason) {
        if (victim == null || killer == null || victim.getUuid().equals(killer.getUuid())) {
            return;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        Role victimRole = game.getRole(victim);
        Collection<Identifier> victimTraits = TraitPlayerComponent.KEY.get(victim).getActiveTraitIds();
        boolean victimIsEffectiveCivilian = isEffectiveCivilian(victimRole, victimTraits);
        Identifier poisonSource = poisonSources.remove(victim.getUuid());
        if (hasConscience(killer)) {
            if (shouldPunishConscienceKill(victimIsEffectiveCivilian, deathReason, poisonSource) && GameFunctions.isPlayerPlayingAndAlive(killer)) {
                GameFunctions.killPlayer(killer, true, null, GameConstants.DeathReasons.SHOT_INNOCENT, true);
            } else if (shouldRewardConscienceKill(victimRole, victimTraits)) {
                int reward = ConscienceSerialKillerService.rewardForConscienceKill(killer, victim, true);
                if (reward > 0) {
                    PlayerShopComponent.KEY.get(killer).addToBalance(reward);
                }
            }
        } else if (hasImpostor(killer)) {
            int reward = impostorKillReward(victimRole, victimTraits, ShopUtils.canAccessShop(killer));
            if (reward > 0) {
                PlayerShopComponent.KEY.get(killer).addToBalance(reward);
            }
        }
    }

    private static CheckWinCondition.WinResult checkWin(
            ServerWorld world,
            GameWorldComponent gameComponent,
            GameFunctions.WinStatus currentStatus
    ) {
        List<ServerPlayerEntity> players = world.getPlayers();
        List<Role> livingRoles = new ArrayList<>();
        boolean realKillerAlive = false;
        boolean effectiveCivilianAlive = false;
        boolean noellesJesterBlocksTeamWin = false;

        for (ServerPlayerEntity player : players) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player) || !gameComponent.hasAnyRole(player)) {
                continue;
            }
            Role role = gameComponent.getRole(player);
            Collection<Identifier> traits = TraitPlayerComponent.KEY.get(player).getActiveTraitIds();
            livingRoles.add(role);
            if (role != null && Noellesroles.JESTER_ID.equals(role.identifier())) {
                JesterPlayerComponent jester = JesterPlayerComponent.KEY.get(player);
                if (shouldDeferTeamWinForNoellesJester(role, jester.inPsychoMode, jester.isTransitioning())) {
                    noellesJesterBlocksTeamWin = true;
                }
            }
            if (shouldDeferTeamWinForNoellesShadowJester(player, role)) {
                noellesJesterBlocksTeamWin = true;
            }
            if (isOriginalKiller(role) && !hasConscience(traits)) {
                realKillerAlive = true;
            }
            if (isEffectiveCivilian(role, traits)) {
                effectiveCivilianAlive = true;
            }
        }

        if (!realKillerAlive) {
            killUnsupportedImpostors(players, gameComponent, false);
            if (shouldDeferTeamWinForBlockingNeutral(GameFunctions.WinStatus.PASSENGERS, livingRoles, noellesJesterBlocksTeamWin)) {
                return null;
            }
            return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.PASSENGERS);
        }
        if (!effectiveCivilianAlive) {
            if (shouldDeferTeamWinForBlockingNeutral(GameFunctions.WinStatus.KILLERS, livingRoles, noellesJesterBlocksTeamWin)) {
                return null;
            }
            return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.KILLERS);
        }
        if (currentStatus == GameFunctions.WinStatus.PASSENGERS || currentStatus == GameFunctions.WinStatus.KILLERS) {
            return CheckWinCondition.WinResult.block();
        }
        return null;
    }

    private static boolean shouldDeferTeamWinForNoellesShadowJester(PlayerEntity player, Role role) {
        return shouldDeferTeamWinForNoellesShadowJester(
                role,
                noellesShadowJesterComponentFlag(player, "isAllied"),
                noellesShadowJesterComponentFlag(player, "isShowdownActive")
        );
    }

    private static boolean noellesShadowJesterComponentFlag(PlayerEntity player, String methodName) {
        if (player == null) {
            return false;
        }
        try {
            Class<?> componentClass = Class.forName("org.agmas.noellesroles.shadowjester.ShadowJesterPlayerComponent");
            Object keyObject = componentClass.getField("KEY").get(null);
            if (!(keyObject instanceof ComponentKey<?> key)) {
                return false;
            }
            Object component = key.get(player);
            Object result = componentClass.getMethod(methodName).invoke(component);
            return result instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException exception) {
            return false;
        }
    }

    /** Kills Impostors once their real killer-side support is gone, even if neutral blockers keep the round active.
     *  当真实杀手支持消失时清理内鬼，即使中立阻塞者让回合继续。 */
    public static void killUnsupportedImpostorsIfNoRealKillers(ServerWorld world, GameWorldComponent gameComponent) {
        if (world == null
                || gameComponent == null
                || gameComponent.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE) {
            return;
        }
        List<ServerPlayerEntity> players = world.getPlayers();
        boolean realKillerAlive = false;
        for (ServerPlayerEntity player : players) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player) || !gameComponent.hasAnyRole(player)) {
                continue;
            }
            if (isRealOriginalKiller(player, gameComponent)) {
                realKillerAlive = true;
                break;
            }
        }
        killUnsupportedImpostors(players, gameComponent, realKillerAlive);
    }

    public static boolean shouldSelfRealizeUnsupportedImpostor(
            Collection<Identifier> traits,
            boolean playerAlive,
            boolean playerInRound,
            boolean realOriginalKillerAlive
    ) {
        return playerAlive && playerInRound && !realOriginalKillerAlive && traits != null && hasImpostor(traits);
    }

    private static void killUnsupportedImpostors(
            List<ServerPlayerEntity> players,
            GameWorldComponent gameComponent,
            boolean realOriginalKillerAlive
    ) {
        for (ServerPlayerEntity player : players) {
            if (shouldSelfRealizeUnsupportedImpostor(
                    TraitPlayerComponent.KEY.get(player).getActiveTraitIds(),
                    GameFunctions.isPlayerPlayingAndAlive(player),
                    gameComponent.hasAnyRole(player),
                    realOriginalKillerAlive
            )) {
                GameFunctions.killPlayer(player, true, null, SELF_REALIZATION, true);
            }
        }
    }
}
