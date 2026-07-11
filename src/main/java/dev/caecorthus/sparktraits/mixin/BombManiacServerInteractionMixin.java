package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyRules;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class BombManiacServerInteractionMixin {
    @Redirect(
            method = "interactItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ItemCooldownManager;isCoolingDown(Lnet/minecraft/item/Item;)Z"
            )
    )
    private boolean sparktraits$allowMarkedGrenade(
            ItemCooldownManager manager,
            Item item,
            ServerPlayerEntity player,
            World world,
            ItemStack stack,
            Hand hand
    ) {
        return ConscienceBomberFrenzyRules.shouldBlockGrenadeUse(
                ConscienceBomberFrenzyService.isMarkedGrenade(stack),
                ConscienceBomberFrenzyService.canUseMarkedGrenade(player, stack),
                manager.isCoolingDown(item)
        );
    }
}
