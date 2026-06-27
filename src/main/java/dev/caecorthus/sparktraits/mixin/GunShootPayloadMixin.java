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
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks SparkTraits gun behavior into Wathe's gun packet handler.
 *  将 SparkTraits 的枪械行为接入 Wathe 枪械发包处理。 */
@Mixin(value = GunShootPayload.Receiver.class, remap = false)
public abstract class GunShootPayloadMixin {
    /** Starts Niko's extra shots before target resolution, so missed first shots still audibly burst.
     *  在目标结算前启动 Niko 补发，让第一发没命中时也能表现为连续射击。 */
    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD")
    )
    private void sparktraits$scheduleNikoRevolverBurst(
            GunShootPayload payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        VigilanteVeteranTraitService.scheduleNikoRevolverBurstRepeats(context.player());
    }

    /** Extends only Niko crouch gun validation; other roles and traits keep Wathe's original cap.
     *  只放宽 Niko 蹲下枪械的服务端距离校验，其他角色和天赋保留 Wathe 原始上限。 */
    @ModifyConstant(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            constant = @Constant(doubleValue = 65.0, ordinal = 0)
    )
    private double sparktraits$extendNikoServerGunRange(
            double range,
            GunShootPayload payload,
            ServerPlayNetworking.Context context
    ) {
        ServerPlayerEntity shooter = context.player();
        return VigilanteVeteranTraitService.serverGunTargetRange(
                range,
                shooter,
                shooter.getMainHandStack().getItem()
        );
    }

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
        VigilanteVeteranTraitService.killPlayerWithPoliceGunTraits(victim, spawnBody, shooter, deathReason);
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
