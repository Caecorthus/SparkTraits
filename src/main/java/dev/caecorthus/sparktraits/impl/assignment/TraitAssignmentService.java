package dev.caecorthus.sparktraits.impl.assignment;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAssignmentReason;
import dev.caecorthus.sparktraits.api.TraitRegistry;
import dev.caecorthus.sparktraits.api.TraitSelectionContext;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.RoleSelectionContext;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.RoleHistoryComponent;
import dev.doctor4t.wathe.game.rotation.RoleCategory;
import dev.doctor4t.wathe.game.rotation.RoleRotation;
import dev.doctor4t.wathe.game.rotation.RotationStrength;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.selection.TraitRoleEligibility;
import dev.caecorthus.sparktraits.impl.selection.TraitRules;
import dev.caecorthus.sparktraits.impl.selection.TraitSelector;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceSerialKillerService;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraits;
import dev.caecorthus.sparktraits.impl.traits.killer.escape.LastEscapeService;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.PoliceRoleCategory;
import dev.caecorthus.sparktraits.impl.traits.global.pig.PigTrait;
import dev.caecorthus.sparktraits.impl.traits.global.pig.PigTraitService;

/**
 * Plans round traits once Wathe has picked every role but before RoleAssigned announces them, then commits the
 * plan after RoleAssigned and before the welcome screen. Conscience compensation rewrites a civilian's role inside
 * the planning step, so RoleAssigned, every role kit and the role history only ever see the final killer role.
 * 在 Wathe 选定全部身份之后、RoleAssigned 公布之前规划本局天赋，并在 RoleAssigned 之后、开局欢迎信息之前提交方案。
 * 善良补偿在规划阶段改写好人身份，因此 RoleAssigned、各职业开局道具与身份历史都只会看到最终的杀手身份。
 */
public final class TraitAssignmentService {
    private static final Set<Identifier> CONSCIENCE_COMPENSATION_REROLL_EXCLUSIONS =
            Set.of(ConscienceTrait.ID, ImpostorTrait.ID);
    /** Slot limit for re-filtering traits a player already owns. / 复核玩家已拥有天赋时不设槽位上限。 */
    static final int UNCAPPED = Integer.MAX_VALUE;

    private TraitAssignmentService() {
    }

    /**
     * Must run before Wathe's RoleAssigned loop: the compensation civilian becomes a killer here, so it never
     * receives the civilian kit (from any mod) and Wathe hands out the killer kit exactly once.
     * 必须在 Wathe 的 RoleAssigned 循环之前运行：补偿好人在此转为杀手，因此不会领到任何模组的原好人道具，
     * Wathe 也只发放一次杀手道具。
     */
    public static RoundPlan planBeforeRoleAssigned(
            ServerWorld world,
            GameWorldComponent gameComponent,
            List<ServerPlayerEntity> players,
            int publicKillerCount,
            Set<UUID> lockedRolePlayers
    ) {
        Set<UUID> protectedLockedRolePlayers = lockedRolePlayers == null ? Set.of() : lockedRolePlayers;
        TraitWorldComponent traitWorld = TraitWorldComponent.KEY.get(world);
        traitWorld.clearRoundState();
        Random random = new Random(world.getRandom().nextLong());
        List<PlayerPlan> plans = new ArrayList<>();
        List<ServerPlayerEntity> randomPlayers = new ArrayList<>();
        LinkedHashSet<Identifier> randomUniqueTraitReservations = new LinkedHashSet<>();
        int lastEscapeCap = LastEscapeService.holderCap(publicKillerCount);

        for (ServerPlayerEntity player : players) {
            TraitPlayerComponent playerTraits = TraitPlayerComponent.KEY.get(player);
            List<Identifier> pendingTraits = new ArrayList<>(playerTraits.getPendingTraitIds());
            if (!pendingTraits.isEmpty()) {
                plans.add(new PlayerPlan(player, applyPendingLocks(world, gameComponent, player, pendingTraits), List.of()));
                playerTraits.clearPendingTraits();
                continue;
            }
            randomPlayers.add(player);
        }

        Collections.shuffle(randomPlayers, random);
        for (ServerPlayerEntity player : randomPlayers) {

            List<Identifier> randomTraits = TraitSelector.selectRandomTraits(
                    world,
                    gameComponent,
                    traitWorld,
                    player,
                    random,
                    players.size(),
                    randomUniqueTraitReservations,
                    excludeLastEscapeAtCap(Set.of(), plans, lastEscapeCap)
            );
            reserveUniqueTraits(randomUniqueTraitReservations, randomTraits);
            plans.add(new PlayerPlan(player, List.of(), randomTraits));
        }

        enforceUniqueTraitLimits(plans);
        if (containsTrait(plans, ImpostorTrait.ID) && !containsTrait(plans, ConscienceTrait.ID)) {
            forceConscienceOntoEligibleKiller(gameComponent, traitWorld, plans, random);
        }
        recalibrateUniqueTraitReservations(randomUniqueTraitReservations, plans);

        addExtraKillersForConscience(
                world,
                gameComponent,
                traitWorld,
                players,
                plans,
                publicKillerCount,
                protectedLockedRolePlayers,
                random,
                randomUniqueTraitReservations,
                lastEscapeCap
        );
        forcePigOntoPigGod(gameComponent, traitWorld, plans);
        enforceRandomDepressionCap(plans, players.size());
        return new RoundPlan(plans);
    }

    /**
     * Commits after RoleAssigned so trait hooks such as Conscience's walkie-talkie strip see the kits it handed out.
     * 在 RoleAssigned 之后提交，使善良收走对讲机等天赋钩子能处理它发放的道具。
     */
    public static int applyAfterRoleAssigned(
            ServerWorld world,
            GameWorldComponent gameComponent,
            List<ServerPlayerEntity> players,
            RoundPlan roundPlan
    ) {
        TraitWorldComponent traitWorld = TraitWorldComponent.KEY.get(world);
        for (PlayerPlan plan : conscienceFirst(roundPlan.plans())) {
            markUniqueTraits(traitWorld, plan.traits());
            TraitAssignmentReason reason = plan.hasLocks() ? TraitAssignmentReason.PENDING_LOCK : TraitAssignmentReason.RANDOM;
            TraitPlayerComponent playerTraits = TraitPlayerComponent.KEY.get(plan.player());
            playerTraits.setActiveTraits(plan.traits(), reason);
            traitWorld.snapshotRoundTraits(plan.player().getUuid(), playerTraits.getActiveTraitIds());
        }
        ConscienceSerialKillerService.normalizeTargets(world, gameComponent, players);
        return EffectiveTraitService.publicKillerCount(gameComponent, players);
    }

    private static List<Identifier> applyPendingLocks(
            ServerWorld world,
            GameWorldComponent gameComponent,
            ServerPlayerEntity player,
            List<Identifier> pendingTraits
    ) {
        return applyPendingLocks(world, gameComponent, player, gameComponent.getRole(player), pendingTraits);
    }

    static List<Identifier> applyPendingLocks(
            ServerWorld world,
            GameWorldComponent gameComponent,
            ServerPlayerEntity player,
            Role role,
            List<Identifier> pendingTraits
    ) {
        return retainTraitsEligibleForRole(world, gameComponent, player, role, pendingTraits, TraitPlayerComponent.MAX_TRAITS);
    }

    /**
     * Shared per-trait rule of pending locks and role-change revalidation: keeps, in order, every trait that is
     * registered, allowed for {@code role}, compatible with the traits kept before it, passes its own selection gate,
     * and keeps the tentative set valid. Keeps at most {@code maxTraits} slot-occupying traits (free traits such as Well
     * Supplied are not counted); unique-per-game limits are never applied here.
     * 待应用锁定与换身份复核共用的逐天赋规则：按原顺序保留已注册、{@code role} 可获得、与先前保留天赋兼容、
     * 通过自身选择条件且整组仍有效的天赋；最多保留 {@code maxTraits} 个占用槽位的天赋（物资充沛等免费天赋不计入），
     * 此处从不套用每局唯一限制。
     */
    static List<Identifier> retainTraitsEligibleForRole(
            ServerWorld world,
            GameWorldComponent gameComponent,
            ServerPlayerEntity player,
            Role role,
            List<Identifier> candidateTraits,
            int maxTraits
    ) {
        LinkedHashSet<Identifier> accepted = new LinkedHashSet<>();
        for (Identifier traitId : candidateTraits) {
            if (TraitRules.occupiesTraitSlot(traitId) && TraitRules.occupiedTraitSlots(accepted) >= maxTraits) {
                continue;
            }
            Trait trait = TraitRegistry.get(traitId);
            if (trait == null || !TraitRoleEligibility.canReceiveTrait(role, trait)) {
                continue;
            }
            if (!TraitRules.isCompatibleWithAll(trait, accepted)) {
                continue;
            }
            if (!trait.canApply(new TraitSelectionContext(world, gameComponent, player, role, accepted))) {
                continue;
            }
            LinkedHashSet<Identifier> tentative = new LinkedHashSet<>(accepted);
            tentative.add(traitId);
            if (!TraitRules.canApplyAll(world, gameComponent, player, role, tentative)) {
                continue;
            }
            accepted.add(traitId);
        }
        return List.copyOf(accepted);
    }

    private static void forceConscienceOntoEligibleKiller(
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            List<PlayerPlan> plans,
            Random random
    ) {
        if (!traitWorld.isTraitEnabled(ConscienceTrait.ID)) {
            SparkTraits.LOGGER.warn("Skipping forced Conscience because sparktraits:conscience is disabled.");
            return;
        }

        List<PlayerPlan> eligiblePlans = new ArrayList<>();
        for (PlayerPlan plan : plans) {
            Role role = gameComponent.getRole(plan.player());
            if (!EffectiveTraitService.canSelectConscience(role, gameComponent, plan.traits())) {
                continue;
            }
            if (plan.canAcceptForcedRandomTrait(ConscienceTrait.ID)) {
                eligiblePlans.add(plan);
            }
        }
        if (!eligiblePlans.isEmpty()) {
            eligiblePlans.get(pickForcedConscienceCandidateIndex(eligiblePlans.size(), random))
                    .addForcedRandomTrait(ConscienceTrait.ID);
            return;
        }

        SparkTraits.LOGGER.warn("Skipping forced Conscience because every eligible killer has locked trait slots.");
    }

    static int pickForcedConscienceCandidateIndex(int eligibleCount, Random random) {
        return random.nextInt(eligibleCount);
    }

    private static void addExtraKillersForConscience(
            ServerWorld world,
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            List<ServerPlayerEntity> players,
            List<PlayerPlan> plans,
            int publicKillerCount,
            Set<UUID> lockedRolePlayers,
            Random random,
            Collection<Identifier> randomUniqueTraitReservations,
            int lastEscapeCap
    ) {
        int originalKillerCount = EffectiveTraitService.originalKillerCount(gameComponent);
        int conscienceCount = countTrait(plans, ConscienceTrait.ID);
        int extraKillers = EffectiveTraitService.requiredExtraKillersForConscience(publicKillerCount, originalKillerCount, conscienceCount);
        RoleSelectionContext selectionContext = createRoleSelectionContext(world, gameComponent, players);
        RoleHistoryComponent roleHistory = RoleHistoryComponent.KEY.get(world.getScoreboard());

        for (int i = 0; i < extraKillers; i++) {
            PlayerPlan extraKiller = chooseExtraKiller(gameComponent, roleHistory, plans, lockedRolePlayers, random);
            if (extraKiller == null) {
                SparkTraits.LOGGER.warn("Could not add an extra killer for Conscience compensation.");
                return;
            }
            Role compensationRole = chooseConscienceCompensationKillerRole(gameComponent, selectionContext, random);
            if (compensationRole == null) {
                SparkTraits.LOGGER.warn("Could not add an extra killer for Conscience compensation because no enabled killer role is available.");
                return;
            }
            replaceRandomTraitsForConscienceCompensation(
                    extraKiller,
                    randomUniqueTraitReservations,
                    List.of()
            );
            // RoleAssigned has not fired yet: Wathe announces, equips and records this player only as the killer.
            // RoleAssigned 尚未触发：Wathe 只会以杀手身份公布、发放道具并记录该玩家。
            gameComponent.addRole(extraKiller.player(), compensationRole);
            rebuildUniqueTraitReservations(randomUniqueTraitReservations, plans);
            List<Identifier> rerolledTraits = TraitSelector.selectRandomTraits(
                    world,
                    gameComponent,
                    traitWorld,
                    extraKiller.player(),
                    random,
                    players.size(),
                    extraKiller.lockedTraits(),
                    randomUniqueTraitReservations,
                    excludeLastEscapeAtCap(CONSCIENCE_COMPENSATION_REROLL_EXCLUSIONS, plans, lastEscapeCap)
            );
            extraKiller.replaceRandomTraits(rerolledTraits);
            rebuildUniqueTraitReservations(randomUniqueTraitReservations, plans);
        }
    }

    private static PlayerPlan chooseExtraKiller(
            GameWorldComponent gameComponent,
            RoleHistoryComponent roleHistory,
            List<PlayerPlan> plans,
            Set<UUID> lockedRolePlayers,
            Random random
    ) {
        List<PlayerPlan> eligiblePlans = plans.stream()
                .filter(plan -> canBecomeExtraKiller(gameComponent, plan, lockedRolePlayers))
                .toList();
        return pickWeightedConscienceCompensationCandidate(
                eligiblePlans,
                List.of(
                        plan -> gameComponent.isRole(plan.player(), WatheRoles.CIVILIAN) && plan.isUnlocked(),
                        plan -> gameComponent.isRole(plan.player(), WatheRoles.CIVILIAN) && !plan.hasLocks(),
                        PlayerPlan::isUnlocked,
                        plan -> !plan.hasLocks()
                ),
                plan -> roleHistory.debt(plan.player().getUuid(), RoleCategory.KILLER),
                gameComponent.getRoleRotationStrength(),
                random
        );
    }

    /** Keeps existing priority buckets, but lets Wathe's role-rotation debt choose within each bucket.
     *  保留原有候选优先级，只在同一优先级内交给 Wathe 的身份轮换债务加权抽选。 */
    static <T> T pickWeightedConscienceCompensationCandidate(
            List<T> candidates,
            List<Predicate<T>> priorityBuckets,
            ToDoubleFunction<T> killerDebtProvider,
            RotationStrength rotationStrength,
            Random random
    ) {
        for (Predicate<T> priorityBucket : priorityBuckets) {
            List<T> bucketCandidates = candidates.stream()
                    .filter(priorityBucket)
                    .toList();
            T selected = pickWeightedConscienceCompensationCandidate(
                    bucketCandidates,
                    killerDebtProvider,
                    rotationStrength,
                    random
            );
            if (selected != null) {
                return selected;
            }
        }
        return null;
    }

    private static <T> T pickWeightedConscienceCompensationCandidate(
            List<T> candidates,
            ToDoubleFunction<T> killerDebtProvider,
            RotationStrength rotationStrength,
            Random random
    ) {
        if (candidates.isEmpty()) {
            return null;
        }
        RotationStrength safeRotationStrength = rotationStrength == null ? RotationStrength.OFF : rotationStrength;
        return RoleRotation.selectWeighted(
                        candidates,
                        candidate -> RoleRotation.weight(killerDebtProvider.applyAsDouble(candidate), safeRotationStrength),
                        1,
                        random::nextDouble
                ).stream()
                .findFirst()
                .orElse(null);
    }

    private static boolean canBecomeExtraKiller(
            GameWorldComponent gameComponent,
            PlayerPlan plan,
            Set<UUID> lockedRolePlayers
    ) {
        Role role = gameComponent.getRole(plan.player());
        return canUseAsConscienceCompensationTarget(
                role,
                plan.traits(),
                lockedRolePlayers.contains(plan.player().getUuid()),
                plan.hasLocks()
        );
    }

    /** Police-category roles are never drafted as compensation killers, so none keeps a police weapon.
     *  警职类别身份永不被选为补偿杀手，避免其带着警用武器转为杀手。 */
    static boolean canUseAsConscienceCompensationTarget(
            Role role,
            Collection<Identifier> traits,
            boolean roleLocked,
            boolean traitLocked
    ) {
        return role != null
                && !roleLocked
                && !traitLocked
                && EffectiveTraitService.isOriginalCivilian(role)
                && !PoliceRoleCategory.isPolice(role)
                && !traits.contains(ImpostorTrait.ID)
                && !traits.contains(ConscienceTrait.ID);
    }

    private static Role chooseConscienceCompensationKillerRole(
            GameWorldComponent gameComponent,
            RoleSelectionContext selectionContext,
            Random random
    ) {
        Role basicKillerFallback = null;
        List<Role> specialKillerCandidates = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (!canUseAsConscienceCompensationKiller(
                    role,
                    gameComponent.isRoleEnabled(role),
                    !gameComponent.getAllWithRole(role).isEmpty(),
                    role.shouldAppear(selectionContext)
            )) {
                continue;
            }
            if (role == WatheRoles.KILLER) {
                basicKillerFallback = role;
                continue;
            }
            specialKillerCandidates.add(role);
        }
        return pickShuffledConscienceCompensationKillerRole(specialKillerCandidates, basicKillerFallback, random);
    }

    static Role pickShuffledConscienceCompensationKillerRole(
            List<Role> specialKillerCandidates,
            Role basicKillerFallback,
            Random random
    ) {
        if (!specialKillerCandidates.isEmpty()) {
            Collections.shuffle(specialKillerCandidates, random);
            return specialKillerCandidates.getFirst();
        }
        return basicKillerFallback;
    }

    static boolean canUseAsConscienceCompensationKiller(
            Role role,
            boolean roleEnabled,
            boolean alreadyAssigned,
            boolean shouldAppear
    ) {
        return role != null
                && role.canUseKiller()
                && roleEnabled
                && shouldAppear
                && (role == WatheRoles.KILLER || (!WatheRoles.VANILLA_ROLES.contains(role) && !alreadyAssigned));
    }

    private static RoleSelectionContext createRoleSelectionContext(
            ServerWorld world,
            GameWorldComponent gameComponent,
            List<ServerPlayerEntity> players
    ) {
        int totalPlayerCount = players.size();
        return new RoleSelectionContext(
                world,
                gameComponent,
                Collections.unmodifiableList(players),
                totalPlayerCount,
                targetRoleCount(totalPlayerCount, gameComponent.getKillerDividend()),
                targetRoleCount(totalPlayerCount, gameComponent.getNeutralDividend()),
                targetRoleCount(totalPlayerCount, gameComponent.getVigilanteDividend())
        );
    }

    private static int targetRoleCount(int playerCount, int dividend) {
        if (dividend <= 0) {
            return 0;
        }
        return (int) Math.floor((double) playerCount / dividend);
    }

    private static boolean containsTrait(List<PlayerPlan> plans, Identifier traitId) {
        return countTrait(plans, traitId) > 0;
    }

    private static int countTrait(List<PlayerPlan> plans, Identifier traitId) {
        int count = 0;
        for (PlayerPlan plan : plans) {
            if (plan.traits().contains(traitId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gives Pig God its required public Pig trait after role-changing assignment steps settle.
     * 在所有可能改写身份的分配步骤完成后，为 Pig God 补上必定拥有的猪天赋。
     */
    private static void forcePigOntoPigGod(
            GameWorldComponent gameComponent,
            TraitWorldComponent traitWorld,
            List<PlayerPlan> plans
    ) {
        if (!traitWorld.isTraitEnabled(PigTrait.ID)) {
            SparkTraits.LOGGER.warn("Skipping forced Pig because sparktraits:pig is disabled.");
            return;
        }
        for (PlayerPlan plan : plans) {
            Role role = gameComponent.getRole(plan.player());
            if (shouldForcePigOntoPigGod(role)) {
                plan.forceRequiredTrait(PigTrait.ID);
            }
        }
    }

    static boolean shouldForcePigOntoPigGod(Role role) {
        return PigTraitService.canSelectRequiredPig(role);
    }

    static ForcedTraitPlan forceRequiredTraitPlan(
            Collection<Identifier> lockedTraits,
            Collection<Identifier> randomTraits,
            Identifier requiredTrait
    ) {
        List<Identifier> locked = new ArrayList<>(lockedTraits);
        List<Identifier> random = new ArrayList<>(randomTraits);
        if (requiredTrait == null) {
            return new ForcedTraitPlan(List.copyOf(locked), List.copyOf(random));
        }
        Trait required = TraitRegistry.get(requiredTrait);
        if (required != null) {
            random.removeIf(randomTraitId -> {
                Trait randomTrait = TraitRegistry.get(randomTraitId);
                return randomTrait != null && TraitRules.areIncompatible(required, randomTrait);
            });
        }
        if (locked.contains(requiredTrait) || random.contains(requiredTrait)) {
            return new ForcedTraitPlan(List.copyOf(locked), List.copyOf(random));
        }
        while (TraitRules.occupiesTraitSlot(requiredTrait)
                && occupiedTraitSlots(locked, random) >= TraitPlayerComponent.MAX_TRAITS) {
            if (removeLastSlotTrait(random) || removeLastSlotTrait(locked)) {
                continue;
            }
            break;
        }
        random.add(requiredTrait);
        return new ForcedTraitPlan(List.copyOf(locked), List.copyOf(random));
    }

    private static int occupiedTraitSlots(Collection<Identifier> lockedTraits, Collection<Identifier> randomTraits) {
        return TraitRules.occupiedTraitSlots(lockedTraits) + TraitRules.occupiedTraitSlots(randomTraits);
    }

    /** Dropping a free trait frees no slot, so only the last slot-occupying trait is removed.
     *  移除免费天赋腾不出槽位，因此只移除最后一个占用槽位的天赋。 */
    private static boolean removeLastSlotTrait(List<Identifier> traits) {
        for (int i = traits.size() - 1; i >= 0; i--) {
            if (TraitRules.occupiesTraitSlot(traits.get(i))) {
                traits.remove(i);
                return true;
            }
        }
        return false;
    }

    record ForcedTraitPlan(List<Identifier> lockedTraits, List<Identifier> randomTraits) {
    }

    /**
     * Every setActiveTraits call syncs at once, so Conscience must land before Impostor: otherwise a killer about to
     * turn civilian would still count as an effective killer and receive the Impostor instinct flag. The per-tick
     * recipient watcher cannot repair this, because roles and traits land in the same tick.
     * 每次 setActiveTraits 都会立即同步，因此善良必须先于内鬼落地；否则即将转为好人的杀手仍被视为有效杀手，
     * 会收到内鬼的本能标记。职业与天赋在同一 tick 内落地，逐 tick 的接收者监视无法事后补救。
     */
    static List<PlayerPlan> conscienceFirst(List<PlayerPlan> plans) {
        List<PlayerPlan> ordered = new ArrayList<>(plans.size());
        for (PlayerPlan plan : plans) {
            if (plan.traits().contains(ConscienceTrait.ID)) {
                ordered.add(plan);
            }
        }
        for (PlayerPlan plan : plans) {
            if (!plan.traits().contains(ConscienceTrait.ID)) {
                ordered.add(plan);
            }
        }
        return ordered;
    }

    static void markUniqueTraits(TraitWorldComponent traitWorld, Collection<Identifier> traits) {
        for (Identifier traitId : traits) {
            Trait trait = TraitRegistry.get(traitId);
            if (trait != null && trait.uniquePerGame()) {
                traitWorld.markUniqueTraitUsed(traitId);
            }
        }
    }

    static void reserveUniqueTraits(Collection<Identifier> reservedUniqueTraits, Collection<Identifier> traits) {
        for (Identifier traitId : traits) {
            if (isUniqueTrait(traitId)) {
                reservedUniqueTraits.add(traitId);
            }
        }
    }

    static void rebuildUniqueTraitReservations(
            Collection<Identifier> reservedUniqueTraits,
            Collection<PlayerPlan> plans
    ) {
        reservedUniqueTraits.clear();
        for (PlayerPlan plan : plans) {
            reserveUniqueTraits(reservedUniqueTraits, plan.traits());
        }
    }

    static void recalibrateUniqueTraitReservations(
            Collection<Identifier> reservedUniqueTraits,
            Collection<PlayerPlan> plans
    ) {
        rebuildUniqueTraitReservations(reservedUniqueTraits, plans);
    }

    static void replaceRandomTraitsForConscienceCompensation(
            PlayerPlan plan,
            Collection<Identifier> reservedUniqueTraits,
            Collection<Identifier> replacementTraits
    ) {
        for (Identifier traitId : plan.randomTraits()) {
            if (isUniqueTrait(traitId)) {
                reservedUniqueTraits.remove(traitId);
            }
        }
        plan.replaceRandomTraits(replacementTraits);
        reserveUniqueTraits(reservedUniqueTraits, plan.randomTraits());
    }

    private static void enforceUniqueTraitLimits(List<PlayerPlan> plans) {
        LinkedHashSet<Identifier> usedUniqueTraits = new LinkedHashSet<>();
        for (PlayerPlan plan : plans) {
            plan.removeDuplicateUniqueLocks(usedUniqueTraits);
        }
        for (PlayerPlan plan : plans) {
            plan.removeDuplicateUniqueRandomTraits(usedUniqueTraits);
        }
    }

    /**
     * Keeps Last Escape out of a random draw once the plans already hold {@code cap} of it, so that slot rolls another
     * trait instead. Locked copies count toward the cap but are never removed.
     * 计划中的绝处逢生已达 {@code cap} 个时，将其排除出本次随机抽取，使该槽位改抽其他天赋。锁定的绝处逢生计入上限，
     * 但永不移除。
     */
    static Set<Identifier> excludeLastEscapeAtCap(Set<Identifier> exclusions, List<PlayerPlan> plans, int cap) {
        if (countTrait(plans, KillerTraits.LAST_ESCAPE) < cap) {
            return exclusions;
        }
        LinkedHashSet<Identifier> capped = new LinkedHashSet<>(exclusions);
        capped.add(KillerTraits.LAST_ESCAPE);
        return Set.copyOf(capped);
    }

    /**
     * Caps only randomly rolled Depression traits; pending/admin locks intentionally bypass this random budget.
     * 只限制随机抽到的抑郁天赋；管理员/待应用锁定不会消耗这个随机名额。
     */
    static void enforceRandomDepressionCap(List<PlayerPlan> plans, int startingPlayerCount) {
        int cap = DepressionTraitService.randomDepressionCap(startingPlayerCount);
        int keptRandomDepressions = 0;
        for (PlayerPlan plan : plans) {
            keptRandomDepressions = plan.removeRandomDepressionsOverLimit(cap, keptRandomDepressions);
        }
    }

    private static boolean isUniqueTrait(Identifier traitId) {
        Trait trait = TraitRegistry.get(traitId);
        return trait != null && trait.uniquePerGame();
    }

    /** Opaque hand-off from {@link #planBeforeRoleAssigned} to {@link #applyAfterRoleAssigned}.
     *  从 {@link #planBeforeRoleAssigned} 交给 {@link #applyAfterRoleAssigned} 的不透明方案。 */
    public static final class RoundPlan {
        private final List<PlayerPlan> plans;

        private RoundPlan(List<PlayerPlan> plans) {
            this.plans = List.copyOf(plans);
        }

        List<PlayerPlan> plans() {
            return plans;
        }
    }

    static final class PlayerPlan {
        private final ServerPlayerEntity player;
        private final List<Identifier> lockedTraits;
        private final List<Identifier> randomTraits;

        PlayerPlan(ServerPlayerEntity player, List<Identifier> lockedTraits, List<Identifier> randomTraits) {
            this.player = player;
            this.lockedTraits = new ArrayList<>(lockedTraits);
            this.randomTraits = new ArrayList<>(randomTraits);
        }

        ServerPlayerEntity player() {
            return player;
        }

        boolean hasLocks() {
            return !lockedTraits.isEmpty();
        }

        boolean isUnlocked() {
            return lockedTraits.isEmpty() && randomTraits.isEmpty();
        }

        List<Identifier> lockedTraits() {
            return List.copyOf(lockedTraits);
        }

        List<Identifier> randomTraits() {
            return List.copyOf(randomTraits);
        }

        List<Identifier> traits() {
            LinkedHashSet<Identifier> traits = new LinkedHashSet<>();
            traits.addAll(lockedTraits);
            traits.addAll(randomTraits);
            return List.copyOf(traits);
        }

        boolean addForcedRandomTrait(Identifier traitId) {
            if (lockedTraits.contains(traitId) || randomTraits.contains(traitId)) {
                return true;
            }
            Trait trait = TraitRegistry.get(traitId);
            if (trait == null || !isCompatibleWithLockedTraits(trait)) {
                return false;
            }
            randomTraits.removeIf(randomTraitId -> {
                Trait randomTrait = TraitRegistry.get(randomTraitId);
                return randomTrait != null && TraitRules.areIncompatible(trait, randomTrait);
            });
            if (trait.occupiesTraitSlot()) {
                if (TraitRules.occupiedTraitSlots(lockedTraits) >= TraitPlayerComponent.MAX_TRAITS) {
                    return false;
                }
                if (occupiedTraitSlots(lockedTraits, randomTraits) >= TraitPlayerComponent.MAX_TRAITS) {
                    removeLastSlotTrait(randomTraits);
                }
            }
            randomTraits.add(traitId);
            return true;
        }

        boolean canAcceptForcedRandomTrait(Identifier traitId) {
            if (lockedTraits.contains(traitId) || randomTraits.contains(traitId)) {
                return true;
            }
            Trait trait = TraitRegistry.get(traitId);
            return trait != null
                    && (!trait.occupiesTraitSlot()
                            || TraitRules.occupiedTraitSlots(lockedTraits) < TraitPlayerComponent.MAX_TRAITS)
                    && isCompatibleWithLockedTraits(trait);
        }

        void clearRandomTraits() {
            randomTraits.clear();
        }

        void replaceRandomTraits(Collection<Identifier> replacementTraits) {
            randomTraits.clear();
            if (replacementTraits == null) {
                return;
            }
            for (Identifier traitId : replacementTraits) {
                if (TraitRules.occupiesTraitSlot(traitId)
                        && occupiedTraitSlots(lockedTraits, randomTraits) >= TraitPlayerComponent.MAX_TRAITS) {
                    continue;
                }
                if (!lockedTraits.contains(traitId) && !randomTraits.contains(traitId)) {
                    randomTraits.add(traitId);
                }
            }
        }

        void forceRequiredTrait(Identifier traitId) {
            ForcedTraitPlan plan = forceRequiredTraitPlan(lockedTraits, randomTraits, traitId);
            lockedTraits.clear();
            lockedTraits.addAll(plan.lockedTraits());
            randomTraits.clear();
            randomTraits.addAll(plan.randomTraits());
        }

        int removeRandomDepressionsOverLimit(int cap, int alreadyKept) {
            int kept = alreadyKept;
            for (int i = 0; i < randomTraits.size(); ) {
                if (!CivilianTraits.DEPRESSION.equals(randomTraits.get(i))) {
                    i++;
                    continue;
                }
                if (kept >= cap) {
                    randomTraits.remove(i);
                    continue;
                }
                kept++;
                i++;
            }
            return kept;
        }

        private boolean isCompatibleWithLockedTraits(Trait trait) {
            for (Identifier lockedTraitId : lockedTraits) {
                Trait lockedTrait = TraitRegistry.get(lockedTraitId);
                if (lockedTrait != null && TraitRules.areIncompatible(trait, lockedTrait)) {
                    return false;
                }
            }
            return true;
        }

        void removeDuplicateUniqueLocks(Collection<Identifier> usedUniqueTraits) {
            lockedTraits.removeIf(traitId -> {
                if (!isUniqueTrait(traitId)) {
                    return false;
                }
                if (usedUniqueTraits.contains(traitId)) {
                    return true;
                }
                usedUniqueTraits.add(traitId);
                return false;
            });
        }

        void removeDuplicateUniqueRandomTraits(Collection<Identifier> usedUniqueTraits) {
            randomTraits.removeIf(traitId -> {
                if (!isUniqueTrait(traitId)) {
                    return false;
                }
                if (usedUniqueTraits.contains(traitId)) {
                    return true;
                }
                usedUniqueTraits.add(traitId);
                return false;
            });
        }
    }
}
