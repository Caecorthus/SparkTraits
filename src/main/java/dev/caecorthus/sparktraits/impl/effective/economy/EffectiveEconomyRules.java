package dev.caecorthus.sparktraits.impl.effective.economy;

import dev.caecorthus.sparktraits.impl.effective.alignment.EffectiveAlignment;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.Collection;

/** Effective economy and reward policy created by alignment-flipping traits.
 *  阵营翻转天赋产生的有效经济与奖励规则。 */
public final class EffectiveEconomyRules {
    public static final int TASK_MONEY_REWARD = 50;

    private EffectiveEconomyRules() {
    }

    /**
     * Grants SparkTraits task money only when the base role does not already pay for tasks.
     * 仅在原职业没有自带任务金币时，由 SparkTraits 给阵营翻转玩家补发任务金币。
     */
    public static boolean shouldRewardTaskMoney(Role role, Collection<Identifier> traits) {
        return traits != null
                && (EffectiveAlignment.hasConscience(traits) || EffectiveAlignment.hasImpostor(traits))
                && !hasNativeTaskMoneyReward(role);
    }

    public static boolean hasNativeTaskMoneyReward(Role role) {
        return role != null
                && (role.equals(Noellesroles.BARTENDER)
                || role.equals(Noellesroles.RECALLER)
                || role.equals(Noellesroles.TIMEKEEPER)
                || role.equals(Noellesroles.REPORTER)
                || role.equals(Noellesroles.WAITER));
    }

    /** Lets Party Animal reward any non-self target; NoellesRoles keeps self-buzz and repeat-level gates.
     *  允许派对狂对任意非自己目标发放变声奖励；自变声和重复等级限制仍由 NoellesRoles 原逻辑处理。 */
    public static boolean shouldBlockPartyAnimalTargetReward(Role targetRole, Collection<Identifier> targetTraits) {
        return false;
    }

    public static boolean shouldRewardConscienceKill(Role victimRole, Collection<Identifier> victimTraits) {
        return victimRole != null && !EffectiveAlignment.isEffectiveCivilian(victimRole, victimTraits);
    }

    /** Rewards Impostors for killing public non-killers, including neutral roles.
     *  内鬼击杀公开非杀手时获得击杀奖励，包含好人与中立角色。 */
    public static boolean shouldRewardImpostorKill(Role victimRole, Collection<Identifier> victimTraits) {
        return victimRole != null
                && victimRole != WatheRoles.NO_ROLE
                && !EffectiveAlignment.isEffectiveKiller(victimRole, victimTraits);
    }

    public static int impostorKillReward(Role victimRole, Collection<Identifier> victimTraits, boolean canAccessShop) {
        return canAccessShop && shouldRewardImpostorKill(victimRole, victimTraits) ? GameConstants.MONEY_PER_KILL : 0;
    }

    /** Keeps Wathe's original killer kill reward away from Conscience killers.
     *  防止善良杀手继续获得 Wathe 原始杀手击杀奖励。 */
    public static boolean shouldReceiveOriginalKillerReward(boolean canUseKillerFeatures, boolean hasConscience) {
        return canUseKillerFeatures && !hasConscience;
    }

}
