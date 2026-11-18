package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorRevolverService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import dev.doctor4t.wathe.util.ShopUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.Collection;

/**
 * 经济: refunds 10% of the price actually paid whenever a holder buys something above 100 coins.
 * Uses the post-purchase event so discounted or modified prices refund the real amount.
 * 经济：持有者单笔消费超过 100 金币时返还实付的 10%。使用后置事件，折扣或改价后返还的是真实支付额。
 */
public final class EconomyService {

    private EconomyService() {
    }

    public static void register() {
        ShopPurchase.AFTER.register((player, entry, index, pricePaid) -> {
            if (!isRefundablePrice(pricePaid)) {
                return;
            }
            if (!TraitPlayerComponent.KEY.get(player).hasActiveTrait(EconomyTrait.ID)) {
                return;
            }
            int refund = pricePaid * EconomyTrait.REFUND_PERCENT / 100;
            if (refund > 0) {
                PlayerShopComponent.KEY.get(player).addToBalance(refund);
            }
        });
    }

    /** Only purchases strictly above the threshold refund. 仅严格超过门槛的消费才返还。 */
    static boolean isRefundablePrice(int price) {
        return price > EconomyTrait.THRESHOLD;
    }

    /**
     * Whether this player's Wathe shop sells anything the refund can fire on. The shop is read the same way the
     * Paranoid gate reads it, so every role whose own or inherited shop (Wathe's killer shop, NoellesRoles, SparkWitch,
     * SparkStrength) has an entry above the threshold qualifies, and shops capped at the threshold, non-coin shops and
     * skills that deduct the balance directly never do. The Impostor revolver is counted from the traits being chosen,
     * because those traits are not active yet while the round's traits are planned.
     * 该玩家的 Wathe 商店里是否有能触发返还的商品。读取方式与偏执的抽取门槛相同：凡是本职业商店（含 Wathe 杀手商店、
     * NoellesRoles、SparkWitch、SparkStrength）有高于门槛条目的都符合；商店封顶在门槛、非金币商店、技能直接扣余额的
     * 都不符合。规划本局天赋时这些天赋尚未生效，因此内鬼左轮按正在选择的天赋计入。
     */
    public static boolean hasRefundableShopEntry(PlayerEntity player, Role role, Collection<Identifier> selectedTraits) {
        if (player == null) {
            return false;
        }
        if (borrowsRefundableShopMidRound(role)) {
            return true;
        }
        if (ImpostorRevolverService.shouldAddRevolverToShop(role, selectedTraits)
                && isRefundablePrice(ImpostorRevolverService.REVOLVER_PRICE)) {
            return true;
        }
        for (ShopEntry entry : ShopUtils.getShopEntriesForPlayer(player)) {
            if (isRefundablePrice(entry.price())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The Coroner opens with a 100-coin body bag only, but SparkStrength hands it the shop of the role it disguises as
     * mid-round (Professor, Engineer, Recaller, Bomber...), which sells entries above the threshold.
     * 验尸官开局只有 100 金币的尸袋，但 SparkStrength 会在局中按其伪装的身份给出对应商店（教授、工程师、回溯者、炸弹客等），
     * 其中有高于门槛的商品。
     */
    static boolean borrowsRefundableShopMidRound(Role role) {
        return role != null && role.equals(Noellesroles.CORONER);
    }
}
