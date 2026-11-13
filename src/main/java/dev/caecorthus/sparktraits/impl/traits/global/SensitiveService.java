package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparkfactionapi.api.SparkFactionApi;
import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;

/**
 * Extends the round timer when a 敏感 holder kills an innocent or a neutral.
 * Only killers benefit: the description ties the bonus to the killer-side roles, and a civilian killer would
 * otherwise farm time off their own faction.
 * 敏感持有者击杀好人 / 中立时延长时间。仅杀手阵营生效：描述只给出杀手侧角色，且好人击杀自己阵营会变成刷时间。
 */
public final class SensitiveService {

    private SensitiveService() {
    }

    public static void register() {
        KillPlayer.AFTER.register((victim, killer, deathReason) -> {
            if (killer == null || killer == victim) {
                return;
            }
            if (!TraitPlayerComponent.KEY.get(killer).hasActiveTrait(SensitiveTrait.ID)) {
                return;
            }
            GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
            // Faction after SparkTraits alignment flips: an Impostor plays for the killers and a Conscience killer for
            // the civilians. Using the role's own faction would deny an Impostor the bonus and pay a killer for killing
            // an Impostor teammate.
            // SparkTraits 阵营翻转后的阵营：内鬼属于杀手方、善良杀手属于好人方。直接用身份原始阵营会让内鬼拿不到加时，
            // 而杀手击杀内鬼队友反而能刷时间。
            if (EffectiveTraitService.effectiveFaction(game, killer) != Faction.KILLER) {
                return;
            }
            int seconds = bonusSeconds(
                    EffectiveTraitService.effectiveFaction(game, victim),
                    SparkFactionApi.isNeutralAligned(game.getRole(victim))
            );
            if (seconds <= 0) {
                return;
            }
            GameTimeComponent.KEY.get(victim.getWorld()).addTime(seconds * 20);
        });
    }

    /**
     * Seconds for one kill, from the victim's faction after SparkTraits alignment flips. "Neutral" is asked of
     * SparkFactionAPI (0.1.6.0+): roles of native factions registered as neutral-aligned (SparkWitch's Witch and
     * Jiahao factions, NoellesRoles' Corrupt Cop) no longer read {@code Faction.NEUTRAL} from Wathe but keep neutral
     * perks, so comparing the constant would pay nothing for them. An effective killer or civilian is never neutral
     * here, whatever its role's faction.
     * 单次击杀的加时秒数，取决于受害者经 SparkTraits 阵营翻转后的阵营。“中立”改为询问 SparkFactionAPI（0.1.6.0+）：
     * 注册为中立对齐的原生阵营（SparkWitch 的魔女与 Jiahao 阵营、NoellesRoles 的黑警）在 Wathe 中已不再是
     * {@code Faction.NEUTRAL}，但保留中立特权；直接比较常量会让击杀它们不加时。有效杀手或有效好人无论身份阵营如何，
     * 在这里都不算中立。
     */
    static int bonusSeconds(Faction effectiveVictimFaction, boolean victimRoleNeutralAligned) {
        if (effectiveVictimFaction == Faction.CIVILIAN) {
            return SensitiveTrait.INNOCENT_KILL_SECONDS;
        }
        if (effectiveVictimFaction == Faction.KILLER || effectiveVictimFaction == Faction.NONE) {
            return 0;
        }
        return victimRoleNeutralAligned ? SensitiveTrait.NEUTRAL_KILL_SECONDS : 0;
    }
}
