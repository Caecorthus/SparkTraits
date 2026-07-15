package dev.caecorthus.sparktraits.impl.traits.global;

/**
 * Owns the conservative visibility boundary for Spirit Sleuth spectator heads.
 * 管理灵探旁观者头部的保守可见性边界。
 */
public final class SpiritSleuthVisibilityRules {
    private SpiritSleuthVisibilityRules() {
    }

    public static boolean shouldRevealSpectatorHead(
            boolean viewerHasTrait,
            boolean viewerIsSpectator,
            boolean targetIsSpectator,
            boolean targetIsDeadParticipant,
            boolean targetIsLastStandPending
    ) {
        return viewerHasTrait
                && !viewerIsSpectator
                && targetIsSpectator
                && targetIsDeadParticipant
                && !targetIsLastStandPending;
    }
}
