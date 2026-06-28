package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.item.SparkTraitsItems;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.api.event.CanSeeMoney;
import dev.doctor4t.wathe.api.event.TaskComplete;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

public final class RoleEnhancementService {
    public static final int TASK_MONEY_REWARD = 50;
    public static final int CAPSULE_PRICE = 100;
    public static final String CAPSULE_SHOP_ENTRY_ID = "sparktraits:capsule";

    private RoleEnhancementService() {
    }

    public static void register() {
        TaskComplete.EVENT.register((player, taskType) -> {
            GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
            if (shouldRewardTaskMoney(game.getRole(player))) {
                PlayerShopComponent.KEY.get(player).addToBalance(TASK_MONEY_REWARD);
            }
        });

        CanSeeMoney.EVENT.register(player -> {
            if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
                return null;
            }
            GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
            return shouldSeeMoney(game.getRole(player), true) ? CanSeeMoney.Result.ALLOW : null;
        });

        BuildShopEntries.EVENT.register(RoleEnhancementService::addCapsuleShopEntry);
    }

    public static boolean shouldRewardTaskMoney(Role role) {
        return Noellesroles.DETECTIVE.equals(role) || Noellesroles.TOXICOLOGIST.equals(role);
    }

    public static boolean shouldSeeMoney(Role role, boolean playingAlive) {
        return playingAlive && shouldRewardTaskMoney(role);
    }

    public static boolean shouldAddCapsuleShopEntry(Role role) {
        return Noellesroles.TOXICOLOGIST.equals(role);
    }

    private static void addCapsuleShopEntry(PlayerEntity player, BuildShopEntries.ShopContext context) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (!shouldAddCapsuleShopEntry(game.getRole(player))) {
            return;
        }
        boolean alreadyPresent = context.getEntries().stream()
                .anyMatch(entry -> CAPSULE_SHOP_ENTRY_ID.equals(entry.id()) || entry.stack().isOf(SparkTraitsItems.CAPSULE));
        if (alreadyPresent) {
            return;
        }
        context.addEntry(new ShopEntry.Builder(
                CAPSULE_SHOP_ENTRY_ID,
                SparkTraitsItems.CAPSULE.getDefaultStack(),
                CAPSULE_PRICE,
                ShopEntry.Type.TOOL
        ).build());
    }
}
