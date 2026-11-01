package dev.caecorthus.sparktraits.impl.effective.gun;

import dev.caecorthus.sparktraits.impl.effective.alignment.EffectiveAlignment;
import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;

import java.util.Collection;

/** Gun punishment rules created by effective-team traits.
 *  有效阵营天赋产生的枪击惩罚规则。 */
public final class EffectiveGunRules {
    private EffectiveGunRules() {
    }

    /** Treats gun victims by effective alignment for innocent-shot penalties.
     *  枪击惩罚按目标的有效阵营判定，确保善良杀手被当作好人。 */
    public static boolean shouldTreatGunVictimAsInnocent(Role victimRole, Collection<Identifier> victimTraits) {
        return EffectiveAlignment.isEffectiveCivilian(victimRole, victimTraits);
    }

    /** Cancels Wathe's innocent-shot punishment for Impostor shots and for the neutral hidden Jester.
     *  内鬼射击有效好人、以及中立的隐藏小丑射击好人时，取消 Wathe 的误杀惩罚。 */
    public static boolean shouldCancelInnocentShotPunishment(
            Role shooterRole,
            Collection<Identifier> shooterTraits,
            Role victimRole,
            Collection<Identifier> victimTraits
    ) {
        if (!shouldTreatGunVictimAsInnocent(victimRole, victimTraits)) {
            return false;
        }
        return EffectiveAlignment.hasJester(shooterTraits)
                || (EffectiveAlignment.isEffectiveKiller(shooterRole, shooterTraits)
                && EffectiveAlignment.hasImpostor(shooterTraits));
    }
}
