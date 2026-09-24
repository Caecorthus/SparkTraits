package dev.caecorthus.sparktraits.impl.traits.killer.combat;

/** Tick arithmetic shared by server enforcement and the owner-only indicator. */
public final class CombatTimingRules {
    public static final int KNIFE_WINDUP_TICKS = 3;
    public static final int MAX_RAISE_TICKS = 200;
    public static final int DEFENDER_LOCK_TICKS = 100;
    public static final int ATTACKER_LOCK_TICKS = 300;

    private CombatTimingRules() {
    }

    public static int remaining(long deadline, long now) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, deadline - now));
    }

    public static int halfRemaining(int remaining, int forcedFloor) {
        int nonnegative = Math.max(0, remaining);
        return Math.max(Math.max(0, forcedFloor), nonnegative / 2 + nonnegative % 2);
    }

    public static int resolveWriteDuration(int requested, int forcedFloor, int current, boolean exact) {
        int duration = Math.max(0, requested);
        if (forcedFloor > 0 && !exact) duration = Math.max(duration, current);
        return Math.max(duration, Math.max(0, forcedFloor));
    }

    public static boolean canRelease(long elapsed) {
        return elapsed >= KNIFE_WINDUP_TICKS && elapsed < MAX_RAISE_TICKS;
    }

    public static boolean canParry(boolean aliveEnemies, boolean validWeapon, boolean activeRaise,
                                   boolean alreadyConsumed, long elapsed) {
        // Windup is part of the raised stance; blocking need not wait for the stab threshold.
        return aliveEnemies && validWeapon && activeRaise && !alreadyConsumed
                && elapsed >= 0 && elapsed < MAX_RAISE_TICKS;
    }
}
