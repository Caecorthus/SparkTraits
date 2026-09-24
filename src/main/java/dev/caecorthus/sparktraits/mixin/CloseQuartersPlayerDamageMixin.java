package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.combat.CloseQuartersService;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only the ordinary knife's accepted vanilla damage can consume a stance; immune shoves cannot. */
@Mixin(PlayerEntity.class)
public abstract class CloseQuartersPlayerDamageMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void sparktraits$parryKnifeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayerEntity victim)
                || !(source.getAttacker() instanceof ServerPlayerEntity attacker)
                || source.getSource() != attacker || amount <= 0 || victim.isInvulnerableTo(source)
                || !victim.isAlive()) return;
        LivingEntityDamageCooldownAccessor cooldown = (LivingEntityDamageCooldownAccessor) victim;
        if (victim.timeUntilRegen > 10 && amount <= cooldown.sparktraits$getLastDamageTaken()) return;
        if (CloseQuartersService.parryScopedKnifeDamage(victim, attacker)) cir.setReturnValue(false);
    }
}
