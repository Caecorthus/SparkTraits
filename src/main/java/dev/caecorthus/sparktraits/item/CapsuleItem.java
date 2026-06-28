package dev.caecorthus.sparktraits.item;

import dev.caecorthus.sparktraits.entity.CapsuleEntity;
import dev.caecorthus.sparktraits.entity.SparkTraitsEntities;
import dev.caecorthus.sparktraits.impl.ToxicologistCapsuleService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class CapsuleItem extends Item {
    public CapsuleItem(Settings settings) {
        super(settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        return ToxicologistCapsuleService.capsuleName(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (!ToxicologistCapsuleService.hasContent(stack)) {
            tooltip.add(Text.translatable("item.sparktraits.capsule.tooltip.empty"));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public boolean onClicked(
            ItemStack stack,
            ItemStack otherStack,
            Slot slot,
            ClickType clickType,
            PlayerEntity player,
            StackReference cursorStackReference
    ) {
        if (clickType != ClickType.RIGHT) {
            return false;
        }
        if (!ToxicologistCapsuleService.tryFillCapsule(stack, otherStack, player)) {
            return false;
        }
        if (otherStack.isEmpty()) {
            cursorStackReference.set(ItemStack.EMPTY);
        }
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!ToxicologistCapsuleService.isToxicologist(user)) {
            return TypedActionResult.pass(stack);
        }
        if (!ToxicologistCapsuleService.hasContent(stack)) {
            if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(Text.translatable("message.sparktraits.capsule.empty"), true);
            }
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            CapsuleEntity capsule = new CapsuleEntity(SparkTraitsEntities.CAPSULE, world);
            capsule.setOwner(user);
            capsule.setPos(user.getX(), user.getEyeY() - 0.1, user.getZ());
            capsule.setItem(stack.copyWithCount(1));
            capsule.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            world.spawnEntity(capsule);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        stack.decrementUnlessCreative(1, user);
        return TypedActionResult.success(stack, world.isClient());
    }
}
