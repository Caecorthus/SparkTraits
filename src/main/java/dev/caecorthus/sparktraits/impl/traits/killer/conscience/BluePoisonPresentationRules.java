package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

public final class BluePoisonPresentationRules {
    public static final int BLUE_POISON_DELAY_TICKS = 200;

    public enum Owner {
        NONE,
        WATHE_NATIVE,
        BLUE_POISON
    }

    private BluePoisonPresentationRules() {
    }

    public static Owner owner(boolean nativePoisoned, boolean bluePoisoned) {
        if (nativePoisoned) {
            return Owner.WATHE_NATIVE;
        }
        return bluePoisoned ? Owner.BLUE_POISON : Owner.NONE;
    }

    public static int remainingTicks(int syncedRemainingTicks, int elapsedTicks) {
        return Math.max(0, syncedRemainingTicks - Math.max(0, elapsedTicks));
    }

    public static boolean canPulse(int initialTicks, int remainingTicks) {
        return remainingTicks > 0 && initialTicks - remainingTicks >= BLUE_POISON_DELAY_TICKS;
    }

    public static int pulseInterval(int remainingTicks, int maximumPoisonTicks) {
        return 10 + (int) (50.0F * remainingTicks / maximumPoisonTicks);
    }

    public static int cooldownAfterPulse(int remainingTicks, int maximumPoisonTicks) {
        return Math.max(0, pulseInterval(remainingTicks, maximumPoisonTicks) - 1);
    }

    public static float heartbeatVolume(int remainingTicks, int maximumPoisonTicks) {
        return 0.5F + 0.5F * (1.0F - (float) remainingTicks / maximumPoisonTicks);
    }
}
