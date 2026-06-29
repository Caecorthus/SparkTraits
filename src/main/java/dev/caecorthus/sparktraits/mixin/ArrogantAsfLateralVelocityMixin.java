package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.CorruptCopTraitService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds Arrogant ASF's lateral bonus after vanilla movement-input normalization.
 * 在原版移动输入归一化之后，为“展示豪度”额外补上左右横移速度。
 */
@Mixin(Entity.class)
public abstract class ArrogantAsfLateralVelocityMixin {
    @Inject(method = "updateVelocity", at = @At("TAIL"))
    private void sparktraits$addArrogantAsfLateralVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) {
            return;
        }

        Vec3d bonus = CorruptCopTraitService.arrogantAsfLateralVelocityBonus(player, movementInput, speed);
        if (bonus.lengthSquared() > 0.0d) {
            player.setVelocity(player.getVelocity().add(bonus));
        }
    }
}
