package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.CautiousTrait;
import dev.caecorthus.sparktraits.impl.GlobalTraitService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Silences vanilla consumption feedback without blocking the consumed item logic.
 * 静音原版消耗反馈，但不阻止物品消耗和任务/毒物逻辑。
 */
@Mixin(LivingEntity.class)
public abstract class CautiousLivingEntitySoundMixin {
    @Redirect(
            method = "spawnConsumptionEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;playSound(Lnet/minecraft/sound/SoundEvent;FF)V"
            )
    )
    private void sparktraits$skipCautiousConsumptionSound(LivingEntity entity, SoundEvent sound, float volume, float pitch) {
        if (entity instanceof PlayerEntity player && GlobalTraitService.hasTrait(player, CautiousTrait.ID)) {
            return;
        }
        entity.playSound(sound, volume, pitch);
    }

    @Redirect(
            method = "eatFood",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
            )
    )
    private void sparktraits$skipCautiousEatFoodSound(
            World world,
            PlayerEntity except,
            double x,
            double y,
            double z,
            SoundEvent sound,
            SoundCategory category,
            float volume,
            float pitch
    ) {
        if ((Object) this instanceof PlayerEntity player && GlobalTraitService.hasTrait(player, CautiousTrait.ID)) {
            return;
        }
        world.playSound(except, x, y, z, sound, category, volume, pitch);
    }
}
