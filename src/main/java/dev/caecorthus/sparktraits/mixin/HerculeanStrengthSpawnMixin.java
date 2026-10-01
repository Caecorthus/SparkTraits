package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.HerculeanStrengthService;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Boosts Herculean Strength throws at spawn, after every item has set its own launch velocity.
 * 在生成时放大力大无穷的投掷初速，此时各物品已写入自己的初速。
 */
@Mixin(ServerWorld.class)
public abstract class HerculeanStrengthSpawnMixin {
    @Inject(method = "spawnEntity", at = @At("HEAD"))
    private void sparktraits$boostThrownProjectile(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        HerculeanStrengthService.boostThrownProjectile(entity);
    }
}
