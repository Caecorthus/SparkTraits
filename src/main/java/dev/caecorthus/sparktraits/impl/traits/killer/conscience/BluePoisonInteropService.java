package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.SparkTraitsDataComponentTypes;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.block_entity.TrimmedBedBlockEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Server-authoritative blue-poison operations that downstream mods reach through {@code SparkTraitsApi}.
 * Conversions reuse the same storage as Conscience Poisoner (plate/bed duck interfaces, stack component),
 * so converted traps spring through the existing blue-trap paths unchanged.
 * 下游模组经 {@code SparkTraitsApi} 调用的服务端权威蓝毒操作。转换复用善良毒师的同一存储
 * （餐盘/床的鸭子接口与物品组件），转换后的陷阱沿现有蓝毒触发路径生效。
 */
public final class BluePoisonInteropService {
    private static final BluePoisonInteropRules.DrainExemptions<ServerPlayerEntity> DRAIN_EXEMPTIONS =
            new BluePoisonInteropRules.DrainExemptions<>(failure -> SparkTraits.LOGGER.warn(
                    "A blue sanity drain exemption threw; treating it as not exempt (further failures are not logged)",
                    failure
            ));

    private BluePoisonInteropService() {
    }

    public static boolean convertPlatePoisonToBlue(@Nullable World world, @Nullable BlockPos pos, @Nullable UUID poisoner) {
        if (world == null || world.isClient || pos == null || poisoner == null
                || !(world.getBlockEntity(pos) instanceof BeveragePlateBlockEntity plate)
                || !(plate instanceof ConsciencePoisonedPlate bluePlate)) {
            return false;
        }
        BluePoisonInteropRules.Conversion conversion = BluePoisonInteropRules.conversion(
                plate.getPoisoner() != null,
                bluePlate.sparktraits$getConsciencePoisoner() != null
        );
        if (conversion == BluePoisonInteropRules.Conversion.NONE) {
            return false;
        }
        // Both setters mark the plate dirty and push a block-entity update to clients.
        // 两个 setter 都会标记餐盘为脏并向客户端推送方块实体更新。
        plate.setPoisoner(null);
        if (conversion == BluePoisonInteropRules.Conversion.REPLACE_NATIVE_WITH_BLUE) {
            bluePlate.sparktraits$setConsciencePoisoner(poisoner.toString());
        }
        return true;
    }

    public static boolean convertBedPoisonToBlue(@Nullable World world, @Nullable BlockPos pos, @Nullable UUID poisoner) {
        if (world == null || world.isClient || pos == null || poisoner == null) {
            return false;
        }
        // Wathe and the blue layer both live on the bed HEAD; either clicked half resolves to it.
        // Wathe 原生蝎子与蓝蝎子都存于床头；点击床的任一半都会解析到床头。
        TrimmedBedBlockEntity head = ConsciencePoisonerService.resolveBedHead(world, pos);
        if (!(head instanceof ConscienceScorpionBed blueBed)) {
            return false;
        }
        BluePoisonInteropRules.Conversion conversion = BluePoisonInteropRules.conversion(
                head.hasScorpion(),
                blueBed.sparktraits$hasConscienceScorpion()
        );
        if (conversion == BluePoisonInteropRules.Conversion.NONE) {
            return false;
        }
        head.setHasScorpion(false, null);
        if (conversion == BluePoisonInteropRules.Conversion.REPLACE_NATIVE_WITH_BLUE) {
            blueBed.sparktraits$setConscienceScorpion(true, poisoner);
        }
        return true;
    }

    public static boolean convertStackPoisonToBlue(@Nullable ItemStack stack, @Nullable UUID poisoner) {
        if (stack == null || stack.isEmpty() || poisoner == null) {
            return false;
        }
        BluePoisonInteropRules.Conversion conversion = BluePoisonInteropRules.conversion(
                stack.contains(WatheDataComponentTypes.POISONER),
                stack.contains(SparkTraitsDataComponentTypes.CONSCIENCE_POISONER)
        );
        if (conversion == BluePoisonInteropRules.Conversion.NONE) {
            return false;
        }
        stack.remove(WatheDataComponentTypes.POISONER);
        if (conversion == BluePoisonInteropRules.Conversion.REPLACE_NATIVE_WITH_BLUE) {
            stack.set(SparkTraitsDataComponentTypes.CONSCIENCE_POISONER, poisoner.toString());
        }
        return true;
    }

    public static void markStackBluePoison(@Nullable ItemStack stack, @Nullable UUID poisoner) {
        if (stack != null && !stack.isEmpty() && poisoner != null) {
            stack.set(SparkTraitsDataComponentTypes.CONSCIENCE_POISONER, poisoner.toString());
        }
    }

    public static @Nullable UUID getStackBluePoisoner(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return BluePoisonInteropRules.parsePoisoner(stack.get(SparkTraitsDataComponentTypes.CONSCIENCE_POISONER));
    }

    public static void applyBlueTrap(@Nullable ServerPlayerEntity target, @Nullable UUID poisoner) {
        if (target != null && poisoner != null && GameFunctions.isPlayerPlayingAndAlive(target)) {
            ConsciencePoisonerService.triggerBlueTrap(target, poisoner);
        }
    }

    public static void applyBlueSanityDrain(@Nullable ServerPlayerEntity target, int ticks) {
        if (target != null && ticks > 0 && GameFunctions.isPlayerPlayingAndAlive(target)) {
            ConsciencePoisonerService.applyBlueSanityDrain(target, ticks);
        }
    }

    /** The drain window is server-only state and is never synced, so clients always read 0.
     *  扣理智窗口仅存在于服务端且不同步，因此客户端始终读到 0。 */
    public static int getBlueSanityDrainTicks(@Nullable PlayerEntity player) {
        if (player == null || player.getWorld() == null || player.getWorld().isClient) {
            return 0;
        }
        return TraitPlayerComponent.KEY.maybeGet(player)
                .map(TraitPlayerComponent::getBlueSanityDrainTicks)
                .orElse(0);
    }

    public static void registerBlueSanityDrainExemption(@Nullable Predicate<ServerPlayerEntity> exemption) {
        DRAIN_EXEMPTIONS.register(exemption);
    }

    /** Called from the server tick of {@link TraitPlayerComponent}; true skips that tick's mood drain only.
     *  由 {@link TraitPlayerComponent} 的服务端 tick 调用；返回 true 只跳过本 tick 的扣理智。 */
    public static boolean isBlueSanityDrainExempt(ServerPlayerEntity player) {
        return DRAIN_EXEMPTIONS.isExempt(player);
    }
}
