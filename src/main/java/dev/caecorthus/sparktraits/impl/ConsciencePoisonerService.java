package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.block_entity.TrimmedBedBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

import java.util.Collection;
import java.util.UUID;

/**
 * Handles Conscience Poisoner's blue-poison rules.
 * 统一处理善良毒师的蓝毒规则。
 */
public final class ConsciencePoisonerService {
    public enum BlueTrapResult {
        NO_BLUE_POISON,
        CONSUME_ONLY,
        CONSUME_AND_POISON
    }

    public static final Identifier FINE_DRINK_ID = Identifier.of(Noellesroles.MOD_ID, "fine_drink");
    public static final Identifier BLUE_POISON_SOURCE = SparkTraits.id("blue_poison");
    public static final int FINE_DRINK_PRICE = 75;
    public static final int FINE_DRINK_MAX_STOCK = -1;
    public static final boolean FINE_DRINK_HAS_STOCK_LIMIT = false;
    public static final int NORMAL_POISONER_INSTINCT_COLOR = 0x1E5014;
    public static final int BLUE_POISON_COLOR = 0x00BFFF;
    public static final int MIXED_POISON_COLOR = mixColors(NORMAL_POISONER_INSTINCT_COLOR, BLUE_POISON_COLOR);
    public static final int BLUE_GAS_POISON_TICKS = 20 * 20;

    private ConsciencePoisonerService() {
    }

    public static void register() {
        BuildShopEntries.EVENT.register(ConsciencePoisonerService::addFineDrink);
    }

    public static boolean isConsciencePoisoner(Role role, Collection<Identifier> traits) {
        return Noellesroles.POISONER.equals(role) && EffectiveTraitService.hasConscience(traits);
    }

    public static boolean isConsciencePoisoner(PlayerEntity player, GameWorldComponent gameComponent) {
        return player != null
                && gameComponent != null
                && isConsciencePoisoner(
                gameComponent.getRole(player),
                TraitPlayerComponent.KEY.get(player).getActiveTraitIds()
        );
    }

    public static boolean shouldAddFineDrinkToShop(Role role, Collection<Identifier> traits) {
        return isConsciencePoisoner(role, traits);
    }

    public static boolean shouldBlueGasAffect(Role role, Collection<Identifier> traits) {
        return !EffectiveTraitService.isEffectiveCivilian(role, traits);
    }

    public static boolean shouldApplyBluePoison(Role role, Collection<Identifier> traits) {
        return shouldBlueGasAffect(role, traits);
    }

    public static boolean shouldBlueGasIgnorePlayer(Role role, Collection<Identifier> traits) {
        return !shouldBlueGasAffect(role, traits);
    }

    public static BlueTrapResult blueTrapResult(boolean hasBluePoison, Role role, Collection<Identifier> traits) {
        if (!hasBluePoison) {
            return BlueTrapResult.NO_BLUE_POISON;
        }
        return shouldBlueGasAffect(role, traits) ? BlueTrapResult.CONSUME_AND_POISON : BlueTrapResult.CONSUME_ONLY;
    }

    public static boolean poisonStateIgnoresConscienceRange(boolean normalPoisoned, boolean bluePoisoned) {
        return normalPoisoned || bluePoisoned;
    }

    public static int poisonInstinctColor(boolean normalPoisoned, boolean bluePoisoned) {
        if (normalPoisoned && bluePoisoned) {
            return MIXED_POISON_COLOR;
        }
        return bluePoisoned ? BLUE_POISON_COLOR : NORMAL_POISONER_INSTINCT_COLOR;
    }

    public static int poisonHighlightColor(boolean normalPoisoned, boolean bluePoisoned, int normalPoisonColor) {
        if (!normalPoisoned && !bluePoisoned) {
            return -1;
        }
        if (normalPoisoned && bluePoisoned) {
            return mixColors(normalPoisonColor, BLUE_POISON_COLOR);
        }
        return bluePoisoned ? BLUE_POISON_COLOR : normalPoisonColor;
    }

    public static int toxicologistPoisonInstinctColor(boolean bluePoisoned, int normalToxicologistColor) {
        return bluePoisoned ? BLUE_POISON_COLOR : normalToxicologistColor;
    }

    public static int bluePoisonTicksAfterTrap(int currentTicks, int rolledTicks, int reductionTicks) {
        return bluePoisonTicksAfterTrap(currentTicks, rolledTicks, reductionTicks, PlayerPoisonComponent.clampTime.getRight());
    }

    static int bluePoisonTicksAfterTrap(int currentTicks, int rolledTicks, int reductionTicks, int maxTicks) {
        if (currentTicks <= 0) {
            return rolledTicks;
        }
        return MathHelper.clamp(currentTicks - reductionTicks, 0, maxTicks);
    }

    public static boolean shouldShowHiddenBluePoisonParticles(
            boolean consciencePoisoner,
            boolean toxicologist,
            boolean spectatorOrCreative
    ) {
        return consciencePoisoner || toxicologist || spectatorOrCreative;
    }

    public static boolean canSeeHiddenBluePoisonParticles(PlayerEntity viewer) {
        if (viewer == null) {
            return false;
        }
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(viewer.getWorld());
        return shouldShowHiddenBluePoisonParticles(
                isConsciencePoisoner(viewer, gameComponent),
                gameComponent.isRole(viewer, Noellesroles.TOXICOLOGIST),
                GameFunctions.isPlayerSpectatingOrCreative(viewer)
        );
    }

    public static void applyBluePoison(ServerPlayerEntity target, UUID poisoner, int ticks) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(target.getWorld());
        if (!shouldApplyBluePoison(gameComponent.getRole(target), TraitPlayerComponent.KEY.get(target).getActiveTraitIds())) {
            return;
        }
        TraitPlayerComponent.KEY.get(target).setConsciencePoisonTicks(ticks, poisoner);
    }

    public static void bedPoison(ServerPlayerEntity player) {
        World world = player.getEntityWorld();
        TrimmedBedBlockEntity blockEntity = findConscienceScorpionHead(world, player.getBlockPos());
        if (!(blockEntity instanceof ConscienceScorpionBed conscienceBed)) {
            return;
        }

        UUID poisoner = conscienceBed.sparktraits$getConscienceScorpionPoisoner();
        conscienceBed.sparktraits$setConscienceScorpion(false, null);
        TraitPlayerComponent targetTraits = TraitPlayerComponent.KEY.get(player);
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(world);
        BlueTrapResult result = blueTrapResult(true, gameComponent.getRole(player), targetTraits.getActiveTraitIds());
        if (result != BlueTrapResult.CONSUME_AND_POISON) {
            return;
        }

        int ticks = bluePoisonTicksAfterTrap(
                targetTraits.getConsciencePoisonTicks(),
                world.getRandom().nextBetween(PlayerPoisonComponent.clampTime.getLeft(), PlayerPoisonComponent.clampTime.getRight()),
                world.getRandom().nextBetween(100, 300)
        );
        applyBluePoison(player, poisoner, ticks);
    }

    private static void addFineDrink(PlayerEntity player, BuildShopEntries.ShopContext context) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (!shouldAddFineDrinkToShop(gameComponent.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds())) {
            return;
        }
        boolean alreadyPresent = context.getEntries().stream()
                .anyMatch(entry -> FINE_DRINK_ID.toString().equals(entry.id()) || entry.stack().isOf(ModItems.FINE_DRINK));
        if (alreadyPresent) {
            return;
        }
        context.addEntry(new ShopEntry.Builder(
                FINE_DRINK_ID.toString(),
                ModItems.FINE_DRINK.getDefaultStack(),
                FINE_DRINK_PRICE,
                ShopEntry.Type.TOOL
        ).build());
    }

    private static int mixColors(int left, int right) {
        int red = (((left >> 16) & 0xFF) + ((right >> 16) & 0xFF)) / 2;
        int green = (((left >> 8) & 0xFF) + ((right >> 8) & 0xFF)) / 2;
        int blue = ((left & 0xFF) + (right & 0xFF)) / 2;
        return (red << 16) | (green << 8) | blue;
    }

    private static TrimmedBedBlockEntity findConscienceScorpionHead(World world, BlockPos centerPos) {
        int radius = 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = centerPos.add(dx, dy, dz);
                    TrimmedBedBlockEntity entity = resolveBedHead(world, pos);
                    if (entity instanceof ConscienceScorpionBed conscienceBed
                            && conscienceBed.sparktraits$hasConscienceScorpion()
                            && isLineClear(world, centerPos, pos)) {
                        return entity;
                    }
                }
            }
        }
        return null;
    }

    public static TrimmedBedBlockEntity resolveBedHead(World world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TrimmedBedBlockEntity entity)) {
            return null;
        }
        BlockState state = world.getBlockState(pos);
        BedPart part = state.get(BedBlock.PART);
        Direction facing = state.get(HorizontalFacingBlock.FACING);
        if (part == BedPart.HEAD) {
            return entity;
        }
        if (part == BedPart.FOOT) {
            BlockPos headPos = pos.offset(facing);
            if (world.getBlockEntity(headPos) instanceof TrimmedBedBlockEntity headEntity
                    && world.getBlockState(headPos).get(BedBlock.PART) == BedPart.HEAD) {
                return headEntity;
            }
        }
        return null;
    }

    private static boolean isLineClear(World world, BlockPos start, BlockPos end) {
        int x0 = start.getX();
        int y0 = start.getY();
        int z0 = start.getZ();
        int x1 = end.getX();
        int y1 = end.getY();
        int z1 = end.getZ();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;

        int ax = 2 * dx;
        int ay = 2 * dy;
        int az = 2 * dz;

        if (dx >= dy && dx >= dz) {
            int err1 = ay - dx;
            int err2 = az - dx;
            while (x0 != x1) {
                x0 += sx;
                if (err1 > 0) {
                    y0 += sy;
                    err1 -= 2 * dx;
                }
                if (err2 > 0) {
                    z0 += sz;
                    err2 -= 2 * dx;
                }
                err1 += ay;
                err2 += az;
                if (isBlocking(world, new BlockPos(x0, y0, z0))) {
                    return false;
                }
            }
        } else if (dy >= dx && dy >= dz) {
            int err1 = ax - dy;
            int err2 = az - dy;
            while (y0 != y1) {
                y0 += sy;
                if (err1 > 0) {
                    x0 += sx;
                    err1 -= 2 * dy;
                }
                if (err2 > 0) {
                    z0 += sz;
                    err2 -= 2 * dy;
                }
                err1 += ax;
                err2 += az;
                if (isBlocking(world, new BlockPos(x0, y0, z0))) {
                    return false;
                }
            }
        } else {
            int err1 = ay - dz;
            int err2 = ax - dz;
            while (z0 != z1) {
                z0 += sz;
                if (err1 > 0) {
                    y0 += sy;
                    err1 -= 2 * dz;
                }
                if (err2 > 0) {
                    x0 += sx;
                    err2 -= 2 * dz;
                }
                err1 += ay;
                err2 += ax;
                if (isBlocking(world, new BlockPos(x0, y0, z0))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isBlocking(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !(state.getBlock() instanceof BedBlock);
    }
}
