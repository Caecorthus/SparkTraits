package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.combat.ExactItemCooldowns;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.ForcedMeleeCooldownService;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerItemCooldownManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Operates below HEAD cooldown modifiers, including NoellesRoles' Stimulation reduction. */
@Mixin(ItemCooldownManager.class)
public abstract class ExactItemCooldownMixin {
    @ModifyArgs(method = "set", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/ItemCooldownManager$Entry;<init>(II)V"))
    private void sparktraits$exactEntryDuration(Args args, Item item, int duration) {
        ItemCooldownManager manager = (ItemCooldownManager) (Object) this;
        ServerPlayerEntity owner = manager instanceof ServerItemCooldownManager
                ? ((ServerItemCooldownManagerAccessor) manager).sparktraits$getPlayer() : null;
        int start = args.get(0);
        int end = args.get(1);
        args.set(1, start + ExactItemCooldowns.durationForWrite(manager, item, end - start, owner));
    }

    @ModifyArg(method = "set", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/ItemCooldownManager;onCooldownUpdate(Lnet/minecraft/item/Item;I)V"), index = 1)
    private int sparktraits$syncExactDuration(Item item, int duration) {
        return ExactItemCooldowns.remainingTicks((ItemCooldownManager) (Object) this, item);
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void sparktraits$preserveForcedFloor(Item item, CallbackInfo ci) {
        if ((Object) this instanceof ServerItemCooldownManager manager) {
            ServerPlayerEntity owner = ((ServerItemCooldownManagerAccessor) manager).sparktraits$getPlayer();
            int floor = ForcedMeleeCooldownService.remainingTicks(owner, item);
            if (floor > 0) {
                ExactItemCooldowns.setExact(owner, item,
                        Math.max(floor, ExactItemCooldowns.remainingTicks(manager, item)));
                ci.cancel();
            }
        }
    }
}
