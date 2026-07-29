package dev.caecorthus.sparktraits.impl.traits.global;

/**
 * Owns the conservative visibility boundary for Spirit Sleuth spectator heads.
 * 管理灵探旁观者头部的保守可见性边界。
 */
public final class SpiritSleuthVisibilityRules {
    private SpiritSleuthVisibilityRules() {
    }

    public static boolean resolveInvisibleToViewer(
            boolean invisibleToViewer,
            boolean reveal
    ) {
        return invisibleToViewer && !reveal;
    }

    public static boolean shouldRevealSpectatorPlayerHead(
            boolean viewerHasTrait,
            boolean viewerIsSpectator,
            boolean targetIsSpectator,
            boolean targetIsGameParticipant,
            boolean targetIsDeadParticipant,
            boolean targetIsLastStandPending,
            boolean targetIsTemporaryFakeDeathPending
    ) {
        return viewerHasTrait
                && !viewerIsSpectator
                && targetIsSpectator
                && targetIsGameParticipant
                && targetIsDeadParticipant
                && !targetIsLastStandPending
                && !targetIsTemporaryFakeDeathPending;
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
