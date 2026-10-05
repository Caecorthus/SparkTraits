package dev.caecorthus.sparktraits.impl.selection;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class TraitRules {
    private TraitRules() {
    }

    /** Unknown ids occupy a slot, so stale or foreign ids never slip past TraitPlayerComponent.MAX_TRAITS.
     *  未注册的 id 同样占用槽位，避免过期或外部 id 绕过 TraitPlayerComponent.MAX_TRAITS。 */
    public static boolean occupiesTraitSlot(Identifier traitId) {
        Trait trait = TraitRegistry.get(traitId);
        return trait == null || trait.occupiesTraitSlot();
    }

    /** Traits counted against TraitPlayerComponent.MAX_TRAITS; free traits such as Well Supplied are left out.
     *  计入 TraitPlayerComponent.MAX_TRAITS 的天赋数量；物资充沛等免费天赋不计入。 */
    public static int occupiedTraitSlots(Collection<Identifier> traitIds) {
        int occupied = 0;
        for (Identifier traitId : traitIds) {
            if (occupiesTraitSlot(traitId)) {
                occupied++;
            }
        }
        return occupied;
    }

    public static boolean isCompatibleWithAll(Trait candidate, Collection<Identifier> selectedTraitIds) {
        for (Identifier selectedTraitId : selectedTraitIds) {
            Trait selected = TraitRegistry.get(selectedTraitId);
            if (selected != null && areIncompatible(candidate, selected)) {
                return false;
            }
        }
        return true;
    }

    public static boolean areIncompatible(Trait first, Trait second) {
        return first.incompatibleTraits().contains(second.id()) || second.incompatibleTraits().contains(first.id());
    }

    /**
     * Rechecks every selected trait after the full set is known.
     * 在整组天赋确定后重新复核每个天赋，防止后选天赋改变前选天赋资格。
     */
    public static boolean canApplyAll(
            ServerWorld world,
            GameWorldComponent gameComponent,
            ServerPlayerEntity player,
            Role role,
            Collection<Identifier> traitIds
    ) {
        LinkedHashSet<Identifier> ids = new LinkedHashSet<>(traitIds);
        for (Identifier id : ids) {
            Trait trait = TraitRegistry.get(id);
            if (trait == null || !TraitRoleEligibility.canReceiveTrait(role, trait)) {
                return false;
            }

            LinkedHashSet<Identifier> others = new LinkedHashSet<>(ids);
            others.remove(id);
            if (!isCompatibleWithAll(trait, others)) {
                return false;
            }
            if (!trait.canApply(new TraitSelectionContext(world, gameComponent, player, role, Set.copyOf(others)))) {
                return false;
            }
        }
        return true;
    }
}
