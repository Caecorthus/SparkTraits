package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraitService;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps held-weapon attribute modifiers for killer traits in step with the held item: Thrust knockback
 * and Manic bat attack speed.
 * 让杀手天赋的手持武器属性修饰随手持物品同步：突刺的击退与狂躁的球棒攻速。
 */
@Mixin(LivingEntity.class)
public abstract class KillerTraitLivingEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void sparktraits$updateHeldWeaponModifiers(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player
                && !SparkTraitsServerConnection.isUnconfirmedClientEntity(player)) {
            KillerTraitService.updateThrustKnockback(player);
            KillerTraitService.updateManicAttackSpeed(player);
        }
    }
}
