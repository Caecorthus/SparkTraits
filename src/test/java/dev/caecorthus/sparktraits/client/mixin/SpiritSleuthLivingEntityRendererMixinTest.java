package dev.caecorthus.sparktraits.client.mixin;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiritSleuthLivingEntityRendererMixinTest {
    @Test
    void revealsConfirmedDeadSpectatorParticipantToLivingTraitHolder() {
        assertFalse(resolveSpectatorPlayerHeadVisibility(
                true, true, false, true, true, true, false, false
        ));
    }

    @Test
    void preservesInvisibilityForLivingAndTemporaryDeathsAndIneligibleViewers() {
        assertTrue(resolveSpectatorPlayerHeadVisibility(
                true, true, false, true, true, false, false, false
        ));
        assertTrue(resolveSpectatorPlayerHeadVisibility(
                true, true, false, true, true, true, true, false
        ));
        assertTrue(resolveSpectatorPlayerHeadVisibility(
                true, true, false, true, true, true, false, true
        ));
        assertTrue(resolveSpectatorPlayerHeadVisibility(
                true, false, false, true, true, true, false, false
        ));
        assertTrue(resolveSpectatorPlayerHeadVisibility(
                true, true, true, true, true, true, false, false
        ));
    }

    @Test
    void requiresSpectatorStateGameParticipationAndConfirmedDeath() {
        assertTrue(resolveSpectatorPlayerHeadVisibility(
                true, true, false, false, true, true, false, false
        ));
        assertTrue(resolveSpectatorPlayerHeadVisibility(
                true, true, false, true, false, true, false, false
        ));
        assertTrue(resolveSpectatorPlayerHeadVisibility(
                true, true, false, true, true, false, false, false
        ));
    }

    @Test
    void neverMakesAnOriginallyVisiblePlayerInvisible() {
        assertFalse(resolveSpectatorPlayerHeadVisibility(
                false, false, false, false, false, false, false, false
        ));
    }

    private static boolean resolveSpectatorPlayerHeadVisibility(
            boolean invisibleToViewer,
            boolean viewerHasTrait,
            boolean viewerIsSpectator,
            boolean targetIsSpectator,
            boolean targetIsGameParticipant,
            boolean targetIsDeadParticipant,
            boolean targetIsLastStandPending,
            boolean targetIsTemporaryFakeDeathPending
    ) {
        try {
            Method helper = SpiritSleuthLivingEntityRendererMixin.class.getDeclaredMethod(
                    "resolveSpectatorPlayerHeadVisibility",
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    boolean.class
            );
            helper.setAccessible(true);
            return (boolean) helper.invoke(
                    null,
                    invisibleToViewer,
                    viewerHasTrait,
                    viewerIsSpectator,
                    targetIsSpectator,
                    targetIsGameParticipant,
                    targetIsDeadParticipant,
                    targetIsLastStandPending,
                    targetIsTemporaryFakeDeathPending
            );
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to invoke mixin visibility helper", exception);
        }
    }
}
