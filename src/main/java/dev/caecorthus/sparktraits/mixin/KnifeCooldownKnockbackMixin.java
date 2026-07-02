package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.KnifeKnockbackService;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps Wathe's documented no-cooldown knife shove working through vanilla hurt cooldown.
 * 让 Wathe 文档中的“刀左键无冷却击退”穿过原版受伤无敌帧继续生效。
 */
@Mixin(PlayerEntity.class)
public abstract class KnifeCooldownKnockbackMixin {
    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"
            )
    )
    private boolean sparktraits$applyKnifeCooldownKnockback(Entity target, DamageSource source, float amount) {
        Vec3d previousVelocity = target.getVelocity();
        boolean damageSucceeded = target.damage(source, amount);
        PlayerEntity attacker = (PlayerEntity) (Object) this;
        if (target instanceof ServerPlayerEntity serverTarget
                && shouldApplyKnifeCooldownKnockback(attacker, serverTarget, source, amount, damageSucceeded)) {
            applyKnifeCooldownKnockback(attacker, serverTarget, previousVelocity);
        }
        return damageSucceeded;
    }

    private static boolean shouldApplyKnifeCooldownKnockback(
            PlayerEntity attacker,
            ServerPlayerEntity target,
            DamageSource source,
            float amount,
            boolean damageSucceeded
    ) {
        LivingEntityDamageCooldownAccessor cooldown = (LivingEntityDamageCooldownAccessor) target;
        return KnifeKnockbackService.shouldApplyCooldownBypassKnockback(
                damageSucceeded,
                attacker.getMainHandStack().isOf(WatheItems.KNIFE),
                GameFunctions.isPlayerPlayingAndAlive(attacker) && GameFunctions.isPlayerAliveAndSurvival(attacker),
                GameFunctions.isPlayerPlayingAndAlive(target),
                GameFunctions.isPlayerAliveAndSurvival(target),
                target.isInvulnerableTo(source),
                target.timeUntilRegen,
                amount,
                cooldown.sparktraits$getLastDamageTaken()
        );
    }

    private static void applyKnifeCooldownKnockback(
            PlayerEntity attacker,
            ServerPlayerEntity target,
            Vec3d previousVelocity
    ) {
        double strength = KnifeKnockbackService.cooldownBypassKnockbackStrength(
                attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_KNOCKBACK)
        );
        Vec3d direction = KnifeKnockbackService.knockbackDirection(attacker.getYaw());
        target.takeKnockback(strength, direction.x, direction.z);
        target.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(target));
        target.velocityModified = false;
        target.setVelocity(previousVelocity);
    }
}
