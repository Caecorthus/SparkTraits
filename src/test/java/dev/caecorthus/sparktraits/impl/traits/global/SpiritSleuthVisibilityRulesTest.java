package dev.caecorthus.sparktraits.impl.traits.global;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpiritSleuthVisibilityRulesTest {
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
}
