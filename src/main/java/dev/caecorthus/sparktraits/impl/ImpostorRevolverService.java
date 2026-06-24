package dev.caecorthus.sparktraits.impl;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;

/**
 * Adds the paid revolver route for Impostors without restoring ground pickup.
 * 为内鬼添加付费购买左轮的路径，但不恢复地面拾取权限。
 */
public final class ImpostorRevolverService {
    public static final Identifier REVOLVER_SHOP_ID = SparkTraits.id("impostor_revolver");
    public static final int REVOLVER_PRICE = 150;

    private ImpostorRevolverService() {
    }

    public static void register() {
        BuildShopEntries.EVENT.register(ImpostorRevolverService::addRevolver);
    }

    public static boolean shouldAddRevolverToShop(Role role, Collection<Identifier> traits) {
        return traits != null && EffectiveTraitService.hasImpostor(traits);
    }

    /** Keeps Impostors on the paid shop route for guns instead of free inventory acquisition.
     *  让内鬼只能通过付费商店路径获得枪械，不能通过免费入包绕过。 */
    public static boolean shouldBlockNonShopGunAcquisition(Collection<Identifier> traits, boolean gunStack) {
        return gunStack && traits != null && EffectiveTraitService.hasImpostor(traits);
    }

    /** Keeps the older ground-pickup call site readable while sharing the stricter policy.
     *  保留地面拾取调用点的可读性，同时复用更严格的非商店拿枪策略。 */
    public static boolean shouldBlockGroundGunPickup(Collection<Identifier> traits, boolean gunStack) {
        return shouldBlockNonShopGunAcquisition(traits, gunStack);
    }

    private static void addRevolver(PlayerEntity player, BuildShopEntries.ShopContext context) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getWorld());
        if (!shouldAddRevolverToShop(gameComponent.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds())) {
            return;
        }
        boolean alreadyPresent = context.getEntries().stream()
                .anyMatch(entry -> REVOLVER_SHOP_ID.toString().equals(entry.id()) || entry.stack().isOf(WatheItems.REVOLVER));
        if (alreadyPresent) {
            return;
        }
        context.addEntry(new ShopEntry.Builder(
                REVOLVER_SHOP_ID.toString(),
                WatheItems.REVOLVER.getDefaultStack(),
                REVOLVER_PRICE,
                ShopEntry.Type.WEAPON
        ).build());
    }
}
