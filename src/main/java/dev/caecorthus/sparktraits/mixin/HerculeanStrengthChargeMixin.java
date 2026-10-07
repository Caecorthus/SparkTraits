package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.HerculeanStrengthService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Shortens the release charge of tagged throwables on both sides so prediction matches the server.
 * 在两端缩短已标记投掷物的松手蓄力，使客户端预测与服务端一致。
 */
@Mixin(ItemStack.class)
public abstract class HerculeanStrengthChargeMixin {
    @ModifyVariable(method = "onStoppedUsing", at = @At("HEAD"), argsOnly = true)
    private int sparktraits$shortenThrowCharge(int remainingUseTicks, World world, LivingEntity user) {
        return HerculeanStrengthService.remainingUseTicksAtRelease((ItemStack) (Object) this, user, remainingUseTicks);
    }
}
