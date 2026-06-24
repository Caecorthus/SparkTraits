package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.CautiousTrait;
import dev.caecorthus.sparktraits.impl.GlobalTraitService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.agmas.noellesroles.item.FineDrinkItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Silences NoellesRoles' fine drink feedback while preserving its mood and record logic.
 * 静音 NoellesRoles 高级饮品反馈，同时保留其理智与记录逻辑。
 */
@Mixin(value = FineDrinkItem.class, remap = false)
public abstract class FineDrinkItemMixin {
    @Redirect(
            method = "finishUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;playSound(Lnet/minecraft/sound/SoundEvent;)V"
            )
    )
    private void sparktraits$skipCautiousFineDrinkSound(PlayerEntity player, SoundEvent sound) {
        if (!GlobalTraitService.hasTrait(player, CautiousTrait.ID)) {
            player.playSound(sound);
        }
    }
}
