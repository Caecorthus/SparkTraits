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
 * Random trait selector for the three independent configurable slots.
 * 三个独立、概率可配置的随机天赋槽位选择器。
 */
public final class TraitSelector {
    public static final int SLOT_COUNT = 3;
    public static final float DEFAULT_SLOT_CHANCE = TraitSlotRollChance.DEFAULT;
    @Deprecated(forRemoval = false)
    public static final float SLOT_CHANCE = DEFAULT_SLOT_CHANCE;

    private TraitSelector() {
    }

    public static List<Identifier> selectRandomTraits(
            ServerWorld world,
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            ServerPlayerEntity player,
            RandomGenerator random,
            int startingPlayerCount
    ) {
        LinkedHashSet<Identifier> selected = new LinkedHashSet<>();
        Role role = gameComponent.getRole(player);
        if (!TraitRoleEligibility.canReceiveTraits(role)) {
            return List.of();
        }
        float slotChance = traitWorld.getTraitSlotRollChance();

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (selected.size() >= TraitPlayerComponent.MAX_TRAITS || !shouldRollSlot(slotChance, random)) {
                continue;
            }

            List<Trait> candidates = collectCandidates(world, gameComponent, traitWorld, player, role, selected, startingPlayerCount);
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

    static boolean shouldRollSlot(float slotChance, RandomGenerator random) {
        return random.nextFloat() < TraitSlotRollChance.normalize(slotChance);
    }

    private static List<Trait> collectCandidates(
            ServerWorld world,
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            ServerPlayerEntity player,
            Role role,
            LinkedHashSet<Identifier> selected,
            int startingPlayerCount
    ) {
        List<Trait> candidates = new ArrayList<>();
        TraitSelectionContext context = new TraitSelectionContext(world, gameComponent, player, role, selected, startingPlayerCount, true);
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
            LinkedHashSet<Identifier> tentative = new LinkedHashSet<>(selected);
            tentative.add(trait.id());
            if (!TraitRules.canApplyAll(world, gameComponent, player, role, tentative)) {
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
