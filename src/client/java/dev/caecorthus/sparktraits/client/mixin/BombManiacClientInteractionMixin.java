package dev.caecorthus.sparktraits.client.mixin;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceBomberFrenzyRules;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class BombManiacClientInteractionMixin {
    @Redirect(
            method = "method_41929(Lnet/minecraft/util/Hand;Lnet/minecraft/entity/player/PlayerEntity;Lorg/apache/commons/lang3/mutable/MutableObject;I)Lnet/minecraft/network/packet/Packet;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ItemCooldownManager;isCoolingDown(Lnet/minecraft/item/Item;)Z"
            )
    )
    private boolean sparktraits$allowMarkedGrenadePrediction(
            ItemCooldownManager manager,
            Item item,
            Hand hand,
            PlayerEntity player,
            MutableObject<?> result,
            int sequence
    ) {
        ItemStack stack = player.getStackInHand(hand);
        return ConscienceBomberFrenzyRules.shouldBlockGrenadeUse(
                ConscienceBomberFrenzyService.isMarkedGrenade(stack),
                ConscienceBomberFrenzyService.canUseMarkedGrenade(player, stack),
                manager.isCoolingDown(item)
        );
    }
}
