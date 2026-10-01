package dev.caecorthus.sparktraits.impl.traits.civilian.chameleon;

/** Pure stillness, fade and instinct rules for Chameleon.
 *  变色龙的静止计时、渐隐与本能屏蔽纯规则。 */
public final class ChameleonRules {
    public static final long NONE = -1L;
    public static final int FADE_TICKS = 20 * 15;
    public static final double MOVE_TOLERANCE_SQUARED = 0.01 * 0.01;
    // Owners and spectators keep a vanilla-invisibility-strength ghost so the faded body stays readable to them.
    // 本人与旁观者保留与原版隐身相同强度的虚影，便于看清已透明的身体。
    public static final float OBSERVER_MIN_ALPHA = 0x26 / 255.0f;

    private ChameleonRules() {
    }

    public static boolean hasMoved(boolean hasAnchor, double squaredDistanceFromAnchor) {
        return !hasAnchor || squaredDistanceFromAnchor > MOVE_TOLERANCE_SQUARED;
    }

    /** Stillness starts on the first motionless tick and only counts while both hands are empty.
     *  静止从第一个未移动的刻开始计时，且只在双手为空时计时。 */
    public static long nextStillSince(long stillSince, boolean moved, boolean emptyHanded, long now) {
        if (moved || !emptyHanded) {
            return NONE;
        }
        return stillSince == NONE ? now : stillSince;
    }

    /** The body fades linearly from the start of stillness and is fully transparent after fifteen seconds.
     *  身体从开始静止起线性淡化，15 秒后完全透明。 */
    public static float alpha(long stillSince, double time) {
        if (stillSince == NONE) {
            return 1.0f;
        }
        double remaining = 1.0 - (time - stillSince) / FADE_TICKS;
        return (float) Math.max(0.0, Math.min(1.0, remaining));
    }

    public static boolean isFullyTransparent(long stillSince, long now) {
        return stillSince != NONE && now - stillSince >= FADE_TICKS;
    }

    public static boolean becameFullyTransparent(long stillSince, long now) {
        return stillSince != NONE && now - stillSince == FADE_TICKS;
    }

    public static float observedAlpha(float alpha, boolean observerSeesThrough) {
        return observerSeesThrough ? Math.max(alpha, OBSERVER_MIN_ALPHA) : alpha;
    }

    /** Final Moment and spectator information still reveal a fully faded Chameleon.
     *  终局时刻与旁观信息仍可看见完全透明的变色龙。 */
    public static boolean shouldSuppressInstinct(
            boolean targetFullyTransparent,
            boolean viewerPlayingAndAlive,
            boolean viewerCanSeeSpectatorInformation,
            boolean finalMomentActive
    ) {
        return targetFullyTransparent
                && viewerPlayingAndAlive
                && !viewerCanSeeSpectatorInformation
                && !finalMomentActive;
    }
}
