package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.silencer.SilencedPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Restricts silenced players who belong to the effective killer team.
 * 限制被静语且属于反转后杀手阵营的玩家。
 */
public final class SilencedKillerRestrictionService {
    public static final String ABILITY_DENY_KEY = "message.sparktraits.silenced_killer.ability";
    public static final String SHOP_DENY_KEY = "shop.error.sparktraits.silenced_killer";

    private SilencedKillerRestrictionService() {
    }

    public static void register() {
        ShopPurchase.BEFORE.register((player, entry, index) -> shopPurchaseResult(isRestricted(player)));
    }

    public static boolean shouldRestrict(boolean silenced, Role role, Collection<Identifier> traitIds) {
        return silenced && EffectiveTraitService.isEffectiveKiller(role, traitIds);
    }

    public static boolean isRestricted(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        return shouldRestrict(
                SilencedPlayerComponent.isPlayerSilenced(player),
                game.getRole(player),
                TraitPlayerComponent.KEY.get(player).getActiveTraitIds()
        );
    }

    @Nullable
    public static ShopPurchase.PurchaseResult shopPurchaseResult(boolean restricted) {
        return restricted ? ShopPurchase.PurchaseResult.deny(SHOP_DENY_KEY) : null;
    }

    public static boolean denyActiveAbilityIfRestricted(ServerPlayerEntity player) {
        if (!isRestricted(player)) {
            return false;
        }
        player.sendMessage(Text.translatable(ABILITY_DENY_KEY), true);
        return true;
    }
}
