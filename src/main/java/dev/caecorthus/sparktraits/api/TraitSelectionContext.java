package dev.caecorthus.sparktraits.api;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Set;

public record TraitSelectionContext(
        ServerWorld world,
        GameWorldComponent gameComponent,
        ServerPlayerEntity player,
        Role role,
        Set<Identifier> selectedTraitIds,
        int startingPlayerCount,
        boolean enforceStartingPlayerCount
) {
    public TraitSelectionContext(
            ServerWorld world,
            GameWorldComponent gameComponent,
            ServerPlayerEntity player,
            Role role,
            Set<Identifier> selectedTraitIds
    ) {
        this(world, gameComponent, player, role, selectedTraitIds, 0, false);
    }

    public boolean hasSelectedTrait(Identifier id) {
        return selectedTraitIds.contains(id);
    }
}
