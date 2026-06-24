package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies effective alignment to Wathe's innocent-shot victim check.
 *  让 Wathe 枪击“目标是否好人”的检查使用有效阵营。 */
@Mixin(value = GunShootPayload.Receiver.class, remap = false)
public abstract class GunShootPayloadMixin {
    @Redirect(
            method = "receive",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isInnocent(Lnet/minecraft/entity/player/PlayerEntity;)Z",
                    ordinal = 0
            )
    )
    private boolean sparktraits$treatEffectiveCivilianGunVictimAsInnocent(GameWorldComponent game, PlayerEntity victim) {
        return EffectiveTraitService.shouldTreatGunVictimAsInnocent(
                game.getRole(victim),
                TraitPlayerComponent.KEY.get(victim).getActiveTraitIds()
        );
    }
}
