package dev.caecorthus.sparktraits.impl.effective.alignment;

import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.impostor.ImpostorTrait;
import dev.caecorthus.sparktraits.impl.traits.civilian.jester.JesterTrait;
import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

import java.util.Collection;

/** Pure effective-alignment rules for Conscience and Impostor.
 *  善良与内鬼的纯有效阵营规则。 */
public final class EffectiveAlignment {
    private EffectiveAlignment() {
    }

    public static boolean hasConscience(Collection<Identifier> traits) {
        return traits.contains(ConscienceTrait.ID);
    }

    public static boolean hasImpostor(Collection<Identifier> traits) {
        return traits.contains(ImpostorTrait.ID);
    }

    public static boolean hasJester(Collection<Identifier> traits) {
        return traits != null && traits.contains(JesterTrait.ID);
    }

    public static boolean isOriginalKiller(Role role) {
        return role != null && role.canUseKiller();
    }

    public static boolean isOriginalCivilian(Role role) {
        return role != null && role.isInnocent();
    }

    /** Resolves killer-team membership after SparkTraits alignment flips.
     *  在 SparkTraits 阵营翻转后判断是否属于杀手阵营。 */
    public static boolean isEffectiveKiller(Role role, Collection<Identifier> traits) {
        if (hasJester(traits)) {
            // The hidden Jester is a neutral, not a killer. / 隐藏小丑是中立，不是杀手。
            return false;
        }
        if (hasImpostor(traits)) {
            return true;
        }
        if (hasConscience(traits)) {
            return false;
        }
        return isOriginalKiller(role);
    }

    /** Resolves civilian-team membership after SparkTraits alignment flips.
     *  在 SparkTraits 阵营翻转后判断是否属于好人阵营。 */
    public static boolean isEffectiveCivilian(Role role, Collection<Identifier> traits) {
        if (hasJester(traits)) {
            // Out of the passenger team while untriggered; settlement is handled separately.
            // 未触发期间不再属于好人数；结算另行处理。
            return false;
        }
        if (hasConscience(traits)) {
            return true;
        }
        if (hasImpostor(traits)) {
            return false;
        }
        return isOriginalCivilian(role);
    }

    /**
     * Round-end settlement membership: an untriggered hidden Jester still settles with the passenger side even though
     * it is neutral for every in-round rule. Once triggered the role itself is Jester, so this is false.
     * 回合结算归属：未触发的隐藏小丑在局内规则中是中立，但仍与好人一起结算；触发后身份已是小丑，返回 false。
     */
    public static boolean isSettlementCivilian(Role role, Collection<Identifier> traits) {
        return isEffectiveCivilian(role, traits)
                || (isOriginalCivilian(role) && hasJester(traits));
    }
}
