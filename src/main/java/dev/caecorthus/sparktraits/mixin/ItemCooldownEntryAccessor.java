package dev.caecorthus.sparktraits.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface ItemCooldownEntryAccessor {
    @Accessor("endTick")
    int sparktraits$getEndTick();
}
