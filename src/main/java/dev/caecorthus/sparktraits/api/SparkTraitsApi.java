package dev.caecorthus.sparktraits.api;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Stable, null-safe queries for optional downstream integrations.
 * 为可选下游集成提供稳定且支持空值的查询接口。
 */
public final class SparkTraitsApi {
    private SparkTraitsApi() {
    }

    /**
     * Returns whether the player currently owns the active trait.
     * 返回玩家当前是否拥有指定的生效天赋。
     */
    public static boolean hasActiveTrait(PlayerEntity player, Identifier traitId) {
        return player != null
                && traitId != null
                && TraitPlayerComponent.KEY.maybeGet(player)
                        .map(component -> component.hasActiveTrait(traitId))
                        .orElse(false);
    }

    /**
     * Returns whether Last Stand has already triggered for the player this round.
     * 返回该玩家的背水一战是否已经在本局触发。
     */
    public static boolean hasLastStandTriggeredThisRound(ServerWorld world, UUID playerUuid) {
        return world != null
                && playerUuid != null
                && LastStandService.hasTriggeredThisRound(world, playerUuid);
    }

    /**
     * Returns whether the world is currently in Last Stand's final moment.
     * 返回当前世界是否处于背水一战的终局时刻。
     */
    public static boolean isFinalMomentActive(World world) {
        return world != null
                && TraitWorldComponent.KEY.maybeGet(world)
                        .map(TraitWorldComponent::isFinalMomentActive)
                        .orElse(false);
    }
}
