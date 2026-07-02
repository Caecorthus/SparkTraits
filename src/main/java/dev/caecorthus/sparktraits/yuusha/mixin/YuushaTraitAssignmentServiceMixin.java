package dev.caecorthus.sparktraits.mixin.yuusha.mixin;

import dev.caecorthus.sparktraits.yuusha.YuushaTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(targets = "dev.caecorthus.sparktraits.impl.TraitAssignmentService")
public abstract class YuushaTraitAssignmentServiceMixin {
    @Inject(method = "enforceUniqueTraitLimits", at = @At("TAIL"), remap = false)
    private static void sparktraits$enforceYuushaCap(List<?> plans, CallbackInfo ci) {
        YuushaTrait.enforceHeroCapOnPlans(plans);
    }
}
