package dev.caecorthus.sparktraits.impl.traits.killer.conscience;

import net.minecraft.util.Identifier;

import java.util.Set;

/** Pure policy for the Conscience Bomber's temporary grenade mode.
 *  善良炸弹客临时手雷模式的纯规则。 */
public final class ConscienceBomberFrenzyRules {
    public static final int MODE_DURATION_TICKS = 20 * 20;
    public static final int SHOP_COOLDOWN_TICKS = 3 * 60 * 20;
    public static final int PRICE = 350;
    public static final float THROW_SPEED = 0.5F * 1.5F;

    private static final Set<Identifier> VULNERABLE_WITCH_ROLES = Set.of(
            Identifier.of("sparkwitch", "grand_witch"),
            Identifier.of("sparkwitch", "accomplice"),
            Identifier.of("sparkwitch", "murderous_witch")
    );

    private ConscienceBomberFrenzyRules() {
    }

    static boolean canBuy(boolean bomber, boolean conscience) {
        return bomber && conscience;
    }

    static boolean isActive(long currentTick, long expiresAtTick) {
        return currentTick < expiresAtTick;
    }

    public static boolean shouldKillTarget(Identifier roleId, boolean effectiveCivilian) {
        return !effectiveCivilian
                || (roleId != null && VULNERABLE_WITCH_ROLES.contains(roleId));
    }

    public static boolean shouldBlockGrenadeUse(
            boolean marked,
            boolean validMarkedGrenade,
            boolean ordinaryCooldown
    ) {
        return marked ? !validMarkedGrenade : ordinaryCooldown;
    }
}
