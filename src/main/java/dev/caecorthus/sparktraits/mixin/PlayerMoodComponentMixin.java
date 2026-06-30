package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.GoodTraitService;
import dev.caecorthus.sparktraits.impl.GlobalTraitService;
import dev.caecorthus.sparktraits.impl.KillerTraitService;
import dev.caecorthus.sparktraits.impl.VigilanteVeteranTraitService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerMoodComponent.class, remap = false)
public abstract class PlayerMoodComponentMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    private float mood;

    @Shadow
    public abstract void setMood(float mood);

    @Redirect(
            method = {"serverTick", "getMood", "setMood"},
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/api/Role;getMoodType()Ldev/doctor4t/wathe/api/Role$MoodType;")
    )
    private Role.MoodType sparktraits$effectiveMoodType(Role role) {
        return EffectiveTraitService.effectiveMoodType(this.player, role);
    }

    @Redirect(
            method = "setMood",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;clamp(FFF)F")
    )
    private float sparktraits$clampMoodWithSteady(float mood, float min, float max) {
        GameWorldComponent game = GameWorldComponent.KEY.get(this.player.getWorld());
        Role role = game.getRole(this.player);
        return GlobalTraitService.clampMood(
                mood,
                EffectiveTraitService.effectiveMoodType(this.player, role),
                TraitPlayerComponent.KEY.get(this.player).getActiveTraitIds()
        );
    }

    @ModifyArg(
            method = {"serverTick", "clientTick"},
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;setMood(F)V"),
            index = 0
    )
    private float sparktraits$applyMoodDrainTraits(float proposedMood) {
        float adjustedMood = KillerTraitService.oppressiveAdjustedMood(this.mood, proposedMood, this.player);
        adjustedMood = VigilanteVeteranTraitService.wellTrainedAdjustedMood(this.mood, adjustedMood, this.player);
        adjustedMood = GoodTraitService.socialMoodAdjustedMood(this.mood, adjustedMood, this.player);
        return DepressionTraitService.depressionAdjustedMood(
                this.mood,
                adjustedMood,
                TraitPlayerComponent.KEY.get(this.player).getActiveTraitIds(),
                DepressionTraitService.isPsychoActive(this.player)
                        || DepressionTraitService.isWathePsychoActive(this.player)
        );
    }

    @Redirect(
            method = "serverTick",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V")
    )
    private void sparktraits$suppressDepressionPsychoMentalBreakdown(
            ServerPlayerEntity player,
            boolean spawnBody,
            ServerPlayerEntity killer,
            Identifier deathReason,
            boolean handleBefore
    ) {
        // Wathe psycho does not block mental breakdown, so Depression handles only its own psycho window here.
        // wathe 疯魔不会自动阻止精神崩溃；这里仅保护抑郁自己的疯魔窗口。
        if (DepressionTraitService.shouldSuppressMentalBreakdown(player, deathReason)) {
            setMood(DepressionTraitService.depressionPsychoMoodFloor(this.mood));
            return;
        }
        GameFunctions.killPlayer(player, spawnBody, killer, deathReason, handleBefore);
    }

    @Inject(method = "serverTick", at = @At("TAIL"))
    private void sparktraits$holdDepressionPsychoMood(CallbackInfo ci) {
        if (DepressionTraitService.shouldSuppressMentalBreakdown(
                this.player,
                dev.doctor4t.wathe.game.GameConstants.DeathReasons.MENTAL_BREAKDOWN
        )) {
            setMood(DepressionTraitService.depressionPsychoMoodFloor(this.mood));
        }
    }

    @Inject(method = {"isLowerThanMid", "isLowerThanDepressed"}, at = @At("HEAD"), cancellable = true)
    private void sparktraits$wellTrainedIgnoresLowMood(CallbackInfoReturnable<Boolean> cir) {
        if (VigilanteVeteranTraitService.ignoresLowMood(this.player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isLowerThanDepressed", at = @At("HEAD"), cancellable = true)
    private void sparktraits$depressionPsychoIgnoresSprintBlock(CallbackInfoReturnable<Boolean> cir) {
        if (DepressionTraitService.shouldAllowLowMoodSprint(this.player)) {
            cir.setReturnValue(false);
        }
    }
}
