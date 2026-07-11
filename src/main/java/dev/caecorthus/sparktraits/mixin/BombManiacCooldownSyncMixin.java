package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyService;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerItemCooldownManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses only transient marked-grenade cooldown packets while local state is restored.
 *  仅在本地状态恢复期间抑制已标记手雷产生的临时冷却数据包。 */
@Mixin(ServerItemCooldownManager.class)
public abstract class BombManiacCooldownSyncMixin {
    @Shadow
    @Final
    private ServerPlayerEntity player;

    @Inject(
            method = "onCooldownUpdate(Lnet/minecraft/item/Item;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sparktraits$suppressMarkedGrenadeCooldownSet(Item item, int duration, CallbackInfo ci) {
        if (ConscienceBomberFrenzyService.shouldSuppressGrenadeCooldownSync(player, item)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onCooldownUpdate(Lnet/minecraft/item/Item;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sparktraits$suppressMarkedGrenadeCooldownRemoval(Item item, CallbackInfo ci) {
        if (ConscienceBomberFrenzyService.shouldSuppressGrenadeCooldownSync(player, item)) {
            ci.cancel();
        }
    }
}
