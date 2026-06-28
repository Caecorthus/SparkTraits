package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.SparkTraitsDataComponentTypes;
import dev.caecorthus.sparktraits.item.SparkTraitsItems;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.item.CocktailItem;
import dev.doctor4t.wathe.util.PoisonUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.Noellesroles;

public final class ToxicologistCapsuleService {
    private ToxicologistCapsuleService() {
    }

    public static boolean isToxicologist(PlayerEntity player) {
        return player != null
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.TOXICOLOGIST);
    }

    public static boolean isEligibleCapsuleContent(boolean food, boolean drink) {
        return food || drink;
    }

    public static boolean isEligibleCapsuleContent(ItemStack stack) {
        return !stack.isEmpty()
                && isEligibleCapsuleContent(stack.get(DataComponentTypes.FOOD) != null, isDrink(stack))
                && !stack.isOf(SparkTraitsItems.CAPSULE);
    }

    public static boolean isDrink(ItemStack stack) {
        return stack.getItem() instanceof CocktailItem;
    }

    public static ItemStack getContent(ItemStack capsuleStack) {
        ItemStack content = capsuleStack.getOrDefault(SparkTraitsDataComponentTypes.CAPSULE_CONTENTS, ItemStack.EMPTY);
        return content == null ? ItemStack.EMPTY : content;
    }

    public static boolean hasContent(ItemStack capsuleStack) {
        return !getContent(capsuleStack).isEmpty();
    }

    public static boolean tryFillCapsule(ItemStack capsuleStack, ItemStack cursorStack, PlayerEntity player) {
        if (!capsuleStack.isOf(SparkTraitsItems.CAPSULE)
                || hasContent(capsuleStack)
                || !isToxicologist(player)
                || !isEligibleCapsuleContent(cursorStack)) {
            return false;
        }
        ItemStack content = cursorStack.copyWithCount(1);
        capsuleStack.set(SparkTraitsDataComponentTypes.CAPSULE_CONTENTS, content);
        cursorStack.decrement(1);
        return true;
    }

    public static String capsuleDisplayName(String contentName, boolean normalPoison, boolean bluePoison) {
        if (contentName == null || contentName.isBlank()) {
            return "胶囊";
        }
        return normalPoison || bluePoison
                ? "胶囊（有毒的" + contentName + "）"
                : "胶囊（" + contentName + "）";
    }

    public static int capsulePoisonColor(boolean normalPoison, boolean bluePoison) {
        if (normalPoison && bluePoison) {
            return ConsciencePoisonerService.MIXED_POISON_COLOR;
        }
        if (normalPoison) {
            return ConsciencePoisonerService.NORMAL_POISONER_INSTINCT_COLOR;
        }
        if (bluePoison) {
            return ConsciencePoisonerService.BLUE_POISON_COLOR;
        }
        return -1;
    }

    public static Text capsuleName(ItemStack capsuleStack) {
        ItemStack content = getContent(capsuleStack);
        if (content.isEmpty()) {
            return Text.literal("胶囊");
        }
        String contentName = content.getName().getString();
        boolean normalPoison = hasNormalPoison(content);
        boolean bluePoison = hasBluePoison(content);
        if (!normalPoison && !bluePoison) {
            return Text.literal(capsuleDisplayName(contentName, false, false));
        }
        int color = capsulePoisonColor(normalPoison, bluePoison);
        MutableText text = Text.literal("胶囊（");
        text.append(Text.literal("有毒的" + contentName).withColor(color));
        text.append(Text.literal("）"));
        return text;
    }

    public static boolean hasNormalPoison(ItemStack stack) {
        return stack.get(WatheDataComponentTypes.POISONER) != null;
    }

    public static boolean hasBluePoison(ItemStack stack) {
        return stack.get(SparkTraitsDataComponentTypes.CONSCIENCE_POISONER) != null;
    }

    public static boolean forceFeedCapsule(ServerPlayerEntity target, ItemStack capsuleStack) {
        ItemStack content = getContent(capsuleStack).copy();
        if (content.isEmpty() || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return false;
        }
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(target);
        if (isDrink(content)) {
            mood.drinkCocktail();
            target.getWorld().playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else {
            mood.eatFood();
            target.getWorld().playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        PoisonUtils.applyFoodPoison(target, content);
        target.sendMessage(Text.translatable("message.sparktraits.capsule.force_fed", content.getName()).formatted(Formatting.YELLOW), true);
        return true;
    }
}
