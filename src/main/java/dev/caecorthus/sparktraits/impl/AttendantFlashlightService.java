package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.RoleEnhancementPlayerComponent;
import dev.caecorthus.sparktraits.item.SparkTraitsItems;
import dev.doctor4t.wathe.api.event.BlackoutEffect;
import dev.doctor4t.wathe.api.event.ResetPlayer;
import dev.doctor4t.wathe.api.event.RoleAssigned;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;

public final class AttendantFlashlightService {
    public static final double MAX_RANGE_BLOCKS = 30.0;

    private AttendantFlashlightService() {
    }

    public static void register() {
        RoleAssigned.EVENT.register((player, role) -> {
            RoleEnhancementPlayerComponent.KEY.get(player).setFlashlightOn(false);
            if (Noellesroles.ATTENDANT.equals(role)) {
                player.giveItemStack(SparkTraitsItems.FLASHLIGHT.getDefaultStack());
            }
        });
        ResetPlayer.EVENT.register(player -> RoleEnhancementPlayerComponent.KEY.get(player).setFlashlightOn(false));
        BlackoutEffect.BEFORE.register((player, durationTicks) ->
                shouldCancelBlackoutBlindness(player) ? BlackoutEffect.BlackoutResult.cancel() : null);
    }

    public static boolean shouldCancelBlackoutBlindness(boolean attendant, boolean alive, boolean flashlightOn) {
        return attendant && alive && flashlightOn;
    }

    public static boolean isAttendant(PlayerEntity player) {
        return player != null && GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.ATTENDANT);
    }

    public static boolean shouldCancelBlackoutBlindness(ServerPlayerEntity player) {
        return shouldCancelBlackoutBlindness(
                isAttendant(player),
                GameFunctions.isPlayerPlayingAndAlive(player),
                RoleEnhancementPlayerComponent.KEY.get(player).isFlashlightOn()
        );
    }

    public static boolean toggleFlashlight(ServerPlayerEntity player) {
        if (!isAttendant(player) || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return false;
        }
        RoleEnhancementPlayerComponent component = RoleEnhancementPlayerComponent.KEY.get(player);
        boolean enabled = !component.isFlashlightOn();
        component.setFlashlightOn(enabled);
        if (enabled) {
            player.removeStatusEffect(StatusEffects.BLINDNESS);
        }
        return true;
    }
}
