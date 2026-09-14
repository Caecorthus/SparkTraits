package dev.caecorthus.sparktraits.client.killer;

/** Pure presentation rules: ordinary item/attack cooldowns never create a forced-lock warning. */
public final class ForcedMeleeHudRules {
    private ForcedMeleeHudRules() {
    }

    public static int seconds(int remainingTicks) {
        return remainingTicks <= 0 ? 0 : 1 + (remainingTicks - 1) / 20;
    }

    public static boolean visible(int remainingTicks, boolean firstPerson, boolean hudHidden,
                                  boolean alive, boolean running) {
        return remainingTicks > 0 && firstPerson && !hudHidden && alive && running;
    }
}
