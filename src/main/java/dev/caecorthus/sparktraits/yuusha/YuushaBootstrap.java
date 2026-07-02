package dev.caecorthus.sparktraits.yuusha;

import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

public final class YuushaBootstrap implements ModInitializer {
    public static final Identifier ID = Identifier.of("sparktraits", "yuusha");

    @Override
    public void onInitialize() {
        YuushaTrait.register();

        ShouldPunishGunShooter.EVENT.register((shooter, target) ->
            YuushaTrait.isYuushaShooter(shooter)
                ? ShouldPunishGunShooter.PunishResult.cancel()
                : ShouldPunishGunShooter.PunishResult.allow()
        );

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (YuushaTrait.isYuushaPhone(stack)) {
                return YuushaTrait.tryUsePhone(player, world, hand, stack);
            }

            if (YuushaTrait.shouldBlockFoodOrDrink(player, stack)) {
                player.sendMessage(YuushaTrait.t("message.sparktraits.hero.taste_lost"), true);
                return TypedActionResult.fail(stack);
            }

            return TypedActionResult.pass(stack);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerManager().getPlayerList()
            .forEach(YuushaTrait::tickPlayer));
    }
}
