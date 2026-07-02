package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.YuushaTraitService;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks food after Yuusha's tasteless Mankai aftereffect.
 * 在勇者“失去味觉”后遗症下阻止进食。
 */
@Mixin(Item.class)
public abstract class YuushaEatingMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void sparktraits$preventYuushaTastelessEating(
            World world,
            PlayerEntity user,
            Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir
    ) {
        ItemStack stack = user.getStackInHand(hand);
        if (stack.get(DataComponentTypes.FOOD) == null || !YuushaTraitService.shouldBlockEatingOrDrinking(user)) {
            return;
        }
        if (!world.isClient) {
            YuushaTraitService.notifyTasteless(user);
        }
        cir.setReturnValue(TypedActionResult.success(stack));
    }
}
