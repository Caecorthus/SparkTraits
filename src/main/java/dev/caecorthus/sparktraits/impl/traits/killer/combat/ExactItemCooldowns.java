package dev.caecorthus.sparktraits.impl.traits.killer.combat;

import dev.caecorthus.sparktraits.mixin.ItemCooldownEntryAccessor;
import dev.caecorthus.sparktraits.mixin.ItemCooldownManagerAccessor;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;

/** Exact durations are installed after all HEAD modifiers, while retaining the manager's update hook. */
public final class ExactItemCooldowns {
    private static final ThreadLocal<Write> EXACT_WRITE = new ThreadLocal<>();

    private ExactItemCooldowns() {
    }

    public static int remainingTicks(PlayerEntity player, Item item) {
        return player == null || item == null ? 0 : remainingTicks(player.getItemCooldownManager(), item);
    }

    public static int remainingTicks(ItemCooldownManager manager, Item item) {
        ItemCooldownManagerAccessor access = (ItemCooldownManagerAccessor) manager;
        Object entry = access.sparktraits$getEntries().get(item);
        return entry instanceof ItemCooldownEntryAccessor end
                ? Math.max(0, end.sparktraits$getEndTick() - access.sparktraits$getTick()) : 0;
    }

    public static void halveCarriedCooldowns(ServerPlayerEntity player) {
        Set<Item> seen = new HashSet<>();
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isEmpty() || stack.isOf(WatheItems.LOCKPICK) || !seen.add(stack.getItem())) {
                continue;
            }
            Item item = stack.getItem();
            int remaining = remainingTicks(player, item);
            if (remaining > 0) {
                setExact(player, item, CombatTimingRules.halfRemaining(remaining,
                        ForcedMeleeCooldownService.remainingTicks(player, item)));
            }
        }
    }

    public static void setExact(ServerPlayerEntity player, Item item, int ticks) {
        ItemCooldownManager manager = player.getItemCooldownManager();
        Write previous = EXACT_WRITE.get();
        EXACT_WRITE.set(new Write(manager, item, Math.max(0, ticks)));
        try {
            // Keep the polymorphic onCooldownUpdate(Item,int) call: it sends the vanilla S2C packet.
            // ExactItemCooldownMixin restores this duration at Entry construction, AFTER modifiers.
            manager.set(item, Math.max(0, ticks));
        } finally {
            if (previous == null) EXACT_WRITE.remove();
            else EXACT_WRITE.set(previous);
        }
    }

    public static int durationForWrite(ItemCooldownManager manager, Item item, int modifiedDuration,
                                       ServerPlayerEntity owner) {
        Write write = EXACT_WRITE.get();
        boolean exact = write != null && write.manager == manager && write.item == item;
        int duration = exact ? write.ticks : Math.max(0, modifiedDuration);
        int floor = owner == null ? 0 : ForcedMeleeCooldownService.remainingTicks(owner, item);
        // A normal rewrite cannot erase a longer existing cooldown while this penalty is active.
        // Deliberate exact halving may reduce that normal component, but never the forced floor.
        return CombatTimingRules.resolveWriteDuration(duration, floor, remainingTicks(manager, item), exact);
    }

    private record Write(ItemCooldownManager manager, Item item, int ticks) {
    }
}
