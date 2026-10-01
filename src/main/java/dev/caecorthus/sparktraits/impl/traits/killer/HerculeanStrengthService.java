package dev.caecorthus.sparktraits.impl.traits.killer;

import dev.caecorthus.sparktraits.SparkTraits;
import dev.doctor4t.wathe.util.ShopEntry;
import dev.doctor4t.wathe.util.ShopUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Herculean Strength: faster thrown projectiles and shorter throw charges.
 * 力大无穷：投掷物初速更大，投掷蓄力更短。
 */
public final class HerculeanStrengthService {
    public static final double THROW_SPEED_MULTIPLIER = 1.3;
    public static final int THROW_CHARGE_PERCENT = 70;
    // Additive gate: downstream mods contribute their own throwables to this tag.
    // 可扩展门槛：下游模组向该标签追加各自的投掷物。
    private static final TagKey<Item> THROWABLES = TagKey.of(RegistryKeys.ITEM, SparkTraits.id("throwables"));

    private HerculeanStrengthService() {
    }

    public static boolean isThrowable(ItemStack stack) {
        return stack != null && stack.isIn(THROWABLES);
    }

    public static boolean hasThrowableAccess(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        for (ShopEntry entry : ShopUtils.getShopEntriesForPlayer(player)) {
            if (isThrowable(entry.stack())) {
                return true;
            }
        }
        return player.getInventory().contains(HerculeanStrengthService::isThrowable);
    }

    /**
     * Scales the launch velocity once, as the projectile enters the world, whichever way its item set it.
     * 在投掷物进入世界时统一放大一次初速，不依赖物品设置速度的具体方式。
     */
    public static void boostThrownProjectile(Entity entity) {
        if (entity instanceof ProjectileEntity projectile
                && projectile.getOwner() instanceof ServerPlayerEntity thrower
                && KillerTraitService.hasEligibleTrait(thrower, KillerTraits.HERCULEAN_STRENGTH)) {
            projectile.setVelocity(projectile.getVelocity().multiply(THROW_SPEED_MULTIPLIER));
        }
    }

    /** For items that charge through vanilla item use: reports a longer charge at release.
     *  适用于走原版使用流程蓄力的物品：松手时按更长的蓄力时间结算。 */
    public static int remainingUseTicksAtRelease(ItemStack stack, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player) || !isThrowable(stack)
                || !KillerTraitService.hasEligibleTrait(player, KillerTraits.HERCULEAN_STRENGTH)) {
            return remainingUseTicks;
        }
        return remainingUseTicksAtRelease(stack.getMaxUseTime(user), remainingUseTicks);
    }

    static int remainingUseTicksAtRelease(int maxUseTime, int remainingUseTicks) {
        int elapsed = maxUseTime - remainingUseTicks;
        if (elapsed <= 0) {
            return remainingUseTicks;
        }
        long charged = (long) elapsed * 100 / THROW_CHARGE_PERCENT;
        return (int) Math.max(0L, maxUseTime - charged);
    }

    /** For throwables that time their own charge, such as server-owned sessions.
     *  适用于自行计时蓄力的投掷物，例如由服务端管理的蓄力会话。 */
    public static int throwChargeTicks(PlayerEntity player, int baseTicks) {
        return throwChargeTicks(baseTicks, KillerTraitService.hasEligibleTrait(player, KillerTraits.HERCULEAN_STRENGTH));
    }

    static int throwChargeTicks(int baseTicks, boolean herculean) {
        if (!herculean || baseTicks <= 0) {
            return baseTicks;
        }
        return (int) (((long) baseTicks * THROW_CHARGE_PERCENT + 99) / 100);
    }
}
