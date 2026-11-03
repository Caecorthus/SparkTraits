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

    /** Cancels Wathe's innocent-shot punishment only for Impostor shots at effective innocents.
     *  只在内鬼射击有效好人时取消 Wathe 的好人枪击惩罚。 */
    public static boolean shouldCancelInnocentShotPunishment(
            Role shooterRole,
            Collection<Identifier> shooterTraits,
            Role victimRole,
            Collection<Identifier> victimTraits
    ) {
        return EffectiveAlignment.isEffectiveKiller(shooterRole, shooterTraits)
                && EffectiveAlignment.hasImpostor(shooterTraits)
                && shouldTreatGunVictimAsInnocent(victimRole, victimTraits);
    }
}
