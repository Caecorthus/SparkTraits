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
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerMoodComponent.class, remap = false)
public abstract class PlayerMoodComponentMixin {
    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    private float mood;

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
                TraitPlayerComponent.KEY.get(this.player).getActiveTraitIds()
        );
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
