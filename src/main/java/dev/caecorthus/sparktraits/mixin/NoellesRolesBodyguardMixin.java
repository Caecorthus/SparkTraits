package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorBodyguardService;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Prevents Impostor Bodyguards from sacrificing themselves for their target.
 *  防止内鬼保镖为保护目标替死。 */
@Mixin(value = Noellesroles.class, remap = false)
public abstract class NoellesRolesBodyguardMixin {
    @Redirect(
            // NoellesRoles 1.7.6 renumbered this KillPlayer listener from $9 to $5.
            // NoellesRoles 1.7.6 将这个 KillPlayer 监听器从 $9 重新编号为 $5。
            method = {"lambda$registerEvents$9", "lambda$registerEvents$5"},
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/game/GameFunctions;isPlayerPlayingAndAlive(Lnet/minecraft/entity/player/PlayerEntity;)Z",
                    ordinal = 0
            )
    )
    private static boolean sparktraits$impostorBodyguardDoesNotSacrifice(PlayerEntity bodyguard) {
        return GameFunctions.isPlayerPlayingAndAlive(bodyguard) && ImpostorBodyguardService.shouldProtectTarget(bodyguard);
    }
}
