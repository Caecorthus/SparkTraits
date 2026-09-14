package dev.caecorthus.sparktraits.impl.traits.killer.escape;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.component.TraitWorldComponent;
import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraitService;
import dev.caecorthus.sparktraits.impl.traits.killer.KillerTraits;
import dev.caecorthus.sparktraits.impl.traits.civilian.depression.DepressionTraitService;
import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import dev.caecorthus.sparktraits.mixin.LastEscapePlayerManagerAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns the finite escape phase separately from the round's consumed/visual memory. */
public final class LastEscapeService {
    private static final Identifier SPEED = SparkTraits.id("last_escape_speed");
    private static final Map<UUID, ServerPlayerEntity> offline = new HashMap<>();
    private static boolean registered;
    private static net.minecraft.server.MinecraftServer runningServer;

    private LastEscapeService() {}

    public static void register() {
        if (registered) return;
        registered = true;
        SparkFactionApi.registerEntityCollisionExemption(entity -> entity instanceof PlayerEntity player && isActive(player));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> runningServer = server);
        GameEvents.ON_GAME_START.register(mode -> {
            if (runningServer != null) runningServer.getWorlds().forEach(LastEscapeService::clearRound);
        });
        ServerTickEvents.END_WORLD_TICK.register(LastEscapeService::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            offline.remove(handler.player.getUuid());
            updateSpeed(handler.player);
            TraitWorldComponent.KEY.get(handler.player.getWorld()).sync();
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (isActive(handler.player)) offline.put(handler.player.getUuid(), handler.player);
            removeSpeed(handler.player);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { offline.clear(); runningServer = null; });
    }

    public static boolean isActive(PlayerEntity player) {
        return player != null && TraitWorldComponent.KEY.get(player.getWorld()).isLastEscapeActive(player.getUuid());
    }

    /** Bilateral vehicle/direct-push supplement to FactionAPI; no block collision changes.
     *  双向补齐载具/直接推挤路径，不改变方块碰撞。 */
    public static boolean blocksEntityCollision(Entity first, Entity second) {
        return (first instanceof PlayerEntity firstPlayer && isActive(firstPlayer))
                || (second instanceof PlayerEntity secondPlayer && isActive(secondPlayer));
    }

    public static boolean hasGrayscale(PlayerEntity player) {
        return player != null && TraitWorldComponent.KEY.get(player.getWorld()).hasLastEscapeGrayscale(player.getUuid());
    }

    public static boolean isTrainDeath(Identifier reason) {
        return GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(reason);
    }

    public static boolean blocksDeath(PlayerEntity player, Identifier reason) {
        return isActive(player) && !isTrainDeath(reason);
    }

    public static boolean tryActivate(ServerPlayerEntity player) {
        var traits = TraitPlayerComponent.KEY.get(player);
        var game = GameWorldComponent.KEY.get(player.getWorld());
        if (!GameFunctions.isPlayerPlayingAndAlive(player)
                || !traits.hasActiveTrait(KillerTraits.LAST_ESCAPE)
                || !KillerTraitService.canSelectKillerTrait(game.getRole(player), traits.getActiveTraitIds())) return false;
        var worldState = TraitWorldComponent.KEY.get(player.getWorld());
        if (!worldState.lastEscape().activate(player.getUuid(), player.getWorld().getTime())) return false;
        // Mark before any psycho cleanup: armour disappearance here is not a shield absorption.
        KillerTraitService.terminateKillAttempt();
        LastEscapeKillScope.halveCooldowns(player);
        player.clearActiveItem(); // stopUsingItem would release a queued knife/bow attack.
        if (!DepressionTraitService.endPsychoForLastEscape(player)) {
            var psycho = PlayerPsychoComponent.KEY.get(player);
            if (psycho.getPsychoTicks() > 0) {
                psycho.stopPsycho();
                psycho.sync();
            }
        }
        updateSpeed(player);
        traits.revealToOwner(KillerTraits.LAST_ESCAPE);
        player.currentScreenHandler.syncState();
        worldState.sync();
        return true;
    }

    public static void onDeath(ServerPlayerEntity player) {
        var state = TraitWorldComponent.KEY.get(player.getWorld());
        state.lastEscape().onDeath(player.getUuid());
        offline.remove(player.getUuid());
        removeSpeed(player);
        state.sync();
    }

    public static void clearRound(ServerWorld world) {
        TraitWorldComponent.KEY.get(world).lastEscape().clear();
        world.getPlayers().forEach(LastEscapeService::removeSpeed);
        offline.entrySet().removeIf(entry -> entry.getValue().getWorld() == world);
        TraitWorldComponent.KEY.get(world).sync();
    }

    private static void tick(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (isActive(player)) player.clearActiveItem();
            updateSpeed(player);
        }
        // Wathe's disconnect death was deferred by immunity. Settle it once, at the original
        // deadline, using Wathe's normal terminal route rather than leaving an immortal member.
        for (ServerPlayerEntity player : java.util.List.copyOf(offline.values())) {
            if (player.getWorld() != world) continue;
            var manager = world.getServer().getPlayerManager();
            // JOIN usually drops the old entity; also guard the pre-JOIN replacement window.
            // JOIN 通常会移除旧实体；仍须防止重连实体在 JOIN 前被旧记录结算。
            if (manager.getPlayer(player.getUuid()) != null) {
                offline.remove(player.getUuid());
                continue;
            }
            if (isActive(player)) continue;
            GameFunctions.killPlayer(player, true, null, GameConstants.DeathReasons.ESCAPED, true);
            if (!GameWorldComponent.KEY.get(world).isPlayerDead(player.getUuid())) continue;
            offline.remove(player.getUuid());
            // The disconnect save preceded this death/reset and still contains the old inventory
            // and traits. Never overwrite a replacement entity's data after death callbacks.
            // 断线存档早于本次死亡/重置，仍含旧物品与词条；死亡回调后不可覆盖重连实体。
            if (manager.getPlayer(player.getUuid()) == null) {
                ((LastEscapePlayerManagerAccessor) manager).sparktraits$saveLastEscapePlayerData(player);
            }
        }
    }

    private static void removeSpeed(ServerPlayerEntity player) {
        var speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED);
    }

    private static void updateSpeed(ServerPlayerEntity player) {
        var speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed == null) return;
        // Owned Speed-II-equivalent contribution: never remove or weaken another mod's potion.
        var effect = player.getStatusEffect(StatusEffects.SPEED);
        double existing = effect == null ? 1.0 : 1.0 + 0.2 * (effect.getAmplifier() + 1);
        double amount = isActive(player) ? Math.max(0.0, 1.4 / existing - 1.0) : 0.0;
        var previous = speed.getModifier(SPEED);
        if (previous != null && Double.compare(previous.value(), amount) == 0) return;
        speed.removeModifier(SPEED);
        if (amount > 0.0) speed.addTemporaryModifier(new EntityAttributeModifier(SPEED, amount,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
