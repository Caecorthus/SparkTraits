package dev.caecorthus.sparktraits.mixin;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.HashSet;
import java.util.UUID;

@Mixin(GameWorldComponent.class)
public interface GameWorldComponentAccessor {
    @Accessor("deadPlayers")
    HashSet<UUID> sparktraits$getDeadPlayers();

    @Accessor("preventGunPickup")
    HashSet<UUID> sparktraits$getPreventGunPickup();

    @Accessor("world")
    World sparktraits$getWorld();
}
