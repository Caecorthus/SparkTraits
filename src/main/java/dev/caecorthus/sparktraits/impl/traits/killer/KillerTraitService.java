package dev.caecorthus.sparktraits.impl.traits.killer;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.laststand.LastStandFinalMomentService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.api.event.PsychoModeEvents;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import dev.doctor4t.wathe.util.ShopUtils;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModEffects;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

/**
 * Shared runtime rules for killer-only traits.
 * 杀手专用天赋的运行时规则集中入口，避免污染阵营翻转服务。
 */
public final class KillerTraitService {
    public static final int SHOWMAN_RANGE = 8;
    public static final int SHOWMAN_MAX_PLAYERS = 10;
    public static final int SHOWMAN_MONEY_PER_PLAYER = 8;
    public static final int CORNERED_TEAMMATE_REWARD = 75;
    public static final int CORNERED_LAST_KILLER_REWARD = 200;
    public static final int PARANOID_EXTRA_TICKS = 20 * 20;
    public static final float OPPRESSIVE_DRAIN_MULTIPLIER = 1.2f;
    public static final double THRUST_EXTRA_KNOCKBACK = 0.25;
    public static final Identifier THRUST_KNOCKBACK_MODIFIER_ID = SparkTraits.id("thrust_knockback");

    private static final ThreadLocal<Deque<KillAttempt>> KILL_ATTEMPTS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Boolean> SECOND_STRIKE_REPLAYING = ThreadLocal.withInitial(() -> false);
    private static final EntityAttributeModifier THRUST_KNOCKBACK_MODIFIER = new EntityAttributeModifier(
            THRUST_KNOCKBACK_MODIFIER_ID,
            THRUST_EXTRA_KNOCKBACK,
            EntityAttributeModifier.Operation.ADD_VALUE
    );

    private KillerTraitService() {
    }

    public static void register() {
        BuildShopEntries.EVENT.register((player, context) -> {
            if (!hasEligibleTrait(player, KillerTraits.CHARISMA)) {
                return;
            }
            for (int i = 0; i < context.size(); i++) {
                context.setEntry(i, discountedShopEntry(context.getEntry(i)));
            }
        });
        PsychoModeEvents.ON_PSYCHO_START.register((player, trackActive) -> {
            if (hasEligibleTrait(player, KillerTraits.PARANOID)) {
                PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
                psycho.setPsychoTicks(paranoidPsychoTicks(psycho.getPsychoTicks()));
                psycho.setArmour(paranoidPsychoArmour(psycho.getArmour()));
            }
        });
    }

    public static boolean canSelectKillerTrait(Role role, Collection<Identifier> selectedTraits) {
        return EffectiveTraitService.isOriginalKiller(role)
                && !EffectiveTraitService.hasConscience(selectedTraits)
                && !EffectiveTraitService.hasImpostor(selectedTraits);
    }

    public static boolean canSelectParanoid(Role role, Collection<Identifier> selectedTraits, boolean hasPsychoModeEntry) {
        return hasPsychoModeEntry && canSelectKillerTrait(role, selectedTraits);
    }

    public static boolean canSelectThrust(Role role, Collection<Identifier> selectedTraits, boolean hasThrustWeaponAccess) {
        return hasThrustWeaponAccess && canSelectKillerTrait(role, selectedTraits);
    }

    static boolean canUseThrust(boolean activeThrust, boolean eligibleKiller, boolean finalMomentLooseEnd) {
        return activeThrust && (eligibleKiller || finalMomentLooseEnd);
    }

    public static boolean hasPsychoModeShopEntry(PlayerEntity player) {
        return hasShopEntry(player, List.of("psycho_mode"), List.of(WatheItems.PSYCHO_MODE));
    }

    public static boolean hasThrustWeaponAccess(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        for (ShopEntry entry : ShopUtils.getShopEntriesForPlayer(player)) {
            if (KillerWeaponTags.isThrustWeapon(entry.stack())) {
                return true;
            }
        }
        return player.getInventory().contains(KillerWeaponTags::isThrustWeapon);
    }

    private static boolean hasShopEntry(PlayerEntity player, Collection<String> ids, Collection<Item> items) {
        if (player == null) {
            return false;
        }
        for (ShopEntry entry : ShopUtils.getShopEntriesForPlayer(player)) {
            if (ids.contains(entry.id()) || items.stream().anyMatch(item -> entry.stack().isOf(item))) {
                return true;
            }
        }
        return false;
    }

    public static int bloodthirstyCooldown(int duration, int killCount, int totalPlayers) {
        if (duration <= 0) {
            return duration;
        }
        int stacks = Math.min(Math.max(0, killCount), Math.max(0, totalPlayers / 3));
        if (stacks <= 0) {
            return duration;
        }
        float multiplier = 1.0f - stacks * 0.05f;
        return Math.max(1, (int) (duration * multiplier));
    }

    public static int bloodthirstyCooldown(ServerPlayerEntity player, int duration) {
        if (!hasEligibleTrait(player, KillerTraits.BLOODTHIRSTY)) {
            return duration;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        return bloodthirstyCooldown(
                duration,
                TraitPlayerComponent.KEY.get(player).getBloodthirstyKillCount(),
                game.getAllPlayers().size()
        );
    }

    public static int showmanReward(int nearbyAlivePlayers) {
        return Math.min(Math.max(0, nearbyAlivePlayers), SHOWMAN_MAX_PLAYERS) * SHOWMAN_MONEY_PER_PLAYER;
    }

    public static int plunderedAmount(int victimBalance) {
        return Math.max(0, victimBalance) / 3;
    }

    public static int charismaPrice(int price) {
        if (price <= 0) {
            return price;
        }
        return Math.max(0, (int) (price * 0.9f));
    }

    public static int effectiveCharismaPurchasePrice(PlayerEntity player, ShopEntry entry) {
        if (!hasEligibleTrait(player, KillerTraits.CHARISMA) || entry instanceof DiscountedShopEntry) {
            return entry.price();
        }
        return charismaPrice(entry.price());
    }

    /**
     * Returns a shop entry wrapped with the charisma discount when the player actually owns that trait.
     * 当且仅当玩家真实拥有【魅力】词条时，返回一个已包装的折扣商店条目。
     *
     * <p>这会让商店界面显示的价格和最终购买结算保持一致；若条目已经是折扣包装或价格本来就是非正数，
     * 则直接原样返回，避免重复打折。</p>
     */
    public static @Nullable ShopEntry applyCharismaDiscount(PlayerEntity player, @Nullable ShopEntry entry) {
        if (player == null || entry == null
                || !hasEligibleTrait(player, KillerTraits.CHARISMA)
                || entry instanceof DiscountedShopEntry
                || entry.price() <= 0) {
            return entry;
        }
        return discountedShopEntry(entry);
    }

    public static int paranoidPsychoTicks(int originalTicks) {
        return originalTicks + PARANOID_EXTRA_TICKS;
    }

    static int paranoidPsychoArmour(int currentArmour) {
        return currentArmour + 1;
    }

    static int corneredReward(boolean becomesSoleTeammate) {
        return becomesSoleTeammate ? CORNERED_LAST_KILLER_REWARD : CORNERED_TEAMMATE_REWARD;
    }

    static boolean shouldCountShowmanWitness(
            boolean sameAsVictim,
            boolean sameAsKiller,
            boolean playingAndAlive,
            boolean withinRange
    ) {
        return !sameAsVictim && !sameAsKiller && playingAndAlive && withinRange;
    }

    public static float oppressiveAdjustedMood(float currentMood, float proposedMood, boolean oppressiveActive) {
        if (!oppressiveActive || proposedMood >= currentMood) {
            return proposedMood;
        }
        return currentMood - (currentMood - proposedMood) * OPPRESSIVE_DRAIN_MULTIPLIER;
    }

    public static float oppressiveAdjustedMood(float currentMood, float proposedMood, PlayerEntity affectedPlayer) {
        return oppressiveAdjustedMood(currentMood, proposedMood, hasActiveOppressiveInWorld(affectedPlayer));
    }

    public static boolean shouldRetrySecondStrike(
            boolean eligible,
            boolean force,
            boolean selfKill,
            boolean replaying,
            boolean victimStillAlive,
            boolean shieldConsumed
    ) {
        return eligible && !force && !selfKill && !replaying && victimStillAlive && shieldConsumed;
    }

    public static void beginKillAttempt(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer, boolean force) {
        boolean eligible = killer != null
                && victim != killer
                && !force
                && !SECOND_STRIKE_REPLAYING.get()
                && hasEligibleTrait(killer, KillerTraits.SECOND_STRIKE);
        KILL_ATTEMPTS.get().push(eligible ? new KillAttempt(ShieldState.capture(victim)) : KillAttempt.EMPTY);
    }

    public static void finishKillAttempt(
            ServerPlayerEntity victim,
            boolean spawnBody,
            @Nullable ServerPlayerEntity killer,
            Identifier deathReason,
            boolean force
    ) {
        Deque<KillAttempt> attempts = KILL_ATTEMPTS.get();
        KillAttempt attempt = attempts.isEmpty() ? KillAttempt.EMPTY : attempts.pop();
        if (attempts.isEmpty()) {
            KILL_ATTEMPTS.remove();
        }
        if (attempt.before == null || killer == null) {
            return;
        }

        ShieldState after = ShieldState.capture(victim);
        boolean retry = shouldRetrySecondStrike(
                true,
                force,
                victim == killer,
                SECOND_STRIKE_REPLAYING.get(),
                GameFunctions.isPlayerPlayingAndAlive(victim),
                attempt.before.wasConsumedBy(after)
        );
        if (!retry) {
            return;
        }

        SECOND_STRIKE_REPLAYING.set(true);
        try {
            GameFunctions.killPlayer(victim, spawnBody, killer, deathReason, false);
        } finally {
            SECOND_STRIKE_REPLAYING.set(false);
        }
    }

    public static void handleAfterRealKill(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer, Identifier deathReason) {
        if (killer != null && victim != killer && hasEligibleKillerTraitOwner(killer)) {
            TraitPlayerComponent killerTraits = TraitPlayerComponent.KEY.get(killer);
            if (killerTraits.hasActiveTrait(KillerTraits.BLOODTHIRSTY)) {
                killerTraits.incrementBloodthirstyKillCount();
            }
            if (killerTraits.hasActiveTrait(KillerTraits.THE_SHOWMAN)) {
                int reward = showmanReward(countNearbyAlivePlayers(victim, killer));
                if (reward > 0) {
                    PlayerShopComponent.KEY.get(killer).addToBalance(reward);
                }
            }
            if (killerTraits.hasActiveTrait(KillerTraits.PLUNDERER)) {
                PlayerShopComponent victimShop = PlayerShopComponent.KEY.get(victim);
                int stolen = plunderedAmount(victimShop.getBalance());
                if (stolen > 0) {
                    victimShop.setBalance(victimShop.getBalance() - stolen);
                    PlayerShopComponent.KEY.get(killer).addToBalance(stolen);
                }
            }
        }
        handleCorneredAfterRealKill(victim);
    }

    public static boolean isCorneredTeamMember(Role role, Collection<Identifier> traits) {
        if (EffectiveTraitService.hasConscience(traits)) {
            return false;
        }
        return EffectiveTraitService.hasImpostor(traits) || EffectiveTraitService.isOriginalKiller(role);
    }

    public static void updateThrustKnockback(PlayerEntity player) {
        EntityAttributeInstance knockback = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_KNOCKBACK);
        if (knockback == null) {
            return;
        }
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        boolean active = canUseThrust(
                traits.hasActiveTrait(KillerTraits.THRUST),
                hasEligibleKillerTraitOwner(player),
                LastStandFinalMomentService.isFinalMomentLooseEnd(player)
        ) && isHoldingThrustWeapon(player);
        if (active && !knockback.hasModifier(THRUST_KNOCKBACK_MODIFIER_ID)) {
            knockback.addTemporaryModifier(THRUST_KNOCKBACK_MODIFIER);
        } else if (!active && knockback.hasModifier(THRUST_KNOCKBACK_MODIFIER_ID)) {
            knockback.removeModifier(THRUST_KNOCKBACK_MODIFIER_ID);
        }
    }

    private static ShopEntry discountedShopEntry(ShopEntry entry) {
        if (entry instanceof DiscountedShopEntry || entry.price() <= 0) {
            return entry;
        }
        return new DiscountedShopEntry(entry);
    }

    private static int countNearbyAlivePlayers(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer) {
        if (!(victim.getWorld() instanceof ServerWorld world)) {
            return 0;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        double rangeSquared = SHOWMAN_RANGE * SHOWMAN_RANGE;
        int count = 0;
        for (ServerPlayerEntity player : world.getPlayers()) {
            boolean sameAsVictim = player.getUuid().equals(victim.getUuid());
            boolean sameAsKiller = killer != null && player.getUuid().equals(killer.getUuid());
            boolean alive = game.hasAnyRole(player) && !game.isPlayerDead(player.getUuid());
            boolean withinRange = player.squaredDistanceTo(victim) <= rangeSquared;
            if (shouldCountShowmanWitness(sameAsVictim, sameAsKiller, alive, withinRange)) {
                count++;
            }
        }
        return count;
    }

    private static void handleCorneredAfterRealKill(ServerPlayerEntity victim) {
        ServerWorld world = victim.getServerWorld();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (!isCorneredTeamMember(game.getRole(victim), TraitPlayerComponent.KEY.get(victim).getActiveTraitIds())) {
            return;
        }

        int aliveTeamMembers = countAliveCorneredTeamMembers(world, game);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getUuid().equals(victim.getUuid()) || !isAliveInGame(player, game)) {
                continue;
            }
            TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
            if (!traits.hasActiveTrait(KillerTraits.CORNERED) || !hasEligibleKillerTraitOwner(player)) {
                continue;
            }
            boolean becomesSoleTeammate = aliveTeamMembers == 1
                    && isCorneredTeamMember(game.getRole(player), traits.getActiveTraitIds())
                    && !traits.hasCorneredLastKillerRewardPaid();
            PlayerShopComponent.KEY.get(player).addToBalance(corneredReward(becomesSoleTeammate));
            if (becomesSoleTeammate) {
                traits.markCorneredLastKillerRewardPaid();
            }
        }
    }

    private static int countAliveCorneredTeamMembers(ServerWorld world, GameWorldComponent game) {
        int count = 0;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (isAliveInGame(player, game)
                    && isCorneredTeamMember(game.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds())) {
                count++;
            }
        }
        return count;
    }

    private static boolean isAliveInGame(ServerPlayerEntity player, GameWorldComponent game) {
        return game.hasAnyRole(player) && !game.isPlayerDead(player.getUuid());
    }

    private static boolean hasActiveOppressiveInWorld(PlayerEntity affectedPlayer) {
        if (affectedPlayer == null || !(affectedPlayer.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (hasEligibleTrait(player, KillerTraits.OPPRESSIVE)
                    && GameFunctions.isPlayerPlayingAndAlive(player)
                    && !SwallowedPlayerComponent.isPlayerSwallowed(player)
                    && game.hasAnyRole(player)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHoldingThrustWeapon(PlayerEntity player) {
        return KillerWeaponTags.isThrustWeapon(player.getMainHandStack());
    }

    private static boolean hasEligibleTrait(PlayerEntity player, Identifier traitId) {
        return player != null
                && TraitPlayerComponent.KEY.get(player).hasActiveTrait(traitId)
                && hasEligibleKillerTraitOwner(player);
    }

    private static boolean hasEligibleKillerTraitOwner(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        return canSelectKillerTrait(game.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds());
    }

    private record KillAttempt(@Nullable ShieldState before) {
        private static final KillAttempt EMPTY = new KillAttempt(null);
    }

    private record ShieldState(int psychoArmour, boolean ironManBuff, int whiskeyLayers) {
        private static ShieldState capture(ServerPlayerEntity player) {
            PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(player);
            int armour = psycho.getPsychoTicks() > 0 ? psycho.getArmour() : 0;
            boolean ironMan = IronManPlayerComponent.KEY.get(player).hasBuff();
            StatusEffectInstance whiskey = player.getStatusEffect(ModEffects.WHISKEY_SHIELD);
            int whiskeyLayers = whiskey == null ? 0 : whiskey.getAmplifier() + 1;
            return new ShieldState(armour, ironMan, whiskeyLayers);
        }

        private boolean wasConsumedBy(ShieldState after) {
            return after.psychoArmour < psychoArmour
                    || (ironManBuff && !after.ironManBuff)
                    || after.whiskeyLayers < whiskeyLayers;
        }
    }

    private static final class DiscountedShopEntry extends ShopEntry {
        private final ShopEntry delegate;

        private DiscountedShopEntry(ShopEntry delegate) {
            super(
                    delegate.id(),
                    delegate.displayStack(),
                    delegate.getActualStack(),
                    charismaPrice(delegate.price()),
                    delegate.type(),
                    delegate.cooldownTicks(),
                    delegate.initialCooldownTicks(),
                    delegate.maxStock(),
                    null
            );
            this.delegate = delegate;
        }

        @Override
        public boolean onBuy(PlayerEntity player) {
            return delegate.onBuy(player);
        }
    }
}
