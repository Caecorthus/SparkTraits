package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.ConsciencePoisonerService;
import dev.caecorthus.sparktraits.impl.ConscienceSerialKillerService;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.LastStandService;
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
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = WatheClient.class, remap = false)
public abstract class WatheClientMixin {
    @Inject(method = "isKiller", at = @At("HEAD"), cancellable = true)
    private static void sparktraits$impostorCanUseInstinct(CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (EffectiveTraitService.hasImpostor(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void sparktraits$effectiveInstinctHighlight(Entity target, CallbackInfoReturnable<Integer> cir) {
        PlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null) {
            return;
        }

        PlayerEntity playerTarget = target instanceof PlayerEntity targetPlayer ? targetPlayer : null;
        GameWorldComponent game = playerTarget == null ? null : GameWorldComponent.KEY.get(viewer.getWorld());
        TraitPlayerComponent targetTraits = playerTarget == null ? null : TraitPlayerComponent.KEY.get(playerTarget);
        if (WatheClient.canSeeSpectatorInformation()) {
            // Last Stand pending players are fake-dead spectators, so vanilla spectator instinct needs a bypass.
            // 背水一战等待复活的玩家处于假死旁观状态，因此原版旁观透视需要单独放行。
            int highlight = LastStandService.pendingSpectatorHighlightColor(
                    true,
                    WatheClient.isInstinctEnabled(),
                    targetTraits != null && targetTraits.isLastStandPending(),
                    game == null ? null : game.getRole(playerTarget)
            );
            if (highlight != -1) {
                cir.setReturnValue(highlight);
            }
            return;
        }

        MorphlingPlayerComponent morphling = playerTarget == null ? null : MorphlingPlayerComponent.KEY.get(playerTarget);

        // Phantom invisibility must beat SparkTraits' effective-alignment instinct overlays.
        // 幽灵隐身优先于 SparkTraits 的有效阵营本能高亮覆盖。
        if (targetTraits != null && EffectiveTraitService.shouldSkipInvisibleTargetFromEffectiveInstinct(
                playerTarget.isInvisible(),
                EffectiveTraitService.isConscienceVisibleToInstinct(playerTarget),
                targetTraits.isLastStandPending(),
                targetTraits.isKillerInstinctHidden()
        )) {
            cir.setReturnValue(-1);
            return;
        }

        // Corpse mode has priority over every non-spectator instinct overlay.
        // 尸体模式优先于所有非旁观者本能描边。
        if (playerTarget != null && EffectiveTraitService.shouldHideConscienceMorphlingFromInstinct(
                EffectiveTraitService.isConscienceVisibleToInstinct(playerTarget),
                game.isRole(playerTarget, Noellesroles.MORPHLING),
                morphling.corpseMode
        )) {
            cir.setReturnValue(-1);
            return;
        }

        // NoellesRoles serial-killer targets are always visible; keep that before Conscience range logic.
        // NoellesRoles 的连环杀手目标不受善良本能距离限制，先保留原目标高亮。
        if (playerTarget != null && ConscienceSerialKillerService.shouldUseSerialKillerTargetHighlight(
                ConscienceSerialKillerService.isConscienceSerialKiller(viewer, game),
                SerialKillerPlayerComponent.KEY.get(viewer).isCurrentTarget(playerTarget.getUuid())
        )) {
            cir.setReturnValue(Noellesroles.SERIAL_KILLER.color());
            return;
        }

        boolean targetHasBluePoison = targetTraits != null && targetTraits.hasConsciencePoison();
        boolean targetHasNormalPoison = playerTarget != null && PlayerPoisonComponent.KEY.get(playerTarget).poisonTicks > 0;
        if (playerTarget != null
                && targetHasBluePoison
                && game.isRole(viewer, Noellesroles.TOXICOLOGIST)
                && viewer.canSee(playerTarget)) {
            cir.setReturnValue(ConsciencePoisonerService.poisonHighlightColor(
                    targetHasNormalPoison,
                    true,
                    Noellesroles.TOXICOLOGIST.color()
            ));
            return;
        }

        if (EffectiveTraitService.hasConscience(viewer)) {
            if (playerTarget != null) {
                // Bomb holders keep Bomber instinct visibility beyond Conscience's normal 10-block range.
                // 持弹者保留炸弹客本能可视，不受善良普通目标 10 格限制影响。
                boolean bombHolderIgnoresRange = game.isRole(viewer, Noellesroles.BOMBER)
                        && BomberPlayerComponent.KEY.get(playerTarget).hasBomb();
                boolean consciencePoisoner = ConsciencePoisonerService.isConsciencePoisoner(viewer, game);
                boolean normalPoisoned = consciencePoisoner && PlayerPoisonComponent.KEY.get(playerTarget).poisonTicks > 0;
                boolean bluePoisoned = consciencePoisoner && targetTraits.hasConsciencePoison();
                boolean poisonedTargetIgnoresRange = ConsciencePoisonerService.poisonStateIgnoresConscienceRange(normalPoisoned, bluePoisoned);
                boolean shouldHighlight = EffectiveTraitService.shouldConscienceInstinctHighlightTarget(
                        WatheClient.isInstinctEnabled(),
                        GameFunctions.isPlayerPlayingAndAlive(playerTarget),
                        GameFunctions.isPlayerSpectatingOrCreative(playerTarget),
                        viewer.squaredDistanceTo(playerTarget),
                        targetTraits.isLastStandPending(),
                        targetTraits.isKillerInstinctHidden(),
                        EffectiveTraitService.isSpiritProjecting(playerTarget),
                        bombHolderIgnoresRange || poisonedTargetIgnoresRange
                );
                cir.setReturnValue(shouldHighlight
                        ? (poisonedTargetIgnoresRange
                        ? ConsciencePoisonerService.poisonInstinctColor(normalPoisoned, bluePoisoned)
                        : (bombHolderIgnoresRange ? Noellesroles.BOMBER.color() : EffectiveTraitService.CONSCIENCE_COLOR))
                        : -1);
            } else {
                cir.setReturnValue(-1);
            }
            return;
        }

        if (!WatheClient.isInstinctEnabled() || playerTarget == null) {
            return;
        }
        if (!EffectiveTraitService.isEffectiveKiller(viewer, game)) {
            return;
        }
        // SparkTraits answers before NoellesRoles' event skip, so block Impostor's civilian overlay here.
        // SparkTraits 会先于 NoellesRoles 的事件 skip 返回，因此这里拦住内鬼对生存大师的好人描边。
        if (EffectiveTraitService.isHiddenFromKillerInstinct(playerTarget)
                || EffectiveTraitService.shouldSkipSurvivalMasterForImpostorInstinct(
                        game.getRole(playerTarget),
                        EffectiveTraitService.hasImpostor(viewer)
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
        } else if (EffectiveTraitService.hasImpostor(viewer)) {
            cir.setReturnValue(EffectiveTraitService.isRealOriginalKiller(playerTarget, game)
                    ? EffectiveTraitService.KILLER_INSTINCT_COLOR
                    : EffectiveTraitService.CIVILIAN_INSTINCT_COLOR);
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
