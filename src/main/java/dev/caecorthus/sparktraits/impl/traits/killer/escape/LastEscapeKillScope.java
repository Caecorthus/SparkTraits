package dev.caecorthus.sparktraits.impl.traits.killer.escape;

import dev.caecorthus.sparktraits.impl.traits.killer.combat.ExactItemCooldowns;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** Nested death calls must not overwrite the inventory snapshot taken before Wathe removes a bat. */
public final class LastEscapeKillScope implements AutoCloseable {
    private static final ThreadLocal<Deque<LastEscapeKillScope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);
    private Map<Item, Integer> cooldowns;

    public LastEscapeKillScope() { SCOPES.get().push(this); }

    public static void beforePsychoStop(ServerPlayerEntity player) {
        LastEscapeKillScope scope = SCOPES.get().peek();
        if (scope == null || scope.cooldowns != null) return;
        scope.cooldowns = new HashMap<>();
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            var stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty() && !stack.isOf(WatheItems.LOCKPICK))
                scope.cooldowns.putIfAbsent(stack.getItem(), ExactItemCooldowns.remainingTicks(player, stack.getItem()));
        }
    }

    public static void halveCooldowns(ServerPlayerEntity player) {
        LastEscapeKillScope scope = SCOPES.get().peek();
        if (scope == null || scope.cooldowns == null) {
            if (scope == null) SCOPES.remove();
            ExactItemCooldowns.halveCarriedCooldowns(player);
            return;
        }
        scope.cooldowns.forEach((item, remaining) -> {
            if (remaining > 0) ExactItemCooldowns.setExact(player, item, remaining / 2 + remaining % 2);
        });
    }

    @Override public void close() {
        Deque<LastEscapeKillScope> scopes = SCOPES.get();
        scopes.pop();
        if (scopes.isEmpty()) SCOPES.remove();
    }
}
