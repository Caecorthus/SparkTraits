package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class LastEscapeInvisibilityMixin {
    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void sparktraits$ownedInvisibility(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player && LastEscapeService.isActive(player)) cir.setReturnValue(true);
    }
}
