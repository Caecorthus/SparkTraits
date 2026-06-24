package dev.caecorthus.sparktraits.impl;

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
import dev.doctor4t.wathe.api.event.RoleAssigned;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Builds round trait plans before Wathe sends welcome information.
 *  在 Wathe 发送开局信息前构建本局天赋方案。 */
public final class TraitAssignmentService {
    private static final String WATHE_MOD_ID = "wathe";
    private static final String NOELLESROLES_MOD_ID = "noellesroles";
    private static final Identifier WATHE_REVOLVER_ID = Identifier.of(WATHE_MOD_ID, "revolver");
    private static final Identifier WATHE_KNIFE_ID = Identifier.of(WATHE_MOD_ID, "knife");
    private static final Identifier WATHE_NOTE_ID = Identifier.of(WATHE_MOD_ID, "note");
    private static final Identifier WATHE_WALKIE_TALKIE_ID = Identifier.of(WATHE_MOD_ID, "walkie_talkie");
    private static final Identifier NOELLES_MASTER_KEY_ID = Identifier.of(NOELLESROLES_MOD_ID, "master_key");
    private static final Identifier NOELLES_ANTIDOTE_ID = Identifier.of(NOELLESROLES_MOD_ID, "antidote");
    private static final Identifier NOELLES_IRON_MAN_VIAL_ID = Identifier.of(NOELLESROLES_MOD_ID, "iron_man_vial");
    private static final Identifier NOELLES_REPAIR_TOOL_ID = Identifier.of(NOELLESROLES_MOD_ID, "repair_tool");
    private static final Identifier MINECRAFT_WRITTEN_BOOK_ID = Identifier.ofVanilla("written_book");

    private TraitAssignmentService() {
    }

    public static void assignForGame(ServerWorld world, GameWorldComponent gameComponent) {
        List<ServerPlayerEntity> players = gameComponent.getAllPlayers().stream()
                .map(world::getPlayerByUuid)
                .filter(ServerPlayerEntity.class::isInstance)
                .map(ServerPlayerEntity.class::cast)
                .toList();
        assignForMurderGameBeforeWelcome(world, gameComponent, players, EffectiveTraitService.originalKillerCount(gameComponent));
    }

    public static int assignForMurderGameBeforeWelcome(
            ServerWorld world,
            GameWorldComponent gameComponent,
            List<ServerPlayerEntity> players,
            int publicKillerCount
    ) {
        return assignForMurderGameBeforeWelcome(world, gameComponent, players, publicKillerCount, Set.of());
    }

    public static int assignForMurderGameBeforeWelcome(
            ServerWorld world,
            GameWorldComponent gameComponent,
            List<ServerPlayerEntity> players,
            int publicKillerCount,
            Set<UUID> lockedRolePlayers
    ) {
        Set<UUID> protectedLockedRolePlayers = lockedRolePlayers == null ? Set.of() : lockedRolePlayers;
        TraitWorldComponent traitWorld = TraitWorldComponent.KEY.get(world);
        Random random = new Random(world.getRandom().nextLong());
        List<PlayerPlan> plans = new ArrayList<>();
        List<ServerPlayerEntity> randomPlayers = new ArrayList<>();

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

            List<Identifier> randomTraits = TraitSelector.selectRandomTraits(world, gameComponent, traitWorld, player, random);
            plans.add(new PlayerPlan(player, List.of(), randomTraits));
        }

        enforceUniqueTraitLimits(plans);
        if (containsTrait(plans, ImpostorTrait.ID) && !containsTrait(plans, ConscienceTrait.ID)) {
            forceConscienceOntoEligibleKiller(gameComponent, traitWorld, plans, random);
        }

        addExtraKillersForConscience(world, gameComponent, players, plans, publicKillerCount, protectedLockedRolePlayers, random);

        traitWorld.clearRoundState();
        for (PlayerPlan plan : plans) {
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
        LinkedHashSet<Identifier> accepted = new LinkedHashSet<>();
        Role role = gameComponent.getRole(player);
        for (Identifier traitId : pendingTraits) {
            if (accepted.size() >= TraitPlayerComponent.MAX_TRAITS) {
                break;
            }
            Trait trait = TraitRegistry.get(traitId);
            if (trait == null) {
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
            List<ServerPlayerEntity> players,
            List<PlayerPlan> plans,
            int publicKillerCount,
            Set<UUID> lockedRolePlayers,
            Random random
    ) {
        int originalKillerCount = EffectiveTraitService.originalKillerCount(gameComponent);
        int conscienceCount = countTrait(plans, ConscienceTrait.ID);
        int extraKillers = EffectiveTraitService.requiredExtraKillersForConscience(publicKillerCount, originalKillerCount, conscienceCount);
        RoleSelectionContext selectionContext = createRoleSelectionContext(world, gameComponent, players);

        for (int i = 0; i < extraKillers; i++) {
            PlayerPlan extraKiller = chooseExtraKiller(gameComponent, plans, lockedRolePlayers);
            if (extraKiller == null) {
                SparkTraits.LOGGER.warn("Could not add an extra killer for Conscience compensation.");
                return;
            }
            Role compensationRole = chooseConscienceCompensationKillerRole(gameComponent, selectionContext, random);
            if (compensationRole == null) {
                SparkTraits.LOGGER.warn("Could not add an extra killer for Conscience compensation because no enabled killer role is available.");
                return;
            }
            Role originalRole = gameComponent.getRole(extraKiller.player());
            extraKiller.clearRandomTraits();
            clearInitialRoleItemsForConscienceCompensation(extraKiller.player(), originalRole);
            gameComponent.addRole(extraKiller.player(), compensationRole);
            RoleAssigned.EVENT.invoker().assignRole(extraKiller.player(), compensationRole);
            TraitPlayerComponent.KEY.get(extraKiller.player()).sync();
            gameComponent.sync();
        }
    }

    private static PlayerPlan chooseExtraKiller(
            GameWorldComponent gameComponent,
            List<PlayerPlan> plans,
            Set<UUID> lockedRolePlayers
    ) {
        for (PlayerPlan plan : plans) {
            if (canBecomeExtraKiller(gameComponent, plan, lockedRolePlayers)
                    && gameComponent.isRole(plan.player(), WatheRoles.CIVILIAN)
                    && plan.isUnlocked()) {
                return plan;
            }
        }
        for (PlayerPlan plan : plans) {
            if (canBecomeExtraKiller(gameComponent, plan, lockedRolePlayers)
                    && gameComponent.isRole(plan.player(), WatheRoles.CIVILIAN)
                    && !plan.hasLocks()) {
                return plan;
            }
        }
        for (PlayerPlan plan : plans) {
            if (canBecomeExtraKiller(gameComponent, plan, lockedRolePlayers) && plan.isUnlocked()) {
                return plan;
            }
        }
        for (PlayerPlan plan : plans) {
            if (canBecomeExtraKiller(gameComponent, plan, lockedRolePlayers) && !plan.hasLocks()) {
                return plan;
            }
        }
        return null;
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
                lockedRolePlayers.contains(plan.player().getUuid())
        );
    }

    static boolean canUseAsConscienceCompensationTarget(
            Role role,
            Collection<Identifier> traits,
            boolean roleLocked
    ) {
        return role != null
                && !roleLocked
                && EffectiveTraitService.isOriginalCivilian(role)
                && role != WatheRoles.VIGILANTE
                && role != WatheRoles.VETERAN
                && !traits.contains(ImpostorTrait.ID)
                && !traits.contains(ConscienceTrait.ID);
    }

    /** Removes old civilian role kit before Conscience compensation assigns a killer role.
     *  在善良补偿杀手改写身份前，移除原好人身份的开局物品。 */
    private static void clearInitialRoleItemsForConscienceCompensation(ServerPlayerEntity player, Role originalRole) {
        for (Identifier itemId : initialRoleItemIdsToClearForConscienceCompensation(originalRole)) {
            Item item = Registries.ITEM.get(itemId);
            player.getInventory().remove(
                    stack -> stack.isOf(item),
                    Integer.MAX_VALUE,
                    player.playerScreenHandler.getCraftingInput()
            );
            player.getItemCooldownManager().remove(item);
        }
    }

    static List<Identifier> initialRoleItemIdsToClearForConscienceCompensation(Role role) {
        if (role == WatheRoles.VIGILANTE) {
            return List.of(WATHE_REVOLVER_ID);
        }
        if (role == WatheRoles.VETERAN) {
            return List.of(WATHE_KNIFE_ID);
        }
        if (role == Noellesroles.CONDUCTOR) {
            return List.of(NOELLES_MASTER_KEY_ID);
        }
        if (role == Noellesroles.AWESOME_BINGLUS) {
            return List.of(WATHE_NOTE_ID);
        }
        if (role == Noellesroles.TOXICOLOGIST) {
            return List.of(NOELLES_ANTIDOTE_ID);
        }
        if (role == Noellesroles.PROFESSOR) {
            return List.of(NOELLES_IRON_MAN_VIAL_ID);
        }
        if (role == Noellesroles.ENGINEER) {
            return List.of(NOELLES_REPAIR_TOOL_ID);
        }
        if (role == Noellesroles.ATTENDANT) {
            return List.of(MINECRAFT_WRITTEN_BOOK_ID);
        }
        if (role == Noellesroles.UNDERCOVER) {
            return List.of(WATHE_WALKIE_TALKIE_ID);
        }
        return List.of();
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

    private static void markUniqueTraits(TraitWorldComponent traitWorld, Collection<Identifier> traits) {
        for (Identifier traitId : traits) {
            Trait trait = TraitRegistry.get(traitId);
            if (trait != null && trait.uniquePerGame()) {
                traitWorld.markUniqueTraitUsed(traitId);
            }
        }
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

    private static boolean isUniqueTrait(Identifier traitId) {
        Trait trait = TraitRegistry.get(traitId);
        return trait != null && trait.uniquePerGame();
    }

    private static final class PlayerPlan {
        private final ServerPlayerEntity player;
        private final List<Identifier> lockedTraits;
        private final List<Identifier> randomTraits;

        private PlayerPlan(ServerPlayerEntity player, List<Identifier> lockedTraits, List<Identifier> randomTraits) {
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
            if (lockedTraits.size() >= TraitPlayerComponent.MAX_TRAITS) {
                return false;
            }
            if (lockedTraits.size() + randomTraits.size() >= TraitPlayerComponent.MAX_TRAITS && !randomTraits.isEmpty()) {
                randomTraits.removeLast();
            }
            randomTraits.add(traitId);
            return true;
        }

        boolean canAcceptForcedRandomTrait(Identifier traitId) {
            if (lockedTraits.contains(traitId) || randomTraits.contains(traitId)) {
                return true;
            }
            return lockedTraits.size() < TraitPlayerComponent.MAX_TRAITS;
        }

        void clearRandomTraits() {
            randomTraits.clear();
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
