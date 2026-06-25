package dev.caecorthus.sparktraits.impl;

import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChildishDoorSafetyServiceTest {
    @Test
    void trainDoorSafetyRunsOnlyForOpenedServerSideChildishDoorUse() {
        assertTrue(ChildishDoorSafetyService.shouldRunDoorSafety(false, ActionResult.CONSUME, true, true));

        assertFalse(ChildishDoorSafetyService.shouldRunDoorSafety(true, ActionResult.CONSUME, true, true));
        assertFalse(ChildishDoorSafetyService.shouldRunDoorSafety(false, ActionResult.SUCCESS, true, true));
        assertFalse(ChildishDoorSafetyService.shouldRunDoorSafety(false, ActionResult.CONSUME, false, true));
        assertFalse(ChildishDoorSafetyService.shouldRunDoorSafety(false, ActionResult.CONSUME, true, false));
    }

    @Test
    void lowerTrainDoorPositionResolvesClickedHalf() {
        BlockPos lower = new BlockPos(10, 64, 20);

        assertEquals(lower, ChildishDoorSafetyService.lowerDoorPos(lower, DoubleBlockHalf.LOWER));
        assertEquals(lower, ChildishDoorSafetyService.lowerDoorPos(lower.up(), DoubleBlockHalf.UPPER));
    }

    @Test
    void nudgeCandidatesPreferNearestSideOfDoorPlane() {
        BlockPos doorPos = new BlockPos(10, 64, 20);
        Vec3d eastSidePlayer = new Vec3d(10.7, 64.0, 20.5);
        Vec3d westSidePlayer = new Vec3d(10.3, 64.0, 20.5);

        List<Vec3d> eastFirst = ChildishDoorSafetyService.candidateNudgePositions(eastSidePlayer, doorPos, Direction.EAST, 0.45);
        List<Vec3d> westFirst = ChildishDoorSafetyService.candidateNudgePositions(westSidePlayer, doorPos, Direction.EAST, 0.45);

        assertEquals(11.275, eastFirst.getFirst().x, 0.0001);
        assertEquals(9.725, eastFirst.get(1).x, 0.0001);
        assertEquals(9.725, westFirst.getFirst().x, 0.0001);
        assertEquals(11.275, westFirst.get(1).x, 0.0001);
        assertEquals(64.0, eastFirst.getFirst().y, 0.0001);
        assertEquals(20.5, eastFirst.getFirst().z, 0.0001);
    }

    @Test
    void nudgeCandidatesRespectNorthSouthDoorPlane() {
        BlockPos doorPos = new BlockPos(10, 64, 20);
        Vec3d southSidePlayer = new Vec3d(10.5, 64.0, 20.7);

        List<Vec3d> candidates = ChildishDoorSafetyService.candidateNudgePositions(southSidePlayer, doorPos, Direction.SOUTH, 0.45);

        assertEquals(20.0 + 1.0 + 0.225 + 0.05, candidates.getFirst().z, 0.0001);
        assertEquals(10.5, candidates.getFirst().x, 0.0001);
    }
}
