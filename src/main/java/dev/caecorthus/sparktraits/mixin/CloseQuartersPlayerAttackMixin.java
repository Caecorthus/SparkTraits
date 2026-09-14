package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.CloseQuartersService;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.ForcedMeleeCooldownService;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * MixinExtras offers higher-priority wrappers first, then wraps them with later offers.
 * Priority 800 therefore puts this guard OUTSIDE Wathe (1000) and Strength (1200).
 */
@Mixin(value = PlayerEntity.class, priority = 800)
public abstract class CloseQuartersPlayerAttackMixin {
    @WrapMethod(method = "attack")
    private void sparktraits$guardMelee(Entity target, Operation<Void> original) {
        if (!((Object) this instanceof ServerPlayerEntity attacker)) {
            original.call(target);
            return;
        }
        ItemStack weapon = attacker.getMainHandStack();
        if (LastEscapeService.isActive(attacker) || ForcedMeleeCooldownService.remainingTicks(attacker, weapon) > 0) return;
        if (!(target instanceof ServerPlayerEntity victim)) {
            original.call(target);
            return;
        }
        if (LastEscapeService.isActive(victim)) return;
        if (weapon.isOf(WatheItems.BAT) && attacker.getAttackCooldownProgress(0.5f) >= 1.0f
                && CloseQuartersService.shouldCancelMeleeAttack(attacker, victim, weapon)) return;
        if (!weapon.isOf(WatheItems.KNIFE)) {
            original.call(target);
            return;
        }
        try (CloseQuartersService.HitScope ignored = CloseQuartersService.beginKnifeHit(attacker, victim, weapon)) {
            original.call(target);
        }
    }
}
