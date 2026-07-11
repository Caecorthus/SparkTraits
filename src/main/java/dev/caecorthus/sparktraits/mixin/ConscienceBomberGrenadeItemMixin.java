package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyService;
import dev.doctor4t.wathe.entity.GrenadeEntity;
import dev.doctor4t.wathe.item.GrenadeItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayDeque;
import java.util.Deque;

/** Changes only the marked stack while delegating sound, spawn, skin, records, and stats to Wathe.
 *  仅修改已标记物品，声音、生成、皮肤、记录和统计仍由 Wathe 处理。 */
// Higher than NoellesRoles' 1100 so the wrapper restores state after its RETURN setter.
// 高于 NoellesRoles 的 1100，确保包装器在其 RETURN 写入后恢复状态。
@Mixin(value = GrenadeItem.class, priority = 1200)
public abstract class ConscienceBomberGrenadeItemMixin {
    @Unique
    private static final ThreadLocal<Deque<ConscienceBomberFrenzyService.GrenadeUseSnapshot>> sparktraits$useContexts =
            ThreadLocal.withInitial(ArrayDeque::new);

    @WrapMethod(method = "use")
    private TypedActionResult<ItemStack> sparktraits$preserveBombManiacUseState(
            World world,
            PlayerEntity user,
            Hand hand,
            Operation<TypedActionResult<ItemStack>> original
    ) {
        ItemStack stack = user.getStackInHand(hand);
        boolean marked = ConscienceBomberFrenzyService.isMarkedGrenade(stack);
        boolean bombManiac = ConscienceBomberFrenzyService.canUseMarkedGrenade(user, stack);
        if (marked && !bombManiac) {
            return TypedActionResult.fail(stack);
        }
        ConscienceBomberFrenzyService.GrenadeUseSnapshot context =
                new ConscienceBomberFrenzyService.GrenadeUseSnapshot(
                        bombManiac,
                        stack,
                        stack.getCount(),
                        bombManiac ? ConscienceBomberFrenzyService.snapshotGrenadeCooldown(user) : null
                );
        Deque<ConscienceBomberFrenzyService.GrenadeUseSnapshot> contexts = sparktraits$useContexts.get();
        contexts.push(context);
        try (ConscienceBomberFrenzyService.CooldownSyncSuppression ignored =
                     ConscienceBomberFrenzyService.suppressGrenadeCooldownSync(user, bombManiac)) {
            try {
                return original.call(world, user, hand);
            } finally {
                if (context.bombManiac()) {
                    ConscienceBomberFrenzyService.restoreGrenadeStackCount(context);
                    ConscienceBomberFrenzyService.restoreGrenadeCooldown(user, context.cooldown());
                }
            }
        } finally {
            contexts.pop();
            if (contexts.isEmpty()) {
                sparktraits$useContexts.remove();
            }
        }
    }

    @Redirect(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/entity/GrenadeEntity;setVelocity(Lnet/minecraft/entity/Entity;FFFFF)V"
            )
    )
    private void sparktraits$configureBombManiacLaunch(
            GrenadeEntity grenade,
            Entity user,
            float pitch,
            float yaw,
            float roll,
            float speed,
            float divergence
    ) {
        ConscienceBomberFrenzyService.GrenadeUseSnapshot context = sparktraits$useContexts.get().peek();
        ConscienceBomberFrenzyService.configureGrenadeLaunch(
                grenade,
                user,
                pitch,
                yaw,
                roll,
                speed,
                divergence,
                context != null && context.bombManiac()
        );
    }
}
