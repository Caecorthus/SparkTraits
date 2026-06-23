package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.doctor4t.wathe.api.Role;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

/**
 * Prevents Impostors from being treated as innocent Jester triggers.
 * 防止内鬼在击杀小丑时被 NoellesRoles 当作触发小丑时刻的好人。
 */
@Mixin(value = Noellesroles.class, remap = false)
public abstract class NoellesRolesJesterMomentMixin {
    @Redirect(
            method = "lambda$registerEvents$9",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/api/Role;isInnocent()Z"
            )
    )
    private static boolean sparktraits$impostorDoesNotTriggerJesterMoment(
            Role killerRole,
            ServerPlayerEntity victim,
            ServerPlayerEntity killer,
            Identifier deathReason
    ) {
        return EffectiveTraitService.shouldTriggerJesterMoment(
                killerRole,
                killer == null ? Set.of() : TraitPlayerComponent.KEY.get(killer).getActiveTraitIds()
        );
    }
}
