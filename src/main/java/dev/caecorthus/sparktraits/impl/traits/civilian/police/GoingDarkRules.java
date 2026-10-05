package dev.caecorthus.sparktraits.impl.traits.civilian.police;

import dev.caecorthus.sparktraits.impl.effective.alignment.EffectiveAlignment;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Pure eligibility and viewer rules for Going Dark instinct suppression.
 *  隐蔽行动本能屏蔽的纯目标资格与观察者规则。 */
public final class GoingDarkRules {
    private static final Set<Identifier> PROTECTED_VIEWER_ROLE_IDS = Set.of(
            Identifier.of("sparkwitch", "grand_witch"),
            Identifier.of("sparkwitch", "murderous_witch"),
            Identifier.of("sparkwitch", "accomplice"),
            // SparkWitch special accomplices share every Accomplice rule. / SparkWitch 特殊共犯沿用共犯的全部规则。
            Identifier.of("sparkwitch", "abyss_listener"),
            Identifier.of("sparkwitch", "potion_gunner"),
            Identifier.of("sparkwitch", "riftwalker"),
            // SparkWitch's Bewitched (魔化使) is an accomplice before promotion. / SparkWitch 魔化使是晋升前的共犯。
            Identifier.of("sparkwitch", "bewitched"),
            Identifier.of("noellesroles", "corrupt_cop"),
            Identifier.of("sparkwitch", "insider")
    );

    private GoingDarkRules() {
    }

    public static boolean isTargetHidden(
            boolean blackoutActive,
            boolean playerPlayingAndAlive,
            Role role,
            Collection<Identifier> traits
    ) {
        return blackoutActive
                && playerPlayingAndAlive
                && role == WatheRoles.VETERAN
                && safeTraits(traits).contains(PoliceTraits.GOING_DARK);
    }

    public static boolean shouldSuppressInstinct(
            boolean targetHidden,
            boolean viewerPlayingAndAlive,
            boolean viewerCanSeeSpectatorInformation,
            boolean finalMomentActive,
            Role viewerRole,
            Collection<Identifier> viewerTraits
    ) {
        if (!targetHidden
                || !viewerPlayingAndAlive
                || viewerCanSeeSpectatorInformation
                || finalMomentActive) {
            return false;
        }
        Collection<Identifier> traits = safeTraits(viewerTraits);
        return EffectiveAlignment.isEffectiveKiller(viewerRole, traits)
                || (viewerRole != null && PROTECTED_VIEWER_ROLE_IDS.contains(viewerRole.identifier()));
    }

    private static Collection<Identifier> safeTraits(Collection<Identifier> traits) {
        return traits == null ? List.of() : traits;
    }
}
