package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.combat.CloseQuartersService;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.ForcedMeleeCooldownService;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerVeteranComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Early locks precede replacement handlers; ordinary parry runs only after Wathe's range/player checks. */
@Mixin(value = KnifeStabPayload.Receiver.class, priority = 2200)
public abstract class CloseQuartersKnifeReceiverMixin {
    @Inject(method = "receive(Ldev/doctor4t/wathe/util/KnifeStabPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void sparktraits$guardKnifePacket(KnifeStabPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity attacker = context.player();
        Entity target = attacker.getServerWorld().getEntityById(payload.target());
        if (LastEscapeService.isActive(attacker)
                || target instanceof ServerPlayerEntity victim && LastEscapeService.isActive(victim)
                || ForcedMeleeCooldownService.remainingTicks(attacker, WatheItems.KNIFE) > 0
                || !CloseQuartersService.consumeKnifeRelease(attacker)) ci.cancel();
    }

    @Inject(method = "receive(Ldev/doctor4t/wathe/util/KnifeStabPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At(value = "FIELD", target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;KEY:Lorg/ladysnake/cca/api/v3/component/ComponentKey;"),
            cancellable = true, remap = false)
    private void sparktraits$parryAcceptedKnife(KnifeStabPayload payload, ServerPlayNetworking.Context context, CallbackInfo ci) {
        ServerPlayerEntity attacker = context.player();
        Entity target = attacker.getServerWorld().getEntityById(payload.target());
        ItemStack weapon = attacker.getMainHandStack().isOf(WatheItems.KNIFE)
                ? attacker.getMainHandStack() : attacker.getOffHandStack();
        // Only the ordinary Wathe path reaches this boundary; validated instant replacements run at HEAD.
        // 此处仅处理 Wathe 原始路径；已校验的瞬刀替换在 HEAD 接管，不套用普通冷却规则。
        if (!weapon.isOf(WatheItems.KNIFE)
                || attacker.getItemCooldownManager().isCoolingDown(weapon.getItem())) {
            ci.cancel();
            return;
        }
        if (GameWorldComponent.KEY.get(attacker.getWorld()).getRole(attacker) == WatheRoles.VETERAN
                && !PlayerVeteranComponent.KEY.get(attacker).hasStabUsesLeft()) return;
        if (target instanceof ServerPlayerEntity victim
                && CloseQuartersService.shouldCancelMeleeAttack(attacker, victim, weapon)) ci.cancel();
    }
}
