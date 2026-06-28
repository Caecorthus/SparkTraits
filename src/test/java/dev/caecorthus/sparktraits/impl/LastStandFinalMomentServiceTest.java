package dev.caecorthus.sparktraits.impl;

import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastStandFinalMomentServiceTest {
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID THIRD = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void finalMomentTriggersWhenOnlyTriggeredLastStandGoodPlayersRemainWithOtherFactions() {
        LastStandFinalMomentService.FinalMomentDecision decision = LastStandFinalMomentService.evaluate(List.of(
                state(FIRST, WatheRoles.CIVILIAN, true, true),
                state(SECOND, WatheRoles.KILLER, true, false)
        ));

        assertTrue(decision.shouldTrigger());
        assertEquals(List.of(FIRST), decision.finalPlayerUuids());
    }

    @Test
    void ordinaryGoodPlayerPreventsFinalMoment() {
        LastStandFinalMomentService.FinalMomentDecision decision = LastStandFinalMomentService.evaluate(List.of(
                state(FIRST, WatheRoles.CIVILIAN, true, true),
                state(SECOND, WatheRoles.VIGILANTE, true, false),
                state(THIRD, WatheRoles.KILLER, true, false)
        ));

        assertFalse(decision.shouldTrigger());
    }

    @Test
    void finalMomentRequiresAnotherLivingFaction() {
        LastStandFinalMomentService.FinalMomentDecision decision = LastStandFinalMomentService.evaluate(List.of(
                state(FIRST, WatheRoles.CIVILIAN, true, true)
        ));

        assertFalse(decision.shouldTrigger());
    }

    @Test
    void pendingLastStandPlayerDoesNotCountAsLivingTriggerCandidate() {
        LastStandFinalMomentService.FinalMomentDecision decision = LastStandFinalMomentService.evaluate(List.of(
                state(FIRST, WatheRoles.CIVILIAN, false, true),
                state(SECOND, WatheRoles.KILLER, true, false)
        ));

        assertFalse(decision.shouldTrigger());
    }

    @Test
    void multipleTriggeredLastStandGoodPlayersAllBecomeFinalPlayers() {
        LastStandFinalMomentService.FinalMomentDecision decision = LastStandFinalMomentService.evaluate(List.of(
                state(FIRST, WatheRoles.CIVILIAN, true, true),
                state(SECOND, WatheRoles.VETERAN, true, true),
                state(THIRD, WatheRoles.KILLER, true, false)
        ));

        assertTrue(decision.shouldTrigger());
        assertEquals(List.of(FIRST, SECOND), decision.finalPlayerUuids());
    }

    @Test
    void activeFinalMomentBlocksOnlyOrdinaryTeamWins() {
        assertTrue(LastStandFinalMomentService.shouldBlockOrdinaryWin(true, GameFunctions.WinStatus.PASSENGERS));
        assertTrue(LastStandFinalMomentService.shouldBlockOrdinaryWin(true, GameFunctions.WinStatus.KILLERS));
        assertFalse(LastStandFinalMomentService.shouldBlockOrdinaryWin(true, GameFunctions.WinStatus.TIME));
        assertFalse(LastStandFinalMomentService.shouldBlockOrdinaryWin(true, GameFunctions.WinStatus.NONE));
        assertFalse(LastStandFinalMomentService.shouldBlockOrdinaryWin(false, GameFunctions.WinStatus.KILLERS));
    }

    @Test
    void activeFinalMomentReturnsPassengerWinWhenOnlyTriggeredLooseEndRemains() {
        assertEquals(
                GameFunctions.WinStatus.PASSENGERS,
                LastStandFinalMomentService.finalMomentSurvivorWinStatus(true, List.of(
                        state(FIRST, WatheRoles.LOOSE_END, true, true),
                        state(SECOND, WatheRoles.KILLER, false, false)
                ))
        );
    }

    @Test
    void activeFinalMomentReturnsPassengerWinBeforeTimeWinWhenOnlyTriggeredLooseEndRemains() {
        CheckWinCondition.WinResult result = LastStandFinalMomentService.activeFinalMomentWinResult(
                true,
                GameFunctions.WinStatus.TIME,
                List.of(state(FIRST, WatheRoles.LOOSE_END, true, true))
        );

        assertEquals(GameFunctions.WinStatus.PASSENGERS, result.status());
    }

    @Test
    void activeFinalMomentDoesNotEndWhileOtherPlayersRemain() {
        assertNull(LastStandFinalMomentService.finalMomentSurvivorWinStatus(true, List.of(
                state(FIRST, WatheRoles.LOOSE_END, true, true),
                state(SECOND, WatheRoles.KILLER, true, false)
        )));
        assertNull(LastStandFinalMomentService.finalMomentSurvivorWinStatus(true, List.of(
                state(FIRST, WatheRoles.LOOSE_END, true, true),
                state(SECOND, WatheRoles.LOOSE_END, true, true)
        )));
        assertNull(LastStandFinalMomentService.finalMomentSurvivorWinStatus(false, List.of(
                state(FIRST, WatheRoles.LOOSE_END, true, true)
        )));
    }

    @Test
    void activeFinalMomentAllowsKillerWinAfterTriggeredLooseEndDies() {
        assertNull(LastStandFinalMomentService.activeFinalMomentWinResult(
                true,
                GameFunctions.WinStatus.KILLERS,
                List.of(
                        state(FIRST, WatheRoles.LOOSE_END, false, true),
                        state(SECOND, WatheRoles.KILLER, true, false)
                )
        ));
    }

    @Test
    void activeFinalMomentAllowsPassengerWinAfterTriggeredLooseEndDies() {
        assertNull(LastStandFinalMomentService.activeFinalMomentWinResult(
                true,
                GameFunctions.WinStatus.PASSENGERS,
                List.of(
                        state(FIRST, WatheRoles.LOOSE_END, false, true),
                        state(SECOND, WatheRoles.CIVILIAN, true, false)
                )
        ));
    }

    @Test
    void activeFinalMomentBlocksOrdinaryWinWhileTriggeredLooseEndLives() {
        CheckWinCondition.WinResult result = LastStandFinalMomentService.activeFinalMomentWinResult(
                true,
                GameFunctions.WinStatus.KILLERS,
                List.of(
                        state(FIRST, WatheRoles.LOOSE_END, true, true),
                        state(SECOND, WatheRoles.KILLER, true, false)
                )
        );

        assertEquals(GameFunctions.WinStatus.NONE, result.status());
    }

    @Test
    void finalMomentTriggeredLooseEndKnifeCooldownIsZero() {
        assertEquals(0, LastStandFinalMomentService.finalMomentKnifeCooldown(
                900,
                true,
                WatheRoles.LOOSE_END,
                true,
                true
        ));
    }

    @Test
    void finalMomentKnifeCooldownLeavesOtherCasesAlone() {
        assertEquals(900, LastStandFinalMomentService.finalMomentKnifeCooldown(
                900,
                false,
                WatheRoles.LOOSE_END,
                true,
                true
        ));
        assertEquals(900, LastStandFinalMomentService.finalMomentKnifeCooldown(
                900,
                true,
                WatheRoles.LOOSE_END,
                false,
                true
        ));
        assertEquals(900, LastStandFinalMomentService.finalMomentKnifeCooldown(
                900,
                true,
                WatheRoles.CIVILIAN,
                true,
                true
        ));
        assertEquals(900, LastStandFinalMomentService.finalMomentKnifeCooldown(
                900,
                true,
                WatheRoles.LOOSE_END,
                true,
                false
        ));
    }

    @Test
    void finalMomentInitialCooldownClearOnlyAppliesToKnife() {
        assertTrue(LastStandFinalMomentService.shouldClearFinalMomentInitialCooldown(true));
        assertFalse(LastStandFinalMomentService.shouldClearFinalMomentInitialCooldown(false));
    }

    @Test
    void finalMomentHighlightColorUsesFactionColors() {
        assertEquals(0x36E51B, LastStandFinalMomentService.finalMomentHighlightColor(WatheRoles.CIVILIAN));
        assertEquals(0xC13838, LastStandFinalMomentService.finalMomentHighlightColor(WatheRoles.KILLER));
        assertEquals(0xB567FF, LastStandFinalMomentService.finalMomentHighlightColor(WatheRoles.LOOSE_END));
        assertEquals(0xFFFFFF, LastStandFinalMomentService.finalMomentHighlightColor(null));
    }

    @Test
    void finalMomentLastStandLooseEndUsesPassengerHighlightColor() {
        assertEquals(0x36E51B, LastStandFinalMomentService.finalMomentHighlightColor(WatheRoles.LOOSE_END, true));
        assertEquals(0xB567FF, LastStandFinalMomentService.finalMomentHighlightColor(WatheRoles.LOOSE_END, false));
    }

    @Test
    void finalMomentLastStandLooseEndWinsWithPassengers() {
        assertTrue(LastStandFinalMomentService.didFinalMomentPlayerWin(
                GameFunctions.WinStatus.PASSENGERS,
                WatheRoles.LOOSE_END,
                true
        ));
        assertTrue(LastStandFinalMomentService.didFinalMomentPlayerWin(
                GameFunctions.WinStatus.TIME,
                WatheRoles.LOOSE_END,
                true
        ));
        assertFalse(LastStandFinalMomentService.didFinalMomentPlayerWin(
                GameFunctions.WinStatus.KILLERS,
                WatheRoles.LOOSE_END,
                true
        ));
        assertFalse(LastStandFinalMomentService.didFinalMomentPlayerWin(
                GameFunctions.WinStatus.PASSENGERS,
                WatheRoles.LOOSE_END,
                false
        ));
    }

    @Test
    void finalMomentLooseEndWithTriggeredLastStandIsBlackoutImmune() {
        assertTrue(LastStandFinalMomentService.isFinalMomentLooseEndBlackoutImmune(
                true,
                WatheRoles.LOOSE_END,
                true
        ));
    }

    @Test
    void ordinaryLooseEndOutsideFinalMomentIsNotBlackoutImmune() {
        assertFalse(LastStandFinalMomentService.isFinalMomentLooseEndBlackoutImmune(
                false,
                WatheRoles.LOOSE_END,
                true
        ));
    }

    @Test
    void looseEndWithoutTriggeredLastStandIsNotBlackoutImmune() {
        assertFalse(LastStandFinalMomentService.isFinalMomentLooseEndBlackoutImmune(
                true,
                WatheRoles.LOOSE_END,
                false
        ));
    }

    @Test
    void triggeredLastStandNonLooseEndIsNotBlackoutImmune() {
        assertFalse(LastStandFinalMomentService.isFinalMomentLooseEndBlackoutImmune(
                true,
                WatheRoles.CIVILIAN,
                true
        ));
    }

    private static LastStandFinalMomentService.PlayerState state(
            UUID uuid,
            dev.doctor4t.wathe.api.Role role,
            boolean alive,
            boolean lastStandTriggered
    ) {
        return new LastStandFinalMomentService.PlayerState(
                uuid,
                role,
                Set.<Identifier>of(),
                alive,
                lastStandTriggered
        );
    }
}
