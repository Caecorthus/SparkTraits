package dev.caecorthus.sparktraits.mixin;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.ConsciencePoisonGasCloud;
import dev.caecorthus.sparktraits.impl.ConsciencePoisonerService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.agmas.noellesroles.entity.PoisonGasCloudEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Replaces only Conscience Poisoner gas clouds with blue-poison gas behavior.
 *  只把善良毒师创建的毒气云替换为蓝毒气逻辑。 */
@Mixin(PoisonGasCloudEntity.class)
public abstract class PoisonGasCloudEntityMixin extends Entity implements ConsciencePoisonGasCloud {
    @Unique
    private static final String CONSCIENCE_GAS_KEY = "sparktraitsConscienceGas";
    @Unique
    private static final int MAX_GAS_BLOCKS = 500;
    @Unique
    private static final int MAX_LIFETIME = 600;
    @Unique
    private static final int SPREAD_INTERVAL = 8;
    @Unique
    private static final int EXPOSURE_THRESHOLD = 100;
    @Unique
    private static final double MAX_SPREAD_RADIUS_SQUARED = 400.0;
    @Unique
    private static final DustParticleEffect BLUE_GAS_PARTICLE = new DustParticleEffect(
            new Vector3f(0.0f, 0.75f, 1.0f),
            1.5f
    );

    @Shadow(remap = false)
    @Final
    private Set<BlockPos> gasBlocks;
    @Shadow(remap = false)
    private Set<BlockPos> frontier;
    @Shadow(remap = false)
    @Final
    private Map<UUID, Integer> exposureTicks;
    @Shadow(remap = false)
    @Final
    private Set<UUID> playersInGas;
    @Shadow(remap = false)
    private UUID ownerUuid;
    @Shadow(remap = false)
    private int age;

    @Unique
    private boolean sparktraits$conscienceGas;

    protected PoisonGasCloudEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public boolean sparktraits$isConscienceGas() {
        return sparktraits$conscienceGas;
    }

    @Override
    public void sparktraits$setConscienceGas(boolean conscienceGas) {
        sparktraits$conscienceGas = conscienceGas;
    }

    @Shadow(remap = false)
    private void clearAllGasExhaustion() {
    }

    @Shadow(remap = false)
    private boolean doesShapeBlockExit(VoxelShape shape, Direction direction) {
        return false;
    }

    @Shadow(remap = false)
    private boolean isBlockTooSolid(VoxelShape shape) {
        return false;
    }

    @Shadow(remap = false)
    private boolean doesShapeBlockEntry(VoxelShape shape, Direction moveDirection) {
        return false;
    }

    @Inject(method = "setOwnerUuid", at = @At("TAIL"), remap = false)
    private void sparktraits$markConscienceGas(UUID uuid, CallbackInfo ci) {
        if (uuid == null || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        ServerPlayerEntity owner = serverWorld.getServer().getPlayerManager().getPlayer(uuid);
        sparktraits$conscienceGas = ConsciencePoisonerService.isConsciencePoisoner(
                owner,
                GameWorldComponent.KEY.get(serverWorld)
        );
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void sparktraits$tickConscienceGas(CallbackInfo ci) {
        if (!sparktraits$conscienceGas) {
            return;
        }
        sparktraits$tickBlueGas();
        ci.cancel();
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void sparktraits$writeConscienceGas(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean(CONSCIENCE_GAS_KEY, sparktraits$conscienceGas);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void sparktraits$readConscienceGas(NbtCompound nbt, CallbackInfo ci) {
        sparktraits$conscienceGas = nbt.getBoolean(CONSCIENCE_GAS_KEY);
    }

    @Unique
    private void sparktraits$tickBlueGas() {
        super.tick();
        age++;
        if (age > MAX_LIFETIME) {
            clearAllGasExhaustion();
            this.discard();
            return;
        }
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        sparktraits$spreadGas(serverWorld);
        sparktraits$applyBlueGas(serverWorld);
        sparktraits$spawnBlueGasParticles(serverWorld);
    }

    @Unique
    private void sparktraits$spreadGas(ServerWorld serverWorld) {
        if (age == 1) {
            BlockPos startPos = this.getBlockPos();
            gasBlocks.add(startPos);
            frontier.add(startPos);
        }
        if (age % SPREAD_INTERVAL != 0 || frontier.isEmpty() || gasBlocks.size() >= MAX_GAS_BLOCKS) {
            return;
        }
        Set<BlockPos> newFrontier = new HashSet<>();
        for (BlockPos pos : frontier) {
            if (gasBlocks.size() >= MAX_GAS_BLOCKS) {
                break;
            }
            boolean stillEdge = false;
            for (Direction direction : Direction.values()) {
                if (gasBlocks.size() >= MAX_GAS_BLOCKS) {
                    break;
                }
                BlockPos neighbor = pos.offset(direction);
                if (gasBlocks.contains(neighbor)
                        || neighbor.getSquaredDistance(this.getBlockPos()) > MAX_SPREAD_RADIUS_SQUARED) {
                    continue;
                }
                VoxelShape fromShape = serverWorld.getBlockState(pos).getCollisionShape(serverWorld, pos);
                VoxelShape toShape = serverWorld.getBlockState(neighbor).getCollisionShape(serverWorld, neighbor);
                if (doesShapeBlockExit(fromShape, direction)
                        || isBlockTooSolid(toShape)
                        || doesShapeBlockEntry(toShape, direction)) {
                    stillEdge = true;
                    continue;
                }
                gasBlocks.add(neighbor);
                newFrontier.add(neighbor);
            }
            if (stillEdge) {
                newFrontier.add(pos);
            }
        }
        frontier = newFrontier;
    }

    @Unique
    private void sparktraits$applyBlueGas(ServerWorld serverWorld) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverWorld);
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
                continue;
            }
            boolean effectiveCivilian = ConsciencePoisonerService.shouldBlueGasIgnorePlayer(
                    gameWorld.getRole(player),
                    TraitPlayerComponent.KEY.get(player).getActiveTraitIds()
            );
            boolean inGas = sparktraits$isPlayerInGas(player);
            if (effectiveCivilian) {
                exposureTicks.put(player.getUuid(), 0);
                sparktraits$clearGasExhaustion(player);
                continue;
            }
            if (inGas) {
                int ticks = exposureTicks.getOrDefault(player.getUuid(), 0) + 1;
                exposureTicks.put(player.getUuid(), ticks);
                sparktraits$applyGasExhaustion(player);
                if (ticks >= EXPOSURE_THRESHOLD) {
                    ConsciencePoisonerService.applyBluePoison(
                            player,
                            ownerUuid,
                            ConsciencePoisonerService.BLUE_GAS_POISON_TICKS
                    );
                    exposureTicks.put(player.getUuid(), 0);
                }
            } else {
                exposureTicks.put(player.getUuid(), 0);
                sparktraits$clearGasExhaustion(player);
            }
        }
    }

    @Unique
    private boolean sparktraits$isPlayerInGas(ServerPlayerEntity player) {
        Box box = player.getBoundingBox();
        for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX); x++) {
            for (int y = MathHelper.floor(box.minY); y <= MathHelper.floor(box.maxY); y++) {
                for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ); z++) {
                    if (gasBlocks.contains(new BlockPos(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Unique
    private void sparktraits$applyGasExhaustion(ServerPlayerEntity player) {
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        if (!stamina.isInfiniteStamina()) {
            stamina.setSprintingTicks(0);
            stamina.setExhausted(true);
            playersInGas.add(player.getUuid());
        }
    }

    @Unique
    private void sparktraits$clearGasExhaustion(ServerPlayerEntity player) {
        if (!playersInGas.remove(player.getUuid())) {
            return;
        }
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        if (!stamina.isInfiniteStamina()) {
            stamina.setExhausted(false);
            stamina.setSprintingTicks(76.0f);
        }
    }

    @Unique
    private void sparktraits$spawnBlueGasParticles(ServerWorld serverWorld) {
        if (gasBlocks.isEmpty()) {
            return;
        }
        List<BlockPos> blockList = new ArrayList<>(gasBlocks);
        int particleCount = 4 + serverWorld.random.nextInt(3);
        for (int i = 0; i < particleCount && !blockList.isEmpty(); i++) {
            BlockPos pos = blockList.get(serverWorld.random.nextInt(blockList.size()));
            serverWorld.spawnParticles(
                    BLUE_GAS_PARTICLE,
                    pos.getX() + 0.5 + serverWorld.random.nextDouble() * 0.3,
                    pos.getY() + 0.5 + serverWorld.random.nextDouble() * 0.3,
                    pos.getZ() + 0.5 + serverWorld.random.nextDouble() * 0.3,
                    1,
                    0,
                    0,
                    0,
                    0
            );
        }
    }
}
