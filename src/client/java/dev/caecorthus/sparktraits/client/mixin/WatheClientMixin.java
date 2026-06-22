package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        if (viewer == null || WatheClient.canSeeSpectatorInformation()) {
            return;
        }

        if (EffectiveTraitService.hasConscience(viewer)) {
            if (target instanceof PlayerEntity playerTarget) {
                TraitPlayerComponent targetTraits = TraitPlayerComponent.KEY.get(playerTarget);
                boolean shouldHighlight = EffectiveTraitService.shouldConscienceInstinctHighlightTarget(
                        WatheClient.isInstinctEnabled(),
                        GameFunctions.isPlayerPlayingAndAlive(playerTarget),
                        GameFunctions.isPlayerSpectatingOrCreative(playerTarget),
                        viewer.squaredDistanceTo(playerTarget),
                        targetTraits.isLastStandPending(),
                        targetTraits.isKillerInstinctHidden(),
                        EffectiveTraitService.isSpiritProjecting(playerTarget)
                );
                cir.setReturnValue(shouldHighlight ? EffectiveTraitService.CONSCIENCE_COLOR : -1);
            } else {
                cir.setReturnValue(-1);
            }
            return;
        }

        if (!WatheClient.isInstinctEnabled() || !(target instanceof PlayerEntity playerTarget)) {
            return;
        }
        if (!EffectiveTraitService.isEffectiveKiller(viewer, GameWorldComponent.KEY.get(viewer.getWorld()))) {
            return;
        }
        if (EffectiveTraitService.isHiddenFromKillerInstinct(playerTarget)) {
            cir.setReturnValue(-1);
            return;
        }
        if (EffectiveTraitService.isImpostorVisibleToInstinct(playerTarget)) {
            cir.setReturnValue(EffectiveTraitService.IMPOSTOR_INSTINCT_COLOR);
        } else if (EffectiveTraitService.isConscienceVisibleToInstinct(playerTarget)) {
            cir.setReturnValue(EffectiveTraitService.CIVILIAN_INSTINCT_COLOR);
        } else if (EffectiveTraitService.hasImpostor(viewer)) {
            GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
            cir.setReturnValue(EffectiveTraitService.isRealOriginalKiller(playerTarget, game)
                    ? MathHelper.hsvToRgb(0F, 1.0F, 0.6F)
                    : EffectiveTraitService.CIVILIAN_INSTINCT_COLOR);
        }
    }
}
