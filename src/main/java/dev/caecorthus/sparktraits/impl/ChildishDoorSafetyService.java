package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Keeps Childish players from falling into the empty collision column created by opened train doors.
 * 防止幼稚体质玩家在列车门打开后掉进被清空碰撞的门框列。
 */
public final class ChildishDoorSafetyService {
    static final double DOOR_EXIT_MARGIN = 0.05D;
    private static final double DOOR_COLUMN_DROP_PADDING = 1.0D;
    private static final double DOOR_COLUMN_HEIGHT = 2.0D;
    private static final double LATERAL_RETRY_DISTANCE = 0.35D;

    private ChildishDoorSafetyService() {
    }

    public static void afterTrainDoorUse(
            BlockState interactedState,
            World world,
            BlockPos interactedPos,
            PlayerEntity player,
            ActionResult result
    ) {
        if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        BlockPos lowerPos = lowerDoorPos(interactedState, interactedPos);
        BlockState currentState = serverWorld.getBlockState(lowerPos);
        boolean doorOpen = currentState.contains(Properties.OPEN) && currentState.get(Properties.OPEN);
        boolean childish = TraitPlayerComponent.KEY.get(serverPlayer).hasActiveTrait(ChildishTrait.ID);
        if (!shouldRunDoorSafety(world.isClient, result, childish, doorOpen)
                || !currentState.contains(Properties.HORIZONTAL_FACING)
                || !isInsideDoorColumn(serverPlayer.getBoundingBox(), lowerPos)) {
            return;
        }

        Direction facing = currentState.get(Properties.HORIZONTAL_FACING);
        findSafeNudgePosition(serverPlayer, serverWorld, lowerPos, facing).ifPresent(targetPos -> {
            serverPlayer.setVelocity(Vec3d.ZERO);
            serverPlayer.fallDistance = 0.0F;
            serverPlayer.requestTeleport(targetPos.x, targetPos.y, targetPos.z);
        });
    }

    static boolean shouldRunDoorSafety(boolean clientWorld, ActionResult result, boolean childish, boolean doorOpen) {
        return !clientWorld && result == ActionResult.CONSUME && childish && doorOpen;
    }

    static BlockPos lowerDoorPos(BlockState state, BlockPos pos) {
        if (!state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            return pos;
        }
        return lowerDoorPos(pos, state.get(Properties.DOUBLE_BLOCK_HALF));
    }

    static BlockPos lowerDoorPos(BlockPos pos, DoubleBlockHalf half) {
        return half == DoubleBlockHalf.UPPER ? pos.down() : pos;
    }

    static List<Vec3d> candidateNudgePositions(Vec3d playerPos, BlockPos lowerDoorPos, Direction facing, double playerWidth) {
        Direction primary = nearestDoorSide(playerPos, lowerDoorPos, facing);
        Direction secondary = primary.getOpposite();
        Direction alongDoor = primary.rotateYClockwise();
        double halfWidth = Math.max(0.0D, playerWidth / 2.0D);
        List<Vec3d> candidates = new ArrayList<>();

        candidates.add(nudgePosition(playerPos, lowerDoorPos, primary, halfWidth, alongDoor, 0.0D));
        candidates.add(nudgePosition(playerPos, lowerDoorPos, secondary, halfWidth, alongDoor, 0.0D));
        candidates.add(nudgePosition(playerPos, lowerDoorPos, primary, halfWidth, alongDoor, LATERAL_RETRY_DISTANCE));
        candidates.add(nudgePosition(playerPos, lowerDoorPos, primary, halfWidth, alongDoor, -LATERAL_RETRY_DISTANCE));
        candidates.add(nudgePosition(playerPos, lowerDoorPos, secondary, halfWidth, alongDoor, LATERAL_RETRY_DISTANCE));
        candidates.add(nudgePosition(playerPos, lowerDoorPos, secondary, halfWidth, alongDoor, -LATERAL_RETRY_DISTANCE));
        return candidates;
    }

    private static Optional<Vec3d> findSafeNudgePosition(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos lowerDoorPos,
            Direction facing
    ) {
        Box currentBox = player.getBoundingBox();
        double playerWidth = Math.max(currentBox.getLengthX(), currentBox.getLengthZ());
        for (Vec3d candidate : candidateNudgePositions(player.getPos(), lowerDoorPos, facing, playerWidth)) {
            if (canSafelyStandAt(player, world, candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static boolean canSafelyStandAt(ServerPlayerEntity player, ServerWorld world, Vec3d pos) {
        if (pos.y < world.getBottomY() || pos.y >= world.getTopY()) {
            return false;
        }
        BlockPos feet = BlockPos.ofFloored(pos);
        BlockPos below = feet.down();
        if (world.getBlockState(below).getCollisionShape(world, below).isEmpty()) {
            return false;
        }

        Box targetBox = player.getBoundingBox().offset(pos.subtract(player.getPos()));
        return world.isSpaceEmpty(player, targetBox);
    }

    private static boolean isInsideDoorColumn(Box box, BlockPos lowerDoorPos) {
        Box doorColumn = new Box(
                lowerDoorPos.getX(),
                lowerDoorPos.getY() - DOOR_COLUMN_DROP_PADDING,
                lowerDoorPos.getZ(),
                lowerDoorPos.getX() + 1.0D,
                lowerDoorPos.getY() + DOOR_COLUMN_HEIGHT,
                lowerDoorPos.getZ() + 1.0D
        );
        return box.intersects(doorColumn);
    }

    private static Direction nearestDoorSide(Vec3d playerPos, BlockPos lowerDoorPos, Direction facing) {
        Direction positive = positiveDirection(facing.getAxis());
        Direction negative = positive.getOpposite();
        double playerCoordinate = coordinate(playerPos, facing.getAxis());
        double centerCoordinate = coordinate(lowerDoorPos, facing.getAxis()) + 0.5D;
        if (playerCoordinate == centerCoordinate) {
            return facing.getDirection() == Direction.AxisDirection.POSITIVE ? positive : negative;
        }
        return playerCoordinate > centerCoordinate ? positive : negative;
    }

    private static Vec3d nudgePosition(
            Vec3d playerPos,
            BlockPos lowerDoorPos,
            Direction side,
            double halfWidth,
            Direction alongDoor,
            double alongOffset
    ) {
        double x = playerPos.x;
        double z = playerPos.z;
        double sideCoordinate = sideCoordinate(lowerDoorPos, side, halfWidth);
        if (side.getAxis() == Direction.Axis.X) {
            x = sideCoordinate;
            z += alongDoor.getOffsetZ() * alongOffset;
        } else {
            z = sideCoordinate;
            x += alongDoor.getOffsetX() * alongOffset;
        }
        return new Vec3d(x, playerPos.y, z);
    }

    private static double sideCoordinate(BlockPos lowerDoorPos, Direction side, double halfWidth) {
        double blockCoordinate = coordinate(lowerDoorPos, side.getAxis());
        if (side == Direction.EAST || side == Direction.SOUTH) {
            return blockCoordinate + 1.0D + halfWidth + DOOR_EXIT_MARGIN;
        }
        return blockCoordinate - halfWidth - DOOR_EXIT_MARGIN;
    }

    private static double coordinate(Vec3d pos, Direction.Axis axis) {
        return axis == Direction.Axis.X ? pos.x : pos.z;
    }

    private static double coordinate(BlockPos pos, Direction.Axis axis) {
        return axis == Direction.Axis.X ? pos.getX() : pos.getZ();
    }

    private static Direction positiveDirection(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }
}
