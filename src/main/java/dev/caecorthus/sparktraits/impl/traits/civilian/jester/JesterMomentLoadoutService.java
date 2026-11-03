package dev.caecorthus.sparktraits.impl.traits.civilian.jester;

import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Server side of the Jester's psycho loadout: the first kill and then every other kill restore one shield (up to
 * three), and the moment closes the Jester's inventory and locks a baseball bat into its hand.
 * 小丑疯魔阶段的服务端部分：第一次击杀以及之后每隔一次击杀回复一层护盾（最多三层）；
 * 时刻开始时关闭小丑背包，并把棒球棍锁进其主手。
 */
public final class JesterMomentLoadoutService {
    public static final int MAX_SHIELDS = 3;

    private JesterMomentLoadoutService() {
    }

    /**
     * Shields after a kill: the first kill adds one, the second adds nothing, the third adds one again, and so on.
     * Never touches a shield that is already at the cap.
     * 击杀后的护盾：第一杀加一层，第二杀不加，第三杀再加一层，以此类推；已到上限时不再变化。
     */
    public static int shieldsAfterKill(int killCount, int currentShields) {
        if (killCount <= 0 || (killCount & 1) == 0) {
            return currentShields;
        }
        return Math.min(MAX_SHIELDS, currentShields + 1);
    }

    /**
     * Moment start: close whatever screen the Jester has open and make the bat its selected hotbar item.
     * 时刻开始：关闭小丑当前打开的界面，并把棒球棍设为其选中的快捷栏物品。
     */
    public static void onPsychoStarted(ServerPlayerEntity jester) {
        jester.closeHandledScreen();
        forceBat(jester);
    }

    private static void forceBat(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        int batSlot = findBatSlot(inventory);
        if (batSlot < 0) {
            batSlot = placeNewBat(player, inventory);
            if (batSlot < 0) {
                return;
            }
        }
        if (batSlot >= 9) {
            // The bat is in the main inventory; swap it into the selected hotbar slot so it can be held.
            // 球棒在主背包里；与当前选中的快捷栏槽位交换，才能拿在手上。
            int hotbar = inventory.selectedSlot;
            ItemStack hotbarStack = inventory.getStack(hotbar);
            inventory.setStack(hotbar, inventory.getStack(batSlot));
            inventory.setStack(batSlot, hotbarStack);
            player.currentScreenHandler.sendContentUpdates();
            batSlot = hotbar;
        }
        if (inventory.selectedSlot != batSlot) {
            // The client keeps its own hotbar slot; sync the forced slot or it keeps holding the old item.
            // 客户端维护自己的快捷栏槽位；不同步的话客户端仍会拿着原来的物品。
            inventory.selectedSlot = batSlot;
            player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(batSlot));
        }
    }

    private static int findBatSlot(PlayerInventory inventory) {
        for (int slot = 0; slot < inventory.main.size(); slot++) {
            if (inventory.main.get(slot).isOf(WatheItems.BAT)) {
                return slot;
            }
        }
        return -1;
    }

    private static int placeNewBat(ServerPlayerEntity player, PlayerInventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getStack(slot).isEmpty()) {
                inventory.setStack(slot, new ItemStack(WatheItems.BAT));
                player.currentScreenHandler.sendContentUpdates();
                return slot;
            }
        }
        // No free hotbar slot: replace the selected one and put the old item into the inventory or on the ground.
        // 快捷栏已满：替换当前选中槽，旧物品放回背包或掉到地上。
        int slot = inventory.selectedSlot;
        ItemStack replaced = inventory.getStack(slot);
        inventory.setStack(slot, new ItemStack(WatheItems.BAT));
        if (!replaced.isEmpty() && !inventory.insertStack(replaced)) {
            player.dropItem(replaced, false);
        }
        player.currentScreenHandler.sendContentUpdates();
        return slot;
    }
}
