package dev.caecorthus.sparktraits.impl.selection;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

import java.util.Set;
import dev.caecorthus.sparktraits.SparkTraits;

/**
 * Role-level SparkTraits eligibility that stays decoupled from optional addon classes.
 * SparkTraits 身份级资格判断：只按完整身份 ID 识别可选附属模组，避免新增编译依赖。
 */
public final class TraitRoleEligibility {
    private static final String SPARKWITCH_MOD_ID = "sparkwitch";
    private static final Set<Identifier> TRAIT_BLOCKED_ROLE_IDS = Set.of(
            Identifier.of(SPARKWITCH_MOD_ID, "grand_witch"),
            Identifier.of(SPARKWITCH_MOD_ID, "accomplice"),
            // SparkWitch special accomplices share every Accomplice rule. / SparkWitch 特殊共犯沿用共犯的全部规则。
            Identifier.of(SPARKWITCH_MOD_ID, "abyss_listener"),
            Identifier.of(SPARKWITCH_MOD_ID, "potion_gunner"),
            Identifier.of(SPARKWITCH_MOD_ID, "riftwalker"),
            Identifier.of(SPARKWITCH_MOD_ID, "apprentice_witch"),
            Identifier.of(SPARKWITCH_MOD_ID, "murderous_witch"),
            Identifier.of(SPARKWITCH_MOD_ID, "fiend")
    );

    private TraitRoleEligibility() {
    }

    public static boolean canReceiveTraits(Role role) {
        return role == null || !TRAIT_BLOCKED_ROLE_IDS.contains(role.identifier());
    }
}
