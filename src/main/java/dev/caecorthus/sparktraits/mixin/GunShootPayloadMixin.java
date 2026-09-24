package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.fakedeath.FakeDeathDerringerService;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.VigilanteVeteranTraitService;
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
import dev.caecorthus.sparktraits.SparkTraits;

/** Hooks SparkTraits gun behavior into Wathe's gun packet handler.
 *  将 SparkTraits 的枪械行为接入 Wathe 枪械发包处理。 */
@Mixin(value = GunShootPayload.Receiver.class, remap = false)
public abstract class GunShootPayloadMixin {
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V")
    private void sparktraits$gunCycleScope(GunShootPayload payload, ServerPlayNetworking.Context context,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        var previous = dev.caecorthus.sparktraits.impl.traits.civilian.police.GunShotCycles.current();
        dev.caecorthus.sparktraits.impl.traits.civilian.police.GunShotCycles.setCurrent(null);
        try {
            original.call(payload, context);
        } finally {
            dev.caecorthus.sparktraits.impl.traits.civilian.police.GunShotCycles.finishNative(
                    dev.caecorthus.sparktraits.impl.traits.civilian.police.GunShotCycles.current());
            dev.caecorthus.sparktraits.impl.traits.civilian.police.GunShotCycles.setCurrent(previous);
        }
    }

    /** Begin only after native packet validation; a missed accepted shot still bursts.
     * 仅在原生发包校验通过后启动；已接受的空枪仍可补射。 */
    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At(value = "INVOKE", target = "Ldev/doctor4t/wathe/record/GameRecordManager;recordItemUse(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/nbt/NbtCompound;)V")
    )
    private void sparktraits$scheduleNikoRevolverBurst(GunShootPayload payload,
            ServerPlayNetworking.Context context, CallbackInfo ci) {
        dev.caecorthus.sparktraits.impl.traits.civilian.police.GunShotCycles.begin(
                context.player(), context.player().getMainHandStack().getItem());
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
        FakeDeathDerringerService.replenishAfterHit(shooter, victim);
        VigilanteVeteranTraitService.killPlayerWithPoliceGunTraits(victim, spawnBody, shooter, deathReason);
    }

    /** Skips only the ordinary civilian-role revolver-hit mood penalty.
     *  仅跳过普通平民阵营左轮命中后的理智惩罚。 */
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
        if (CivilianTraitService.shouldPreventGunMoodPenalty(
                game.getRole(shooter),
                TraitPlayerComponent.KEY.get(shooter).getActiveTraitIds()
        )) {
            return;
        }
        moodComponent.setMood(proposedMood);
    }
}
