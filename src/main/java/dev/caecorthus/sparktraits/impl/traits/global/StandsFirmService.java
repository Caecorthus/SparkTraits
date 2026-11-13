package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.caecorthus.sparktraits.impl.lifecycle.TerminalDeathRules;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;

/**
 * Blocks same-faction kills for 立场坚定 holders.
 * <p>Wathe resolves gun shots, knives and most scripted deaths through {@code GameFunctions.killPlayer(...)} rather
 * than {@code LivingEntity#damage}, so the {@link KillPlayer#BEFORE} hook is the only reliable interception point.
 * <p>Voodoo and falling out of the train are both invoked with a {@code null} killer, so the "killer must be a real
 * player" rule exempts them without hard-coding any death reason.
 * 立场坚定的持有者不会被同阵营杀死。Wathe 的枪击、刀刃与多数脚本致死都走 {@code killPlayer} 而非 {@code damage}，
 * 因此只有 {@link KillPlayer#BEFORE} 能可靠拦截。巫毒与坠落列车的 killer 均为 null，"必须存在真实凶手"这一条
 * 天然豁免了它们，无需硬编码死亡原因。
 */
public final class StandsFirmService {

    private StandsFirmService() {
    }

    public static void register() {
        // Returning null defers to the next listener. KillResult.allow() would end the BEFORE chain here and skip
        // every later protection (Last Stand, Depression, Jester, other mods' shields).
        // 返回 null 才是“交给下一个监听器”；返回 KillResult.allow() 会直接终止 BEFORE 链，
        // 让后面的保护（背水一战、抑郁、小丑、其他模组的护盾）全部失效。
        KillPlayer.BEFORE.register((victim, killer, deathReason) -> {
            // Terminal penalties are not combat attempts, so no kill protection applies to them.
            // 终结惩罚不是战斗攻击，任何击杀保护都不对其生效。
            if (killer == null || killer == victim || TerminalDeathRules.contains(deathReason)) {
                return null;
            }
            if (!TraitPlayerComponent.KEY.get(victim).hasActiveTrait(StandsFirmTrait.ID)) {
                return null;
            }
            GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
            // Faction after SparkTraits alignment flips: an Impostor plays for the killers and a Conscience killer for
            // the civilians, so the role's own faction would protect them from the wrong side.
            // SparkTraits 阵营翻转后的阵营：内鬼属于杀手方、善良杀手属于好人方，直接用身份原始阵营会保护错对象。
            Faction victimFaction = EffectiveTraitService.effectiveFaction(game, victim);
            if (victimFaction == Faction.NONE || victimFaction != EffectiveTraitService.effectiveFaction(game, killer)) {
                return null;
            }
            return KillPlayer.KillResult.cancel();
        });
    }
}
