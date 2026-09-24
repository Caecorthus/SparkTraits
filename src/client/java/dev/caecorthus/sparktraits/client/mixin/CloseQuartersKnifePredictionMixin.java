package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.api.SparkTraitsApi;
import dev.caecorthus.sparktraits.client.killer.LastEscapeInput;
import dev.caecorthus.sparktraits.net.version.SparkTraitsServerConnection;
import dev.doctor4t.wathe.item.KnifeItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The common CloseQuartersKnifeItemMixin owns max-use 200 and the strict release threshold 2
 * (first valid tick 3) on BOTH sides. Do not duplicate that constant modifier here or replace
 * Wathe's release body: role-specific instant-knife handlers must keep their own semantics.
 */
@Mixin(value = KnifeItem.class, priority = 2000)
public abstract class CloseQuartersKnifePredictionMixin {
    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    private void sparktraits$discardBlockedRelease(ItemStack stack, World world, LivingEntity user,
                                                  int remainingUseTicks, CallbackInfo ci) {
        if (world.isClient && user instanceof PlayerEntity player
                && SparkTraitsServerConnection.isConfirmedServer()
                && (LastEscapeInput.blocked()
                    || SparkTraitsApi.getForcedMeleeCooldownTicks(player, stack) > 0)) {
            ci.cancel();
        }
    }
}
