package dev.caecorthus.sparktraits.impl.lifecycle;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.Identifier;

/** Registered terminal penalties are not protectable combat attempts.
 * 已注册的终结惩罚不是可保护的战斗攻击。 */
public final class TerminalDeathRules {
    private static final Set<Identifier> REASONS = new HashSet<>();
    private TerminalDeathRules() { }
    public static void register(Identifier reason) { REASONS.add(java.util.Objects.requireNonNull(reason)); }
    public static boolean contains(Identifier reason) { return reason != null && REASONS.contains(reason); }
}
