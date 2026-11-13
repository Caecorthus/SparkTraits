package dev.caecorthus.sparktraits.impl.effective;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.death.EffectiveDeathConsequenceRules;
import dev.caecorthus.sparktraits.impl.effective.economy.EffectiveEconomyRules;
import dev.caecorthus.sparktraits.impl.effective.gun.EffectiveGunRules;
import dev.caecorthus.sparktraits.impl.traits.killer.conscience.ConscienceSerialKillerService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;
import dev.doctor4t.wathe.api.event.TaskComplete;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collection;

/** Money and punishment consequences of tasks, shots and kills for flipped players: task money, kill rewards,
 *  Conscience and Veteran kill punishment, innocent-shot punishment, Jester Moment and Party Animal rewards.
 *  阵营翻转玩家在任务、开枪与击杀上的金钱与惩罚后果：任务金币、击杀奖励、善良与老兵击杀惩罚、好人枪击惩罚、小丑时刻与派对狂奖励。 */
public final class EffectiveConsequenceService {
    public static final int TASK_MONEY_REWARD = 50;

    private EffectiveConsequenceService() {
    }

    static void register() {
        TaskComplete.EVENT.register((player, taskType) -> {
            GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
            if (shouldRewardTaskMoney(game.getRole(player), TraitPlayerComponent.KEY.get(player).getActiveTraitIds())) {
                PlayerShopComponent.KEY.get(player).addToBalance(TASK_MONEY_REWARD);
            }
        });
        ShouldPunishGunShooter.EVENT.register((shooter, victim) -> {
            GameWorldComponent game = GameWorldComponent.KEY.get(shooter.getWorld());
            if (shouldCancelInnocentShotPunishment(
                    game.getRole(shooter),
                    TraitPlayerComponent.KEY.get(shooter).getActiveTraitIds(),
                    game.getRole(victim),
                    TraitPlayerComponent.KEY.get(victim).getActiveTraitIds()
            )) {
                return ShouldPunishGunShooter.PunishResult.cancel();
            }
            return null;
        });
    }

    /**
     * Conscience/Impostor +50 task money; stacks on top of role-owned task income (see EffectiveEconomyRules).
     * 善良/内鬼的 +50 任务金币；与职业自带的任务收入叠加（见 EffectiveEconomyRules）。
     */
    public static boolean shouldRewardTaskMoney(Role role, Collection<Identifier> traits) {
        return EffectiveEconomyRules.shouldRewardTaskMoney(role, traits);
    }

    public static boolean hasNativeTaskMoneyReward(Role role) {
        return EffectiveEconomyRules.hasNativeTaskMoneyReward(role);
    }

    /** Lets Party Animal reward any non-self target; NoellesRoles keeps self-buzz and repeat-level gates.
     *  允许派对狂对任意非自己目标发放变声奖励；自变声和重复等级限制仍由 NoellesRoles 原逻辑处理。 */
    public static boolean shouldBlockPartyAnimalTargetReward(Role targetRole, Collection<Identifier> targetTraits) {
        return EffectiveEconomyRules.shouldBlockPartyAnimalTargetReward(targetRole, targetTraits);
    }

    public static boolean shouldRewardConscienceKill(Role victimRole, Collection<Identifier> victimTraits) {
        return EffectiveEconomyRules.shouldRewardConscienceKill(victimRole, victimTraits);
    }

    /** Rewards Impostors for killing public non-killers, including neutral roles.
     *  内鬼击杀公开非杀手时获得击杀奖励，包含好人与中立角色。 */
    public static boolean shouldRewardImpostorKill(Role victimRole, Collection<Identifier> victimTraits) {
        return EffectiveEconomyRules.shouldRewardImpostorKill(victimRole, victimTraits);
    }

    public static int impostorKillReward(Role victimRole, Collection<Identifier> victimTraits, boolean canAccessShop) {
        return EffectiveEconomyRules.impostorKillReward(victimRole, victimTraits, canAccessShop);
    }

    /**
     * Keeps NoellesRoles Jester Moment tied to unflipped original innocents.
     * 让 NoellesRoles 的小丑时刻只由未被内鬼翻阵营的原始好人触发。
     */
    public static boolean shouldTriggerJesterMoment(Role killerRole, Collection<Identifier> killerTraits) {
        return EffectiveDeathConsequenceRules.shouldTriggerJesterMoment(killerRole, killerTraits);
    }

    /** Treat gun victims by their effective alignment for innocent-shot penalties.
     *  枪击惩罚按目标的有效阵营判定，确保善良杀手被当作好人。 */
    public static boolean shouldTreatGunVictimAsInnocent(Role victimRole, Collection<Identifier> victimTraits) {
        return EffectiveGunRules.shouldTreatGunVictimAsInnocent(victimRole, victimTraits);
    }

    /** Cancels Wathe's innocent-shot punishment only for Impostor shots.
     *  只在内鬼开枪时取消 Wathe 的好人枪击惩罚，普通杀手保留原版消耗枪械逻辑。 */
    public static boolean shouldCancelInnocentShotPunishment(
            Role shooterRole,
            Collection<Identifier> shooterTraits,
            Role victimRole,
            Collection<Identifier> victimTraits
    ) {
        return EffectiveGunRules.shouldCancelInnocentShotPunishment(shooterRole, shooterTraits, victimRole, victimTraits);
    }

    public static boolean shouldPunishConscienceKill(boolean victimIsEffectiveCivilian, Identifier deathReason) {
        return EffectiveDeathConsequenceRules.shouldPunishConscienceKill(victimIsEffectiveCivilian, deathReason);
    }

    public static boolean shouldPunishConscienceKill(boolean victimIsEffectiveCivilian, Identifier deathReason, Identifier poisonSource) {
        return EffectiveDeathConsequenceRules.shouldPunishConscienceKill(victimIsEffectiveCivilian, deathReason, poisonSource);
    }

    public static void rememberPoisonSource(PlayerEntity player, Identifier poisonSource) {
        if (player != null && poisonSource != null) {
            EffectiveDeathConsequenceRules.rememberPoisonSource(player.getUuid(), poisonSource);
        }
    }

    public static void handleAfterKill(ServerPlayerEntity victim, ServerPlayerEntity killer, Identifier deathReason) {
        if (victim == null || killer == null || victim.getUuid().equals(killer.getUuid())) {
            return;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        Role victimRole = game.getRole(victim);
        Collection<Identifier> victimTraits = TraitPlayerComponent.KEY.get(victim).getActiveTraitIds();
        boolean victimIsEffectiveCivilian = EffectiveTraitService.isEffectiveCivilian(victimRole, victimTraits);
        Identifier poisonSource = EffectiveDeathConsequenceRules.consumePoisonSource(victim.getUuid());
        Collection<Identifier> killerTraits = TraitPlayerComponent.KEY.get(killer).getActiveTraitIds();
        boolean conscience = EffectiveTraitService.hasConscience(killerTraits);
        boolean punish = (conscience && EffectiveDeathConsequenceRules.shouldPunishConscienceKill(
                victimIsEffectiveCivilian, deathReason, poisonSource
        )) || EffectiveDeathConsequenceRules.shouldPunishVeteranKnifeKill(
                game.getRole(killer), killerTraits, victimRole, victimTraits, deathReason
        );
        if (punish && GameFunctions.isPlayerPlayingAndAlive(killer)) {
            GameFunctions.killPlayer(killer, true, null, GameConstants.DeathReasons.SHOT_INNOCENT, true);
        } else if (conscience) {
            if (shouldRewardConscienceKill(victimRole, victimTraits)) {
                int reward = ConscienceSerialKillerService.rewardForConscienceKill(killer, victim, true);
                if (reward > 0) {
                    PlayerShopComponent.KEY.get(killer).addToBalance(reward);
                }
            }
        } else if (EffectiveTraitService.hasImpostor(killer)) {
            int reward = impostorKillReward(victimRole, victimTraits, ShopUtils.canAccessShop(killer));
            if (reward > 0) {
                PlayerShopComponent.KEY.get(killer).addToBalance(reward);
            }
        }
    }
}
