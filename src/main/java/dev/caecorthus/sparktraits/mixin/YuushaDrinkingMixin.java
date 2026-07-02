package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.YuushaTraitService;
import dev.doctor4t.wathe.item.CocktailItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks Wathe drinks after Yuusha's tasteless Mankai aftereffect.
 * 在勇者“失去味觉”后遗症下阻止饮用列车饮品。
 */
@Mixin(CocktailItem.class)
public abstract class YuushaDrinkingMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void sparktraits$preventYuushaTastelessDrinking(
            World world,
            PlayerEntity user,
            Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir
    ) {
        if (!YuushaTraitService.shouldBlockEatingOrDrinking(user)) {
            return;
        }
        if (!world.isClient) {
            YuushaTraitService.notifyTasteless(user);
        }
        cir.setReturnValue(TypedActionResult.success(user.getStackInHand(hand)));
    }
}
