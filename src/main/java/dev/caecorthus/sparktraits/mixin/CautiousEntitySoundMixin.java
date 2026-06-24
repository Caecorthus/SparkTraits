package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.CautiousTrait;
import dev.caecorthus.sparktraits.impl.GlobalTraitService;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the whole player step sound path so special block step effects stay quiet too.
 * 取消玩家整条脚步音路径，避免特殊方块的脚步反馈漏出。
 */
@Mixin(Entity.class)
public abstract class CautiousEntitySoundMixin {
    @Inject(method = "playStepSounds", at = @At("HEAD"), cancellable = true)
    private void sparktraits$skipCautiousStepSounds(BlockPos pos, BlockState state, CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player && GlobalTraitService.hasTrait(player, CautiousTrait.ID)) {
            ci.cancel();
        }
    }
}
