package dev.caecorthus.sparktraits.impl.traits.global;

import dev.caecorthus.sparktraits.component.TraitPlayerComponent;
import dev.caecorthus.sparktraits.impl.effective.EffectiveTraitService;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;

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
            if (effectiveFaction(game, killer) != Faction.KILLER) {
                return;
            }
            int seconds = switch (effectiveFaction(game, victim)) {
                case CIVILIAN -> SensitiveTrait.INNOCENT_KILL_SECONDS;
                case NEUTRAL -> SensitiveTrait.NEUTRAL_KILL_SECONDS;
                default -> 0;
            };
            if (seconds <= 0) {
                return;
            }
            GameTimeComponent.KEY.get(victim.getWorld()).addTime(seconds * 20);
        });
    }

    /**
     * Faction after SparkTraits alignment flips: an Impostor plays for the killers and a Conscience killer for the
     * civilians. Using the role's own faction would deny an Impostor the bonus and pay a killer for killing an
     * Impostor teammate.
     * SparkTraits 阵营翻转后的阵营：内鬼属于杀手方、善良杀手属于好人方。直接用身份原始阵营会让内鬼拿不到加时，
     * 而杀手击杀内鬼队友反而能刷时间。
     */
    private static Faction effectiveFaction(GameWorldComponent game, ServerPlayerEntity player) {
        Role role = game.getRole(player);
        if (role == null || role.getFaction() == Faction.NONE) {
            return Faction.NONE;
        }
        if (EffectiveTraitService.isEffectiveKiller(player, game)) {
            return Faction.KILLER;
        }
        if (EffectiveTraitService.isEffectiveCivilian(player, game)) {
            return Faction.CIVILIAN;
        }
        return role.getFaction();
    }
}
