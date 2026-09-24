package dev.caecorthus.sparktraits.impl.effective.death;

import dev.caecorthus.sparktraits.impl.effective.alignment.EffectiveAlignment;
import dev.caecorthus.sparktraits.impl.effective.economy.EffectiveEconomyRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Death consequence rules created by effective-team traits.
 *  有效阵营天赋产生的击杀后果规则。 */
public final class EffectiveDeathConsequenceRules {
    private static final Map<UUID, Identifier> poisonSources = new HashMap<>();

    private EffectiveDeathConsequenceRules() {
    }

    /** Keeps NoellesRoles Jester Moment tied to unflipped original innocents.
     *  让 NoellesRoles 的小丑时刻只由未被内鬼翻阵营的原始好人触发。 */
    public static boolean shouldTriggerJesterMoment(Role killerRole, Collection<Identifier> killerTraits) {
        return EffectiveAlignment.isOriginalCivilian(killerRole) && !EffectiveAlignment.hasImpostor(killerTraits);
    }

    public static boolean shouldPunishConscienceKill(boolean victimIsEffectiveCivilian, Identifier deathReason) {
        return shouldPunishConscienceKill(victimIsEffectiveCivilian, deathReason, null);
    }

    /** Ordinary grenades punish Conscience; only launch-marked Bomb Maniac grenades are exempt.
     *  普通手雷误杀会惩罚善良，仅发射时标记的炸弹狂手雷豁免。 */
    public static boolean shouldPunishConscienceKill(
            boolean victimIsEffectiveCivilian,
            Identifier deathReason,
            Identifier poisonSource
    ) {
        return shouldPunishConscienceKill(victimIsEffectiveCivilian, deathReason, poisonSource, false);
    }

    public static boolean shouldPunishConscienceKill(
            boolean victimIsEffectiveCivilian,
            Identifier deathReason,
            Identifier poisonSource,
            boolean bombManiacGrenade
    ) {
        return victimIsEffectiveCivilian
                && !(bombManiacGrenade && GameConstants.DeathReasons.GRENADE.equals(deathReason))
                && !(GameConstants.DeathReasons.POISON.equals(deathReason)
                && Noellesroles.POISON_SOURCE_GAS_BOMB.equals(poisonSource));
    }

    public static boolean shouldPunishVeteranKnifeKill(
            Role killerRole,
            Collection<Identifier> killerTraits,
            Role victimRole,
            Collection<Identifier> victimTraits,
            Identifier deathReason
    ) {
        return killerRole == WatheRoles.VETERAN
                && !EffectiveAlignment.hasImpostor(killerTraits)
                && GameConstants.DeathReasons.KNIFE.equals(deathReason)
                && EffectiveAlignment.isOriginalKiller(victimRole)
                && EffectiveAlignment.hasConscience(victimTraits);
    }

    /** Records poison source attribution for one later victim-death decision.
     *  记录一次毒源归因，供后续受害者死亡判定消费。 */
    public static void rememberPoisonSource(UUID victimUuid, Identifier poisonSource) {
        if (victimUuid != null && poisonSource != null) {
            poisonSources.put(victimUuid, poisonSource);
        }
    }

    /** Consumes poison source attribution exactly once.
     *  毒源归因只消费一次。 */
    public static Identifier consumePoisonSource(UUID victimUuid) {
        return victimUuid == null ? null : poisonSources.remove(victimUuid);
    }

    /** Clears test/runtime leftovers when a caller owns the lifecycle boundary.
     *  在调用方拥有生命周期边界时清理测试或运行时残留。 */
    public static void clearPoisonSources() {
        poisonSources.clear();
    }

    public static boolean shouldRewardConscienceKill(Role victimRole, Collection<Identifier> victimTraits) {
        return EffectiveEconomyRules.shouldRewardConscienceKill(victimRole, victimTraits);
    }

    public static int impostorKillReward(Role victimRole, Collection<Identifier> victimTraits, boolean canAccessShop) {
        return EffectiveEconomyRules.impostorKillReward(victimRole, victimTraits, canAccessShop);
    }

}
