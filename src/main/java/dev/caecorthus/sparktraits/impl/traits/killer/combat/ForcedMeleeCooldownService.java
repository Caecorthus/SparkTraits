package dev.caecorthus.sparktraits.impl.traits.killer.combat;

import dev.caecorthus.sparktraits.net.combat.ForcedMeleeCooldownPayload;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.api.event.ResetPlayer;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Independent item-type deadlines survive stack replacement, normal cooldown writes and disconnects. */
public final class ForcedMeleeCooldownService {
    private static final Map<UUID, Penalties> SERVER = new HashMap<>();
    private static final Map<Identifier, Long> CLIENT = new HashMap<>();
    private static UUID clientOwner;
    private static boolean registered;

    private ForcedMeleeCooldownService() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        PayloadTypeRegistry.playS2C().register(ForcedMeleeCooldownPayload.ID, ForcedMeleeCooldownPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SERVER.clear());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            restoreFloors(player);
            sync(player); // Empty snapshots explicitly reset stale owner state on a new connection.
        });
        ResetPlayer.EVENT.register(ForcedMeleeCooldownService::clearPlayer);
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof ServerWorld serverWorld) clearRound(serverWorld);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.getOverworld().getTime();
            SERVER.values().forEach(state -> state.deadlines.values().removeIf(deadline -> deadline <= now));
            SERVER.values().removeIf(state -> state.deadlines.isEmpty());
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
                    if (SERVER.containsKey(player.getUuid())) clearPlayer(player);
                    continue;
                }
                restoreFloors(player);
                if (server.getTicks() % 20 == 0) sync(player);
            }
        });
    }

    public static int remainingTicks(PlayerEntity player, ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0 : remainingTicks(player, stack.getItem());
    }

    public static int remainingTicks(PlayerEntity player, Item item) {
        if (player == null || item == null) return 0;
        if (player.getWorld().isClient) {
            if (!player.getUuid().equals(clientOwner)) return 0;
            return CombatTimingRules.remaining(CLIENT.getOrDefault(Registries.ITEM.getId(item), 0L), player.age);
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return 0;
        Penalties state = SERVER.get(player.getUuid());
        if (state == null || !state.world.equals(player.getWorld().getRegistryKey())) return 0;
        return CombatTimingRules.remaining(state.deadlines.getOrDefault(item, 0L), now(serverPlayer));
    }

    public static void impose(ServerPlayerEntity player, Item item, int ticks) {
        if (ticks <= 0) return;
        Penalties state = SERVER.computeIfAbsent(player.getUuid(), ignored ->
                new Penalties(player.getWorld().getRegistryKey(), new HashMap<>()));
        state.deadlines.merge(item, now(player) + ticks, Math::max);
        ExactItemCooldowns.setExact(player, item,
                Math.max(remainingTicks(player, item), ExactItemCooldowns.remainingTicks(player, item)));
        sync(player);
    }

    /** Called by the client receiver on its main thread; no client classes enter the common initializer. */
    public static void acceptClientSnapshot(PlayerEntity player, ForcedMeleeCooldownPayload payload) {
        clearClient();
        if (player == null || !player.getUuid().equals(payload.owner())) return;
        clientOwner = payload.owner();
        payload.remaining().forEach((item, ticks) -> CLIENT.put(item, (long) player.age + ticks));
    }

    public static void clearClient() {
        CLIENT.clear();
        clientOwner = null;
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        Penalties state = SERVER.remove(player.getUuid());
        if (state != null) {
            // Remove only the floor we installed; do not erase a longer normal cooldown.
            state.deadlines.forEach((item, deadline) -> {
                int floor = CombatTimingRules.remaining(deadline, now(player));
                if (ExactItemCooldowns.remainingTicks(player, item) <= floor) player.getItemCooldownManager().remove(item);
            });
        }
        sync(player);
    }

    public static void clearRound(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) clearPlayer(player);
        SERVER.values().removeIf(state -> state.world.equals(world.getRegistryKey()));
    }

    private static void restoreFloors(ServerPlayerEntity player) {
        Penalties state = SERVER.get(player.getUuid());
        if (state == null) return;
        state.deadlines.forEach((item, deadline) -> {
            int floor = remainingTicks(player, item);
            if (floor > ExactItemCooldowns.remainingTicks(player, item)) ExactItemCooldowns.setExact(player, item, floor);
        });
    }

    private static void sync(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, ForcedMeleeCooldownPayload.ID)) return;
        Map<Identifier, Integer> remaining = new HashMap<>();
        Penalties state = SERVER.get(player.getUuid());
        if (state != null) state.deadlines.forEach((item, deadline) -> {
            int ticks = remainingTicks(player, item);
            if (ticks > 0) remaining.put(Registries.ITEM.getId(item), ticks);
        });
        ServerPlayNetworking.send(player, new ForcedMeleeCooldownPayload(player.getUuid(), remaining));
    }

    private static long now(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTime();
    }

    private record Penalties(RegistryKey<World> world, Map<Item, Long> deadlines) {
    }
}
