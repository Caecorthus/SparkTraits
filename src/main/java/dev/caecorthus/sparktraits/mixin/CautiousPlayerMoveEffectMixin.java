package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.GlobalTraitService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps Cautious movement events but removes movement-generated sounds.
 * 保留小心翼翼玩家的移动事件，但移除移动产生的声音。
 */
@Mixin(PlayerEntity.class)
public abstract class CautiousPlayerMoveEffectMixin {
    @Inject(method = "getMoveEffect", at = @At("RETURN"), cancellable = true)
    private void sparktraits$skipCautiousMovementSounds(CallbackInfoReturnable<Entity.MoveEffect> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        cir.setReturnValue(GlobalTraitService.suppressMovementSounds(
                cir.getReturnValue(),
                GlobalTraitService.shouldSuppressCautiousSounds(player)
        ));
    }
}
