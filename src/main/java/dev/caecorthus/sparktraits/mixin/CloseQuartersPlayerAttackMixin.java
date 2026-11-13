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
 * Mixins apply in ascending priority and every later {@code @WrapMethod} wraps the method as the earlier ones left
 * it, so a HIGHER priority is the OUTER wrapper. Priority 1500 puts this guard outside Wathe (1000) and SparkStrength's
 * Silencer bat wrapper (1200), and inside {@link LastEscapeAttackMixin} (2000). It has to be outside Wathe: Wathe's
 * wrapper resolves a charged bat hit itself (kill, sound, return) without calling the wrapped method, so at the former
 * priority 800 the bat branch below never ran and a raised Close Quarters knife could not parry a Wathe bat.
 * SparkStrength asks the same parry rule again for its own bat kill; that is safe, because a parry consumes the raise
 * before any effect and an attempt that was not parried here is not parried there either.
 * Mixin 按优先级升序应用，后应用的 {@code @WrapMethod} 会包住先前的结果，因此优先级越高越在外层。优先级 1500 让本守卫位于
 * Wathe（1000）与 SparkStrength 静语者球棒包装（1200）之外、{@link LastEscapeAttackMixin}（2000）之内。它必须在 Wathe 之外：
 * Wathe 的包装会自行结算蓄满的球棒命中（击杀、音效、返回）而不调用被包装的方法，所以在原先的优先级 800 下，
 * 下方的球棒分支从未执行，举起的狭路相逢小刀无法格挡 Wathe 球棒。SparkStrength 会为自己的球棒击杀再次询问同一条格挡规则；
 * 这是安全的，因为格挡在产生任何效果前就已消费举刀状态，在这里未被格挡的攻击在那里同样不会被格挡。
 */
@Mixin(value = PlayerEntity.class, priority = 1500)
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
