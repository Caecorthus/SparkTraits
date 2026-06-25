package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

/**
 * Shared rules for good-side SparkTraits traits.
 * SparkTraits 好人阵营天赋的共享规则入口；保持原始阵营判断，避免扩大到内鬼或中立。
 */
public final class GoodTraitService {
    public static final int EXTROVERTED_COLOR = 0x54C6A4;
    public static final int INTROVERTED_COLOR = 0x8C7AE6;
    public static final int MONEY_TREE_COLOR = 0x68B84F;
    public static final int FOCUS_COLOR = 0x5DADEC;
    public static final int SOCIAL_RADIUS = 8;
    public static final int MONEY_TREE_REWARD = 5;
    public static final int MONEY_TREE_INTERVAL_TICKS = 20 * 30;
    private static final double SOCIAL_RADIUS_SQUARED = SOCIAL_RADIUS * SOCIAL_RADIUS;

    private GoodTraitService() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(GoodTraitService::tickWorld);
    }

    public static boolean canSelectGoodTrait(Role role, Collection<Identifier> selectedTraits) {
        return EffectiveTraitService.isOriginalCivilian(role)
                && !EffectiveTraitService.hasImpostor(safeTraits(selectedTraits));
    }

    /** Good-trait gate for effects that should not attach to Undercover.
     *  不给卧底使用的好人词条资格入口。 */
    public static boolean canSelectNonUndercoverGoodTrait(Role role, Collection<Identifier> selectedTraits) {
        return canSelectGoodTrait(role, selectedTraits) && !EffectiveTraitService.isUndercover(role);
    }

    public static boolean canSelectMoneyTree(
            Role role,
            Collection<Identifier> selectedTraits,
            boolean canSeeMoney
    ) {
        return canSelectGoodTrait(role, selectedTraits)
                && (canSeeMoney || EffectiveTraitService.hasNativeTaskMoneyReward(role));
    }

    public static boolean canSelectFocus(Role role, Collection<Identifier> selectedTraits) {
        return canSelectNonUndercoverGoodTrait(role, selectedTraits) && !isWathePoliceRole(role);
    }

    public static boolean shouldPreventSocialMoodDrain(Collection<Identifier> traits, int nearbyOtherPlayers) {
        Collection<Identifier> safeTraits = safeTraits(traits);
        return (safeTraits.contains(GoodTraits.EXTROVERTED) && nearbyOtherPlayers >= 2)
                || (safeTraits.contains(GoodTraits.INTROVERTED) && nearbyOtherPlayers <= 1);
    }

    public static float socialMoodAdjustedMood(
            float currentMood,
            float proposedMood,
            Collection<Identifier> traits,
            int nearbyOtherPlayers
    ) {
        if (proposedMood >= currentMood || !shouldPreventSocialMoodDrain(traits, nearbyOtherPlayers)) {
            return proposedMood;
        }
        return currentMood;
    }

    public static float socialMoodAdjustedMood(float currentMood, float proposedMood, PlayerEntity player) {
        if (proposedMood >= currentMood || player == null) {
            return proposedMood;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Collection<Identifier> traits = traitsOf(player);
        if (!canSelectGoodTrait(game.getRole(player), traits)) {
            return proposedMood;
        }
        return socialMoodAdjustedMood(currentMood, proposedMood, traits, nearbyAliveOtherPlayers(player, game));
    }

    public static boolean shouldPreventGunMoodPenalty(Role role, Collection<Identifier> traits) {
        Collection<Identifier> safeTraits = safeTraits(traits);
        return safeTraits.contains(GoodTraits.FOCUS) && canSelectFocus(role, safeTraits);
    }

    static boolean canReceiveMoneyTree(ServerPlayerEntity player, GameWorldComponent game) {
        if (player == null || game == null || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return false;
        }
        TraitPlayerComponent traits = TraitPlayerComponent.KEY.get(player);
        if (!traits.hasActiveTrait(GoodTraits.MONEY_TREE)) {
            return false;
        }
        Role role = game.getRole(player);
        return canSelectMoneyTree(
                role,
                traits.getActiveTraitIds(),
                GlobalTraitService.canSeeMoneyForTrait(player, game, role)
        );
    }

    private static void tickWorld(ServerWorld world) {
        long time = world.getTime();
        if (time == 0 || time % MONEY_TREE_INTERVAL_TICKS != 0) {
            return;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        if (game == null || !game.isRunning()) {
            return;
        }
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (canReceiveMoneyTree(player, game)) {
                PlayerShopComponent.KEY.get(player).addToBalance(MONEY_TREE_REWARD);
            }
        }
    }

    private static int nearbyAliveOtherPlayers(PlayerEntity player, GameWorldComponent game) {
        int count = 0;
        for (PlayerEntity other : player.getWorld().getPlayers()) {
            if (other == player
                    || !GameFunctions.isPlayerPlayingAndAlive(other)
                    || !game.hasAnyRole(other)
                    || player.squaredDistanceTo(other) > SOCIAL_RADIUS_SQUARED) {
                continue;
            }
            count++;
        }
        return count;
    }

    private static boolean isWathePoliceRole(Role role) {
        return role == WatheRoles.VIGILANTE || role == WatheRoles.VETERAN;
    }

    private static Collection<Identifier> traitsOf(PlayerEntity player) {
        if (player == null) {
            return List.of();
        }
        return TraitPlayerComponent.KEY.get(player).getActiveTraitIds();
    }

    private static Collection<Identifier> safeTraits(Collection<Identifier> traits) {
        return traits == null ? List.of() : traits;
    }
}
