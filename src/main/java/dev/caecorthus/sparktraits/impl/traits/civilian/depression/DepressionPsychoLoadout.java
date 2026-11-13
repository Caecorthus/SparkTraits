package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheAttributes;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Stateless player-side effects of a Depression psycho: the bat-only inventory, held infinite stamina and Speed II
 * layer kept every tick, and the Speed, stamina and mood restores on exit. {@link DepressionTraitService} decides when.
 * 抑郁疯魔对玩家本身施加的无状态效果：每 tick 维持的只持球棒背包、无限体力与速度 II，以及结束时对速度、体力与理智的恢复。
 * 何时调用由 {@link DepressionTraitService} 决定。
 */
final class DepressionPsychoLoadout {
    static final int DEPRESSION_PSYCHO_SPEED_DURATION_TICKS = Integer.MAX_VALUE;
    static final int DEPRESSION_PSYCHO_SPEED_AMPLIFIER = 1;

    private DepressionPsychoLoadout() {
    }

    static void restorePostPsychoMood(ServerPlayerEntity player, boolean restoreMoodIfSurvived) {
        if (!restoreMoodIfSurvived || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        // Successful Depression psycho exits at a stable sanity value instead of preserving crisis mood.
        // 抑郁疯魔成功结束后固定恢复到稳定理智值，而不是保留濒临崩溃的理智。
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        mood.setMood(DepressionRules.depressionPsychoRestoredMood());
        mood.sync();
    }

    static void restorePostPsychoStamina(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game == null ? null : game.getRole(player);
        int roleMaxSprintTime = role == null ? -1 : role.getMaxSprintTime();
        EntityAttributeInstance sprintAttribute = player.getAttributeInstance(WatheAttributes.MAX_SPRINT_TIME);
        int effectiveMaxSprintTime = sprintAttribute == null ? roleMaxSprintTime : (int) sprintAttribute.getValue();
        DepressionRules.PostPsychoStaminaState state = DepressionRules.postPsychoStaminaState(roleMaxSprintTime, effectiveMaxSprintTime);
        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
        stamina.setMaxSprintTime(state.maxSprintTime());
        stamina.setSprintingTicks(state.sprintingTicks());
        stamina.setExhausted(state.exhausted());
        stamina.sync();
    }

    static void clearInventory(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.selectedSlot = 0;
    }

    static void enforceBatOnly(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack bat = null;
        boolean changed = false;
        for (List<ItemStack> list : List.of(inventory.main, inventory.armor, inventory.offHand)) {
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = list.get(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (bat == null && stack.isOf(WatheItems.BAT)) {
                    bat = stack;
                    if (list != inventory.main || i != 0) {
                        // Keep the bat in the forced slot, or the client reselects wherever it was moved to.
                        // 让球棒留在强制选中的槽位，否则客户端会重新选中球棒被移到的槽位。
                        list.set(i, ItemStack.EMPTY);
                        changed = true;
                    }
                    continue;
                }
                list.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (bat == null) {
            inventory.main.set(0, new ItemStack(WatheItems.BAT));
            changed = true;
        } else if (inventory.main.get(0) != bat) {
            inventory.main.set(0, bat);
        }
        if (inventory.selectedSlot != 0) {
            // The client keeps its own hotbar slot, so a server-only write would leave it holding a different slot.
            // 客户端维护自己的快捷栏槽位；只改服务端会让客户端仍停在另一个槽位。
            inventory.selectedSlot = 0;
            player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(0));
        }
        if (changed) {
            player.currentScreenHandler.sendContentUpdates();
        }
    }

    static void holdInfiniteStamina(PlayerStaminaComponent stamina) {
        // Keep the owner-only sync every tick: Wathe's sprint limiter also runs on the client and would drain the
        // owner's copy to exhaustion between server corrections.
        // 保留每 tick 仅发给本人的同步：Wathe 的冲刺限制也在客户端运行，两次服务端校正之间会把本人副本耗尽。
        stamina.setMaxSprintTime(-1);
        stamina.setSprintingTicks(Integer.MAX_VALUE);
        stamina.setExhausted(false);
        stamina.sync();
    }

    static void applyPsychoSpeed(ServerPlayerEntity player) {
        StatusEffectInstance speed = player.getStatusEffect(StatusEffects.SPEED);
        // An equal layer that is still effectively permanent is ours: re-adding it every tick only resets its
        // duration and sends the player an effect packet each time.
        // 仍近乎永久的同级速度层就是本状态添加的：每 tick 重新添加只会重置时长，并每次给玩家发送效果数据包。
        if (speed != null
                && (speed.getAmplifier() > DEPRESSION_PSYCHO_SPEED_AMPLIFIER
                || (speed.getAmplifier() == DEPRESSION_PSYCHO_SPEED_AMPLIFIER
                && speed.getDuration() > DEPRESSION_PSYCHO_SPEED_DURATION_TICKS / 2))) {
            return;
        }
        // English: Depression psycho owns a persistent Speed II layer until endPsycho restores/clears it.
        // 中文：抑郁疯魔期间由本状态维持一层永久速度 II，并在 endPsycho 时恢复或清除。
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED,
                DEPRESSION_PSYCHO_SPEED_DURATION_TICKS,
                DEPRESSION_PSYCHO_SPEED_AMPLIFIER,
                false,
                false,
                true
        ));
    }

    static void restorePrePsychoSpeed(ServerPlayerEntity player, @Nullable StatusEffectInstance prePsychoSpeed) {
        player.removeStatusEffect(StatusEffects.SPEED);
        if (prePsychoSpeed != null && GameFunctions.isPlayerPlayingAndAlive(player)) {
            player.addStatusEffect(new StatusEffectInstance(prePsychoSpeed));
        }
    }

    static @Nullable StatusEffectInstance copyStatusEffect(@Nullable StatusEffectInstance effect) {
        return effect == null ? null : new StatusEffectInstance(effect);
    }
}
