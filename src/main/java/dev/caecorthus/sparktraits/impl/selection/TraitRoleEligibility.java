package dev.caecorthus.sparktraits.impl.selection;

import dev.caecorthus.sparktraits.api.Trait;
import dev.caecorthus.sparktraits.api.TraitAudience;
import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Candidate-level role restrictions, decoupled from optional addon classes.
 * 候选天赋的身份限制：只按完整身份 ID 识别可选附属模组，避免新增编译依赖。
 */
public final class TraitRoleEligibility {
    private static final String SPARKWITCH_MOD_ID = "sparkwitch";
    // Owner decision 2026-10-04: these roles may hold only traits that declare UNIVERSAL audience.
    // 所有者 2026-10-04 决定：这些身份只能获得声明为通用受众（UNIVERSAL）的天赋。
    private static final Set<Identifier> UNIVERSAL_ONLY_ROLE_IDS = Set.of(
            Identifier.of(SPARKWITCH_MOD_ID, "grand_witch"),
            Identifier.of(SPARKWITCH_MOD_ID, "accomplice"),
            // SparkWitch special accomplices share every Accomplice rule. / SparkWitch 特殊共犯沿用共犯的全部规则。
            Identifier.of(SPARKWITCH_MOD_ID, "abyss_listener"),
            Identifier.of(SPARKWITCH_MOD_ID, "potion_gunner"),
            Identifier.of(SPARKWITCH_MOD_ID, "riftwalker"),
            // SparkWitch's Bewitched (魔化使) is an accomplice before promotion. / SparkWitch 魔化使是晋升前的共犯。
            Identifier.of(SPARKWITCH_MOD_ID, "bewitched"),
            Identifier.of(SPARKWITCH_MOD_ID, "apprentice_witch"),
            Identifier.of(SPARKWITCH_MOD_ID, "murderous_witch"),
            // The Fiend follows the same rule by owner decision. / 魔人按所有者决定沿用同一规则。
            Identifier.of(SPARKWITCH_MOD_ID, "fiend")
    );

    private TraitRoleEligibility() {
    }

    /**
     * Additional role restriction only; callers must still apply their existing predicate/audience checks.
     * Reads the declared {@link Trait#audience()}, not {@code canApply}, so overridden predicates (Impostor,
     * Conscience) cannot widen it.
     * 仅附加身份限制；调用方仍须执行原有的天赋谓词或阵营校验。这里读取声明的受众而非 {@code canApply}，
     * 因此重写了谓词的天赋（内鬼、善良）无法绕过该限制。
     */
    public static boolean canReceiveTrait(Role role, Trait candidate) {
        return role == null || !UNIVERSAL_ONLY_ROLE_IDS.contains(role.identifier())
                || candidate.audience() == TraitAudience.UNIVERSAL;
    }
}
