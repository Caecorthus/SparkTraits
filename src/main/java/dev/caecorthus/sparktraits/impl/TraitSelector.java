package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Random trait selector for the three independent 25% slots.
 * 三个独立 25% 槽位的随机天赋选择器。
 */
public final class TraitSelector {
    public static final int SLOT_COUNT = 3;
    public static final float SLOT_CHANCE = 0.25f;

    private TraitSelector() {
    }

    public static List<Identifier> selectRandomTraits(
            ServerWorld world,
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            ServerPlayerEntity player,
            RandomGenerator random
    ) {
        LinkedHashSet<Identifier> selected = new LinkedHashSet<>();
        Role role = gameComponent.getRole(player);

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (selected.size() >= TraitPlayerComponent.MAX_TRAITS || random.nextFloat() >= SLOT_CHANCE) {
                continue;
            }

            List<Trait> candidates = collectCandidates(world, gameComponent, traitWorld, player, role, selected);
            if (candidates.isEmpty()) {
                continue;
            }

            Trait picked = pickWeighted(candidates, random);
            selected.add(picked.id());
            if (picked.uniquePerGame()) {
                traitWorld.markUniqueTraitUsed(picked.id());
            }
        }

        return List.copyOf(selected);
    }

    private static List<Trait> collectCandidates(
            ServerWorld world,
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            ServerPlayerEntity player,
            Role role,
            LinkedHashSet<Identifier> selected
    ) {
        List<Trait> candidates = new ArrayList<>();
        TraitSelectionContext context = new TraitSelectionContext(world, gameComponent, player, role, selected);
        for (Trait trait : TraitRegistry.values()) {
            if (trait.weight() <= 0) {
                continue;
            }
            if (!traitWorld.isTraitEnabled(trait.id())) {
                continue;
            }
            if (selected.contains(trait.id())) {
                continue;
            }
            if (trait.uniquePerGame() && traitWorld.isUniqueTraitUsed(trait.id())) {
                continue;
            }
            if (!TraitRules.isCompatibleWithAll(trait, selected)) {
                continue;
            }
            if (!trait.canApply(context)) {
                continue;
            }
            candidates.add(trait);
        }
        return candidates;
    }

    private static Trait pickWeighted(List<Trait> candidates, RandomGenerator random) {
        int totalWeight = 0;
        for (Trait candidate : candidates) {
            totalWeight += candidate.weight();
        }

        int roll = random.nextInt(totalWeight);
        for (Trait candidate : candidates) {
            roll -= candidate.weight();
            if (roll < 0) {
                return candidate;
            }
        }
        return candidates.getLast();
    }
}
