package dev.caecorthus.sparktraits.mixin.yuusha.mixin;

import dev.caecorthus.sparktraits.yuusha.YuushaTrait;
import dev.caecorthus.sparktraits.yuusha.component.YuushaComponents;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.doctor4t.wathe.cca.PlayerMoodComponent")
public abstract class YuushaPlayerMoodComponentMixin {
    @Shadow(remap = false) @Final private PlayerEntity player;

    @Inject(method = "eatFood", at = @At("HEAD"), cancellable = true, remap = false)
    private void sparktraits$blockFoodSanRecovery(CallbackInfo ci) {
        if (YuushaComponents.YUUSHA.get(player).tasteLossCost()) {
            player.sendMessage(YuushaTrait.t("message.sparktraits.hero.taste_lost"), true);
            ci.cancel();
        }
    }

    @Inject(method = "drinkCocktail", at = @At("HEAD"), cancellable = true, remap = false)
    private void sparktraits$blockDrinkSanRecovery(CallbackInfo ci) {
        if (YuushaComponents.YUUSHA.get(player).tasteLossCost()) {
            player.sendMessage(YuushaTrait.t("message.sparktraits.hero.taste_lost"), true);
            ci.cancel();
        }
    }
}
