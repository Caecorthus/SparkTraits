package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.cca.PlayerShopComponent;

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
            if (pricePaid <= EconomyTrait.THRESHOLD) {
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
}
