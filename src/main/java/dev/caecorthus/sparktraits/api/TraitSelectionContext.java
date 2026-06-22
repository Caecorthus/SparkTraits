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
        Set<Identifier> selectedTraitIds
) {
    public boolean hasSelectedTrait(Identifier id) {
        return selectedTraitIds.contains(id);
    }
}

