package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.api.SparkTraitsApi.GunShotCycleListener;
import dev.caecorthus.sparktraits.mixin.ItemCooldownEntryAccessor;
import dev.caecorthus.sparktraits.mixin.ItemCooldownManagerAccessor;
import dev.doctor4t.wathe.api.event.GameEvents;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

/** A trigger pull and its scheduled Niko shots share one observer scope, not a second firing path.
 * 一次扣动扳机及其 Niko 补射共用观察作用域，不另造射击流程。 */
public final class GunShotCycles {
    private static final List<GunShotCycleListener> LISTENERS = new ArrayList<>();
    private static final Set<Cycle> LIVE = new HashSet<>();
    private static final ThreadLocal<Cycle> CURRENT = new ThreadLocal<>();
    private static boolean registered;

    private GunShotCycles() { }
    public static void register() {
        if (registered) return;
        registered = true;
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            for (Cycle cycle : List.copyOf(LIVE)) if (cycle.shooter.getWorld() == world) close(cycle);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            for (Cycle cycle : List.copyOf(LIVE)) close(cycle);
            CURRENT.remove();
        });
    }
    public static void addListener(GunShotCycleListener listener) { LISTENERS.add(java.util.Objects.requireNonNull(listener)); }
    public static Cycle current() { return CURRENT.get(); }
    public static void setCurrent(Cycle cycle) { if (cycle == null) CURRENT.remove(); else CURRENT.set(cycle); }

    public static void begin(ServerPlayerEntity shooter, Item weapon) {
        Cycle cycle = new Cycle(shooter, weapon, List.copyOf(LISTENERS));
        CURRENT.set(cycle);
        LIVE.add(cycle);
        for (GunShotCycleListener listener : cycle.listeners) safely(() -> listener.cycleStarted(shooter, cycle.id, weapon));
    }

    public static void finishNative(Cycle cycle) {
        if (cycle == null || cycle.closed) return;
        var access = (ItemCooldownManagerAccessor) cycle.shooter.getItemCooldownManager();
        int tick = access.sparktraits$getTick();
        Object entry = access.sparktraits$getEntries().get(cycle.weapon);
        int end = entry instanceof ItemCooldownEntryAccessor value ? value.sparktraits$getEndTick() : tick;
        for (GunShotCycleListener listener : cycle.listeners) safely(() -> listener.initialCooldownEstablished(
                cycle.shooter, cycle.id, cycle.weapon, cycle.startTick, end, tick));
        cycle.nativeFinished = true;
        if (cycle.pendingRepeats == 0) close(cycle);
    }

    public static void expectRepeat(Cycle cycle) { if (cycle != null) cycle.pendingRepeats++; }
    public static void runRepeat(Cycle cycle, Runnable shot) {
        if (cycle == null) { shot.run(); return; }
        if (cycle.closed) return;
        Cycle previous = current();
        setCurrent(cycle);
        try { shot.run(); } finally {
            setCurrent(previous);
            cycle.pendingRepeats--;
            if (cycle.nativeFinished && cycle.pendingRepeats == 0) close(cycle);
        }
    }

    public static void observeTargetKill(ServerPlayerEntity shooter, ServerPlayerEntity victim, Runnable kill) {
        Cycle cycle = current();
        if (cycle == null || cycle.closed || cycle.shooter != shooter || shooter == victim) { kill.run(); return; }
        Object[] tokens = new Object[cycle.listeners.size()];
        for (int i = 0; i < tokens.length; i++) {
            try { tokens[i] = cycle.listeners.get(i).beforeTargetKill(shooter, cycle.id, victim); }
            catch (RuntimeException | LinkageError exception) { log(exception); }
        }
        kill.run();
        for (int i = 0; i < tokens.length; i++) {
            GunShotCycleListener listener = cycle.listeners.get(i);
            Object token = tokens[i];
            safely(() -> listener.afterTargetKill(shooter, cycle.id, victim, token));
        }
    }

    private static void close(Cycle cycle) {
        if (cycle.closed) return;
        cycle.closed = true;
        LIVE.remove(cycle);
        for (GunShotCycleListener listener : cycle.listeners) safely(() -> listener.cycleClosed(cycle.shooter, cycle.id));
    }
    private static void safely(Runnable call) {
        try { call.run(); } catch (RuntimeException | LinkageError exception) { log(exception); }
    }
    private static void log(Throwable exception) { SparkTraits.LOGGER.warn("Gun-cycle observer failed", exception); }

    public static final class Cycle {
        private final UUID id = UUID.randomUUID();
        private final ServerPlayerEntity shooter;
        private final Item weapon;
        private final List<GunShotCycleListener> listeners;
        private final int startTick;
        private int pendingRepeats;
        private boolean nativeFinished;
        private boolean closed;
        private Cycle(ServerPlayerEntity shooter, Item weapon, List<GunShotCycleListener> listeners) {
            this.shooter = shooter; this.weapon = weapon; this.listeners = listeners;
            this.startTick = ((ItemCooldownManagerAccessor) shooter.getItemCooldownManager()).sparktraits$getTick();
        }
    }
}
