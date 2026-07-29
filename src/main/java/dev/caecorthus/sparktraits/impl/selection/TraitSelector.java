package dev.caecorthus.sparktraits.impl.selection;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * Random trait selector for the three independent configurable slots.
 * 三个独立、概率可配置的随机天赋槽位选择器。
 */
public final class TraitSelector {
    public static final int SLOT_COUNT = 3;
    public static final float DEFAULT_SLOT_CHANCE = TraitSlotRollChance.DEFAULT;
    private static final double FACTION_SCOPED_ROLL_MULTIPLIER = 1.5D;
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
        return selectRandomTraits(world, gameComponent, traitWorld, player, random, startingPlayerCount, List.of());
    }

    public static List<Identifier> selectRandomTraits(
            ServerWorld world,
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            ServerPlayerEntity player,
            RandomGenerator random,
            int startingPlayerCount,
            Collection<Identifier> reservedUniqueTraits
    ) {
        return selectRandomTraits(
                world,
                gameComponent,
                traitWorld,
                player,
                random,
                startingPlayerCount,
                List.of(),
                reservedUniqueTraits,
                Set.of()
        );
    }

    /** Redraws through the standard slot pipeline while excluding narrowly forbidden traits.
     *  通过标准槽位流程重抽，同时排除当前流程明确禁止的天赋。 */
    public static List<Identifier> selectRandomTraits(
            ServerWorld world,
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            ServerPlayerEntity player,
            RandomGenerator random,
            int startingPlayerCount,
            Collection<Identifier> reservedUniqueTraits,
            Collection<Identifier> excludedTraits
    ) {
        return selectRandomTraits(
                world,
                gameComponent,
                traitWorld,
                player,
                random,
                startingPlayerCount,
                List.of(),
                reservedUniqueTraits,
                excludedTraits
        );
    }

    public static List<Identifier> selectRandomTraits(
            ServerWorld world,
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            ServerPlayerEntity player,
            RandomGenerator random,
            int startingPlayerCount,
            Collection<Identifier> retainedTraits,
            Collection<Identifier> reservedUniqueTraits,
            Collection<Identifier> excludedTraits
    ) {
        LinkedHashSet<Identifier> selected = new LinkedHashSet<>(retainedTraits == null ? List.of() : retainedTraits);
        Role role = gameComponent.getRole(player);
        if (!TraitRoleEligibility.canReceiveTraits(role)) {
            return List.of();
        }
        float slotChance = traitWorld.getTraitSlotRollChance();
        Collection<Identifier> uniqueTraitReservations = reservedUniqueTraits == null ? List.of() : reservedUniqueTraits;
        Collection<Identifier> rerollExclusions = excludedTraits == null ? Set.of() : excludedTraits;

        return rollTraits(
                selected,
                slotChance,
                random,
                currentSelection -> {
                    List<Trait> candidates = collectCandidates(
                            world,
                            gameComponent,
                            traitWorld,
                            player,
                            role,
                            currentSelection,
                            startingPlayerCount,
                            uniqueTraitReservations,
                            rerollExclusions
                    );
                    return candidates.isEmpty() ? null : pickWeighted(candidates, random).id();
                }
        );
    }

    static List<Identifier> rollTraits(
            LinkedHashSet<Identifier> selected,
            float slotChance,
            RandomGenerator random,
            Function<LinkedHashSet<Identifier>, Identifier> candidatePicker
    ) {
        List<Identifier> newlyRolled = new ArrayList<>();
        for (int slot = selected.size(); slot < SLOT_COUNT; slot++) {
            if (!canSelectAnotherTrait(selected.size()) || !shouldRollSlot(slotChance, random)) {
                continue;
            }
            Identifier picked = candidatePicker.apply(selected);
            if (picked != null && selected.add(picked)) {
                newlyRolled.add(picked);
            }
        }
        return List.copyOf(newlyRolled);
    }

    static boolean isExcluded(Identifier traitId, Collection<Identifier> excludedTraits) {
        return excludedTraits != null && excludedTraits.contains(traitId);
    }

    static boolean canSelectAnotherTrait(int selectedCount) {
        return selectedCount < TraitPlayerComponent.MAX_TRAITS;
    }

    static boolean shouldRollSlot(float slotChance, RandomGenerator random) {
        return random.nextFloat() < TraitSlotRollChance.normalize(slotChance);
    }

    /**
     * Changes random-choice weight only; the public base weight remains unchanged.
     * 仅调整随机候选权重，不改变公开基础权重。
     */
    static double randomSelectionWeight(Trait trait) {
        double baseWeight = trait.rollWeight();
        return trait.audience() == TraitAudience.UNIVERSAL
                ? baseWeight
                : baseWeight * FACTION_SCOPED_ROLL_MULTIPLIER;
    }

    private static List<Trait> collectCandidates(
            ServerWorld world,
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            ServerPlayerEntity player,
            Role role,
            LinkedHashSet<Identifier> selected,
            int startingPlayerCount,
            Collection<Identifier> reservedUniqueTraits,
            Collection<Identifier> excludedTraits
    ) {
        TraitSelectionContext context = new TraitSelectionContext(
                world,
                gameComponent,
                player,
                role,
                selected,
                startingPlayerCount,
                true
        );
        return collectEligibleCandidates(
                TraitRegistry.values(),
                context,
                selected,
                reservedUniqueTraits,
                excludedTraits,
                traitWorld::isTraitEnabled,
                traitWorld::isUniqueTraitUsed
        );
    }

    static List<Trait> collectEligibleCandidates(
            Collection<Trait> traits,
            TraitSelectionContext context,
            Collection<Identifier> selectedTraits,
            Collection<Identifier> reservedUniqueTraits,
            Collection<Identifier> excludedTraits,
            Predicate<Identifier> enabled,
            Predicate<Identifier> uniqueAlreadyUsed
    ) {
        List<Trait> candidates = new ArrayList<>();
        Collection<Identifier> selected = selectedTraits == null ? List.of() : selectedTraits;
        Collection<Identifier> reservations = reservedUniqueTraits == null ? List.of() : reservedUniqueTraits;
        Collection<Identifier> exclusions = excludedTraits == null ? List.of() : excludedTraits;
        for (Trait trait : traits) {
            if (trait == null || trait.rollWeight() <= 0.0D) {
                continue;
            }
            if (!enabled.test(trait.id()) || exclusions.contains(trait.id())) {
                continue;
            }
            if (selected.contains(trait.id())) {
                continue;
            }
            if (trait.uniquePerGame()
                    && (uniqueAlreadyUsed.test(trait.id()) || reservations.contains(trait.id()))) {
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
            if (!TraitRules.canApplyAll(
                    context.world(),
                    context.gameComponent(),
                    context.player(),
                    context.role(),
                    tentative
            )) {
                continue;
            }
            candidates.add(trait);
        }
        return candidates;
    }

    static Trait pickWeighted(List<Trait> candidates, RandomGenerator random) {
        double totalWeight = 0.0D;
        for (Trait candidate : candidates) {
            totalWeight += randomSelectionWeight(candidate);
        }

        double roll = random.nextDouble() * totalWeight;
        for (Trait candidate : candidates) {
            roll -= randomSelectionWeight(candidate);
            if (roll < 0.0D) {
                return candidate;
            }
        }
        return candidates.getLast();
    }
}
