package dev.caecorthus.sparktraits.impl.effective;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.compat.SparkStrengthCoronerBridge;
import dev.caecorthus.sparktraits.compat.SparkWitchWraithBridge;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.ShouldShowCohort;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** How flipped players appear to killer instinct and killer cohort hints: visibility, highlight colour,
 *  Conscience Morphling disguises and cohort overrides.
 *  阵营翻转玩家在杀手本能与杀手同伙提示中的呈现：可见性、高亮颜色、善良变形者伪装与同伙覆盖。 */
public final class EffectiveInstinctService {
    public static final int KILLER_INSTINCT_COLOR = 0x990000;
    public static final int IMPOSTOR_INSTINCT_COLOR = 0x0013FF;
    public static final int CIVILIAN_INSTINCT_COLOR = 0x4EDD35;
    public static final double CONSCIENCE_INSTINCT_RANGE_SQUARED = 100.0;

    private EffectiveInstinctService() {
    }

    static void register() {
        ShouldShowCohort.EVENT.register((viewer, target) -> {
            if (SparkTraitsServerConnection.isUnconfirmedClientEntity(viewer)) {
                return null;
            }
            GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
            // A dead Conscience killer must not see the killer team as cohorts once death clears its traits.
            // 死亡会清空天赋，善良杀手死后仍不能把杀手队伍看成同伙。
            Collection<Identifier> viewerTraits = EffectiveTraitService.effectiveTraitIds(viewer, game);
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
        return lastStandPending || killerInstinctHidden;
    }

    /** Final Moment overrides every SparkTraits-owned instinct suppression, matching Wathe's highlight priority.
     *  终局时刻覆盖所有 SparkTraits 本能屏蔽，与 Wathe 的高亮优先级保持一致。 */
    public static boolean shouldHideFromInstinct(
            boolean finalMomentActive,
            boolean lastStandPending,
            boolean killerInstinctHidden,
            boolean goingDarkSuppressed
    ) {
        return !finalMomentActive
                && (goingDarkSuppressed || shouldHideFromKillerInstinct(
                        lastStandPending,
                        killerInstinctHidden
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

    public static boolean shouldDelegateConscienceInstinctToNative(PlayerEntity player) {
        return shouldDelegateConscienceInstinctToNative(SparkWitchWraithBridge.isWraithActive(player));
    }

    public static boolean shouldDelegateConscienceInstinctToNative(boolean activeWraith) {
        return activeWraith;
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
            boolean ignoreRangeLimit
    ) {
        return instinctEnabled
                && !targetSpectatingOrCreative
                && !targetInvisible
                && (ignoreRangeLimit || targetDistanceSquared <= CONSCIENCE_INSTINCT_RANGE_SQUARED)
                && (targetPlayingAndAlive || lastStandPending || killerInstinctHidden);
    }

    /** Conscience Morphling copies only the disguise target's effective alignment.
     *  善良变形者只复制伪装目标的有效阵营，不复制临时高亮或隐藏状态。 */
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
                appearsAsKillerToKillerInstinct(targetRole, EffectiveTraitService.isOriginalKiller(targetRole)),
                EffectiveTraitService.hasConscience(targetTraits),
                EffectiveTraitService.hasImpostor(targetTraits)
        );
    }

    public static int effectiveKillerInstinctColor(
            Role targetRole,
            boolean targetHasConscience,
            boolean targetHasImpostor
    ) {
        return effectiveKillerInstinctColor(
                appearsAsKillerToKillerInstinct(targetRole, EffectiveTraitService.isOriginalKiller(targetRole)),
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
        return targetCanUseKillerFeatures || EffectiveTraitService.isUndercover(targetRole) || targetHasCoronerKillerDisguise;
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
                EffectiveTraitService.isOriginalKiller(disguiseRole),
                EffectiveTraitService.hasConscience(disguiseTraits),
                EffectiveTraitService.hasImpostor(disguiseTraits)
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
                EffectiveTraitService.isOriginalKiller(disguiseRole),
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
        if (!shouldUseConscienceMorphlingDisguiseInstinct(
                targetHasConscience,
                targetIsMorphling,
                targetCorpseMode,
                targetMorphing,
                hasDisguise
        )) {
            return null;
        }
        if (!EffectiveTraitService.isEffectiveKiller(viewerRole, viewerTraits)) {
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
                : EffectiveTraitService.isOriginalKiller(disguiseRole);

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
        if (!EffectiveTraitService.isEffectiveKiller(viewerRole, viewerTraits)) {
            return null;
        }
        return EffectiveTraitService.isEffectiveKiller(targetRole, targetTraits) ? Boolean.TRUE : null;
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
        boolean conscience = isConscienceVisibleToInstinct(player) || EffectiveTraitService.hasConscience(deathTraits);
        boolean impostor = isImpostorVisibleToInstinct(player) || EffectiveTraitService.hasImpostor(deathTraits);
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
        if (EffectiveTraitService.hasConscience(viewerTraits)) {
            return Boolean.FALSE;
        }
        if (!EffectiveTraitService.isEffectiveKiller(viewerRole, viewerTraits)) {
            return null;
        }
        if (EffectiveTraitService.hasConscience(targetTraits)) {
            return Boolean.FALSE;
        }
        if (EffectiveTraitService.hasImpostor(targetTraits)) {
            return Boolean.TRUE;
        }
        // SparkStrength's Coroner cohort hook requires native killer features, so Impostor needs it mirrored here.
        // SparkStrength 的验尸官同伙提示要求原生杀手功能，内鬼需要在这里同步。
        if (EffectiveTraitService.hasImpostor(viewerTraits)
                && (EffectiveTraitService.isUndercover(targetRole) || targetHasCoronerKillerDisguise)) {
            return Boolean.TRUE;
        }
        return null;
    }
}
