package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.ImpostorTimekeeperService;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Reverses Timekeeper's reduce-time purchase for Impostors.
 *  让内鬼计时员购买减少时间时改为增加时间。 */
@Mixin(targets = "org.agmas.noellesroles.timekeeper.TimekeeperShopHandler$1", remap = false)
public abstract class TimekeeperShopHandlerMixin {
    @Redirect(
            method = "onBuy",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/cca/GameTimeComponent;addTime(I)V")
    )
    private void sparktraits$impostorTimekeeperAddsTime(GameTimeComponent timeComponent, int originalDeltaTicks, PlayerEntity buyer) {
        timeComponent.addTime(ImpostorTimekeeperService.timekeeperPurchaseDelta(buyer, originalDeltaTicks));
    }
}
