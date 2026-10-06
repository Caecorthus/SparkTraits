package dev.caecorthus.sparktraits.impl.effective;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.compat.SparkStrengthCoronerBridge;
import dev.caecorthus.sparktraits.compat.SparkWitchWraithBridge;
import dev.caecorthus.sparktraits.impl.effective.alignment.EffectiveAlignment;
import dev.caecorthus.sparktraits.impl.effective.death.EffectiveDeathConsequenceRules;
import dev.caecorthus.sparktraits.impl.effective.economy.EffectiveEconomyRules;
import dev.caecorthus.sparktraits.impl.effective.gun.EffectiveGunRules;
import dev.caecorthus.sparktraits.mixin.GameWorldComponentAccessor;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceSerialKillerService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.PoliceRoleCategory;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.BlackoutEffect;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;
import dev.doctor4t.wathe.api.event.ShouldShowCohort;
import dev.doctor4t.wathe.api.event.TaskComplete;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopUtils;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    // SparkWitch special accomplices block team wins exactly like the Accomplice.
    // SparkWitch 特殊共犯与共犯一样阻止队伍胜利。
    private static final Identifier SPARKWITCH_ABYSS_LISTENER_ID = Identifier.of("sparkwitch", "abyss_listener");
    private static final Identifier SPARKWITCH_POTION_GUNNER_ID = Identifier.of("sparkwitch", "potion_gunner");
    private static final Identifier SPARKWITCH_RIFTWALKER_ID = Identifier.of("sparkwitch", "riftwalker");
    // SparkWitch's Bewitched (魔化使) is an accomplice before promotion.
    // SparkWitch 魔化使是晋升前的共犯。
    private static final Identifier SPARKWITCH_BEWITCHED_ID = Identifier.of("sparkwitch", "bewitched");
    private static final Identifier SPARKWITCH_MURDEROUS_WITCH_ID = Identifier.of("sparkwitch", "murderous_witch");
    // SparkWitch's Insider shares the Corrupt Cop's team and keeps its win rule after the Corrupt Cop dies.
    // SparkWitch 内应与黑警同属一个阵营，黑警死后由内应继续承担该阵营的胜利规则。
    private static final Identifier SPARKWITCH_INSIDER_ID = Identifier.of("sparkwitch", "insider");
    private static final Identifier SPARKWITCH_PIG_GOD_ID = Identifier.of("sparkwitch", "pig_god");
    private static final Identifier SPARKWITCH_SAINT_ID = Identifier.of("sparkwitch", "saint");
    // SparkWitch's Blind sees only through sounds, so Impostor instinct and night vision would void the role.
    // SparkWitch 盲人只能靠声音感知，内鬼的本能透视与夜视会让该职业失效。
    private static final Identifier SPARKWITCH_BLIND_ID = Identifier.of("sparkwitch", "blind");
    private static final Identifier SPARKWITCH_BELL_RINGER_ID = Identifier.of("sparkwitch", "bell_ringer");
    private static final Identifier NOELLES_SHADOW_JESTER_ID = Identifier.of("noellesroles", "shadow_jester");

    private EffectiveTraitService() {
    }

    public static void register() {
        CheckWinCondition.EVENT.register(EffectiveTraitService::checkWin);
        BlackoutEffect.BEFORE.register(EffectiveTraitService::beforeBlackoutEffect);
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
            if (SparkTraitsServerConnection.isUnconfirmedClientEntity(viewer)) {
                return null;
            }
            GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
            // A dead Conscience killer must not see the killer team as cohorts once death clears its traits.
            // 死亡会清空天赋，善良杀手死后仍不能把杀手队伍看成同伙。
            Collection<Identifier> viewerTraits = effectiveTraitIds(viewer, game);
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
                    publicEffectiveTraitIds(target, game),
                    SparkStrengthCoronerBridge.appearsAsKillerCohort(target)
            );
            if (override == null) {
                return null;
            }
            return override
                    ? ShouldShowCohort.CohortResult.show(ShouldShowCohort.CohortResult.PRIORITY_HIGH)
                    : ShouldShowCohort.CohortResult.hide();
        });
    }

    private static BlackoutEffect.BlackoutResult beforeBlackoutEffect(ServerPlayerEntity player, int durationTicks) {
        Collection<Identifier> traits = TraitPlayerComponent.KEY.get(player).getActiveTraitIds();
        if (!shouldApplyImpostorBlackoutImmunity(traits)) {
            return null;
        }

        // Match Wathe's killer blackout branch without granting other killer features.
        // 对齐 wathe 杀手熄灯分支，但不授予其他杀手功能。
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                durationTicks,
                0,
                false,
                false,
                true
        ));
        return BlackoutEffect.BlackoutResult.cancel();
    }

    public static boolean hasConscience(PlayerEntity player) {
        return player != null && hasConscience(TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    public static boolean hasImpostor(PlayerEntity player) {
        return player != null && hasImpostor(TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    /** Keeps effective alignment stable after death listeners clear a dead player's active traits.
     *  死亡监听清空当前天赋后，回退到死亡快照以保持有效阵营判定稳定。 */
    public static Collection<Identifier> effectiveTraitIds(PlayerEntity player, GameWorldComponent game) {
        return effectiveTraitIds(
                TraitPlayerComponent.KEY.get(player).getActiveTraitIds(),
                game.isPlayerDead(player.getUuid()),
                TraitWorldComponent.KEY.get(player.getWorld()).getDeathTraitSnapshot(player.getUuid())
        );
    }

    public static Collection<Identifier> effectiveTraitIds(
            Collection<Identifier> activeTraits,
            boolean dead,
            Collection<Identifier> deathTraits
    ) {
        return activeTraits.isEmpty() && dead ? deathTraits : activeTraits;
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
        // Spirit projection leaves a defenseless body; it must stay instinct-visible, so projection is not an input here.
        // 灵魂出窍留下的肉身毫无防备，必须保持可被本能透视，因此出窍状态不参与此处判断。
        return shouldHideFromKillerInstinct(traits.isLastStandPending(), traits.isKillerInstinctHidden());
    }

    public static boolean shouldHideFromKillerInstinct(boolean lastStandPending, boolean killerInstinctHidden) {
        return shouldHideFromKillerInstinct(lastStandPending, killerInstinctHidden, false);
    }

    public static boolean shouldHideFromKillerInstinct(boolean lastStandPending, boolean killerInstinctHidden, boolean spiritProjecting) {
        return lastStandPending || killerInstinctHidden || spiritProjecting;
    }

    /** Final Moment overrides every SparkTraits-owned instinct suppression, matching Wathe's highlight priority.
     *  终局时刻覆盖所有 SparkTraits 本能屏蔽，与 Wathe 的高亮优先级保持一致。 */
    public static boolean shouldHideFromInstinct(
            boolean finalMomentActive,
            boolean lastStandPending,
            boolean killerInstinctHidden,
            boolean spiritProjecting,
            boolean goingDarkSuppressed
    ) {
        return !finalMomentActive
                && (goingDarkSuppressed || shouldHideFromKillerInstinct(
                        lastStandPending,
                        killerInstinctHidden,
                        spiritProjecting
                ));
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

    /** No longer feeds any instinct rule: the projecting body stays instinct-visible. Kept with field 18's relay until an approved cleanup.
     *  已不再参与任何本能规则：出窍本体保持可被透视。在获批清理前与第 18 号同步字段的转发一同保留。 */
    public static boolean isSpiritProjecting(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        if (player.getWorld().isClient) {
            return TraitPlayerComponent.KEY.maybeGet(player)
                    .map(TraitPlayerComponent::isSpiritProjectionInstinctHidden)
                    .orElse(false);
        }
        return SpiritPlayerComponent.KEY.maybeGet(player)
                .map(SpiritPlayerComponent::isProjecting)
                .orElse(false);
    }

    public static boolean shouldDelegateConscienceInstinctToNative(PlayerEntity player) {
        return shouldDelegateConscienceInstinctToNative(SparkWitchWraithBridge.isWraithActive(player));
    }

    public static boolean shouldDelegateConscienceInstinctToNative(boolean activeWraith) {
        return activeWraith;
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
                false,
                targetDistanceSquared,
                lastStandPending,
                killerInstinctHidden,
                spiritProjecting,
                false
        );
    }

    /** Invisible targets (Phantom, Phantom backpack, Last Escape) never show, matching NoellesRoles' invisible skip.
     *  隐身目标（幽灵、幽灵背包隐身、最后逃亡）从不显示，与 NoellesRoles 跳过隐身目标的规则一致。 */
    public static boolean shouldConscienceInstinctHighlightTarget(
            boolean instinctEnabled,
            boolean targetPlayingAndAlive,
            boolean targetSpectatingOrCreative,
            boolean targetInvisible,
            double targetDistanceSquared,
            boolean lastStandPending,
            boolean killerInstinctHidden,
            boolean spiritProjecting,
            boolean ignoreRangeLimit
    ) {
        return instinctEnabled
                && !targetSpectatingOrCreative
                && !targetInvisible
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
        return appearsAsKillerToKillerInstinct(targetRole, targetCanUseKillerFeatures, false);
    }

    /** Also mirrors SparkStrength's Coroner killer/Undercover body disguise, which native killers see as a cohort.
     *  同时同步 SparkStrength 验尸官伪装成杀手/卧底尸体身份时被原生杀手视为同伙的规则。 */
    public static boolean appearsAsKillerToKillerInstinct(
            Role targetRole,
            boolean targetCanUseKillerFeatures,
            boolean targetHasCoronerKillerDisguise
    ) {
        return targetCanUseKillerFeatures || isUndercover(targetRole) || targetHasCoronerKillerDisguise;
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
                publicEffectiveTraitIds(target, game),
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

    /** Uses client-synced alignment flags without exposing hidden trait text; non-killer viewers receive them as false.
     *  Dead players fall back to their death snapshot, which the server and spectator or body-inspector clients hold.
     *  使用客户端已同步的阵营标记，不暴露隐藏天赋文本；非杀手观察者收到的值恒为 false。
     *  死亡玩家回退到死亡快照，服务端与旁观者、验尸官客户端持有该快照。 */
    private static Collection<Identifier> publicEffectiveTraitIds(PlayerEntity player, GameWorldComponent game) {
        Collection<Identifier> deathTraits = game.isPlayerDead(player.getUuid())
                ? TraitWorldComponent.KEY.get(player.getWorld()).getDeathTraitSnapshot(player.getUuid())
                : List.of();
        boolean conscience = isConscienceVisibleToInstinct(player) || hasConscience(deathTraits);
        boolean impostor = isImpostorVisibleToInstinct(player) || hasImpostor(deathTraits);
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
        return EffectiveAlignment.hasConscience(traits);
    }

    public static boolean hasImpostor(Collection<Identifier> traits) {
        return EffectiveAlignment.hasImpostor(traits);
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
                && !isConscienceBlockedRole(role)
                && !hasImpostor(selectedTraits);
    }

    /** Owner rule: SparkWitch's Bell Ringer never receives Conscience; other killer traits stay eligible.
     *  所有者规则：SparkWitch 的敲钟人永不获得善良；其他杀手词条不受影响。 */
    public static boolean isConscienceBlockedRole(Role role) {
        return role != null && role.identifier().equals(SPARKWITCH_BELL_RINGER_ID);
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
        return EffectiveAlignment.isOriginalKiller(role);
    }

    private static boolean isRoleEnabled(GameWorldComponent gameComponent, Role role) {
        return gameComponent != null && role != null && gameComponent.isRoleEnabled(role);
    }

    public static boolean isOriginalCivilian(Role role) {
        return EffectiveAlignment.isOriginalCivilian(role);
    }

    public static boolean isUndercover(Role role) {
        return role != null && role.identifier().equals(Noellesroles.UNDERCOVER_ID);
    }

    /** Keeps high-agency innocent roles, including every police-category role, from being converted into Impostor.
     *  防止强机制无辜者角色（含所有警职类别身份）被转换成内鬼。 */
    private static boolean isImpostorBlockedRole(Role role) {
        return role != null
                && (PoliceRoleCategory.isPolice(role)
                || role.identifier().equals(Noellesroles.SURVIVAL_MASTER_ID)
                // Toxicologist owns SparkStrength's blue-poison kit, which must stay on the innocent side.
                // 毒理学家持有 SparkStrength 的蓝毒道具，必须保持好人身份。
                || role.identifier().equals(Noellesroles.TOXICOLOGIST_ID)
                || role.identifier().equals(SPARKWITCH_PIG_GOD_ID)
                || role.identifier().equals(SPARKWITCH_SAINT_ID)
                || role.identifier().equals(SPARKWITCH_BLIND_ID));
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
        return cohortOverride(viewerRole, viewerTraits, targetRole, targetTraits, false);
    }

    public static Boolean cohortOverride(
            Role viewerRole,
            Collection<Identifier> viewerTraits,
            Role targetRole,
            Collection<Identifier> targetTraits,
            boolean targetHasCoronerKillerDisguise
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
        // SparkStrength's Coroner cohort hook requires native killer features, so Impostor needs it mirrored here.
        // SparkStrength 的验尸官同伙提示要求原生杀手功能，内鬼需要在这里同步。
        if (hasImpostor(viewerTraits) && (isUndercover(targetRole) || targetHasCoronerKillerDisguise)) {
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

    /** Grants blackout vision only to active Impostor trait holders.
     *  只让当前拥有内鬼天赋的玩家获得熄灯视野免疫。 */
    public static boolean shouldApplyImpostorBlackoutImmunity(Collection<Identifier> traits) {
        return traits != null && hasImpostor(traits);
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
        return EffectiveAlignment.isEffectiveKiller(role, traits);
    }

    /**
     * Returns the killer-team UUIDs that NoellesRoles should use for Shadow Jester showdown checks.
     * 返回 NoellesRoles 双影谢幕判定中应使用的有效杀手阵营 UUID 列表。
     *
     * <p>Wathe 的 {@code getAllKillerTeamPlayers()} 只读取角色的原始
     * {@code canUseKiller()} 属性，因此会遗漏“原始好人 + impostor”玩家，
     * 也会把带有 conscience 的原始杀手继续算入杀手列表。这里按 SparkTraits
     * 已确定的阵营规则重新构造列表：</p>
     *
     * <ul>
     *     <li>保留原始杀手，但排除已经翻为好人阵营的 conscience；</li>
     *     <li>加入原始好人且拥有 impostor 的玩家；</li>
     *     <li>不把普通好人或其他中立角色加入杀手阵营。</li>
     * </ul>
     */
    public static List<UUID> getEffectiveKillerTeamPlayers(GameWorldComponent gameComponent) {
        if (gameComponent == null) {
            return List.of();
        }

        GameWorldComponentAccessor accessor = (GameWorldComponentAccessor) (Object) gameComponent;
        if (accessor.sparktraits$getWorld() == null) {
            return List.of();
        }

        Set<UUID> effectiveKillers = new LinkedHashSet<>();

        // 先处理 Wathe 原本认定的杀手。善良杀手不能在双影谢幕中继续充当杀手对手。
        for (UUID uuid : gameComponent.getAllKillerTeamPlayers()) {
            PlayerEntity player = accessor.sparktraits$getWorld().getPlayerByUuid(uuid);
            if (player == null || !hasConscience(player)) {
                effectiveKillers.add(uuid);
            }
        }

        // 再补入被 impostor 翻转为杀手阵营的原始好人。
        for (UUID uuid : gameComponent.getAllPlayers()) {
            Role role = gameComponent.getRole(uuid);
            PlayerEntity player = accessor.sparktraits$getWorld().getPlayerByUuid(uuid);
            if (player != null
                    && isOriginalCivilian(role)
                    && hasImpostor(player)
                    && !hasConscience(player)) {
                effectiveKillers.add(uuid);
            }
        }

        return new ArrayList<>(effectiveKillers);
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
        return EffectiveAlignment.isEffectiveCivilian(role, traits);
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
                || SPARKWITCH_ABYSS_LISTENER_ID.equals(role.identifier())
                || SPARKWITCH_POTION_GUNNER_ID.equals(role.identifier())
                || SPARKWITCH_RIFTWALKER_ID.equals(role.identifier())
                || SPARKWITCH_BEWITCHED_ID.equals(role.identifier())
                || SPARKWITCH_MURDEROUS_WITCH_ID.equals(role.identifier())
                || Noellesroles.CORRUPT_COP_ID.equals(role.identifier())
                || SPARKWITCH_INSIDER_ID.equals(role.identifier())
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
        return shouldDeferTeamWinForNoellesShadowJester(
                GameFunctions.WinStatus.KILLERS,
                GameFunctions.WinStatus.KILLERS,
                role,
                allied,
                showdownActive
        ) || shouldDeferTeamWinForNoellesShadowJester(
                GameFunctions.WinStatus.PASSENGERS,
                GameFunctions.WinStatus.PASSENGERS,
                role,
                allied,
                showdownActive
        );
    }

    /** Routes only Shadow Jester's killer-win takeover before showdown; active showdown keeps all ordinary wins deferred.
     *  双影谢幕开始前只接管杀手胜利；谢幕已开始后继续延后所有普通队伍胜利。 */
    public static boolean shouldDeferTeamWinForNoellesShadowJester(
            GameFunctions.WinStatus currentStatus,
            GameFunctions.WinStatus proposedWinStatus,
            Role role,
            boolean allied,
            boolean showdownActive
    ) {
        if (proposedWinStatus != GameFunctions.WinStatus.KILLERS
                && proposedWinStatus != GameFunctions.WinStatus.PASSENGERS) {
            return false;
        }
        if (role == null || !NOELLES_SHADOW_JESTER_ID.equals(role.identifier())) {
            return false;
        }
        if (showdownActive) {
            return true;
        }
        return allied
                && (currentStatus == GameFunctions.WinStatus.KILLERS
                || proposedWinStatus == GameFunctions.WinStatus.KILLERS);
    }

    public static Role.MoodType effectiveMoodType(PlayerEntity player, Role role) {
        if (hasConscience(player)) {
            return effectiveConscienceMoodType(SparkWitchWraithBridge.isWraithActive(player));
        }
        if (hasImpostor(player)) {
            return Role.MoodType.FAKE;
        }
        return role == null ? Role.MoodType.NONE : role.getMoodType();
    }

    /** Active Wraith state suppresses Conscience's mood HUD without removing the trait. */
    public static Role.MoodType effectiveConscienceMoodType(boolean activeWraith) {
        return activeWraith ? Role.MoodType.NONE : Role.MoodType.REAL;
    }

    public static Role.MoodType effectiveMoodType(Role role, Collection<Identifier> traits) {
        return effectiveMoodType(role, traits, false);
    }

    static Role.MoodType effectiveMoodType(Role role, Collection<Identifier> traits, boolean activeWraith) {
        if (hasConscience(traits)) {
            return effectiveConscienceMoodType(activeWraith);
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
     * Conscience/Impostor +50 task money; stacks on top of role-owned task income (see EffectiveEconomyRules).
     * 善良/内鬼的 +50 任务金币；与职业自带的任务收入叠加（见 EffectiveEconomyRules）。
     */
    public static boolean shouldRewardTaskMoney(Role role, Collection<Identifier> traits) {
        return EffectiveEconomyRules.shouldRewardTaskMoney(role, traits);
    }

    public static boolean hasNativeTaskMoneyReward(Role role) {
        return EffectiveEconomyRules.hasNativeTaskMoneyReward(role);
    }

    /** Lets Party Animal reward any non-self target; NoellesRoles keeps self-buzz and repeat-level gates.
     *  允许派对狂对任意非自己目标发放变声奖励；自变声和重复等级限制仍由 NoellesRoles 原逻辑处理。 */
    public static boolean shouldBlockPartyAnimalTargetReward(Role targetRole, Collection<Identifier> targetTraits) {
        return EffectiveEconomyRules.shouldBlockPartyAnimalTargetReward(targetRole, targetTraits);
    }

    public static boolean shouldRewardConscienceKill(Role victimRole, Collection<Identifier> victimTraits) {
        return EffectiveEconomyRules.shouldRewardConscienceKill(victimRole, victimTraits);
    }

    /** Rewards Impostors for killing public non-killers, including neutral roles.
     *  内鬼击杀公开非杀手时获得击杀奖励，包含好人与中立角色。 */
    public static boolean shouldRewardImpostorKill(Role victimRole, Collection<Identifier> victimTraits) {
        return EffectiveEconomyRules.shouldRewardImpostorKill(victimRole, victimTraits);
    }

    public static int impostorKillReward(Role victimRole, Collection<Identifier> victimTraits, boolean canAccessShop) {
        return EffectiveEconomyRules.impostorKillReward(victimRole, victimTraits, canAccessShop);
    }

    /**
     * Keeps NoellesRoles Jester Moment tied to unflipped original innocents.
     * 让 NoellesRoles 的小丑时刻只由未被内鬼翻阵营的原始好人触发。
     */
    public static boolean shouldTriggerJesterMoment(Role killerRole, Collection<Identifier> killerTraits) {
        return EffectiveDeathConsequenceRules.shouldTriggerJesterMoment(killerRole, killerTraits);
    }

    /** Treat gun victims by their effective alignment for innocent-shot penalties.
     *  枪击惩罚按目标的有效阵营判定，确保善良杀手被当作好人。 */
    public static boolean shouldTreatGunVictimAsInnocent(Role victimRole, Collection<Identifier> victimTraits) {
        return EffectiveGunRules.shouldTreatGunVictimAsInnocent(victimRole, victimTraits);
    }

    /** Cancels Wathe's innocent-shot punishment only for Impostor shots.
     *  只在内鬼开枪时取消 Wathe 的好人枪击惩罚，普通杀手保留原版消耗枪械逻辑。 */
    public static boolean shouldCancelInnocentShotPunishment(
            Role shooterRole,
            Collection<Identifier> shooterTraits,
            Role victimRole,
            Collection<Identifier> victimTraits
    ) {
        return EffectiveGunRules.shouldCancelInnocentShotPunishment(shooterRole, shooterTraits, victimRole, victimTraits);
    }

    public static boolean shouldPunishConscienceKill(boolean victimIsEffectiveCivilian, Identifier deathReason) {
        return EffectiveDeathConsequenceRules.shouldPunishConscienceKill(victimIsEffectiveCivilian, deathReason);
    }

    public static boolean shouldPunishConscienceKill(boolean victimIsEffectiveCivilian, Identifier deathReason, Identifier poisonSource) {
        return EffectiveDeathConsequenceRules.shouldPunishConscienceKill(victimIsEffectiveCivilian, deathReason, poisonSource);
    }

    public static void rememberPoisonSource(PlayerEntity player, Identifier poisonSource) {
        if (player != null && poisonSource != null) {
            EffectiveDeathConsequenceRules.rememberPoisonSource(player.getUuid(), poisonSource);
        }
    }

    public static void handleAfterKill(ServerPlayerEntity victim, ServerPlayerEntity killer, Identifier deathReason) {
        if (victim == null || killer == null || victim.getUuid().equals(killer.getUuid())) {
            return;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        Role victimRole = game.getRole(victim);
        Collection<Identifier> victimTraits = TraitPlayerComponent.KEY.get(victim).getActiveTraitIds();
        boolean victimIsEffectiveCivilian = isEffectiveCivilian(victimRole, victimTraits);
        Identifier poisonSource = EffectiveDeathConsequenceRules.consumePoisonSource(victim.getUuid());
        Collection<Identifier> killerTraits = TraitPlayerComponent.KEY.get(killer).getActiveTraitIds();
        boolean conscience = hasConscience(killerTraits);
        boolean punish = (conscience && EffectiveDeathConsequenceRules.shouldPunishConscienceKill(
                victimIsEffectiveCivilian, deathReason, poisonSource
        )) || EffectiveDeathConsequenceRules.shouldPunishVeteranKnifeKill(
                game.getRole(killer), killerTraits, victimRole, victimTraits, deathReason
        );
        if (punish && GameFunctions.isPlayerPlayingAndAlive(killer)) {
            GameFunctions.killPlayer(killer, true, null, GameConstants.DeathReasons.SHOT_INNOCENT, true);
        } else if (conscience) {
            if (shouldRewardConscienceKill(victimRole, victimTraits)) {
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

    static boolean preservesResolvedWinStatus(GameFunctions.WinStatus currentStatus) {
        return currentStatus == GameFunctions.WinStatus.TIME
                || currentStatus == GameFunctions.WinStatus.NEUTRAL;
    }

    private static CheckWinCondition.WinResult checkWin(
            ServerWorld world,
            GameWorldComponent gameComponent,
            GameFunctions.WinStatus currentStatus
    ) {
        if (preservesResolvedWinStatus(currentStatus)) {
            return null;
        }
        List<ServerPlayerEntity> players = world.getPlayers();
        List<Role> livingRoles = new ArrayList<>();
        boolean realKillerAlive = false;
        boolean effectiveCivilianAlive = false;
        boolean noellesJesterBlocksTeamWin = false;
        boolean noellesShadowJesterDefersPassengerWin = false;
        boolean noellesShadowJesterDefersKillerWin = false;

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
            if (shouldDeferTeamWinForNoellesShadowJester(
                    player,
                    role,
                    currentStatus,
                    GameFunctions.WinStatus.PASSENGERS
            )) {
                noellesShadowJesterDefersPassengerWin = true;
            }
            if (shouldDeferTeamWinForNoellesShadowJester(
                    player,
                    role,
                    currentStatus,
                    GameFunctions.WinStatus.KILLERS
            )) {
                noellesShadowJesterDefersKillerWin = true;
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
            if (shouldDeferTeamWinForBlockingNeutral(
                    GameFunctions.WinStatus.PASSENGERS,
                    livingRoles,
                    noellesJesterBlocksTeamWin || noellesShadowJesterDefersPassengerWin
            )) {
                return null;
            }
            return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.PASSENGERS);
        }
        if (!effectiveCivilianAlive) {
            if (shouldDeferTeamWinForBlockingNeutral(
                    GameFunctions.WinStatus.KILLERS,
                    livingRoles,
                    noellesJesterBlocksTeamWin || noellesShadowJesterDefersKillerWin
            )) {
                return null;
            }
            return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.KILLERS);
        }
        if (currentStatus == GameFunctions.WinStatus.PASSENGERS || currentStatus == GameFunctions.WinStatus.KILLERS) {
            return CheckWinCondition.WinResult.block();
        }
        return null;
    }

    private static boolean shouldDeferTeamWinForNoellesShadowJester(
            PlayerEntity player,
            Role role,
            GameFunctions.WinStatus currentStatus,
            GameFunctions.WinStatus proposedWinStatus
    ) {
        return shouldDeferTeamWinForNoellesShadowJester(
                currentStatus,
                proposedWinStatus,
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
