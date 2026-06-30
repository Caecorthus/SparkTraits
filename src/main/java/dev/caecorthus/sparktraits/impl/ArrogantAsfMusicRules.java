package dev.caecorthus.sparktraits.impl;

/**
 * Timing rules for Arrogant ASF's owner-only music loop.
 * “展示豪度”仅本人可听循环音乐的计时规则。
 */
public final class ArrogantAsfMusicRules {
    public static final int RESUME_WINDOW_TICKS = 200;

    private ArrogantAsfMusicRules() {
    }

    public static boolean shouldRetainPausedTrack(int inactiveTicks) {
        return inactiveTicks < RESUME_WINDOW_TICKS;
    }

    public static boolean shouldDiscardPausedTrack(int inactiveTicks) {
        return !shouldRetainPausedTrack(inactiveTicks);
    }

    public static int remainingResumeSeconds(int inactiveTicks) {
        int remainingTicks = Math.max(0, RESUME_WINDOW_TICKS - inactiveTicks);
        return (remainingTicks + 19) / 20;
    }
}
