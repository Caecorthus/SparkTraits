package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.mixin.ItemCooldownEntryAccessor;
import dev.caecorthus.sparktraits.mixin.ItemCooldownManagerAccessor;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.GrenadeEntity;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

/** Owns the Bomb Maniac shop purchase, temporary grenade marker, and runtime state.
 *  统一管理炸弹狂商店购买、临时手雷标记与运行时状态。 */
public final class ConscienceBomberFrenzyService {
    public static final Identifier SHOP_ID = SparkTraits.id("bomb_maniac");

    private static final String MARKER_KEY = "sparktraits_bomb_maniac";
    private static final String OWNER_KEY = "sparktraits_bomb_maniac_owner";
    private static final String EXPIRES_KEY = "sparktraits_bomb_maniac_expires";
    private static final Map<UUID, Long> ACTIVE_UNTIL = new HashMap<>();
    private static final ThreadLocal<Deque<UUID>> GRENADE_COOLDOWN_SYNC_SUPPRESSIONS = new ThreadLocal<>();

    private ConscienceBomberFrenzyService() {
    }

    public static void register() {
        BuildShopEntries.EVENT.register(ConscienceBomberFrenzyService::addShopEntry);
        ServerTickEvents.END_WORLD_TICK.register(ConscienceBomberFrenzyService::tickWorld);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> clearPlayer(handler.getPlayer()));
    }

    record Marker(UUID ownerUuid, long expiresAtTick) {
    }

    static void writeMarker(NbtCompound nbt, UUID ownerUuid, long expiresAtTick) {
        nbt.putBoolean(MARKER_KEY, true);
        nbt.putUuid(OWNER_KEY, ownerUuid);
        nbt.putLong(EXPIRES_KEY, expiresAtTick);
    }

    static @Nullable Marker readMarker(NbtCompound nbt) {
        if (!nbt.getBoolean(MARKER_KEY)
                || !nbt.containsUuid(OWNER_KEY)
                || !nbt.contains(EXPIRES_KEY)) {
            return null;
        }
        return new Marker(nbt.getUuid(OWNER_KEY), nbt.getLong(EXPIRES_KEY));
    }

    public static boolean isMarkedGrenade(ItemStack stack) {
        return marker(stack) != null;
    }

    public static boolean canUseMarkedGrenade(PlayerEntity player, ItemStack stack) {
        Marker marker = marker(stack);
        if (marker == null
                || !marker.ownerUuid().equals(player.getUuid())
                || !ConscienceBomberFrenzyRules.isActive(
                        player.getWorld().getTime(), marker.expiresAtTick())) {
            return false;
        }
        return player.getWorld().isClient
                || ACTIVE_UNTIL.getOrDefault(player.getUuid(), Long.MIN_VALUE) == marker.expiresAtTick();
    }

    public record CooldownSnapshot(@Nullable Object entry, int remainingTicks) {
    }

    public record GrenadeUseSnapshot(
            boolean bombManiac,
            ItemStack stack,
            int originalCount,
            @Nullable CooldownSnapshot cooldown
    ) {
    }

    public static CooldownSnapshot snapshotGrenadeCooldown(PlayerEntity player) {
        ItemCooldownManager manager = player.getItemCooldownManager();
        ItemCooldownManagerAccessor access = (ItemCooldownManagerAccessor) manager;
        Object entry = access.sparktraits$getEntries().get(WatheItems.GRENADE);
        int remaining = entry instanceof ItemCooldownEntryAccessor entryAccess
                ? Math.max(0, entryAccess.sparktraits$getEndTick() - access.sparktraits$getTick())
                : 0;
        return new CooldownSnapshot(entry, remaining);
    }

    /** Restores the original entry object so cooldown modifiers cannot shorten it twice.
     *  恢复原始冷却条目，避免冷却修改器对同一冷却重复缩短。 */
    public static void restoreGrenadeCooldown(PlayerEntity player, CooldownSnapshot snapshot) {
        ItemCooldownManager manager = player.getItemCooldownManager();
        ItemCooldownManagerAccessor access = (ItemCooldownManagerAccessor) manager;
        restoreCooldownEntry(access.sparktraits$getEntries(), WatheItems.GRENADE, snapshot);
    }

    static <K> void restoreCooldownEntry(Map<K, Object> entries, K item, CooldownSnapshot snapshot) {
        if (snapshot.entry() == null || snapshot.remainingTicks() <= 0) {
            entries.remove(item);
            return;
        }
        entries.put(item, snapshot.entry());
    }

    static int restoredGrenadeStackCount(boolean bombManiac, int originalCount, int currentCount) {
        return bombManiac ? originalCount : currentCount;
    }

    public static void restoreGrenadeStackCount(GrenadeUseSnapshot snapshot) {
        snapshot.stack().setCount(restoredGrenadeStackCount(
                snapshot.bombManiac(),
                snapshot.originalCount(),
                snapshot.stack().getCount()
        ));
    }

    /** Prevents transient server packets only while a marked throw restores exact local cooldown state.
     *  仅在已标记投掷恢复精确本地冷却状态时抑制临时服务端数据包。 */
    public static CooldownSyncSuppression suppressGrenadeCooldownSync(PlayerEntity player, boolean active) {
        return suppressGrenadeCooldownSync(player.getUuid(), active);
    }

    static CooldownSyncSuppression suppressGrenadeCooldownSync(UUID ownerUuid, boolean active) {
        if (active) {
            Deque<UUID> owners = GRENADE_COOLDOWN_SYNC_SUPPRESSIONS.get();
            if (owners == null) {
                owners = new ArrayDeque<>();
                GRENADE_COOLDOWN_SYNC_SUPPRESSIONS.set(owners);
            }
            owners.push(ownerUuid);
        }
        return new CooldownSyncSuppression(ownerUuid, active);
    }

    static boolean isGrenadeCooldownSyncSuppressed(UUID ownerUuid) {
        Deque<UUID> owners = GRENADE_COOLDOWN_SYNC_SUPPRESSIONS.get();
        return owners != null && owners.contains(ownerUuid);
    }

    public static boolean shouldSuppressGrenadeCooldownSync(PlayerEntity player, Item item) {
        return item == WatheItems.GRENADE
                && isGrenadeCooldownSyncSuppressed(player.getUuid());
    }

    public static final class CooldownSyncSuppression implements AutoCloseable {
        private final UUID ownerUuid;
        private final boolean active;
        private boolean closed;

        private CooldownSyncSuppression(UUID ownerUuid, boolean active) {
            this.ownerUuid = ownerUuid;
            this.active = active;
        }

        @Override
        public void close() {
            if (!active || closed) {
                return;
            }
            closed = true;
            Deque<UUID> owners = GRENADE_COOLDOWN_SYNC_SUPPRESSIONS.get();
            if (owners == null) {
                return;
            }
            owners.removeFirstOccurrence(ownerUuid);
            if (owners.isEmpty()) {
                GRENADE_COOLDOWN_SYNC_SUPPRESSIONS.remove();
            }
        }
    }

    /** Delegates Wathe's complete throw path while changing only the snapshotted launch properties.
     *  委托 Wathe 的完整投掷流程，只修改已快照的发射属性。 */
    public static void configureGrenadeLaunch(
            GrenadeEntity grenade,
            Entity user,
            float pitch,
            float yaw,
            float roll,
            float originalSpeed,
            float divergence,
            boolean bombManiac
    ) {
        grenade.setVelocity(
                user,
                pitch,
                yaw,
                roll,
                bombManiac ? ConscienceBomberFrenzyRules.THROW_SPEED : originalSpeed,
                divergence
        );
        if (bombManiac) {
            ((BombManiacGrenadeAccess) grenade).sparktraits$setBombManiac(true);
        }
    }

    static boolean startMode(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (!ConscienceBomberFrenzyRules.canBuy(
                game.isRole(player, Noellesroles.BOMBER),
                EffectiveTraitService.hasConscience(player))
                || ACTIVE_UNTIL.containsKey(player.getUuid())) {
            return false;
        }

        int freeSlot = IntStream.range(0, 9)
                .filter(slot -> player.getInventory().getStack(slot).isEmpty())
                .findFirst()
                .orElse(-1);
        if (freeSlot < 0) {
            return false;
        }

        long expiresAtTick = player.getWorld().getTime()
                + ConscienceBomberFrenzyRules.MODE_DURATION_TICKS;
        ACTIVE_UNTIL.put(player.getUuid(), expiresAtTick);
        player.getInventory().setStack(freeSlot, markedGrenade(player.getUuid(), expiresAtTick));
        return true;
    }

    public static void clearPlayer(PlayerEntity player) {
        if (player == null) {
            return;
        }
        ACTIVE_UNTIL.remove(player.getUuid());
        removeMarkedGrenades(player);
    }

    public static void clearAll(ServerWorld world) {
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            clearPlayer(player);
        }
        ACTIVE_UNTIL.clear();
    }

    public static void tickWorld(ServerWorld world) {
        long currentTick = world.getTime();
        ACTIVE_UNTIL.entrySet().removeIf(entry ->
                !ConscienceBomberFrenzyRules.isActive(currentTick, entry.getValue()));
        for (ServerPlayerEntity player : world.getPlayers()) {
            retainOnlyValidMarkedGrenade(player, currentTick);
        }
    }

    private static void addShopEntry(PlayerEntity player, BuildShopEntries.ShopContext context) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        boolean eligible = ConscienceBomberFrenzyRules.canBuy(
                game.isRole(player, Noellesroles.BOMBER),
                EffectiveTraitService.hasConscience(player)
        );
        if (!eligible || context.getEntries().stream()
                .anyMatch(entry -> SHOP_ID.toString().equals(entry.id()))) {
            return;
        }

        ItemStack display = Items.TNT.getDefaultStack();
        display.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.translatable("shop.sparktraits.bomb_maniac").formatted(Formatting.RED)
        );
        context.addEntry(new ShopEntry.Builder(
                SHOP_ID.toString(),
                display,
                ConscienceBomberFrenzyRules.PRICE,
                ShopEntry.Type.WEAPON
        )
                .cooldown(ConscienceBomberFrenzyRules.SHOP_COOLDOWN_TICKS)
                .onBuy(candidate -> candidate instanceof ServerPlayerEntity serverPlayer
                        && startMode(serverPlayer))
                .build());
    }

    private static ItemStack markedGrenade(UUID ownerUuid, long expiresAtTick) {
        ItemStack stack = WatheItems.GRENADE.getDefaultStack();
        NbtComponent.set(
                DataComponentTypes.CUSTOM_DATA,
                stack,
                nbt -> writeMarker(nbt, ownerUuid, expiresAtTick)
        );
        return stack;
    }

    private static @Nullable Marker marker(ItemStack stack) {
        if (!stack.isOf(WatheItems.GRENADE)) {
            return null;
        }
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null ? null : readMarker(data.copyNbt());
    }

    private static void removeMarkedGrenades(PlayerEntity player) {
        for (List<ItemStack> inventory : inventories(player)) {
            for (int slot = 0; slot < inventory.size(); slot++) {
                if (isMarkedGrenade(inventory.get(slot))) {
                    inventory.set(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    private static void retainOnlyValidMarkedGrenade(PlayerEntity player, long currentTick) {
        Long activeExpiry = ACTIVE_UNTIL.get(player.getUuid());
        boolean retained = false;
        for (List<ItemStack> inventory : inventories(player)) {
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.get(slot);
                Marker marker = marker(stack);
                if (marker == null) {
                    continue;
                }
                boolean valid = !retained
                        && activeExpiry != null
                        && marker.ownerUuid().equals(player.getUuid())
                        && marker.expiresAtTick() == activeExpiry
                        && ConscienceBomberFrenzyRules.isActive(currentTick, marker.expiresAtTick());
                if (valid) {
                    retained = true;
                } else {
                    inventory.set(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    private static List<List<ItemStack>> inventories(PlayerEntity player) {
        return List.of(
                player.getInventory().main,
                player.getInventory().armor,
                player.getInventory().offHand
        );
    }
}
