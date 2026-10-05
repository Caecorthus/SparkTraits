package dev.caecorthus.sparktraits.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparktraits.impl.traits.killer.combat.ForcedMeleeCooldownService;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerItemCooldownManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SparkFactionAPI's admin {@code /sparkfactionapi:clearCooldown} also lifts a forced melee floor (Close Quarters parry)
 * on the held item; otherwise {@code ExactItemCooldownMixin} cancels its {@code remove}. Only this admin path does so;
 * every other {@code remove} still keeps the floor.
 */
@Mixin(targets = "dev.caecorthus.sparkfactionapi.command.admin.CooldownCommand", remap = false)
public abstract class SparkFactionClearCooldownMixin {
    @WrapOperation(method = "clearCooldown", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/ItemCooldownManager;remove(Lnet/minecraft/item/Item;)V", remap = true),
            require = 1)
    private static void sparktraits$liftForcedMeleeFloor(ItemCooldownManager manager, Item item, Operation<Void> original) {
        if (manager instanceof ServerItemCooldownManager server) {
            ForcedMeleeCooldownService.clearItem(((ServerItemCooldownManagerAccessor) server).sparktraits$getPlayer(), item);
        }
        original.call(manager, item);
    }
}
