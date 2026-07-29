package dev.caecorthus.sparktraits.impl.traits.global;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritSleuthVisibilityRulesTest {
    @Test
    void revealsSpectatorPlayerHeadsWithoutDependingOnDeathBookkeeping() {
        assertTrue(SpiritSleuthVisibilityRules.shouldRevealSpectatorPlayerHead(
                true, false, true, true, false, false
        ));
        assertFalse(SpiritSleuthVisibilityRules.shouldRevealSpectatorPlayerHead(
                false, false, true, true, false, false
        ));
        assertFalse(SpiritSleuthVisibilityRules.shouldRevealSpectatorPlayerHead(
                true, true, true, true, false, false
        ));
        assertFalse(SpiritSleuthVisibilityRules.shouldRevealSpectatorPlayerHead(
                true, false, false, true, false, false
        ));
        assertFalse(SpiritSleuthVisibilityRules.shouldRevealSpectatorPlayerHead(
                true, false, true, false, false, false
        ));
    }

    @Test
    void keepsTemporaryFakeDeathsHidden() {
        assertFalse(SpiritSleuthVisibilityRules.shouldRevealSpectatorPlayerHead(
                true, false, true, true, true, false
        ));
        assertFalse(SpiritSleuthVisibilityRules.shouldRevealSpectatorPlayerHead(
                true, false, true, true, false, true
        ));
    }

    @Test
    void revealsOnlyDeadSpectatorTargetsToLivingTraitHolders() {
        for (boolean viewerHasTrait : new boolean[] {false, true}) {
            for (boolean viewerIsSpectator : new boolean[] {false, true}) {
                for (boolean targetIsSpectator : new boolean[] {false, true}) {
                    for (boolean targetIsDeadParticipant : new boolean[] {false, true}) {
                        for (boolean targetIsLastStandPending : new boolean[] {false, true}) {
                            boolean expected = viewerHasTrait
                                    && !viewerIsSpectator
                                    && targetIsSpectator
                                    && targetIsDeadParticipant
                                    && !targetIsLastStandPending;

                            assertEquals(
                                    expected,
                                    SpiritSleuthVisibilityRules.shouldRevealSpectatorHead(
                                            viewerHasTrait,
                                            viewerIsSpectator,
                                            targetIsSpectator,
                                            targetIsDeadParticipant,
                                            targetIsLastStandPending
                                    ),
                                    "viewerHasTrait=" + viewerHasTrait
                                            + ", viewerIsSpectator=" + viewerIsSpectator
                                            + ", targetIsSpectator=" + targetIsSpectator
                                            + ", targetIsDeadParticipant=" + targetIsDeadParticipant
                                            + ", targetIsLastStandPending=" + targetIsLastStandPending
                            );
                        }
                    }
                }
            }
        }
    }

    @Test
    void revealsSpectatorPlayerHeadOnlyToTheLivingTraitHolder() {
        boolean spiritSleuthReveal = SpiritSleuthVisibilityRules.shouldRevealSpectatorPlayerHead(
                true, false, true, true, false, false
        );
        boolean ordinaryViewerReveal = SpiritSleuthVisibilityRules.shouldRevealSpectatorPlayerHead(
                false, false, true, true, false, false
        );
        boolean nonParticipantReveal = SpiritSleuthVisibilityRules.shouldRevealSpectatorPlayerHead(
                true, false, true, false, false, false
        );

        assertFalse(SpiritSleuthVisibilityRules.resolveInvisibleToViewer(true, spiritSleuthReveal));
        assertTrue(SpiritSleuthVisibilityRules.resolveInvisibleToViewer(true, ordinaryViewerReveal));
        assertTrue(SpiritSleuthVisibilityRules.resolveInvisibleToViewer(true, nonParticipantReveal));
    }

    @Test
    void revealDecisionOverridesOnlyAnOriginallyInvisibleRepresentation() {
        assertFalse(SpiritSleuthVisibilityRules.resolveInvisibleToViewer(true, true));
        assertTrue(SpiritSleuthVisibilityRules.resolveInvisibleToViewer(true, false));
        assertFalse(SpiritSleuthVisibilityRules.resolveInvisibleToViewer(false, true));
        assertFalse(SpiritSleuthVisibilityRules.resolveInvisibleToViewer(false, false));
    }
}
