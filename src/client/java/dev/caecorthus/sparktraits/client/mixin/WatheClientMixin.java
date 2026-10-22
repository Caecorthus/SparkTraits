package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.client.compat.SparkStrengthCoronerClientBridge;
import dev.caecorthus.sparktraits.client.compat.SparkWitchBlackRavenBridge;
import dev.caecorthus.sparktraits.client.instinct.GoingDarkInstinctClientHooks;
import dev.caecorthus.sparktraits.client.instinct.InstinctViewerContext;
import dev.caecorthus.sparktraits.compat.SparkStrengthCoronerBridge;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConsciencePoisonerService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceSerialKillerService;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandFinalMomentService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.jester.JesterPlayerComponent;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = WatheClient.class, remap = false)
public abstract class WatheClientMixin {
    @Inject(method = "isKiller", at = @At("HEAD"), cancellable = true)
    private static void sparktraits$impostorCanUseInstinct(CallbackInfoReturnable<Boolean> cir) {
        if (!SparkTraitsServerConnection.isConfirmedServer()) {
            return;
        }
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (InstinctViewerContext.viewerHasImpostor(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void sparktraits$effectiveInstinctHighlight(Entity target, CallbackInfoReturnable<Integer> cir) {
        if (!SparkTraitsServerConnection.isConfirmedServer()) {
            return;
        }
        PlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null) {
            return;
        }
        // Wathe asks for every visible entity twice per frame, so viewer-only facts come from one per-frame context.
        // Wathe 每帧为每个可见实体询问两次，因此仅与观察者有关的事实取自每帧一份的上下文。
        InstinctViewerContext context = InstinctViewerContext.of(viewer);
        if (!(target instanceof PlayerEntity playerTarget)) {
            sparktraits$nonPlayerInstinctHighlight(context, target, cir);
            return;
        }

        if (playerTarget.getUuid().equals(context.depressionPsychoAttacker())) {
            cir.setReturnValue(DepressionTraitService.ATTACKER_HIGHLIGHT_COLOR);
            return;
        }
        if (playerTarget.getUuid().equals(context.depressionCounterTarget())) {
            cir.setReturnValue(DepressionTraitService.VICTIM_HIGHLIGHT_COLOR);
            return;
        }

        GameWorldComponent game = context.game();
        TraitPlayerComponent targetTraits = TraitPlayerComponent.KEY.get(playerTarget);
        if (context.finalMomentActive()
                && game.hasAnyRole(playerTarget)
                && GameFunctions.isPlayerPlayingAndAlive(playerTarget)) {
            // Final Moment must beat Last Stand's normal killer-instinct hiding.
            // 终局时刻必须优先于背水一战的普通杀手本能隐藏。
            cir.setReturnValue(LastStandFinalMomentService.finalMomentHighlightColorForViewer(
                    viewer,
                    playerTarget,
                    game,
                    context.traitWorld().isFinalMomentLooseEnd(playerTarget.getUuid()),
                    // Answering here skips SparkStrength's Coroner disguise event, so ask it for the disguise color.
                    // 这里提前返回会跳过 SparkStrength 的验尸官伪装事件，因此直接查询其伪装色。
                    SparkStrengthCoronerClientBridge.resolveKillerDisguiseInstinctColor(playerTarget)
            ));
            return;
        }
        if (context.canSeeSpectatorInformation()) {
            // Last Stand targets can be hidden while pending or after revival, so spectator instinct needs a bypass.
            // 背水一战目标在等待复活或复活后都可能被隐藏，因此旁观透视需要单独放行。
            int highlight = LastStandService.spectatorLastStandHighlightColor(
                    true,
                    context.instinctEnabled(),
                    targetTraits.isLastStandPending(),
                    targetTraits.isKillerInstinctHidden(),
                    game.getRole(playerTarget)
            );
            if (highlight != -1) {
                cir.setReturnValue(highlight);
            }
            return;
        }

        if (GoingDarkInstinctClientHooks.shouldSuppress(context, playerTarget)) {
            cir.setReturnValue(-1);
            return;
        }

        MorphlingPlayerComponent morphling = MorphlingPlayerComponent.KEY.get(playerTarget);

        // Phantom invisibility must beat SparkTraits' effective-alignment instinct overlays.
        // Only effective-killer viewers receive the Conscience flag; others treat the target like any Phantom.
        // 幽灵隐身优先于 SparkTraits 的有效阵营本能高亮覆盖。
        // 只有有效杀手观察者能收到善良标记；其他观察者把目标当作普通幽灵处理。
        if (EffectiveTraitService.shouldSkipInvisibleTargetFromEffectiveInstinct(
                playerTarget.isInvisible(),
                EffectiveTraitService.isConscienceVisibleToInstinct(playerTarget),
                targetTraits.isLastStandPending(),
                targetTraits.isKillerInstinctHidden()
        )) {
            cir.setReturnValue(-1);
            return;
        }

        // Corpse mode has priority over every effective-killer instinct overlay; other viewers never receive the
        // Conscience flag, so they treat the target like any Morphling.
        // 尸体模式优先于所有有效杀手本能描边；其他观察者收不到善良标记，因此把目标当作普通变形者处理。
        if (EffectiveTraitService.shouldHideConscienceMorphlingFromInstinct(
                EffectiveTraitService.isConscienceVisibleToInstinct(playerTarget),
                game.isRole(playerTarget, Noellesroles.MORPHLING),
                morphling.corpseMode
        )) {
            cir.setReturnValue(-1);
            return;
        }

        // NoellesRoles serial-killer targets are always visible unless invisible; keep that before Conscience range logic.
        // NoellesRoles 的连环杀手目标不受善良本能距离限制（隐身时除外），先保留原目标高亮。
        if (ConscienceSerialKillerService.shouldUseSerialKillerTargetHighlight(
                context.isSerialKiller(),
                context.serialKillerTargets().isCurrentTarget(playerTarget.getUuid()),
                playerTarget.isInvisible()
        )) {
            cir.setReturnValue(Noellesroles.SERIAL_KILLER.color());
            return;
        }

        boolean targetHasBluePoison = targetTraits.hasConsciencePoison();
        boolean targetHasNormalPoison = PlayerPoisonComponent.KEY.get(playerTarget).poisonTicks > 0;
        // Like NoellesRoles' Toxicologist outline, blue poison never reveals an invisible target.
        // 与 NoellesRoles 毒理学家描边一致，蓝毒不会暴露隐身目标。
        if (targetHasBluePoison
                && context.isToxicologist()
                && !playerTarget.isInvisible()
                && viewer.canSee(playerTarget)) {
            cir.setReturnValue(ConsciencePoisonerService.poisonHighlightColor(
                    targetHasNormalPoison,
                    true,
                    Noellesroles.TOXICOLOGIST.color()
            ));
            return;
        }

        /*
         * SparkWitch's Black Raven is an always-civilian police role (2026-10-21): while it holds the instinct key,
         * players it has sensed within 8 blocks show one raven-feather color. This priority-1000 HEAD callback runs
         * before SparkWitch's priority-2000 HEAD hook, so SparkWitch is asked here, ahead of the Conscience and
         * effective-killer branches below, for every viewer. null means SparkWitch does not take over; a color or -1
         * is final.
         * SparkWitch 黑羽鸦现为始终属于好人的警察身份（2026-10-21）：按住本能键时，8 格内被感知过的玩家显示
         * 统一的鸦羽色描边。此优先级 1000 的 HEAD 回调早于 SparkWitch 优先级 2000 的 HEAD 钩子执行，因此对所有
         * 观察者都在下方善良与有效杀手分支之前，先经可选反射桥询问 SparkWitch。返回 null 表示不接管；返回颜色
         * 或 -1 即为最终结果，SparkTraits 不再覆盖。
         */
        Integer blackRavenSensedHighlight = SparkWitchBlackRavenBridge.resolveSensedInstinctHighlight(target);
        if (blackRavenSensedHighlight != null) {
            cir.setReturnValue(blackRavenSensedHighlight);
            return;
        }

        if (context.hasConscience()) {
            if (context.conscienceDelegatesToNative()) {
                return;
            }
            // Bomb holders keep Bomber instinct visibility beyond Conscience's normal 10-block range.
            // 持弹者保留炸弹客本能可视，不受善良普通目标 10 格限制影响。
            boolean bombHolderIgnoresRange = context.isBomber()
                    && BomberPlayerComponent.KEY.get(playerTarget).hasBomb();
            boolean consciencePoisoner = context.isConsciencePoisoner();
            boolean normalPoisoned = consciencePoisoner && targetHasNormalPoison;
            boolean bluePoisoned = consciencePoisoner && targetHasBluePoison;
            boolean poisonedTargetIgnoresRange = ConsciencePoisonerService.poisonStateIgnoresConscienceRange(normalPoisoned, bluePoisoned);
            boolean shouldHighlight = EffectiveTraitService.shouldConscienceInstinctHighlightTarget(
                    context.instinctEnabled(),
                    GameFunctions.isPlayerPlayingAndAlive(playerTarget),
                    GameFunctions.isPlayerSpectatingOrCreative(playerTarget),
                    playerTarget.isInvisible(),
                    viewer.squaredDistanceTo(playerTarget),
                    targetTraits.isLastStandPending(),
                    targetTraits.isKillerInstinctHidden(),
                    // A projecting Spirit Walker's body stays visible within the normal Conscience range.
                    // 灵界行者出窍时留下的肉身在善良的正常范围内仍可被透视。
                    false,
                    bombHolderIgnoresRange || poisonedTargetIgnoresRange
            );
            cir.setReturnValue(shouldHighlight
                    ? (poisonedTargetIgnoresRange
                    ? ConsciencePoisonerService.poisonInstinctColor(normalPoisoned, bluePoisoned)
                    : (bombHolderIgnoresRange ? Noellesroles.BOMBER.color() : EffectiveTraitService.CONSCIENCE_COLOR))
                    : -1);
            return;
        }

        // Mirror NoellesRoles' Jester Moment skip before later always-on highlight events can expose the active Jester.
        // 在后续全局高亮事件暴露小丑前，同步 NoellesRoles 的小丑时刻跳过规则。
        if (EffectiveTraitService.shouldSkipJesterMomentHighlight(
                context.role(),
                game.getRole(playerTarget),
                JesterPlayerComponent.KEY.get(playerTarget).inPsychoMode,
                viewer.getUuid().equals(playerTarget.getUuid())
        )) {
            cir.setReturnValue(-1);
            return;
        }

        if (!context.instinctEnabled()) {
            return;
        }
        if (!context.isEffectiveKiller()) {
            return;
        }
        // SparkTraits answers before NoellesRoles' event skip, so mirror sight-gated target skips here.
        // SparkTraits 会先于 NoellesRoles 的事件 skip 返回，因此这里同步只阻止遮挡透视的目标规则。
        if (EffectiveTraitService.isHiddenFromKillerInstinct(playerTarget)
                || EffectiveTraitService.shouldSkipSurvivalMasterForImpostorInstinct(
                        game.getRole(playerTarget),
                        context.hasImpostor(),
                        viewer.canSee(playerTarget)
                )) {
            cir.setReturnValue(-1);
            return;
        }
        Integer morphlingColor = sparktraits$conscienceMorphlingDisguiseColor(playerTarget, game, morphling);
        if (morphlingColor != null) {
            cir.setReturnValue(morphlingColor);
            return;
        }
        if (EffectiveTraitService.isImpostorVisibleToInstinct(playerTarget)) {
            cir.setReturnValue(EffectiveTraitService.IMPOSTOR_INSTINCT_COLOR);
        } else if (EffectiveTraitService.isConscienceVisibleToInstinct(playerTarget)) {
            cir.setReturnValue(EffectiveTraitService.CIVILIAN_INSTINCT_COLOR);
        } else {
            // Answering here skips SparkStrength's Coroner disguise event, so mirror it.
            // 这里提前返回会跳过 SparkStrength 的验尸官伪装事件，因此同步其判定。
            Integer coronerDisguiseColor = SparkStrengthCoronerClientBridge.resolveKillerDisguiseInstinctColor(playerTarget);
            if (coronerDisguiseColor != null) {
                cir.setReturnValue(coronerDisguiseColor);
                return;
            }
            if (context.hasImpostor()) {
                cir.setReturnValue(EffectiveTraitService.effectiveKillerInstinctColor(
                        EffectiveTraitService.appearsAsKillerToKillerInstinct(
                                game.getRole(playerTarget),
                                game.canUseKillerFeatures(playerTarget),
                                SparkStrengthCoronerBridge.appearsAsKillerCohort(playerTarget)
                        ),
                        false,
                        false
                ));
            }
        }
    }

    /**
     * Items, notes, bodies and other non-player entities: every player-only branch above is skipped, and the spectator
     * Last Stand bypass never colours a non-player, so only the Black Raven answer and Conscience's hide remain.
     * 物品、纸条、尸体等非玩家实体：上方所有仅针对玩家的分支都会跳过，旁观背水一战放行也从不为非玩家着色，
     * 因此只剩黑羽鸦的结果与善良的隐藏。
     */
    private static void sparktraits$nonPlayerInstinctHighlight(
            InstinctViewerContext context,
            Entity target,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (context.canSeeSpectatorInformation()) {
            return;
        }
        Integer blackRavenSensedHighlight = SparkWitchBlackRavenBridge.resolveSensedInstinctHighlight(target);
        if (blackRavenSensedHighlight != null) {
            cir.setReturnValue(blackRavenSensedHighlight);
            return;
        }
        if (context.hasConscience() && !context.conscienceDelegatesToNative()) {
            cir.setReturnValue(-1);
        }
    }

    private static Integer sparktraits$conscienceMorphlingDisguiseColor(
            PlayerEntity target,
            GameWorldComponent game,
            MorphlingPlayerComponent morphling
    ) {
        UUID disguise = morphling.disguise;
        if (!EffectiveTraitService.shouldUseConscienceMorphlingDisguiseInstinct(
                EffectiveTraitService.isConscienceVisibleToInstinct(target),
                game.isRole(target, Noellesroles.MORPHLING),
                morphling.corpseMode,
                morphling.getMorphTicks() > 0,
                disguise != null
        )) {
            return null;
        }

        Role disguiseRole = game.getRole(disguise);
        if (disguiseRole == null) {
            return null;
        }

        PlayerEntity disguisePlayer = target.getWorld().getPlayerByUuid(disguise);
        return EffectiveTraitService.effectiveKillerInstinctColor(
                disguiseRole,
                disguisePlayer != null && EffectiveTraitService.isConscienceVisibleToInstinct(disguisePlayer),
                disguisePlayer != null && EffectiveTraitService.isImpostorVisibleToInstinct(disguisePlayer)
        );
    }
}
