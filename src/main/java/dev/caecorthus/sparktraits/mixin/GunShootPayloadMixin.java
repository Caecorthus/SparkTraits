package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.GoodTraitService;
import dev.caecorthus.sparktraits.impl.VigilanteVeteranTraitService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
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

    @Redirect(
            method = "receive",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/game/GameFunctions;killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;)V",
                    ordinal = 1
            )
    )
    private void sparktraits$applyHeavyArtilleryGunDamage(
            ServerPlayerEntity victim,
            boolean spawnBody,
            ServerPlayerEntity shooter,
            Identifier deathReason
    ) {
        VigilanteVeteranTraitService.killPlayerWithHeavyArtillery(victim, spawnBody, shooter, deathReason);
    }

    /** Skips only the ordinary good-role revolver-hit mood penalty.
     *  仅跳过普通好人左轮命中后的理智惩罚。 */
    @Redirect(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;setMood(F)V",
                    ordinal = 0
            )
    )
    private void sparktraits$focusPreventsOrdinaryGunMoodPenalty(
            PlayerMoodComponent moodComponent,
            float proposedMood,
            GunShootPayload payload,
            ServerPlayNetworking.Context context
    ) {
        ServerPlayerEntity shooter = context.player();
        GameWorldComponent game = GameWorldComponent.KEY.get(shooter.getWorld());
        if (GoodTraitService.shouldPreventGunMoodPenalty(
                game.getRole(shooter),
                TraitPlayerComponent.KEY.get(shooter).getActiveTraitIds()
        )) {
            return;
        }
        moodComponent.setMood(proposedMood);
    }
}
