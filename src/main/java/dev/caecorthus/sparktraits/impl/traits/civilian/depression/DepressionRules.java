package dev.caecorthus.sparktraits.impl.traits.civilian.depression;

import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraitService;
import dev.caecorthus.sparktraits.impl.traits.civilian.CivilianTraits;
import dev.caecorthus.sparktraits.impl.traits.civilian.police.PoliceRoleCategory;
import dev.caecorthus.sparktraits.impl.traits.global.GlobalTraitService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.Collection;
import java.util.UUID;

/**
 * Pure rules and tuning numbers of the Depression trait: selection, mood and stamina maths, chances, psycho armour
 * and fake-death rewards. Holds no game state; the runtime that applies them is {@link DepressionTraitService}.
 * 抑郁天赋的纯规则与数值：选取、理智与体力计算、概率、疯魔护盾与假死奖励。不持有任何游戏状态；
 * 应用这些规则的运行时逻辑在 {@link DepressionTraitService}。
 */
public final class DepressionRules {
    public static final int MIN_RANDOM_PLAYERS = 24;
    public static final float MOOD_DRAIN_MULTIPLIER = 1.5f;
    public static final float STAMINA_MULTIPLIER = 0.8f;
    public static final float GUARANTEED_TRIGGER_MOOD = -0.20f;
    public static final float FULL_GRAYSCALE_MOOD = 0.0f;
    public static final float MAX_SCREEN_EFFECT_STRENGTH = 0.75f;
    public static final float POST_PSYCHO_MOOD = 0.7f;
    public static final double MAX_SUICIDE_TRIGGER_CHANCE = 30.0;
    public static final int RANDOM_CAP_STEP_PLAYERS = 8;
    public static final int JESTER_MOMENT_PSYCHO_ARMOUR = 2;
    public static final int FAKE_DEATH_REWARD_DIVISOR = 2;
    public static final Identifier APPRENTICE_WITCH_ID = Identifier.of("sparkwitch", "apprentice_witch");
    /**
     * SparkWitch's Blind: its bat-only psycho phase, fake death and voice mute fight the black-screen kit.
     * SparkWitch 盲人：抑郁的只能持棒阶段、假死与语音静音都与黑屏道具冲突。
     */
    public static final Identifier SPARKWITCH_BLIND_ID = Identifier.of("sparkwitch", "blind");

    private DepressionRules() {
    }

    public static boolean canSelectDepression(
            Role role,
            Collection<Identifier> selectedTraits,
            int startingPlayerCount,
            boolean enforceStartingPlayerCount
    ) {
        if (enforceStartingPlayerCount && startingPlayerCount < MIN_RANDOM_PLAYERS) {
            return false;
        }
        // Police = PoliceRoleCategory, so registered police variants inherit Vigilante/Veteran's exclusion.
        // 警职按 PoliceRoleCategory 判定，下游注册的警职沿用义警与老兵的排除。
        return CivilianTraitService.canSelectNonUndercoverCivilianTrait(role, selectedTraits)
                && !PoliceRoleCategory.isPolice(role)
                && !roleIdentifierEquals(role, Noellesroles.SURVIVAL_MASTER_ID)
                && !roleIdentifierEquals(role, APPRENTICE_WITCH_ID)
                && !roleIdentifierEquals(role, SPARKWITCH_BLIND_ID);
    }

    public static int randomDepressionCap(int startingPlayerCount) {
        if (startingPlayerCount < MIN_RANDOM_PLAYERS) {
            return 0;
        }
        return 1 + (startingPlayerCount - MIN_RANDOM_PLAYERS) / RANDOM_CAP_STEP_PLAYERS;
    }

    public static float depressionAdjustedMood(float currentMood, float proposedMood, Collection<Identifier> traits) {
        return depressionAdjustedMood(currentMood, proposedMood, traits, false);
    }

    public static float depressionAdjustedMood(
            float currentMood,
            float proposedMood,
            Collection<Identifier> traits,
            boolean depressionPsychoActive
    ) {
        if (traits == null
                || !traits.contains(CivilianTraits.DEPRESSION)
                || proposedMood >= currentMood) {
            return proposedMood;
        }
        if (depressionPsychoActive) {
            return currentMood;
        }
        return currentMood - (currentMood - proposedMood) * MOOD_DRAIN_MULTIPLIER;
    }

    public static double triggerChance(float mood) {
        return linearChance(mood, GameConstants.MID_MOOD_THRESHOLD, GUARANTEED_TRIGGER_MOOD, 0.0, MAX_SUICIDE_TRIGGER_CHANCE);
    }

    public static double psychoCounterChance(float mood) {
        return 100.0;
    }

    public static boolean shouldRunSuicideCountdown(float mood) {
        return triggerChance(mood) > 0.0;
    }

    public static boolean shouldPauseSuicideCountdown(
            boolean hasDepression,
            boolean playingAndAlive,
            boolean pending,
            boolean depressionPsychoActive,
            boolean wathePsychoActive
    ) {
        // Pause only Depression's own countdown during fake death or psycho windows.
        // 只暂停抑郁自己的倒计时；假死或疯魔窗口内不推进自毁判定。
        return !hasDepression || !playingAndAlive || pending || depressionPsychoActive || wathePsychoActive;
    }

    public static boolean shouldSuppressMentalBreakdown(
            boolean hasDepression,
            boolean depressionPsychoActive,
            boolean wathePsychoActive,
            Identifier deathReason
    ) {
        return hasDepression
                && (depressionPsychoActive || wathePsychoActive)
                && GameConstants.DeathReasons.MENTAL_BREAKDOWN.equals(deathReason);
    }

    public static float depressionPsychoMoodFloor(float mood) {
        return Math.max(mood, FULL_GRAYSCALE_MOOD);
    }

    public static float depressionPsychoRestoredMood() {
        return POST_PSYCHO_MOOD;
    }

    public static float depressionScreenEffectStrength(boolean hasDepression, boolean psychoActive, float mood) {
        if (!hasDepression) {
            return 0.0f;
        }
        if (psychoActive) {
            return MAX_SCREEN_EFFECT_STRENGTH;
        }
        if (mood >= GameConstants.MID_MOOD_THRESHOLD) {
            return 0.0f;
        }
        if (mood <= FULL_GRAYSCALE_MOOD) {
            return MAX_SCREEN_EFFECT_STRENGTH;
        }
        return MAX_SCREEN_EFFECT_STRENGTH
                * (GameConstants.MID_MOOD_THRESHOLD - mood)
                / (GameConstants.MID_MOOD_THRESHOLD - FULL_GRAYSCALE_MOOD);
    }

    public static float depressionRecoveredStamina(float previousTicks, float proposedTicks, boolean hasDepression) {
        if (!hasDepression || proposedTicks <= previousTicks) {
            return proposedTicks;
        }
        return previousTicks + (proposedTicks - previousTicks) * STAMINA_MULTIPLIER;
    }

    public static boolean shouldApplyDepressionStamina(Role role, boolean hasDepression, boolean psychoActive) {
        return hasDepression && !psychoActive && GlobalTraitService.hasFiniteStamina(role);
    }

    public static boolean shouldMuteVoice(boolean psychoActive) {
        return psychoActive;
    }

    public static boolean shouldAllowLowMoodSprint(boolean syncedPsychoActive, boolean runtimePsychoActive) {
        return syncedPsychoActive || runtimePsychoActive;
    }

    /**
     * Depression psycho is maintained only while both chase participants are still active players.
     * 只有疯魔者和目标都仍在存活游玩时，抑郁疯魔才继续维持。
     */
    public static boolean shouldEndActivePsycho(boolean playerAlive, boolean attackerAlive) {
        return !playerAlive || !attackerAlive;
    }

    public static boolean shouldEndActivePsychoForDeadTarget(UUID deadPlayerUuid, UUID activeTargetUuid) {
        return activeTargetUuid.equals(deadPlayerUuid);
    }

    /**
     * Restores post-psycho stamina without forcing finite-role players into exhaustion.
     * 疯魔结束后恢复体力，避免有限体力角色被重置到 0 体力并立即疲惫。
     */
    public static PostPsychoStaminaState postPsychoStaminaState(int roleMaxSprintTime, int effectiveMaxSprintTime) {
        if (roleMaxSprintTime < 0) {
            return new PostPsychoStaminaState(-1, Integer.MAX_VALUE, false);
        }
        int maxSprintTime = Math.max(1, effectiveMaxSprintTime);
        return new PostPsychoStaminaState(maxSprintTime, maxSprintTime, false);
    }

    public static boolean shouldBlockInventoryInsert(boolean psychoActive, boolean batStack) {
        return psychoActive && !batStack;
    }

    public static int initialPsychoArmour(Role killerRole, boolean activeJesterMoment) {
        return activeJesterMoment && roleIdentifierEquals(killerRole, Noellesroles.JESTER_ID)
                ? JESTER_MOMENT_PSYCHO_ARMOUR
                : 0;
    }

    public static int maintainedPsychoArmour(int currentArmour, int maxArmour) {
        if (maxArmour <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(currentArmour, maxArmour));
    }

    /**
     * Direct money a real kill would pay the attacker: Wathe killer, SparkFactionAPI custom faction, or Impostor.
     * 真实击杀会直接付给攻击者的钱：Wathe 杀手、SparkFactionAPI 自定义阵营或内鬼。
     */
    public static int normalDirectKillReward(boolean originalKiller, boolean customFactionKiller, int impostorReward) {
        // SparkFactionAPI pays the same literal 100 as Wathe's MONEY_PER_KILL.
        // SparkFactionAPI 的自定义阵营击杀奖励与 Wathe 的 MONEY_PER_KILL 同为 100。
        return (originalKiller ? GameConstants.MONEY_PER_KILL : 0)
                + (customFactionKiller ? GameConstants.MONEY_PER_KILL : 0)
                + Math.max(0, impostorReward);
    }

    /**
     * A non-final Depression kill pays half of the normal reward, rounded up (100 -> 50, teammate 15 -> 8).
     * 非最终击杀抑郁只给正常击杀一半的钱，向上取整（100 -> 50，队友 15 -> 8）。
     */
    public static int fakeDeathReward(int normalReward) {
        if (normalReward <= 0) {
            return 0;
        }
        return (normalReward + FAKE_DEATH_REWARD_DIVISOR - 1) / FAKE_DEATH_REWARD_DIVISOR;
    }

    public static boolean shouldPunishDepressionPsychoBatKill(
            boolean depressionPsychoActive,
            Identifier deathReason,
            Role victimRole,
            Collection<Identifier> victimTraits
    ) {
        return depressionPsychoActive
                && GameConstants.DeathReasons.BAT.equals(deathReason)
                && EffectiveTraitService.isEffectiveCivilian(victimRole, victimTraits);
    }

    private static boolean roleIdentifierEquals(Role role, Identifier id) {
        return role != null && role.identifier().equals(id);
    }

    private static double linearChance(float mood, float startMood, float fullMood, double startChance, double fullChance) {
        if (mood >= startMood) {
            return startChance;
        }
        if (mood <= fullMood) {
            return fullChance;
        }
        double progress = (startMood - mood) / (double) (startMood - fullMood);
        return startChance + (fullChance - startChance) * progress;
    }

    public record PostPsychoStaminaState(int maxSprintTime, float sprintingTicks, boolean exhausted) {
    }
}
