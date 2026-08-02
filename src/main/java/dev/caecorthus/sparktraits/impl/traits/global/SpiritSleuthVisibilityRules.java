package dev.caecorthus.sparktraits.impl.traits.global;

/**
 * Viewer-relative visibility rule for spectator players.
 * 旁观玩家针对观察者的可见性规则。
 */
public final class SpiritSleuthVisibilityRules {
    private SpiritSleuthVisibilityRules() {
    }

    public static boolean resolveInvisibleToViewer(
            boolean originallyInvisible,
            boolean viewerHasActiveTrait,
            boolean targetIsSpectatorPlayer
    ) {
        return originallyInvisible && !(viewerHasActiveTrait && targetIsSpectatorPlayer);
    }
}
