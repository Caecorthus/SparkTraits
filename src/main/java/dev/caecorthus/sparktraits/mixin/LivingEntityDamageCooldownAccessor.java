package dev.caecorthus.sparktraits.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads vanilla living-damage state for the knife knockback compatibility shim.
 * 读取原版生物伤害状态，供刀击退兼容补偿使用。
 */
@Mixin(LivingEntity.class)
public interface LivingEntityDamageCooldownAccessor {
    @Accessor("lastDamageTaken")
    float sparktraits$getLastDamageTaken();
}
