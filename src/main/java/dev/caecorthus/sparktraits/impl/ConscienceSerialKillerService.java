package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.mixin.SerialKillerPlayerComponentAccessor;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Handles Serial Killer target rules and Conscience Serial Killer's protected-target rules.
 *  统一处理连环杀手目标判定，以及善良连环杀手的保护目标、追凶奖励与商店规则。 */
public final class ConscienceSerialKillerService {
    public static final int CONSCIENCE_SERIAL_KILLER_REWARD = 150;
    public static final int TARGET_MURDERER_REWARD = 200;
    public static final int CONSCIENCE_SERIAL_KILLER_PSYCHO_PRICE = 250;

    private static final Map<UUID, UUID> targetMurderers = new HashMap<>();

    private ConscienceSerialKillerService() {
    }

    public static void register() {
        BuildShopEntries.EVENT.register(ConscienceSerialKillerService::replacePsychoModePrice);
    }

    public static boolean canBeProtectedTarget(
            Role role,
            Collection<Identifier> traits,
            boolean samePlayer,
            boolean playingAndAlive,
            boolean swallowed
    ) {
        return canBeSerialKillerTarget(role, traits, samePlayer, playingAndAlive, swallowed);
    }

    /** Uses SparkTraits effective alignment for every Serial Killer target pool.
     *  所有连环杀手目标池都按 SparkTraits 的有效阵营判定，避免锁定内鬼好人。 */
    public static boolean canBeSerialKillerTarget(
            Role role,
            Collection<Identifier> traits,
            boolean samePlayer,
            boolean playingAndAlive,
            boolean swallowed
    ) {
        return !samePlayer
                && playingAndAlive
                && !swallowed
                && !isExcludedProtectedTargetRole(role)
                && EffectiveTraitService.isEffectiveCivilian(role, traits);
    }

    public static boolean shouldBlockSerialKillerRetarget(boolean conscienceSerialKiller) {
        return conscienceSerialKiller;
    }

    public static boolean shouldReceivePassiveMoney(boolean conscienceSerialKiller, boolean protectedTargetAlive) {
        return conscienceSerialKiller && protectedTargetAlive;
    }

    /** Gates Wathe's passive killer income while preserving the Conscience Serial Killer exception.
     *  控制 Wathe 杀手随时间加钱：善良普通杀手不拿，善良连环杀手仅在保护目标存活时拿。 */
    public static boolean shouldReceiveKillerPassiveMoney(
            boolean canUseKillerFeatures,
            boolean hasConscience,
            boolean conscienceSerialKiller,
            boolean protectedTargetAlive
    ) {
        if (!canUseKillerFeatures) {
            return false;
        }
        if (!hasConscience) {
            return true;
        }
        return shouldReceivePassiveMoney(conscienceSerialKiller, protectedTargetAlive);
    }

    /** Keeps NoellesRoles target instinct as an always-visible Serial Killer outline.
     *  保留 NoellesRoles 目标本能：无距离限制，并使用连环杀手边框色。 */
    public static boolean shouldUseSerialKillerTargetHighlight(boolean conscienceSerialKiller, boolean currentTarget) {
        return conscienceSerialKiller && currentTarget;
    }

    public static int conscienceKillReward(boolean conscienceSerialKiller, boolean rewardableVictim, boolean targetMurderer) {
        if (!rewardableVictim) {
            return 0;
        }
        if (!conscienceSerialKiller) {
            return GameConstants.MONEY_PER_KILL;
        }
        return targetMurderer ? TARGET_MURDERER_REWARD : CONSCIENCE_SERIAL_KILLER_REWARD;
    }

    public static boolean shouldRewardTargetMurderer(boolean actualTargetMurderer, boolean rewardableVictim) {
        return actualTargetMurderer && rewardableVictim;
    }

    public static int psychoModePrice(boolean conscienceSerialKiller, int originalPrice) {
        return conscienceSerialKiller ? CONSCIENCE_SERIAL_KILLER_PSYCHO_PRICE : originalPrice;
    }

    public static Identifier murdererRoleClue(Identifier murdererRoleId) {
        return murdererRoleId;
    }

    public static boolean isConscienceSerialKiller(PlayerEntity player, GameWorldComponent gameComponent) {
        return player != null
                && gameComponent != null
                && gameComponent.isRole(player, Noellesroles.SERIAL_KILLER)
                && EffectiveTraitService.hasConscience(player);
    }

    public static boolean shouldBlockSerialKillerRetarget(PlayerEntity player) {
        return shouldBlockSerialKillerRetarget(isConscienceSerialKiller(
                player,
                player == null ? null : GameWorldComponent.KEY.get(player.getWorld())
        ));
    }

    public static boolean shouldReceivePassiveMoney(GameWorldComponent gameComponent, PlayerEntity player) {
        if (!isConscienceSerialKiller(player, gameComponent)) {
            return false;
        }
        return shouldReceivePassiveMoney(true, hasLivingProtectedTarget(gameComponent, player));
    }

    public static boolean shouldReceiveKillerPassiveMoney(GameWorldComponent gameComponent, PlayerEntity player) {
        if (gameComponent == null || player == null || !gameComponent.canUseKillerFeatures(player)) {
            return false;
        }
        boolean conscienceSerialKiller = isConscienceSerialKiller(player, gameComponent);
        boolean protectedTargetAlive = conscienceSerialKiller && hasLivingProtectedTarget(gameComponent, player);
        return shouldReceiveKillerPassiveMoney(
                true,
                EffectiveTraitService.hasConscience(player),
                conscienceSerialKiller,
                protectedTargetAlive
        );
    }

    public static boolean hasLivingProtectedTarget(GameWorldComponent gameComponent, PlayerEntity player) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
            return false;
        }
        SerialKillerPlayerComponent serialKiller = SerialKillerPlayerComponent.KEY.get(player);
        UUID targetUuid = serialKiller.getCurrentTarget();
        if (targetUuid == null) {
            return false;
        }
        PlayerEntity target = serverWorld.getPlayerByUuid(targetUuid);
        return target != null && isProtectedTargetValid(player, target, gameComponent);
    }

    public static List<UUID> protectedTargets(ServerWorld world, GameWorldComponent gameComponent, PlayerEntity serialKiller) {
        return serialKillerTargets(world, gameComponent, serialKiller);
    }

    public static List<UUID> serialKillerTargets(ServerWorld world, GameWorldComponent gameComponent, PlayerEntity serialKiller) {
        List<UUID> eligibleTargets = new ArrayList<>();
        if (world == null || gameComponent == null || serialKiller == null) {
            return eligibleTargets;
        }
        for (UUID uuid : gameComponent.getAllPlayers()) {
            PlayerEntity target = world.getPlayerByUuid(uuid);
            if (target != null && isSerialKillerTargetValid(serialKiller, target, gameComponent)) {
                eligibleTargets.add(uuid);
            }
        }
        return eligibleTargets;
    }

    public static boolean isProtectedTargetValid(ServerWorld world, GameWorldComponent gameComponent, PlayerEntity serialKiller, UUID targetUuid) {
        return isSerialKillerTargetValid(world, gameComponent, serialKiller, targetUuid);
    }

    public static boolean isSerialKillerTargetValid(ServerWorld world, GameWorldComponent gameComponent, PlayerEntity serialKiller, UUID targetUuid) {
        if (world == null || targetUuid == null || serialKiller == null) {
            return false;
        }
        PlayerEntity target = world.getPlayerByUuid(targetUuid);
        return target != null && isSerialKillerTargetValid(serialKiller, target, gameComponent);
    }

    public static void normalizeTargets(ServerWorld world, GameWorldComponent gameComponent, Collection<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            if (isConscienceSerialKiller(player, gameComponent)) {
                normalizeTarget(world, gameComponent, player);
            }
        }
    }

    public static void normalizeTarget(ServerWorld world, GameWorldComponent gameComponent, ServerPlayerEntity serialKiller) {
        SerialKillerPlayerComponent component = SerialKillerPlayerComponent.KEY.get(serialKiller);
        UUID currentTarget = component.getCurrentTarget();
        if (currentTarget != null) {
            PlayerEntity target = world.getPlayerByUuid(currentTarget);
            if (target != null && isSerialKillerTargetValid(serialKiller, target, gameComponent)) {
                return;
            }
        }
        setProtectedTarget(component, chooseProtectedTarget(world, gameComponent, serialKiller));
        clearMurdererClue(serialKiller);
    }

    public static void handleAfterKill(ServerPlayerEntity victim, ServerPlayerEntity killer, Identifier deathReason) {
        if (!(victim.getWorld() instanceof ServerWorld world)) {
            return;
        }
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(world);
        recordProtectedTargetDeath(world, gameComponent, victim, killer);
        clearDeadTrackedMurderer(world, victim);
        if (victim != null) {
            targetMurderers.entrySet().removeIf(entry -> entry.getKey().equals(victim.getUuid()));
        }
    }

    public static int rewardForConscienceKill(ServerPlayerEntity killer, ServerPlayerEntity victim, boolean rewardableVictim) {
        if (killer == null || victim == null) {
            return conscienceKillReward(false, rewardableVictim, false);
        }
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(killer.getWorld());
        boolean conscienceSerialKiller = isConscienceSerialKiller(killer, gameComponent);
        boolean targetMurderer = isTrackedTargetMurderer(killer, victim);
        int reward = conscienceKillReward(conscienceSerialKiller, rewardableVictim, targetMurderer);
        if (shouldRewardTargetMurderer(targetMurderer, rewardableVictim)) {
            clearMurdererClue(killer);
        }
        return reward;
    }

    public static void clearPlayer(PlayerEntity player) {
        if (player == null) {
            return;
        }
        targetMurderers.remove(player.getUuid());
        TraitPlayerComponent.KEY.get(player).setSerialKillerMurdererRole(null);
    }

    public static void clearAll() {
        targetMurderers.clear();
    }

    private static void replacePsychoModePrice(PlayerEntity player, BuildShopEntries.ShopContext context) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getWorld());
        boolean conscienceSerialKiller = isConscienceSerialKiller(player, gameComponent);
        if (!conscienceSerialKiller) {
            return;
        }
        List<ShopEntry> entries = context.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            ShopEntry entry = entries.get(i);
            if (!"psycho_mode".equals(entry.id()) && !entry.stack().isOf(WatheItems.PSYCHO_MODE)) {
                continue;
            }
            ShopEntry.Builder builder = new ShopEntry.Builder(
                    entry.id(),
                    entry.displayStack().copy(),
                    psychoModePrice(true, entry.price()),
                    entry.type()
            )
                    .cooldown(entry.cooldownTicks())
                    .initialCooldown(entry.initialCooldownTicks())
                    .onBuy(PlayerShopComponent::usePsychoMode);
            if (entry.hasStockLimit()) {
                builder.stock(entry.maxStock());
            }
            entries.set(i, builder.build());
            return;
        }
    }

    private static void recordProtectedTargetDeath(
            ServerWorld world,
            GameWorldComponent gameComponent,
            ServerPlayerEntity victim,
            ServerPlayerEntity killer
    ) {
        if (killer == null) {
            return;
        }
        for (UUID uuid : gameComponent.getAllWithRole(Noellesroles.SERIAL_KILLER)) {
            if (!(world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity serialKiller)) {
                continue;
            }
            if (!isConscienceSerialKiller(serialKiller, gameComponent) || serialKiller.getUuid().equals(killer.getUuid())) {
                continue;
            }
            SerialKillerPlayerComponent component = SerialKillerPlayerComponent.KEY.get(serialKiller);
            if (!victim.getUuid().equals(component.getCurrentTarget())) {
                continue;
            }
            targetMurderers.put(serialKiller.getUuid(), killer.getUuid());
            Role killerRole = gameComponent.getRole(killer);
            TraitPlayerComponent.KEY.get(serialKiller).setSerialKillerMurdererRole(
                    killerRole == null ? null : murdererRoleClue(killerRole.identifier())
            );
        }
    }

    private static boolean isTrackedTargetMurderer(ServerPlayerEntity serialKiller, ServerPlayerEntity victim) {
        return victim.getUuid().equals(targetMurderers.get(serialKiller.getUuid()));
    }

    private static void clearDeadTrackedMurderer(ServerWorld world, ServerPlayerEntity victim) {
        List<UUID> clearedSerialKillers = new ArrayList<>();
        targetMurderers.forEach((serialKillerUuid, murdererUuid) -> {
            if (victim.getUuid().equals(murdererUuid)) {
                clearedSerialKillers.add(serialKillerUuid);
            }
        });
        for (UUID serialKillerUuid : clearedSerialKillers) {
            targetMurderers.remove(serialKillerUuid);
            if (world.getPlayerByUuid(serialKillerUuid) instanceof ServerPlayerEntity serialKiller) {
                TraitPlayerComponent.KEY.get(serialKiller).setSerialKillerMurdererRole(null);
            }
        }
    }

    private static boolean isProtectedTargetValid(PlayerEntity serialKiller, PlayerEntity target, GameWorldComponent gameComponent) {
        return isSerialKillerTargetValid(serialKiller, target, gameComponent);
    }

    private static boolean isSerialKillerTargetValid(PlayerEntity serialKiller, PlayerEntity target, GameWorldComponent gameComponent) {
        if (serialKiller == null || target == null || gameComponent == null) {
            return false;
        }
        return canBeSerialKillerTarget(
                gameComponent.getRole(target),
                TraitPlayerComponent.KEY.get(target).getActiveTraitIds(),
                serialKiller.getUuid().equals(target.getUuid()),
                GameFunctions.isPlayerPlayingAndAlive(target),
                SwallowedPlayerComponent.isPlayerSwallowed(target)
        );
    }

    private static UUID chooseProtectedTarget(ServerWorld world, GameWorldComponent gameComponent, ServerPlayerEntity serialKiller) {
        List<UUID> eligibleTargets = protectedTargets(world, gameComponent, serialKiller);
        if (eligibleTargets.isEmpty()) {
            return null;
        }
        return eligibleTargets.get(world.getRandom().nextInt(eligibleTargets.size()));
    }

    private static void setProtectedTarget(SerialKillerPlayerComponent component, UUID targetUuid) {
        ((SerialKillerPlayerComponentAccessor) component).sparktraits$setCurrentTarget(targetUuid);
        component.sync();
    }

    private static void clearMurdererClue(ServerPlayerEntity serialKiller) {
        targetMurderers.remove(serialKiller.getUuid());
        TraitPlayerComponent.KEY.get(serialKiller).setSerialKillerMurdererRole(null);
    }

    private static boolean isExcludedProtectedTargetRole(Role role) {
        return role == null
                || role == Noellesroles.UNDERCOVER
                || role == Noellesroles.BODYGUARD
                || role == Noellesroles.SURVIVAL_MASTER;
    }
}
