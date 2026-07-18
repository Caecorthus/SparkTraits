package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Silences the completion burp without blocking food or drink consumption.
 * 只静音吃喝完成后的打嗝声，不阻止食物或饮品的消耗逻辑。
 */
@Mixin(PlayerEntity.class)
public abstract class CautiousPlayerConsumptionSoundMixin {
    @Redirect(
            method = "eatFood",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
            )
    )
    private void sparktraits$skipCautiousBurpSound(
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
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (sound == SoundEvents.ENTITY_PLAYER_BURP
                && !SparkTraitsServerConnection.isUnconfirmedClientEntity(player)
                && GlobalTraitService.shouldSuppressCautiousSounds(player)) {
            return;
        }
        world.playSound(except, x, y, z, sound, category, volume, pitch);
    }
}
