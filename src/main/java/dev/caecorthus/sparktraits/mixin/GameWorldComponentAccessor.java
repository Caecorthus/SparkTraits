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

    @Accessor("world")
    World sparktraits$getWorld();

    /** Raw write without {@code setGameStatus}'s sync; only {@code PlanningRoundView} uses it. 不经同步的原始写入，仅供 PlanningRoundView 使用。 */
    @Accessor("gameStatus")
    void sparktraits$setGameStatusField(GameWorldComponent.GameStatus status);
}
