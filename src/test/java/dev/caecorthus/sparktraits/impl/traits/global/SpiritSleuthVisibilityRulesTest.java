package dev.caecorthus.sparktraits.impl.traits.global;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SpiritSleuthVisibilityRulesTest {
    @Test
    void resolvesEveryVisibilityCombination() {
        for (boolean originallyInvisible : new boolean[]{false, true}) {
            for (boolean viewerHasActiveTrait : new boolean[]{false, true}) {
                for (boolean targetIsSpectatorPlayer : new boolean[]{false, true}) {
                    boolean expected = originallyInvisible
                            && !(viewerHasActiveTrait && targetIsSpectatorPlayer);

                    assertEquals(
                            expected,
                            SpiritSleuthVisibilityRules.resolveInvisibleToViewer(
                                    originallyInvisible,
                                    viewerHasActiveTrait,
                                    targetIsSpectatorPlayer
                            ),
                            () -> "Unexpected result for originallyInvisible=" + originallyInvisible
                                    + ", viewerHasActiveTrait=" + viewerHasActiveTrait
                                    + ", targetIsSpectatorPlayer=" + targetIsSpectatorPlayer
                    );
                }
            }
        }
    }

    @Test
    void neverHidesAnOriginallyVisibleTarget() {
        assertFalse(SpiritSleuthVisibilityRules.resolveInvisibleToViewer(false, false, false));
        assertFalse(SpiritSleuthVisibilityRules.resolveInvisibleToViewer(false, false, true));
        assertFalse(SpiritSleuthVisibilityRules.resolveInvisibleToViewer(false, true, false));
        assertFalse(SpiritSleuthVisibilityRules.resolveInvisibleToViewer(false, true, true));
    }
}
